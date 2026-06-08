package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CartItem
import com.example.data.MenuItem

/**
 * An advanced, highly interactive Product Selection Grid for the POS checkout screen.
 * Centered around Material Design 3 guidelines:
 * - Clear category filtering pills.
 * - Dynamic product search with clean visual clearing.
 * - Quick-Add interactive visual adjustment pills directly on the cards.
 * - Visual availability indicators (sold out overlays).
 * - Compact and high-fidelity representations tailored for instant POS usage.
 */
@Composable
fun ProductSelectionGrid(
    allMenus: List<MenuItem>,
    activeCart: List<CartItem>,
    searchQuery: String,
    selectedCategory: String,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onItemClick: (MenuItem) -> Unit,
    onQuickAddClick: (MenuItem) -> Unit,
    onUpdateCartQuantity: (index: Int, newQty: Int) -> Unit,
    modifier: Modifier = Modifier,
    activeTableNum: String = "Take Away"
) {
    // 1. FILTER REFINEMENTS
    val filteredMenus = remember(allMenus, searchQuery, selectedCategory) {
        allMenus.filter { item ->
            val matchQuery = item.name.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == "Semua" || item.category == selectedCategory
            matchQuery && matchCat
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        
        // 2. HEADER SEARCH & SELECTOR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Pesan Menu Hidangan",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "Meja Aktif: $activeTableNum",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Cari bakso/minuman...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search icon",
                        tint = Color(0xFF64748B)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear, 
                                contentDescription = "Clear search query",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .width(260.dp)
                    .testTag("product_search")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. HORIZONTAL CATEGORY PILLS FILTER CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("Semua", "Makanan", "Minuman", "Cemilan")
            categories.forEach { cat ->
                val isCatSelected = selectedCategory == cat
                
                Surface(
                    onClick = { onCategorySelect(cat) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCatSelected) MaterialTheme.colorScheme.primary else Color.White,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isCatSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .testTag("cat_chip_${cat.lowercase()}")
                        .animateContentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = cat,
                            color = if (isCatSelected) Color.White else Color(0xFF475569),
                            fontSize = 13.sp,
                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. MENU ITEMS SELECTION GRID
        if (filteredMenus.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color(0xFFF1F5F9)), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox, 
                        contentDescription = "Empty state icon", 
                        modifier = Modifier.size(56.dp), 
                        tint = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Belum ada menu di kategori ini", 
                        color = Color(0xFF64748B), 
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Coba masukkan query pencarian lain atau pilih kategori yang berbeda.", 
                        color = Color(0xFF94A3B8), 
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 175.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredMenus, key = { it.id }) { item ->
                    
                    // Match with active cart (with no modifiers) for high-fidelity interactive Quick-Add counter
                    val cartMatchResult = remember(activeCart, item.id) {
                        val index = activeCart.indexOfFirst { it.menuItem.id == item.id && it.modifiersAndNotes.isEmpty() }
                        if (index != -1) {
                            Pair(index, activeCart[index].quantity)
                        } else {
                            Pair(-1, 0)
                        }
                    }
                    val cartIndex = cartMatchResult.first
                    val cartQty = cartMatchResult.second

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (cartQty > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (cartQty > 0) 3.dp else 1.dp,
                                shape = RoundedCornerShape(16.dp),
                                clip = false
                            )
                            .clickable(enabled = item.isAvailable) { onItemClick(item) }
                            .testTag("menu_card_${item.id}")
                    ) {
                        Column {
                            // Dynamic visually stunning placeholder image with color values
                            val itemColor = remember(item.imageUrlOrColor) {
                                try {
                                    Color(android.graphics.Color.parseColor(item.imageUrlOrColor))
                                } catch (e: Exception) {
                                    Color(0xFF6750A4).copy(alpha = 0.4f)
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(itemColor, itemColor.copy(alpha = 0.85f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Monogram / Culinary typography
                                Text(
                                    text = item.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 40.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )

                                // Soft overlay category label
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = item.category.uppercase(), 
                                        color = Color.White, 
                                        fontSize = 9.sp, 
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                // Interactive Counter Float Badge in Top Corner
                                if (cartQty > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(26.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .border(width = 1.5.dp, color = Color.White, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cartQty.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Unavailable indicator over the image
                                if (!item.isAvailable) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                "HABIS",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Dynamic detailed name and price section
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (item.isAvailable) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Rp " + item.price.toInt().toLocaleString(),
                                    color = if (item.isAvailable) MaterialTheme.colorScheme.secondary else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick-Add Section
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!item.isAvailable) {
                                        // Disabled placeholder
                                        Text(
                                            "Tidak Tersedia",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else if (cartQty > 0) {
                                        // Polished compact numeric dial control directly on the grid
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .padding(horizontal = 2.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            IconButton(
                                                onClick = { onUpdateCartQuantity(cartIndex, cartQty - 1) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Remove,
                                                    contentDescription = "Kurangi item",
                                                    tint = Color(0xFF0F172A),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            
                                            Text(
                                                text = cartQty.toString(),
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(
                                                onClick = { onUpdateCartQuantity(cartIndex, cartQty + 1) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Tambah item",
                                                    tint = Color(0xFF0F172A),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Quick Add instant addition button
                                        Button(
                                            onClick = { onQuickAddClick(item) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("quick_add_${item.id}")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Tambah default",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Cepat Tambah",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
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
    }
}

// Local formatting extension so component compiles independently
private fun Int.toLocaleString(): String {
    return String.format("%,d", this).replace(',', '.')
}
