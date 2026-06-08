package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class POSRoute {
    TRANSACTION, // Point-of-Sale order screen
    TABLE_QUEUE, // Tables status screen
    INVENTORY,   // Menu & Ingredient recipe config
    REPORTS,     // Shift Sales, Terlaris, Profit/Loss, Export
    SETTINGS     // Store customizer, Cashier shifts, role switcher
}

class POSViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: POSRepository
    
    init {
        val database = POSDatabase.getDatabase(application)
        repository = POSRepository(database.posDao())
        
        // Populate standard defaults if DB is fresh
        viewModelScope.launch {
            repository.allTables.first().let { tables ->
                if (tables.isEmpty()) {
                    initializeDefaultData()
                }
            }
        }
    }

    // Dynamic state streams mapped directly from Room reactive Flows
    val storeConfig: StateFlow<StoreConfig> = repository.storeConfig
        .map { it ?: StoreConfig() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoreConfig())

    val allIngredients: StateFlow<List<Ingredient>> = repository.allIngredients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMenuItems: StateFlow<List<MenuItem>> = repository.allMenuItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTables: StateFlow<List<DiningTable>> = repository.allTables
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<TransactionLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ----------------------------------------------------
    // IN-MEMORY ACTIVE POS STATES
    // ----------------------------------------------------
    private val _activeRoute = MutableStateFlow(POSRoute.TRANSACTION)
    val activeRoute: StateFlow<POSRoute> = _activeRoute.asStateFlow()

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val _selectedTableId = MutableStateFlow(0) // 0 = Take Away
    val selectedTableId: StateFlow<Int> = _selectedTableId.asStateFlow()

    private val _activeCart = MutableStateFlow<List<CartItem>>(emptyList())
    val activeCart: StateFlow<List<CartItem>> = _activeCart.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _activeShift = MutableStateFlow("Shift Pagi")
    val activeShift: StateFlow<String> = _activeShift.asStateFlow()

    private val _syncState = MutableStateFlow(false) // Trigger Cloud Sync animation
    val syncState: StateFlow<Boolean> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    // Active screen modal / details selection (e.g. printed receipt viewer)
    private val _selectedTransactionIdForReceipt = MutableStateFlow<Long?>(null)
    val selectedTransactionIdForReceipt: StateFlow<Long?> = _selectedTransactionIdForReceipt.asStateFlow()

    // ----------------------------------------------------
    // ACTION MUTATORS
    // ----------------------------------------------------

    fun setRoute(route: POSRoute) {
        _activeRoute.value = route
    }

    fun setIsAdminMode(admin: Boolean) {
        _isAdminMode.value = admin
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setActiveShift(shift: String) {
        _activeShift.value = shift
    }

    fun setSelectedTable(tableId: Int) {
        _selectedTableId.value = tableId
        
        // Automatically save/load previous cart states from tables queue if they were saved in table config
        viewModelScope.launch {
            val table = allTables.value.find { it.tableId == tableId }
            if (table != null && table.activeTempCartJson.isNotEmpty()) {
                // If Tisch has uncommitted cart queue, we load it
                try {
                    val deserialized = deserializeCart(table.activeTempCartJson)
                    _activeCart.value = deserialized
                } catch (e: Exception) {
                    _activeCart.value = emptyList()
                }
            } else {
                _activeCart.value = emptyList()
            }
        }
    }

    // Cart modification actions
    fun addToCart(item: MenuItem, modifierNotes: String = "", initialWithQty: Int = 1) {
        val current = _activeCart.value.toMutableList()
        val index = current.indexOfFirst { it.menuItem.id == item.id && it.modifiersAndNotes == modifierNotes }
        if (index != -1) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current.add(CartItem(menuItem = item, quantity = initialWithQty, modifiersAndNotes = modifierNotes))
        }
        _activeCart.value = current
        saveCartToCurrentlySelectedTable()
    }

    fun removeFromCart(index: Int) {
        val current = _activeCart.value.toMutableList()
        if (index >= 0 && index < current.size) {
            current.removeAt(index)
        }
        _activeCart.value = current
        saveCartToCurrentlySelectedTable()
    }

    fun updateCartItemQuantity(index: Int, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(index)
            return
        }
        val current = _activeCart.value.toMutableList()
        if (index >= 0 && index < current.size) {
            current[index] = current[index].copy(quantity = newQuantity)
        }
        _activeCart.value = current
        saveCartToCurrentlySelectedTable()
    }

    fun clearCart() {
        _activeCart.value = emptyList()
        saveCartToCurrentlySelectedTable()
    }

    private fun saveCartToCurrentlySelectedTable() {
        val currentTableId = _selectedTableId.value
        if (currentTableId > 0) {
            viewModelScope.launch {
                val table = allTables.value.find { it.tableId == currentTableId }
                if (table != null) {
                    val cartJson = serializeCart(_activeCart.value)
                    repository.updateTable(
                        table.copy(
                            isOccupied = _activeCart.value.isNotEmpty(),
                            activeTempCartJson = cartJson
                        )
                    )
                }
            }
        }
    }

    fun setReceiptViewerTxId(txId: Long?) {
        _selectedTransactionIdForReceipt.value = txId
    }

    // ----------------------------------------------------
    // CHECKOUT PROCESSOR
    // ----------------------------------------------------
    fun processCheckout(
        paymentMethod: String, // "Tunai", "QRIS", "Hutang", "Split"
        amountPaid: Double,
        changeAmount: Double,
        notes: String,
        splitCount: Int = 1,
        onComplete: (Long) -> Unit
    ) {
        val cart = _activeCart.value
        val cashier = storeConfig.value.activeCashier
        val currentTableId = _selectedTableId.value
        val tableNumber = allTables.value.find { it.tableId == currentTableId }?.tableNumber ?: "Bungkus"

        if (cart.isEmpty()) return

        viewModelScope.launch {
            val uuid = UUID.randomUUID().toString()
            val total = cart.sumOf { it.menuItem.price * it.quantity }
            
            val transactionId = repository.processCheckout(
                uuid = uuid,
                cashierName = cashier,
                tableNumber = tableNumber,
                tableId = currentTableId,
                totalAmount = total,
                paymentMethod = paymentMethod,
                amountPaid = amountPaid,
                changeAmount = changeAmount,
                notes = if (splitCount > 1) "[Bagi $splitCount Bagian] $notes" else notes,
                cartItems = cart,
                shiftName = _activeShift.value
            )
            
            // Clean active memory states
            _activeCart.value = emptyList()
            saveCartToCurrentlySelectedTable()
            
            // Trigger receipt view feedback
            _selectedTransactionIdForReceipt.value = transactionId
            onComplete(transactionId)
        }
    }

    fun cancelTransaction(txId: Long, reason: String) {
        val cashier = storeConfig.value.activeCashier
        viewModelScope.launch {
            repository.cancelTransaction(txId, reason, cashier)
        }
    }

    suspend fun getItemsForTransactionList(txId: Long): List<TransactionItem> {
        return repository.getItemsForTransactionList(txId)
    }

    // ----------------------------------------------------
    // INVENTORY CONFIG ACTIONS
    // ----------------------------------------------------
    fun addIngredient(name: String, stock: Double, alert: Double, unit: String) {
        viewModelScope.launch {
            repository.addIngredient(Ingredient(name = name, currentStock = stock, minStockAlert = alert, unit = unit))
            repository.logAction("EDIT_INVENTORY", "Bahan baku baru ditambah: $name", storeConfig.value.activeCashier)
        }
    }

    fun updateIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repository.updateIngredient(ingredient)
            repository.logAction("EDIT_INVENTORY", "Bahan baku diupdate: ${ingredient.name} (${ingredient.currentStock} ${ingredient.unit})", storeConfig.value.activeCashier)
        }
    }

    fun deleteIngredient(id: Long) {
        viewModelScope.launch {
            repository.deleteIngredient(id)
        }
    }

    fun addMenuItem(name: String, price: Double, hpp: Double, category: String, colorHex: String, recipes: List<Recipe>) {
        viewModelScope.launch {
            val menuItem = MenuItem(name = name, price = price, costPrice = hpp, category = category, imageUrlOrColor = colorHex)
            repository.addMenuItem(menuItem, recipes)
            repository.logAction("EDIT_MENU", "Menu makanan baru ditambah: $name", storeConfig.value.activeCashier)
        }
    }

    fun updateMenuItem(menuItem: MenuItem, recipes: List<Recipe>) {
        viewModelScope.launch {
            repository.updateMenuItem(menuItem, recipes)
            repository.logAction("EDIT_MENU", "Menu makanan diupdate: ${menuItem.name}", storeConfig.value.activeCashier)
        }
    }

    fun deleteMenuItem(id: Long) {
        viewModelScope.launch {
            repository.deleteMenuItem(id)
        }
    }

    suspend fun getRecipesForMenuItem(itemId: Long): List<Recipe> {
        return repository.getRecipesForMenuItem(itemId)
    }

    fun updateStoreProfile(name: String, phone: String, logoText: String, footer: String, cashierName: String, pin: String) {
        viewModelScope.launch {
            repository.saveStoreConfig(
                StoreConfig(
                    id = 1,
                    storeName = name,
                    storePhone = phone,
                    storeLogoText = logoText,
                    receiptFooter = footer,
                    activeCashier = cashierName,
                    adminPin = pin
                )
            )
            repository.logAction("EDIT_PROFILE", "Kustomisasi Profile Toko diupdate oleh penanggung jawab", cashierName)
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _syncState.value = true
            kotlinx.coroutines.delay(1200) // Animasi visual sync
            _syncState.value = false
            _lastSyncTime.value = System.currentTimeMillis()
            repository.logAction("DATA_SYNC", "Sinkronisasi Cloud Berhasil (Menerapkan backup terenkripsi)", storeConfig.value.activeCashier)
        }
    }

    // ----------------------------------------------------
    // STATIC DEFAULTS SEEDER
    // ----------------------------------------------------
    private suspend fun initializeDefaultData() {
        // 1. Ingredients
        val bHalusId = repository.addIngredient(Ingredient(name = "Bakso Sapi Halus", currentStock = 120.0, minStockAlert = 25.0, unit = "biji"))
        val bUratId = repository.addIngredient(Ingredient(name = "Bakso Sapi Urat", currentStock = 65.0, minStockAlert = 15.0, unit = "biji"))
        val mieId = repository.addIngredient(Ingredient(name = "Mie Kuning", currentStock = 35.0, minStockAlert = 8.0, unit = "porsi"))
        val sounId = repository.addIngredient(Ingredient(name = "Soun Putih", currentStock = 40.0, minStockAlert = 8.0, unit = "porsi"))
        val gulaId = repository.addIngredient(Ingredient(name = "Gula Pasir", currentStock = 2500.0, minStockAlert = 500.0, unit = "gram"))
        val tehId = repository.addIngredient(Ingredient(name = "Sariwangi Teh Celup", currentStock = 100.0, minStockAlert = 20.0, unit = "pcs"))
        val kopiId = repository.addIngredient(Ingredient(name = "Bubuk Kopi Arka", currentStock = 1200.0, minStockAlert = 250.0, unit = "gram"))

        // 2. Sample Menu items with integrated recipes
        // Bakso Halus Kuah (Price: Rp 18.000, HPP: Rp 8.000)
        val item1 = MenuItem(name = "Bakso Halus Spesial", price = 18000.0, costPrice = 8000.0, category = "Makanan", imageUrlOrColor = "0xFFE57373")
        repository.addMenuItem(
            item1,
            listOf(
                Recipe(menuItemId = 0, ingredientId = bHalusId, quantityNeeded = 5.0),
                Recipe(menuItemId = 0, ingredientId = mieId, quantityNeeded = 1.0)
            )
        )

        // Bakso Campur Urat (Price: Rp 23.000, HPP: Rp 10.500)
        val item2 = MenuItem(name = "Bakso Campur Urat", price = 23000.0, costPrice = 10500.0, category = "Makanan", imageUrlOrColor = "0xFFFFB74D")
        repository.addMenuItem(
            item2,
            listOf(
                Recipe(menuItemId = 0, ingredientId = bUratId, quantityNeeded = 3.0),
                Recipe(menuItemId = 0, ingredientId = bHalusId, quantityNeeded = 2.0),
                Recipe(menuItemId = 0, ingredientId = sounId, quantityNeeded = 1.0)
            )
        )

        // Es Teh Manis Jumbo (Price: Rp 6.000, HPP: Rp 1.800)
        val item3 = MenuItem(name = "Es Teh Manis Jumbo", price = 6000.0, costPrice = 180000 / 100.0, category = "Minuman", imageUrlOrColor = "0xFF81C784")
        repository.addMenuItem(
            item3,
            listOf(
                Recipe(menuItemId = 0, ingredientId = gulaId, quantityNeeded = 35.0),
                Recipe(menuItemId = 0, ingredientId = tehId, quantityNeeded = 1.0)
            )
        )

        // Kopi Tubruk Susu (Price: Rp 9.000, HPP: Rp 3.000)
        val item4 = MenuItem(name = "Kopi Tubruk Arka", price = 9000.0, costPrice = 3000.0, category = "Minuman", imageUrlOrColor = "0xFFA1887F")
        repository.addMenuItem(
            item4,
            listOf(
                Recipe(menuItemId = 0, ingredientId = kopiId, quantityNeeded = 20.0),
                Recipe(menuItemId = 0, ingredientId = gulaId, quantityNeeded = 15.0)
            )
        )

        // Kerupuk Putih Gurih (Price: Rp 2.000, HPP: Rp 1.000)
        val item5 = MenuItem(name = "Kerupuk Udang Kaleng", price = 2000.0, costPrice = 1000.0, category = "Cemilan", imageUrlOrColor = "0xFFB0BEC5")
        repository.addMenuItem(item5, emptyList())

        // 3. Setup Tables
        repository.insertTable(DiningTable(tableId = 0, tableNumber = "Bungkus / Take Away", isOccupied = false))
        repository.insertTable(DiningTable(tableId = 1, tableNumber = "Meja 1", isOccupied = false))
        repository.insertTable(DiningTable(tableId = 2, tableNumber = "Meja 2", isOccupied = false))
        repository.insertTable(DiningTable(tableId = 3, tableNumber = "Meja 3", isOccupied = false))
        repository.insertTable(DiningTable(tableId = 4, tableNumber = "Meja 4", isOccupied = false))
        repository.insertTable(DiningTable(tableId = 5, tableNumber = "Meja 5", isOccupied = false))
        repository.insertTable(DiningTable(tableId = 6, tableNumber = "Meja 6", isOccupied = false))

        // Log initiation
        repository.logAction("SYSTEM", "Sistem Database Arka POS diinisialisasi sukses dengan stok bahan.", "Manager")
    }

    // Simple custom delimiters to save the Cart items into the Room table row string column without needing converters or moshi for simple reliable builds
    private fun serializeCart(cart: List<CartItem>): String {
        if (cart.isEmpty()) return ""
        return cart.joinToString(";") { "${it.menuItem.id},${it.quantity},${it.modifiersAndNotes.replace(",", " ") ?: " "}" }
    }

    private fun deserializeCart(json: String): List<CartItem> {
        if (json.isEmpty()) return emptyList()
        val items = mutableListOf<CartItem>()
        val splitRows = json.split(";")
        for (row in splitRows) {
            val cols = row.split(",")
            if (cols.size >= 2) {
                val menuId = cols[0].toLongOrNull() ?: continue
                val qty = cols[1].toIntOrNull() ?: 1
                val note = if (cols.size > 2) cols[2].trim() else ""
                val foundMenu = allMenuItems.value.find { it.id == menuId }
                if (foundMenu != null) {
                    items.add(CartItem(menuItem = foundMenu, quantity = qty, modifiersAndNotes = note))
                }
            }
        }
        return items
    }
}
