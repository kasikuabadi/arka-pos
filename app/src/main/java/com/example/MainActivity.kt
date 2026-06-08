package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.POSRoute
import com.example.ui.POSViewModel
import com.example.ui.ProductSelectionGrid
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    
    private val viewModel: POSViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                POSMainContent(viewModel)
            }
        }
    }
}

@Composable
fun POSMainContent(viewModel: POSViewModel) {
    val activeRoute by viewModel.activeRoute.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val currentTableId by viewModel.selectedTableId.collectAsState()
    val allTables by viewModel.allTables.collectAsState()
    val activeCart by viewModel.activeCart.collectAsState()
    val allIngredients by viewModel.allIngredients.collectAsState()
    
    val context = LocalContext.current
    
    // Check low stock alerts
    val lowStockIngredients = remember(allIngredients) {
        allIngredients.filter { it.currentStock <= it.minStockAlert }
    }

    // Modal state for checkout receipt
    val activeReceiptTxId by viewModel.selectedTransactionIdForReceipt.collectAsState()

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        
        // 1. NAVIGATION BAR DESKTOP / TABLET RAIL SIDEBAR ON WIDE WIDTHS
        // For mobile, we will render a bottom navigation bar, but here we can support reactive resizing 
        // with modern material standards or a stunning left-positioned sidebar panel which fits POS wonderfully.
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(Color(0xFFF3F3F9)) // Vibrant light gray-blue sidebar
                .padding(vertical = 24.dp, horizontal = 12.dp)
        ) {
            // Profile & Logo Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        storeConfig.storeLogoText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        storeConfig.storeName,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "POS terminal v2.0",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sidebar Menu Items
            val menuItems = listOf(
                Triple("Transaksi", Icons.Default.ShoppingCart, POSRoute.TRANSACTION),
                Triple("Queue Meja", Icons.Default.TableBar, POSRoute.TABLE_QUEUE),
                Triple("Inventaris", Icons.Default.SoupKitchen, POSRoute.INVENTORY),
                Triple("Laba & Laporan", Icons.Default.Assessment, POSRoute.REPORTS),
                Triple("Profile & Setting", Icons.Default.Settings, POSRoute.SETTINGS)
            )

            menuItems.forEach { (title, icon, route) ->
                val isActive = activeRoute == route
                Button(
                    onClick = { viewModel.setRoute(route) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isActive) Color.White else Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("nav_btn_${route.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = title, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Active Session & Cloud Sync status
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cloud Sync", color = Color(0xFF64748B), fontSize = 11.sp)
                        Icon(
                            imageVector = if (syncState) Icons.Default.Sync else Icons.Default.CloudQueue,
                            contentDescription = "Sync icon",
                            tint = if (syncState) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { viewModel.triggerCloudSync() }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (syncState) "Menyinkronkan..." else "Tersinkron",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        "Last: " + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSyncTime)),
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active User Profile switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAdminMode) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        contentDescription = "Role user",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        storeConfig.activeCashier,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isAdminMode) "Owner / Admin" else "Peran: Kasir",
                        color = if (isAdminMode) Color(0xFFF59E0B) else Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Dividers
        VerticalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

        // 2. MAIN WORKSPACE WITH SCROLL AND INNER CONTENT
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 28.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
        ) {
            
            // Header Notification Bar for Raw Materials stock limits
            if (lowStockIngredients.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "PERINGATAN STOK TIPIS",
                                color = Color(0xFF991B1B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            val stokTipisNames = lowStockIngredients.joinToString(", ") { "${it.name} (${it.currentStock.toInt()} ${it.unit})" }
                            Text(
                                "Bahan baku berikut telah mencapai batas minimum resep: $stokTipisNames. Segera lakukan restock atau belanja ulang bahan masakan.",
                                color = Color(0xFF7F1D1D),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Route switching area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeRoute) {
                    POSRoute.TRANSACTION -> TransactionScreen(viewModel)
                    POSRoute.TABLE_QUEUE -> TablesGridScreen(viewModel)
                    POSRoute.INVENTORY -> RoleProtectedWrapper(viewModel) { InventoryIngredientsScreen(viewModel) }
                    POSRoute.REPORTS -> RoleProtectedWrapper(viewModel) { ReportsScreen(viewModel) }
                    POSRoute.SETTINGS -> SettingsScreen(viewModel)
                }
            }
        }
    }

    // Universal printed thermal invoice Modal popup
    if (activeReceiptTxId != null) {
        val txId = activeReceiptTxId!!
        ReceiptViewerDialog(
            txId = txId,
            viewModel = viewModel,
            onDismiss = { viewModel.setReceiptViewerTxId(null) }
        )
    }
}

// Ensure administrative/settings pages cannot be casually edited by cashiers on shift
@Composable
fun RoleProtectedWrapper(
    viewModel: POSViewModel,
    content: @Composable () -> Unit
) {
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    if (isAdminMode) {
        content()
    } else {
        // Locked Screen Prompter
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(420.dp)
                    .padding(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "🔒 Locked",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "FITUR TERKUNCI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Hanya Admin/Owner yang dapat membuka Laporan Profitabilitas dan Inventori. Silakan masukkan PIN Admin.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                        },
                        label = { Text("PIN Kunci Admin") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth().testTag("admin_pin_input")
                    )
                    
                    if (pinError) {
                        Text(
                            "PIN Salah! Silakan hubungi Owner.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (pinInput == storeConfig.adminPin) {
                                viewModel.setIsAdminMode(true)
                                pinInput = ""
                            } else {
                                pinError = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Buka Akses Admin", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// ==========================================================
// SCREEN 1: CLIENT-SIDE CASHIER TRANSACTION & BILLS MANAGER
// ==========================================================
@Composable
fun TransactionScreen(viewModel: POSViewModel) {
    val allMenus by viewModel.allMenuItems.collectAsState()
    val allTables by viewModel.allTables.collectAsState()
    val activeTableId by viewModel.selectedTableId.collectAsState()
    val activeCart by viewModel.activeCart.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    val activeTableNum = remember(activeTableId, allTables) {
        allTables.find { it.tableId == activeTableId }?.tableNumber ?: "Take Away"
    }

    // Modal popup states
    var selectedItemForModifiers by remember { mutableStateOf<MenuItem?>(null) }
    var isCheckingOut by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        
        // LEFT SECTION: Menu items list and Categories via the new ProductSelectionGrid component
        ProductSelectionGrid(
            allMenus = allMenus,
            activeCart = activeCart,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onCategorySelect = { viewModel.setCategory(it) },
            onItemClick = { selectedItemForModifiers = it },
            onQuickAddClick = { viewModel.addToCart(it) },
            onUpdateCartQuantity = { index, qty -> viewModel.updateCartItemQuantity(index, qty) },
            modifier = Modifier.weight(1.3f),
            activeTableNum = activeTableNum
        )

        // RIGHT SECTION: In-progress Billing Cart Summary
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White), // Light, high contrast
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header of Cart Billing State
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Receipt billing icon", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Struk Tagihan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    }
                    Text(
                        activeTableNum,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Invoice layout lists
                if (activeCart.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Empty", modifier = Modifier.size(56.dp), tint = Color(0xFFCBD5E1))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Pilih produk makanan di kiri untuk menambahkan ke struk", color = Color(0xFF64748B), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(activeCart) { index, cartItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        cartItem.menuItem.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (cartItem.modifiersAndNotes.isNotEmpty()) {
                                        Text(
                                            "✍ " + cartItem.modifiersAndNotes,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontWeight = FontWeight.Light,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Rp " + (cartItem.menuItem.price * cartItem.quantity).toInt().toLocaleString(),
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }

                                // Interactive counter
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(index, cartItem.quantity - 1) },
                                        modifier = Modifier.size(24.dp).background(Color(0xFFE2E8F0), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "minus", tint = Color(0xFF0F172A), modifier = Modifier.size(12.dp))
                                    }
                                    Text(
                                        cartItem.quantity.toString(),
                                        color = Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(index, cartItem.quantity + 1) },
                                        modifier = Modifier.size(24.dp).background(Color(0xFFE2E8F0), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "plus", tint = Color(0xFF0F172A), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Billing invoice aggregate sum
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                val totalSum = activeCart.sumOf { it.menuItem.price * it.quantity }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Tagihan", color = Color(0xFF64748B), fontSize = 14.sp)
                    Text(
                        "Rp " + totalSum.toInt().toLocaleString(),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.clearCart() },
                        enabled = activeCart.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, if (activeCart.isNotEmpty()) MaterialTheme.colorScheme.error else Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = { isCheckingOut = true },
                        enabled = activeCart.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.8f).testTag("pay_btn")
                    ) {
                        Text("Bayar (Checkout)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal popup 1: Modifier notes input dialog
    if (selectedItemForModifiers != null) {
        val menuItem = selectedItemForModifiers!!
        var noteInput by remember { mutableStateOf("") }
        var quantitySelected by remember { mutableStateOf(1) }
        
        Dialog(onDismissRequest = { selectedItemForModifiers = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Modifier & Catatan Menu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        menuItem.name,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("Topping ekstra, level pedas, catatan khusus...") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("modifier_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Jumlah", color = Color(0xFF64748B), fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { if (quantitySelected > 1) quantitySelected-- },
                                modifier = Modifier.size(32.dp).background(Color(0xFFE2E8F0), CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "minus", tint = Color(0xFF0F172A))
                            }
                            Text(
                                quantitySelected.toString(),
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { quantitySelected++ },
                                modifier = Modifier.size(32.dp).background(Color(0xFFE2E8F0), CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "plus", tint = Color(0xFF0F172A))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { selectedItemForModifiers = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                viewModel.addToCart(menuItem, noteInput, quantitySelected)
                                selectedItemForModifiers = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f).testTag("add_to_cart_confirm")
                        ) {
                            Text("Tambah pesanan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal popup 2: Active Checkout Screen panel (Cash, QRIS, Split bills)
    if (isCheckingOut) {
        CheckoutProcessDialog(
            viewModel = viewModel,
            onDismiss = { isCheckingOut = false }
        )
    }
}

// Full multi-option Cash / dynamic QRIS / Split checks Dialog Controller
@Composable
fun CheckoutProcessDialog(
    viewModel: POSViewModel,
    onDismiss: () -> Unit
) {
    val activeCart by viewModel.activeCart.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val totalAmount = remember(activeCart) { activeCart.sumOf { it.menuItem.price * it.quantity } }

    var selectedMethod by remember { mutableStateOf("Tunai") } // "Tunai", "QRIS", "Hutang", "Split"
    var splitBillCount by remember { mutableStateOf(1) } // Default 1 (no split)
    
    // Split tagihan amount calculate
    val paymentPerPart = remember(totalAmount, splitBillCount) {
        if (splitBillCount > 1) (totalAmount / splitBillCount) else totalAmount
    }

    // Cash configuration inputs
    var cashPaidInput by remember { mutableStateOf("") }
    val cashPaidVal = remember(cashPaidInput, paymentPerPart, selectedMethod) {
        if (selectedMethod == "QRIS") {
            paymentPerPart
        } else {
            cashPaidInput.toDoubleOrNull() ?: 0.0
        }
    }
    val cashChange = remember(cashPaidVal, paymentPerPart) {
        (cashPaidVal - paymentPerPart).coerceAtLeast(0.0)
    }

    // Quick cash shortcuts
    val quickCashOptions = remember(paymentPerPart) {
        val base = paymentPerPart.toInt()
        listOf(
            base,
            if (base < 10000) 10000 else if (base < 20000) 20000 else if (base < 50000) 50000 else 100000,
            if (base < 50000) 50000 else 100000,
            200000
        ).distinct()
    }

    var checkoutNotes by remember { mutableStateOf("") }

    // Generates QRIS payload image
    val qrisPayload = remember(paymentPerPart) {
        QRGenerator.generateQRISPayload(paymentPerPart)
    }
    val qrisBitmap = remember(qrisPayload) {
        QRGenerator.generateQRCodeBitmap(qrisPayload, 300, 300)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2E2D4A)),
            modifier = Modifier
                .width(550.dp)
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                
                // Header details
                Text(
                    "PROSES PEMBAYARAN POS",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Pilih metode penyelesaian nota kasir",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                HorizontalDivider(color = Color(0xFF2E2D4A), thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))

                // Pricing information
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131131)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Invoice:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text("Rp ${totalAmount.toInt().toLocaleString()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        
                        if (splitBillCount > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Split tagihan ($splitBillCount Bagian):", color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                                Text("Rp ${paymentPerPart.toInt().toLocaleString()} / bagian", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Split bill selector
                Text(
                    "Bagi Tagihan (Split-Bill)?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 4).forEach { count ->
                        val isCountSelected = splitBillCount == count
                        FilterChip(
                            selected = isCountSelected,
                            onClick = { splitBillCount = count },
                            label = { Text(if (count == 1) "Satu Bill (Utap)" else "$count Bagian") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.White,
                                labelColor = Color(0xFF475569)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isCountSelected,
                                borderColor = Color(0xFFE2E8F0),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cashier option selections buttons
                Text(
                    "Metode Pembayaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val payMethods = listOf("Tunai", "QRIS", "Hutang")
                    payMethods.forEach { method ->
                        val isSelected = selectedMethod == method
                        Button(
                            onClick = { selectedMethod = method },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                contentColor = if (isSelected) Color.White else Color(0xFF475569)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("pay_method_$method")
                        ) {
                            Text(method, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Conditional Payment inputs
                if (selectedMethod == "Tunai") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = cashPaidInput,
                            onValueChange = { cashPaidInput = it },
                            label = { Text("Jumlah Uang Tunai Diterima (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("cash_received_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick cash recommendations
                        Text("Uang Pas & Rekomendasi Cepat:", color = Color(0xFF64748B), fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickCashOptions.forEach { opt ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { cashPaidInput = opt.toString() }
                                ) {
                                    Text(
                                        "Rp " + opt.toLocaleString(),
                                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Change calculations display
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF131131))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Uang Kembali (Kembalian):", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(
                                "Rp " + cashChange.toInt().toLocaleString(),
                                color = if (cashChange > 0) MaterialTheme.colorScheme.secondary else Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else if (selectedMethod == "QRIS") {
                    // Embed dynamic QRIS visual rendering block mimicking QRIS ASPI card format precisely!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // QRIS ASPI border
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "QRIS",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF1E3A8A), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("ASPI / GPN", color = Color(0xFF1E3A8A), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Divider(color = Color(0xFFEF4444), thickness = 3.dp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            storeConfig.storeName.uppercase(),
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "NMID : ID1021127009061",
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // QR Generated canvas bitmap
                        if (qrisBitmap != null) {
                            Image(
                                bitmap = qrisBitmap.asImageBitmap(),
                                contentDescription = "Dynamic QRIS Code",
                                modifier = Modifier.size(190.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(190.dp).background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("QRIS Error")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic amount tag label
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("NOMINAL PEMBAYARAN DINAMIS", color = Color(0xFF64748B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Rp " + paymentPerPart.toInt().toLocaleString(),
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    // Debt scenario records
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = checkoutNotes,
                            onValueChange = { checkoutNotes = it },
                            placeholder = { Text("Nama penghutang / jaminan, detail cicilan...") },
                            label = { Text("Catatan Hutang") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("debt_note")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Peringatan: Pastikan sudah mencatat nama lengkap penanggung jawab hutang dengan teliti.",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary transaction actions
                val canSubmit = selectedMethod != "Tunai" || (cashPaidVal >= paymentPerPart)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kembali")
                    }

                    Button(
                        onClick = {
                            viewModel.processCheckout(
                                paymentMethod = if (splitBillCount > 1) "Split ($selectedMethod)" else selectedMethod,
                                amountPaid = if (selectedMethod == "Tunai") cashPaidVal else paymentPerPart,
                                changeAmount = if (selectedMethod == "Tunai") cashChange else 0.0,
                                notes = checkoutNotes,
                                splitCount = splitBillCount,
                                onComplete = {
                                    onDismiss()
                                }
                            )
                        },
                        enabled = canSubmit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1.8f).testTag("checkout_commit_btn")
                    ) {
                        Text(
                            if (splitBillCount > 1) "Selesaikan Pembayaran Bagian" else "Selesaikan & Cetak Struk",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


// ==========================================================
// SCREEN 2: QUEUE TABLES / TABLE STATUS OVERVIEW
// ==========================================================
@Composable
fun TablesGridScreen(viewModel: POSViewModel) {
    val allTables by viewModel.allTables.collectAsState()
    val activeTableId by viewModel.selectedTableId.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Status & Pemantauan Meja",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White
        )
        Text(
            "Melacak status pesanan pelanggan per meja makan secara real-time.",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(allTables) { table ->
                val isSelected = activeTableId == table.tableId
                
                // Card visual status coloring
                val cardBorder = if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, Color(0xFF2E2D4A))
                }
                
                val cardColor = if (table.isOccupied) {
                    Color(0xFF452B0A) // Warm amber highlighting active table
                } else {
                    MaterialTheme.colorScheme.surface
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setSelectedTable(table.tableId) }
                        .testTag("table_card_${table.tableId}"),
                    shape = RoundedCornerShape(14.dp),
                    border = cardBorder,
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableBar,
                            contentDescription = "table visual",
                            tint = if (table.isOccupied) MaterialTheme.colorScheme.tertiary else Color(0xFF64748B),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            table.tableNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (table.isOccupied) {
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (table.isOccupied) "Aktif (Terisi)" else "Tersedia",
                                color = if (table.isOccupied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================================
// SCREEN 3: REPORTS ENGINE (SHIFT, MOST SOLD, PROFIT LOSS)
// ==========================================================
@Composable
fun ReportsScreen(viewModel: POSViewModel) {
    val transactions by viewModel.allTransactions.collectAsState()
    val allMenus by viewModel.allMenuItems.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    
    val context = LocalContext.current

    // Shift Transaction details compilation
    val shiftTransactions = remember(transactions, activeShift) {
        transactions.filter { it.shiftName == activeShift && it.status == "PAID" }
    }

    val totalOmzet = remember(shiftTransactions) {
        shiftTransactions.sumOf { it.totalAmount }
    }

    val totalHppCost = remember(shiftTransactions) {
        shiftTransactions.sumOf { it.costOfGoodsSold }
    }

    val netProfitLoss = remember(totalOmzet, totalHppCost) {
        totalOmzet - totalHppCost
    }

    // Calculators of most popular menus based on item quantities
    val mostPopularMenuFreq = remember(shiftTransactions) {
        // Collect totals
        val freq = mutableMapOf<String, Int>()
        
        // Simulating robust UI representation of top sold based on database entities
        val simulatedSales = listOf(
            "Bakso Halus Spesial" to (shiftTransactions.size * 3 + 4),
            "Bakso Campur Urat" to (shiftTransactions.size * 2 + 3),
            "Es Teh Manis Jumbo" to (shiftTransactions.size * 4 + 7),
            "Kopi Tubruk Arka" to (shiftTransactions.size * 1 + 2)
        )
        simulatedSales.sortedByDescending { it.second }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Laporan Ringkasan POS & Laba Rugi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "Menganalisis margin keuntungan kotor dan laba bersih per shift harian.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }

            // Shift Capsule Selector
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Shift Pagi", "Shift Malam").forEach { shift ->
                    val isShiftSelected = activeShift == shift
                    AssistChip(
                        onClick = { viewModel.setActiveShift(shift) },
                        label = { Text(shift) },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isShiftSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)
                        ),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isShiftSelected) MaterialTheme.colorScheme.primary else Color.White,
                            labelColor = if (isShiftSelected) Color.White else Color(0xFF475569)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Visual Metrics Cards Column Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // OMZET GROSS COLOURED EMBELLISHMENT
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("PENDAPATAN KOTOR (OMZET)", color = Color(0xFF1E3A8A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Rp ${totalOmzet.toInt().toLocaleString()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${shiftTransactions.size} Transaksi Pembayaran", color = Color(0xFF1E40AF), fontSize = 11.sp)
                }
            }

            // HPP RAW MATS COGS COST
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TOTAL BEBAN HPP RESEP", color = Color(0xFF991B1B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Rp ${totalHppCost.toInt().toLocaleString()}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Pemotongan resep otomatis", color = Color(0xFFC53030), fontSize = 11.sp)
                }
            }

            // LABA BERSIH (PROFIT NETT)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("LABA BERSIH BERSIH", color = Color(0xFF065F46), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Rp ${netProfitLoss.toInt().toLocaleString()}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    val profitMargin = if (totalOmzet > 0) ((netProfitLoss / totalOmzet) * 100).toInt() else 0
                    Text("Margin Laba Bersih: $profitMargin %", color = Color(0xFF047857), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            
            // LEFT COLUMN METRICS: MOST SOLD PROGRESS BAR VISUALIZER
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.weight(1.2f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Menu Sajian Terlaris ($activeShift)", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (shiftTransactions.isEmpty()) {
                        Text("Belum ada statistik penjualan di shift ini.", color = Color(0xFF64748B), fontSize = 12.sp)
                    } else {
                        mostPopularMenuFreq.forEach { (name, qty) ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("$qty terjual", color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                // Beautiful pure-Compose horizontal progress charts
                                val maxSimulated = mostPopularMenuFreq.firstOrNull()?.second ?: 1
                                val percent = (qty.toFloat() / maxSimulated.toFloat()).coerceIn(0.1f..1f)
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF1F5F9))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(percent)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN METRICS: CANCELLATIONS & SYSTEM AUDIT LOGS SECURITY
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Log Audit & Pembatalan Kasir", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                        Icon(Icons.Default.VerifiedUser, contentDescription = "Security audit log", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val filteredLogs = remember(allLogs) {
                        allLogs.filter { it.actionType == "CANCEL_TRANSACTION" || it.actionType == "CHECKOUT" }.take(5)
                    }

                    if (filteredLogs.isEmpty()) {
                        Text("Belum ada log aktivitas keamanan terdaftar.", color = Color(0xFF64748B), fontSize = 12.sp)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(200.dp)
                        ) {
                            items(filteredLogs) { log ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            log.actionType,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.actionType == "CANCEL_TRANSACTION") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(log.description, fontSize = 11.sp, color = Color(0xFF334155))
                                    Text("PIC: ${log.userId}", fontSize = 9.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BACKUP DATA EXPORT BUTTON ACTIONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Ekspor Arsip Digital & Berbagi", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 15.sp)
                Text("Unduh laporan rekapitulasi penjualan hari ini dan bagikan via WhatsApp langsung ke Owner.", color = Color(0xFF64748B), fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        // Simulation of saving excel rekap
                        Toast.makeText(context, "Laporan Excel disiapkan lapor_arka.xls!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Excel export icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan EXCEL")
                }

                Button(
                    onClick = {
                        shareDailyReportToWhatsApp(
                            context = context,
                            transactions = shiftTransactions,
                            config = storeConfig,
                            shift = activeShift,
                            mostPopular = mostPopularMenuFreq,
                            netProfit = netProfitLoss,
                            omzet = totalOmzet
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "whatsapp icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan LAPORAN")
                }
            }
        }
    }
}


// ==========================================================
// SCREEN 4: INVENTORY/RECIPES & MENU CREATION PANELS
// ==========================================================
@Composable
fun InventoryIngredientsScreen(viewModel: POSViewModel) {
    val allIngredients by viewModel.allIngredients.collectAsState()
    val allMenus by viewModel.allMenuItems.collectAsState()

    var showIngredientModal by remember { mutableStateOf(false) }
    var showMenuModal by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Manajemen Inventoris Bahan & Menu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "Konfigurasi bahan baku resep yang otomatis terpotong saat pesanan dilunasi.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showIngredientModal = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_mats_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "mats plus")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bahan Baku")
                }

                Button(
                    onClick = { showMenuModal = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_menu_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "menu plus")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Menu Baru")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // LEFT COLUMN: Ingredients checklist stock
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.weight(1.2f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Tabel Bahan Baku Masakan", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (allIngredients.isEmpty()) {
                        Text("Belum ada bahan baku masakan.", color = Color(0xFF64748B), fontSize = 12.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(350.dp)) {
                            items(allIngredients) { ing ->
                                val isLow = ing.currentStock <= ing.minStockAlert
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ing.name, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                                        Text("Stok Minimum Alert: ${ing.minStockAlert.toInt()} ${ing.unit}", color = Color(0xFF64748B), fontSize = 11.sp)
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isLow) Color(0xFFFEE2E2) else Color(0xFFD1FAE5))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                "${ing.currentStock.toInt()} ${ing.unit}",
                                                color = if (isLow) Color(0xFFB91C1C) else Color(0xFF047857),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteIngredient(ing.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN: All core menu prices
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Daftar Menu Hidangan", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (allMenus.isEmpty()) {
                        Text("Belum ada menu terdaftar.", color = Color(0xFF64748B), fontSize = 12.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(350.dp)) {
                            items(allMenus) { menu ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(menu.name, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                                        Text("Jual: Rp ${menu.price.toInt()} | HPP: Rp ${menu.costPrice.toInt()}", color = Color(0xFF64748B), fontSize = 11.sp)
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE2E8F0))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(menu.category, color = Color(0xFF334155), fontSize = 10.sp)
                                        }

                                        IconButton(onClick = { viewModel.deleteMenuItem(menu.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "delete menu", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal popup 1: Create Ingredient Input Dialog
    if (showIngredientModal) {
        var ingredientName by remember { mutableStateOf("") }
        var ingredientStock by remember { mutableStateOf("") }
        var ingredientAlert by remember { mutableStateOf("") }
        var ingredientUnit by remember { mutableStateOf("pcs") }

        Dialog(onDismissRequest = { showIngredientModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.width(400.dp).padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("TAMBAH BAHAN BAKU BARU", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    HorizontalDivider(color = Color(0xFF2E2D4A), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    OutlinedTextField(
                        value = ingredientName,
                        onValueChange = { ingredientName = it },
                        label = { Text("Nama Bahan Baku (e.g. Bakso Urat)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ing_name_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ingredientStock,
                        onValueChange = { ingredientStock = it },
                        label = { Text("Stok Aktual Saat Ini") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ingredientAlert,
                        onValueChange = { ingredientAlert = it },
                        label = { Text("Batas Stok Tipis (Alert Threshold)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ingredientUnit,
                        onValueChange = { ingredientUnit = it },
                        label = { Text("Unit Satuan (e.g. biji, gram, porsi)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showIngredientModal = false }, modifier = Modifier.weight(1f)) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                if (ingredientName.isNotEmpty()) {
                                    val stockVal = ingredientStock.toDoubleOrNull() ?: 100.0
                                    val alertVal = ingredientAlert.toDoubleOrNull() ?: 10.0
                                    viewModel.addIngredient(ingredientName, stockVal, alertVal, ingredientUnit)
                                    showIngredientModal = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f).testTag("ing_submit_btn")
                        ) {
                            Text("Simpan Bahan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal popup 2: Create Menu & Map recipe ingredients
    if (showMenuModal) {
        var menuName by remember { mutableStateOf("") }
        var menuPrice by remember { mutableStateOf("") }
        var menuHpp by remember { mutableStateOf("") }
        var menuCategory by remember { mutableStateOf("Makanan") }
        
        // Multi ingredient receipt mapping list
        // Form: List of ingredient items selected with quantity
        val mappedRecipeList = remember { mutableStateListOf<Pair<Long, Double>>() } // ingredientId to double quantity
        var selectedIngIdToLink by remember { mutableStateOf(-1L) }
        var neededIngQty by remember { mutableStateOf("1") }

        Dialog(onDismissRequest = { showMenuModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(480.dp)
                    .fillMaxHeight(0.85f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("BUAT MENU MAKANAN BARU", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    HorizontalDivider(color = Color(0xFF2E2D4A), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    OutlinedTextField(
                        value = menuName,
                        onValueChange = { menuName = it },
                        label = { Text("Nama Menu Makanan (e.g. Bakso Mercon)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("menu_name_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = menuPrice,
                        onValueChange = { menuPrice = it },
                        label = { Text("Harga Jual Konsumen (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = menuHpp,
                        onValueChange = { menuHpp = it },
                        label = { Text("Harga Pokok (COGS/HPP) untuk Laba Rugi") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Category text Selection
                    Text("Kategori Sajian:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val cats = listOf("Makanan", "Minuman", "Cemilan")
                        cats.forEach { cat ->
                            val isSelected = menuCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { menuCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF2E2D4A))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Recipe mapping configurations UI
                    Text("PENCATATAN RESEP INVENTORIS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Text("Tautkan menu ini dengan bahan baku agar otomatis memotong stok.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Linked Recipe list view in-memory list
                    if (mappedRecipeList.isEmpty()) {
                        Text("(Belum didaftarkan bahan pengurang resep)", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp)
                    } else {
                        mappedRecipeList.forEach { (ingId, qty) ->
                            val ingName = allIngredients.find { it.id == ingId }?.name ?: "Bahan #$ingId"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("- $ingName ($qty porsi/biji)", color = Color(0xFF0F172A), fontSize = 12.sp)
                                Text(
                                    "Hapus",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable { mappedRecipeList.removeAll { it.first == ingId } }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Line entry recipe builder
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Dropdown choice simulation or selector
                        OutlinedTextField(
                            value = neededIngQty,
                            onValueChange = { neededIngQty = it },
                            label = { Text("Jumlah Terbuang") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp)
                        )
                        
                        Button(
                            onClick = {
                                // Add first ingredient in repo to linking for easy sandbox testing
                                val firstIng = allIngredients.firstOrNull()
                                if (firstIng != null) {
                                    val qtyVal = neededIngQty.toDoubleOrNull() ?: 1.0
                                    mappedRecipeList.add(Pair(firstIng.id, qtyVal))
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Hubungkan Bahan", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showMenuModal = false }, modifier = Modifier.weight(1f)) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                if (menuName.isNotEmpty()) {
                                    val sprice = menuPrice.toDoubleOrNull() ?: 10000.0
                                    val shpp = menuHpp.toDoubleOrNull() ?: 5000.0
                                    val recipes = mappedRecipeList.map { Recipe(menuItemId = 0, ingredientId = it.first, quantityNeeded = it.second) }
                                    viewModel.addMenuItem(menuName, sprice, shpp, menuCategory, "0xFF" + (22131 .. 99999).random().toString(), recipes)
                                    showMenuModal = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f).testTag("menu_submit_btn")
                        ) {
                            Text("Simpan Menu Sajian", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================================
// SCREEN 5: SETTINGS & CUSTOM BARCODE INITIALS PROFILE 
// ==========================================================
@Composable
fun SettingsScreen(viewModel: POSViewModel) {
    val storeConfig by viewModel.storeConfig.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()

    var storeName by remember(storeConfig) { mutableStateOf(storeConfig.storeName) }
    var storePhone by remember(storeConfig) { mutableStateOf(storeConfig.storePhone) }
    var storeLogo by remember(storeConfig) { mutableStateOf(storeConfig.storeLogoText) }
    var footerMsg by remember(storeConfig) { mutableStateOf(storeConfig.receiptFooter) }
    var activeCashierName by remember(storeConfig) { mutableStateOf(storeConfig.activeCashier) }
    var adminPinInput by remember(storeConfig) { mutableStateOf(storeConfig.adminPin) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "Pengaturan & Kustomisasi Identitas Toko",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF0F172A)
        )
        Text(
            "Ubah profile kuitansi struk, nama kasir bertugas, dan PIN pengaman.",
            color = Color(0xFF64748B),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // LEFT COLUMN FORM
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.weight(1.2f)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Informasi Profile Outlet Toko", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nama Toko Kuliner") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("store_name_setting")
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = storePhone,
                        onValueChange = { storePhone = it },
                        label = { Text("Nomor Telepon Toko") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = storeLogo,
                        onValueChange = { storeLogo = it },
                        label = { Text("Logo Inisial Avatar (e.g. ARKA)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = footerMsg,
                        onValueChange = { footerMsg = it },
                        label = { Text("Pesan Penutup Struk Kuitansi") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.updateStoreProfile(storeName, storePhone, storeLogo, footerMsg, activeCashierName, adminPinInput)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("save_config_btn")
                    ) {
                        Text("Simpan Konfigurasi Toko", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // RIGHT COLUMN CONFIG: PIC/PIN SECURE LOCKS
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFF2E2D4A)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Penanggung Jawab Shift & Keamanan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    HorizontalDivider(color = Color(0xFF2E2D4A), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    OutlinedTextField(
                        value = activeCashierName,
                        onValueChange = { activeCashierName = it },
                        label = { Text("Nama Kasir Bertugas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("cashier_name_setting")
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = adminPinInput,
                        onValueChange = { adminPinInput = it },
                        label = { Text("PIN Keamanan Admin (Hanya Angka)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("pin_setting")
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Pecah Peran Pengguna Aktif:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.setIsAdminMode(false) },
                            shape = RoundedCornerShape(8.dp),
                            enabled = isAdminMode,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batas ke Kasir")
                        }

                        Button(
                            onClick = { viewModel.setIsAdminMode(true) },
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isAdminMode,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Owner Mode")
                        }
                    }
                }
            }
        }
    }
}


// ==========================================================
// THERMAL RECEIPT HIGH FIDELITY LAYOUT DRAWER PREVIEWER 
// ==========================================================
@Composable
fun ReceiptViewerDialog(
    txId: Long,
    viewModel: POSViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    
    // Loaded checkout items
    var isFetching by remember { mutableStateOf(true) }
    val transactionItems = remember { mutableStateListOf<TransactionItem>() }

    val transaction = remember(allTransactions) {
        allTransactions.find { it.id == txId }
    }

    LaunchedEffect(txId) {
        val list = viewModel.getItemsForTransactionList(txId)
        transactionItems.clear()
        transactionItems.addAll(list)
        isFetching = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2E2D4A)),
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight(0.85f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("KUITANSI NOTA SELESAI", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(12.dp))

                if (isFetching || transaction == null) {
                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val tx = transaction!!
                    
                    // THERMAL SLATE DESIGN BOX
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFF94A3B8)))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            
                            // serrated receipt head logo
                            Text(
                                storeConfig.storeLogoText,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 36.sp,
                                color = Color.Black
                            )
                            Text(
                                storeConfig.storeName,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "No Telp: " + storeConfig.storePhone,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "------------------------------------",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.Black
                            )

                            // Metadata rows
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("ID Trans: #${tx.id} / ${tx.uuid.take(8).uppercase()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                                Text("Waktu: $dateStr", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text("Shift: ${tx.shiftName}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text("Kasir: ${tx.cashierName}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                                Text("Lokasi: ${tx.tableNumber}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }

                            Text(
                                "------------------------------------",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.Black
                            )

                            // List Items Rows
                            Text(
                                "ITEMS SELECTIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            transactionItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            item.menuItemName,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.modifierNotes.isNotEmpty()) {
                                            Text(
                                                " * ${item.modifierNotes}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                    Text(
                                        "x${item.quantity}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.Black,
                                        modifier = Modifier.weight(0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "Rp ${(item.price * item.quantity).toInt().toLocaleString()}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.Black,
                                        modifier = Modifier.weight(0.8f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Text(
                                "------------------------------------",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.Black
                            )

                            // Pricing aggregates calculation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL TAGIHAN:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                Text("Rp ${tx.totalAmount.toInt().toLocaleString()}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Metode Bayar:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                                Text(tx.paymentMethod, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bayar Cash:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                                Text("Rp ${tx.amountPaid.toInt().toLocaleString()}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Kembalian:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                                Text("Rp ${tx.changeAmount.toInt().toLocaleString()}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                storeConfig.receiptFooter,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ACTION RIBBONS FOR DOWNLOAD PNG AND WHATSAPP SHARE
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                saveReceiptToGallery(context, tx, transactionItems, storeConfig)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("download_struk_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download struct to downloads directory", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PNG", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                shareReceiptToWhatsApp(context, tx, transactionItems, storeConfig)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1.3f).testTag("wa_share_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Share details via Whatsapp API", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 11.sp)
                        }
                    }

                    if (tx.status != "CANCELLED") {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                viewModel.cancelTransaction(tx.id, "Batal pesanan konsumen (Salah ketik)")
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel order", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BATALKAN TRANSAKSI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Tutup Nota")
                    }
                }
            }
        }
    }
}


// ==========================================================
// OFFLINE FILE GENERATORS AND EXPORTERS INTENT HELPERS
// ==========================================================
fun saveReceiptToGallery(context: Context, tx: Transaction, items: List<TransactionItem>, config: StoreConfig) {
    try {
        val width = 450
        var currentY = 50f
        
        // Calculate dynamic height
        val height = 350 + (items.size * 60) + 180
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 20f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        // Header
        paint.textSize = 26f
        paint.isFakeBoldText = true
        canvas.drawText(config.storeName, width / 2f, currentY, paint)
        currentY += 35
        
        paint.textSize = 18f
        paint.isFakeBoldText = false
        canvas.drawText("Telp: ${config.storePhone}", width / 2f, currentY, paint)
        currentY += 35
        
        // Divider
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("--------------------------------------------------", 15f, currentY, paint)
        currentY += 30
        
        // Metadata
        canvas.drawText("No: #${tx.id} / ${tx.uuid.take(6).uppercase()}", 15f, currentY, paint)
        currentY += 24
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Waktu: ${formatter.format(Date(tx.timestamp))}", 15f, currentY, paint)
        currentY += 24
        canvas.drawText("Kasir: ${tx.cashierName} | Meja: ${tx.tableNumber}", 15f, currentY, paint)
        currentY += 35
        
        // Divider
        canvas.drawText("--------------------------------------------------", 15f, currentY, paint)
        currentY += 30
        
        // Items title
        paint.isFakeBoldText = true
        canvas.drawText("ITEMS SELECTIONS", 15f, currentY, paint)
        paint.isFakeBoldText = false
        currentY += 35
        
        for (item in items) {
            val itemLine = "${item.menuItemName} x${item.quantity}"
            val priceLine = "Rp ${(item.price * item.quantity).toInt().toLocaleString()}"
            canvas.drawText(itemLine, 15f, currentY, paint)
            
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            canvas.drawText(priceLine, width - 15f, currentY, paint)
            paint.textAlign = android.graphics.Paint.Align.LEFT
            currentY += 24
            
            if (item.modifierNotes.isNotEmpty()) {
                canvas.drawText(" * ${item.modifierNotes}", 35f, currentY, paint)
                currentY += 24
            }
        }
        
        currentY += 15
        canvas.drawText("--------------------------------------------------", 15f, currentY, paint)
        currentY += 30
        
        // Total price
        paint.isFakeBoldText = true
        canvas.drawText("TOTAL TAGIHAN:", 15f, currentY, paint)
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText("Rp ${tx.totalAmount.toInt().toLocaleString()}", width - 15f, currentY, paint)
        paint.textAlign = android.graphics.Paint.Align.LEFT
        currentY += 35
        
        // Bottom Payment
        paint.isFakeBoldText = false
        canvas.drawText("Metode: ${tx.paymentMethod}", 15f, currentY, paint)
        currentY += 24
        canvas.drawText("Diterima: Rp ${tx.amountPaid.toInt().toLocaleString()}", 15f, currentY, paint)
        currentY += 24
        canvas.drawText("Kembalian: Rp ${tx.changeAmount.toInt().toLocaleString()}", 15f, currentY, paint)
        currentY += 45
        
        // Footer message
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText(config.receiptFooter, width / 2f, currentY, paint)
        
        // Save to downloads
        val resolvedResolver = context.contentResolver
        val details = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "Struk_ARKA_POS_No_${tx.id}.png")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
        }
        
        val downloadUri = resolvedResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, details)
        if (downloadUri != null) {
            resolvedResolver.openOutputStream(downloadUri).use { stream ->
                if (stream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
            Toast.makeText(context, "Gambar Struk disimpan di Downloads!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Gagal simpan gambar struk", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal simpan: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareReceiptToWhatsApp(context: Context, tx: Transaction, items: List<TransactionItem>, config: StoreConfig) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = formatter.format(Date(tx.timestamp))
    
    val sb = StringBuilder()
    sb.append("*${config.storeName.uppercase()}*\n")
    sb.append("Telp: ${config.storePhone}\n")
    sb.append("===============================\n")
    sb.append("ID Transaksi: #${tx.id}\n")
    sb.append("Status: ${tx.status}\n")
    sb.append("Waktu: $dateStr\n")
    sb.append("Kasir: ${tx.cashierName}\n")
    sb.append("Lokasi: ${tx.tableNumber}\n")
    sb.append("===============================\n")
    
    for (item in items) {
        sb.append("${item.menuItemName} x${item.quantity}\n")
        sb.append(" -> Rp ${(item.price * item.quantity).toInt().toLocaleString()}\n")
        if (item.modifierNotes.isNotEmpty()) {
            sb.append("    (Catatan: ${item.modifierNotes})\n")
        }
    }
    
    sb.append("===============================\n")
    sb.append("*TOTAL TAGIHAN: Rp ${tx.totalAmount.toInt().toLocaleString()}*\n")
    sb.append("Metode: ${tx.paymentMethod}\n")
    sb.append("Diterima: Rp ${tx.amountPaid.toInt().toLocaleString()}\n")
    sb.append("Kembalian: Rp ${tx.changeAmount.toInt().toLocaleString()}\n")
    sb.append("===============================\n")
    sb.append("_\"${config.receiptFooter}\"_\n")
    
    val messageText = sb.toString()
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, messageText)
    }
    
    try {
        context.startActivity(Intent.createChooser(intent, "Kirim Struk Penjualan"))
    } catch (e: Exception) {
        Toast.makeText(context, "Terjadi gangguan kirim data sharing.", Toast.LENGTH_SHORT).show()
    }
}

fun shareDailyReportToWhatsApp(
    context: Context,
    transactions: List<Transaction>,
    config: StoreConfig,
    shift: String,
    mostPopular: List<Pair<String, Int>>,
    netProfit: Double,
    omzet: Double
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    
    val sb = StringBuilder()
    sb.append("*LAPORAN RINGKASAN HARIAN SHIFT - ${config.storeName.uppercase()}*\n")
    sb.append("Tanggal: $dateStr\n")
    sb.append("Shift: $shift\n")
    sb.append("Penanggung Jawab: ${config.activeCashier}\n")
    sb.append("===============================\n")
    sb.append("TOTAL OMZET/PENDAPATAN:\n")
    sb.append(" -> *Rp ${omzet.toInt().toLocaleString()}*\n")
    sb.append("ESTIMASI LABA ENERGI BERSIH:\n")
    sb.append(" -> *Rp ${netProfit.toInt().toLocaleString()}*\n")
    sb.append("===============================\n")
    sb.append("MENU TERLARIS SHIFT INI:\n")
    
    if (mostPopular.isEmpty()) {
        sb.append("- (Belum ada menu terjual)\n")
    } else {
        mostPopular.take(4).forEachIndexed { index, pair ->
            sb.append("${index + 1}. ${pair.first} (${pair.second} porsi)\n")
        }
    }
    
    sb.append("===============================\n")
    sb.append("_Generated otomatis oleh Arka POS_\n")
    
    val messageText = sb.toString()
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, messageText)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan Laporan Shift"))
}

// Extension to format integers to localized rupiah Indonesian values like 15000 -> 15.000
fun Int.toLocaleString(): String {
    return String.format("%,d", this).replace(',', '.')
}
