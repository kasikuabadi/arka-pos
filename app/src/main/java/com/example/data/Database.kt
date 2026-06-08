package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.io.Serializable

// ==========================================
// 1. DATABASE ENTITIES (MODELS)
// ==========================================

@Entity(tableName = "store_config")
data class StoreConfig(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "Arka Studio POS",
    val storePhone: String = "08123456789",
    val storeLogoText: String = "ARKA",
    val receiptFooter: String = "Terima kasih atas kunjungan Anda!",
    val activeCashier: String = "Budi Kasir",
    val adminPin: String = "1234"
) : Serializable

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currentStock: Double,
    val minStockAlert: Double,
    val unit: String // e.g., "pcs", "gram", "porsi"
) : Serializable

@Entity(tableName = "menu_items")
data class MenuItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val costPrice: Double, // HPP for Profit/Loss calc
    val category: String, // e.g., "Makanan", "Minuman", "Cemilan"
    val imageUrlOrColor: String = "0xFFE57373", // Use hex colors for elegant native generation
    val isAvailable: Boolean = true
) : Serializable

@Entity(
    tableName = "recipes",
    foreignKeys = [
        ForeignKey(
            entity = MenuItem::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("menuItemId"), Index("ingredientId")]
)
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val menuItemId: Long,
    val ingredientId: Long,
    val quantityNeeded: Double
) : Serializable

@Entity(tableName = "dining_tables")
data class DiningTable(
    @PrimaryKey val tableId: Int, // 0 = Take Away, 1 = Meja 1, etc.
    val tableNumber: String,
    val isOccupied: Boolean = false,
    val activeTempCartJson: String = "" // Holds uncommitted intermediate items as order queue
) : Serializable

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val cashierName: String,
    val tableNumber: String,
    val timestamp: Long,
    val totalAmount: Double,
    val costOfGoodsSold: Double, // Total recipe HPP for net profit
    val paymentMethod: String, // "Tunai", "QRIS", "Hutang", "Split"
    val amountPaid: Double,
    val changeAmount: Double,
    val status: String = "PAID", // "PAID", "CANCELLED"
    val cancelReason: String? = null,
    val notes: String = "",
    val tableId: Int = 0,
    val shiftName: String = "Shift Pagi"
) : Serializable

@Entity(tableName = "transaction_items")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val menuItemId: Long,
    val menuItemName: String,
    val price: Double,
    val costPrice: Double,
    val quantity: Int,
    val modifierNotes: String = "" // Toppings / extra requests
) : Serializable

@Entity(tableName = "transaction_logs")
data class TransactionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val timestamp: Long,
    val actionType: String, // "CHECKOUT", "UPDATE_STOCK", "CANCEL_TRANSACTION", "EDIT_MENU"
    val description: String
) : Serializable


// ==========================================
// 2. DATA ACCESS OBJECT (DAO) -> Combined Interface
// ==========================================

@Dao
interface POSDao {
    
    // Store profile metadata
    @Query("SELECT * FROM store_config WHERE id = 1 LIMIT 1")
    fun getStoreConfig(): Flow<StoreConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: StoreConfig)

    // Ingredients
    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    fun getAllIngredients(): Flow<List<Ingredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: Ingredient): Long

    @Update
    suspend fun updateIngredient(ingredient: Ingredient)

    @Query("DELETE FROM ingredients WHERE id = :id")
    suspend fun deleteIngredientById(id: Long)

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    suspend fun getIngredientById(id: Long): Ingredient?

    // Menu Items
    @Query("SELECT * FROM menu_items ORDER BY name ASC")
    fun getAllMenuItems(): Flow<List<MenuItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(menuItem: MenuItem): Long

    @Update
    suspend fun updateMenuItem(menuItem: MenuItem)

    @Query("DELETE FROM menu_items WHERE id = :id")
    suspend fun deleteMenuItemById(id: Long)

    @Query("SELECT * FROM menu_items WHERE id = :id LIMIT 1")
    suspend fun getMenuItemById(id: Long): MenuItem?

    // Recipes
    @Query("SELECT * FROM recipes WHERE menuItemId = :menuItemId")
    fun getRecipesForMenuItem(menuItemId: Long): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE menuItemId = :menuItemId")
    suspend fun getRecipesForMenuItemList(menuItemId: Long): List<Recipe>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE menuItemId = :menuItemId")
    suspend fun deleteRecipesByMenuItem(menuItemId: Long)

    // Dining Tables
    @Query("SELECT * FROM dining_tables ORDER BY tableId ASC")
    fun getAllTables(): Flow<List<DiningTable>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: DiningTable)

    @Update
    suspend fun updateTable(table: DiningTable)

    @Query("SELECT * FROM dining_tables WHERE tableId = :tableId LIMIT 1")
    suspend fun getTableById(tableId: Int): DiningTable?

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    // Transaction Items
    @Query("SELECT * FROM transaction_items WHERE transactionId = :txId")
    fun getItemsForTransaction(txId: Long): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :txId")
    suspend fun getItemsForTransactionList(txId: Long): List<TransactionItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItem(item: TransactionItem)

    // Logs
    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<TransactionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TransactionLog)
}

// ==========================================
// 3. DATABASE HOLDER & TYPE CONVERTERS
// ==========================================

@Database(
    entities = [
        StoreConfig::class,
        Ingredient::class,
        MenuItem::class,
        Recipe::class,
        DiningTable::class,
        Transaction::class,
        TransactionItem::class,
        TransactionLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class POSDatabase : RoomDatabase() {
    abstract fun posDao(): POSDao

    companion object {
        @Volatile
        private var INSTANCE: POSDatabase? = null

        fun getDatabase(context: Context): POSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    POSDatabase::class.java,
                    "arka_pos_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 4. THE REPOSITORY PATTERN IMPLEMENTATION
// ==========================================

class POSRepository(private val posDao: POSDao) {

    val storeConfig: Flow<StoreConfig?> = posDao.getStoreConfig()
    val allIngredients: Flow<List<Ingredient>> = posDao.getAllIngredients()
    val allMenuItems: Flow<List<MenuItem>> = posDao.getAllMenuItems()
    val allTables: Flow<List<DiningTable>> = posDao.getAllTables()
    val allTransactions: Flow<List<Transaction>> = posDao.getAllTransactions()
    val allLogs: Flow<List<TransactionLog>> = posDao.getAllLogs()

    suspend fun saveStoreConfig(config: StoreConfig) {
        posDao.insertOrUpdateConfig(config)
    }

    suspend fun addIngredient(ingredient: Ingredient): Long {
        return posDao.insertIngredient(ingredient)
    }

    suspend fun updateIngredient(ingredient: Ingredient) {
        posDao.updateIngredient(ingredient)
    }

    suspend fun deleteIngredient(id: Long) {
        posDao.deleteIngredientById(id)
    }

    suspend fun getIngredientById(id: Long): Ingredient? {
        return posDao.getIngredientById(id)
    }

    suspend fun addMenuItem(menuItem: MenuItem, recipes: List<Recipe>): Long {
        val menuItemId = posDao.insertMenuItem(menuItem)
        posDao.deleteRecipesByMenuItem(menuItemId)
        for (recipe in recipes) {
            posDao.insertRecipe(recipe.copy(menuItemId = menuItemId))
        }
        return menuItemId
    }

    suspend fun updateMenuItem(menuItem: MenuItem, recipes: List<Recipe>) {
        posDao.updateMenuItem(menuItem)
        posDao.deleteRecipesByMenuItem(menuItem.id)
        for (recipe in recipes) {
            posDao.insertRecipe(recipe.copy(menuItemId = menuItem.id))
        }
    }

    suspend fun deleteMenuItem(id: Long) {
        posDao.deleteRecipesByMenuItem(id)
        posDao.deleteMenuItemById(id)
    }

    suspend fun getRecipesForMenuItem(menuItemId: Long): List<Recipe> {
        return posDao.getRecipesForMenuItemList(menuItemId)
    }

    suspend fun insertTable(table: DiningTable) {
        posDao.insertTable(table)
    }

    suspend fun updateTable(table: DiningTable) {
        posDao.updateTable(table)
    }

    suspend fun logAction(actionType: String, description: String, cashier: String) {
        posDao.insertLog(
            TransactionLog(
                userId = cashier,
                timestamp = System.currentTimeMillis(),
                actionType = actionType,
                description = description
            )
        )
    }

    // Process a full Checkout transaction.
    // Automatically deducts recipe ingredients and checks stock.
    suspend fun processCheckout(
        uuid: String,
        cashierName: String,
        tableNumber: String,
        tableId: Int,
        totalAmount: Double,
        paymentMethod: String,
        amountPaid: Double,
        changeAmount: Double,
        notes: String,
        cartItems: List<CartItem>,
        shiftName: String
    ): Long {
        // Calculate total cost of goods sold (COGS) based on recipe ingredients or MenuItem costPrice
        var computedCostPriceTotal = 0.0
        for (item in cartItems) {
            computedCostPriceTotal += (item.menuItem.costPrice * item.quantity)
            
            // Deduct from Ingredient Stock
            val recipes = posDao.getRecipesForMenuItemList(item.menuItem.id)
            for (recipe in recipes) {
                val ingredient = posDao.getIngredientById(recipe.ingredientId)
                if (ingredient != null) {
                    val deductedQuantity = recipe.quantityNeeded * item.quantity
                    val nextStock = (ingredient.currentStock - deductedQuantity).coerceAtLeast(0.0)
                    posDao.updateIngredient(ingredient.copy(currentStock = nextStock))
                }
            }
        }

        // Insert Transaction
        val transaction = Transaction(
            uuid = uuid,
            cashierName = cashierName,
            tableNumber = tableNumber,
            tableId = tableId,
            timestamp = System.currentTimeMillis(),
            totalAmount = totalAmount,
            costOfGoodsSold = computedCostPriceTotal,
            paymentMethod = paymentMethod,
            amountPaid = amountPaid,
            changeAmount = changeAmount,
            status = "PAID",
            notes = notes,
            shiftName = shiftName
        )
        val transactionId = posDao.insertTransaction(transaction)

        // Insert Transaction Items
        for (item in cartItems) {
            val txItem = TransactionItem(
                transactionId = transactionId,
                menuItemId = item.menuItem.id,
                menuItemName = item.menuItem.name,
                price = item.menuItem.price,
                costPrice = item.menuItem.costPrice,
                quantity = item.quantity,
                modifierNotes = item.modifiersAndNotes
            )
            posDao.insertTransactionItem(txItem)
        }

        // Update Table status to empty/unoccupied if it was tied to a table
        if (tableId > 0) {
            val table = posDao.getTableById(tableId)
            if (table != null) {
                posDao.updateTable(table.copy(isOccupied = false, activeTempCartJson = ""))
            }
        }

        // Log the checkout
        logAction(
            actionType = "CHECKOUT",
            description = "Transaksi Rp ${totalAmount.toInt()} via $paymentMethod berhasil disimpan. Meja: $tableNumber",
            cashier = cashierName
        )

        return transactionId
    }

    // Cancel Transaction (Return Stocks back)
    suspend fun cancelTransaction(transactionId: Long, reason: String, callerCashierName: String) {
        val transaction = posDao.getTransactionById(transactionId) ?: return
        if (transaction.status == "CANCELLED") return // Already cancelled

        // Mark cancelled
        val updatedTransaction = transaction.copy(
            status = "CANCELLED",
            cancelReason = reason
        )
        posDao.updateTransaction(updatedTransaction)

        // Return Item Stock
        val items = posDao.getItemsForTransactionList(transactionId)
        for (item in items) {
            val recipes = posDao.getRecipesForMenuItemList(item.menuItemId)
            for (recipe in recipes) {
                val ingredient = posDao.getIngredientById(recipe.ingredientId)
                if (ingredient != null) {
                    val addedQuantity = recipe.quantityNeeded * item.quantity
                    val nextStock = ingredient.currentStock + addedQuantity
                    posDao.updateIngredient(ingredient.copy(currentStock = nextStock))
                }
            }
        }

        // Log the cancellation
        logAction(
            actionType = "CANCEL_TRANSACTION",
            description = "Transaksi #${transactionId} (Rp ${transaction.totalAmount.toInt()}) DIBATALKAN oleh $callerCashierName. Alasan: $reason",
            cashier = callerCashierName
        )
    }

    suspend fun getItemsForTransactionList(txId: Long): List<TransactionItem> {
        return posDao.getItemsForTransactionList(txId)
    }
}

// Temporary Cart helper structure not persisted directly as Room entity, but serialized/used in application
data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int,
    val modifiersAndNotes: String = ""
) : Serializable
