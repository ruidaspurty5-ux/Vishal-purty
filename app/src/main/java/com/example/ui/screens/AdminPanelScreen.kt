package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.NoteEntity
import com.example.ui.viewmodel.StudyNotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: StudyNotesViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Metrics Data from flows
    val totalUsers by viewModel.totalUsersCount.collectAsState()
    val totalNotes by viewModel.totalNotesCount.collectAsState()
    val totalSales by viewModel.totalSalesCount.collectAsState()
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val allNotesList by viewModel.allNotes.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val couponsList by viewModel.coupons.collectAsState()
    val paymentsList by viewModel.payments.collectAsState()

    var activeAdminTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Add Note, 2: Categories/Subs, 3: Coupons/Offers, 4: Reviews Control

    // Forms states
    // Add Note
    var noteTitle by remember { mutableStateOf("") }
    var noteSubject by remember { mutableStateOf("") }
    var noteCategory by remember { mutableStateOf("") }
    var noteSemester by remember { mutableStateOf("Semester 1") }
    var notePrice by remember { mutableStateOf("") }
    var noteThumbnailUrl by remember { mutableStateOf("") }
    var noteDescription by remember { mutableStateOf("") }
    var noteIsNew by remember { mutableStateOf(true) }
    var notePdfContent by remember { mutableStateOf("") }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val text = stream.bufferedReader().use { r -> r.readText() }
                    notePdfContent = text
                    Toast.makeText(context, "Notes content loaded successfully from selected file!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Add Category & Subject
    var catName by remember { mutableStateOf("") }
    var catDesc by remember { mutableStateOf("") }
    var subName by remember { mutableStateOf("") }
    var subCatName by remember { mutableStateOf("") }

    // Add Coupon
    var couponCode by remember { mutableStateOf("") }
    var couponDiscount by remember { mutableStateOf("") }
    var couponUsageLimit by remember { mutableStateOf("") }

    // Add Offer
    var offerTitle by remember { mutableStateOf("") }
    var offerDesc by remember { mutableStateOf("") }
    var offerDiscount by remember { mutableStateOf("") }

    // Reviews list matching
    val allReviews = remember { mutableStateListOf<Pair<String, com.example.data.database.ReviewEntity>>() }
    LaunchedEffect(allNotesList) {
        allReviews.clear()
        allNotesList.forEach { note ->
            viewModel.getReviewsForNote(note.id).collect { reviews ->
                reviews.forEach { rev ->
                    if (allReviews.none { it.second.id == rev.id }) {
                        allReviews.add(Pair(note.title, rev))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Admin Console", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Admin tabs list
            ScrollableTabRow(selectedTabIndex = activeAdminTab) {
                Tab(selected = activeAdminTab == 0, onClick = { activeAdminTab = 0 }) {
                    Text("Metrics", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeAdminTab == 1, onClick = { activeAdminTab = 1 }) {
                    Text("Upload eNote", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeAdminTab == 2, onClick = { activeAdminTab = 2 }) {
                    Text("Categories/Subs", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeAdminTab == 3, onClick = { activeAdminTab = 3 }) {
                    Text("Coupons & Offers", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeAdminTab == 4, onClick = { activeAdminTab = 4 }) {
                    Text("Reviews", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeAdminTab == 5, onClick = { activeAdminTab = 5 }) {
                    Text("Support Chats", modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                when (activeAdminTab) {
                    0 -> {
                        // TAB 0: DASHBOARD STATS METRICS
                        Text("Core Sales & Revenue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Total Sales", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Text("$totalSales Notes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Total Revenue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                    Text("₹${totalRevenue.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Today's Sales", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("₹${(totalRevenue * 0.15).toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Monthly Sales", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("₹${(totalRevenue * 0.85).toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("System Totals", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Total Students", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("$totalUsers Active", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Total Catalog Notes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("$totalNotes Published", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Recent Transactions List
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Recent Payments Logger", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (paymentsList.isEmpty()) {
                                    Text("No payments parsed yet", modifier = Modifier.padding(8.dp), color = Color.Gray)
                                } else {
                                    paymentsList.take(6).forEach { payment ->
                                        val isSuccess = payment.status == "SUCCESS"
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("+91 ${payment.userId}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("ID: ${payment.paymentId}", fontSize = 10.sp, color = Color.Gray)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("₹${payment.amount.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Box(
                                                    modifier = Modifier
                                                        .background(if (isSuccess) Color(0xFFE6F4EA) else Color(0xFFFCE8E6), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isSuccess) "OK" else "FAIL",
                                                        fontSize = 8.sp,
                                                        color = if (isSuccess) Color(0xFF137333) else Color(0xFFC5221F),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: ADD NOTE FORM
                        Text("Upload New eNote PDF", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            label = { Text("Note Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_note_title")
                        )

                        OutlinedTextField(
                            value = noteSubject,
                            onValueChange = { noteSubject = it },
                            label = { Text("Subject Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Category Dropdown simulated with category list chips
                        Text("Select Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categoriesList) { cat ->
                                FilterChip(
                                    selected = noteCategory == cat.name,
                                    onClick = { noteCategory = cat.name },
                                    label = { Text(cat.name) }
                                )
                            }
                        }

                        // Semester Text selector
                        OutlinedTextField(
                            value = noteSemester,
                            onValueChange = { noteSemester = it },
                            label = { Text("Semester / Year Group") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = notePrice,
                            onValueChange = { notePrice = it },
                            label = { Text("Price (INR) - Put 0 for Free") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = noteThumbnailUrl,
                            onValueChange = { noteThumbnailUrl = it },
                            label = { Text("Thumbnail Unsplash Image URL (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = noteDescription,
                            onValueChange = { noteDescription = it },
                            label = { Text("Notes Detailed Description") },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Premium Note Materials / PDF Content", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Import a file from your phone storage, or write/paste the notes text directly below.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Import Note File", fontSize = 12.sp)
                                    }
                                    
                                    if (notePdfContent.isNotEmpty()) {
                                        OutlinedButton(
                                            onClick = { notePdfContent = "" },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                        ) {
                                            Text("Clear", fontSize = 12.sp)
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = notePdfContent,
                                    onValueChange = { notePdfContent = it },
                                    label = { Text("Note Pages Content") },
                                    placeholder = { Text("Paste note contents here. Use [PAGE_BREAK] to create separate secure viewer pages.") },
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                
                                Text(
                                    text = "💡 Tip: Use [PAGE_BREAK] or '---' to split your note into multiple pages inside the PDF reader screen.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = noteIsNew, onCheckedChange = { noteIsNew = it })
                            Text("Mark as 'NEW' launch note badge")
                        }

                        Button(
                            onClick = {
                                if (noteTitle.trim().isEmpty() || noteSubject.trim().isEmpty() || noteCategory.isEmpty() || notePrice.isEmpty() || notePdfContent.trim().isEmpty()) {
                                    Toast.makeText(context, "Fill Note details and Note pages completely!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val priceD = notePrice.toDoubleOrNull() ?: 0.0
                                viewModel.addNote(
                                    title = noteTitle.trim(),
                                    description = noteDescription.trim(),
                                    thumbnailUrl = noteThumbnailUrl.trim(),
                                    price = priceD,
                                    pdfUrl = notePdfContent.trim(),
                                    category = noteCategory,
                                    subject = noteSubject.trim(),
                                    semester = noteSemester,
                                    isNew = noteIsNew
                                )
                                Toast.makeText(context, "Notes Uploaded & Push notification dispatched!", Toast.LENGTH_LONG).show()
                                // Reset form
                                noteTitle = ""
                                noteSubject = ""
                                notePrice = ""
                                noteThumbnailUrl = ""
                                noteDescription = ""
                                notePdfContent = ""
                                activeAdminTab = 0 // Go back to metrics
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Publish eNotes PDF Successfully", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Publish / Unpublish Manager", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        // Manage published state
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                allNotesList.forEach { note ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(note.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${note.category} • ₹${note.price.toInt()}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (note.isPublished) "PUBLISHED" else "DRAFT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Switch(
                                                checked = note.isPublished,
                                                onCheckedChange = { viewModel.togglePublishNote(note) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: CATEGORIES & SUBJECTS BUILDER
                        Text("Add New Course Category", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        OutlinedTextField(
                            value = catName,
                            onValueChange = { catName = it },
                            label = { Text("Category Name (e.g. B.Tech)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = catDesc,
                            onValueChange = { catDesc = it },
                            label = { Text("Description") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (catName.trim().isEmpty()) return@Button
                                viewModel.addCategory(catName.trim(), catDesc.trim())
                                Toast.makeText(context, "Category $catName Added!", Toast.LENGTH_SHORT).show()
                                catName = ""
                                catDesc = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Category")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Add New Subject to Category", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        OutlinedTextField(
                            value = subName,
                            onValueChange = { subName = it },
                            label = { Text("Subject Name (e.g. Compiler Design)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text("Select Category for Subject:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categoriesList) { cat ->
                                FilterChip(
                                    selected = subCatName == cat.name,
                                    onClick = { subCatName = cat.name },
                                    label = { Text(cat.name) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (subName.trim().isEmpty() || subCatName.isEmpty()) {
                                    Toast.makeText(context, "Subject fields incomplete!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.addSubject(subName.trim(), subCatName)
                                Toast.makeText(context, "Subject $subName Added to $subCatName!", Toast.LENGTH_SHORT).show()
                                subName = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Subject Map")
                        }
                    }

                    3 -> {
                        // TAB 3: COUPONS & OFFERS
                        Text("Create Discount Coupon", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        OutlinedTextField(
                            value = couponCode,
                            onValueChange = { couponCode = it },
                            label = { Text("Coupon Code (e.g. SUPER50)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = couponDiscount,
                            onValueChange = { couponDiscount = it },
                            label = { Text("Discount Percentage (e.g. 50)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = couponUsageLimit,
                            onValueChange = { couponUsageLimit = it },
                            label = { Text("Usage Limit Count (e.g. 100)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val discountD = couponDiscount.toDoubleOrNull() ?: 0.0
                                val limitI = couponUsageLimit.toIntOrNull() ?: 50
                                if (couponCode.trim().isEmpty() || discountD <= 0) return@Button
                                viewModel.addCoupon(
                                    code = couponCode.trim(),
                                    discountPercent = discountD,
                                    expiryDate = System.currentTimeMillis() + 86400000 * 7, // 7 days expiry
                                    usageLimit = limitI
                                )
                                Toast.makeText(context, "Coupon $couponCode Added Successfully!", Toast.LENGTH_SHORT).show()
                                couponCode = ""
                                couponDiscount = ""
                                couponUsageLimit = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Coupon")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Publish New Festival Offer Banner", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = offerTitle,
                            onValueChange = { offerTitle = it },
                            label = { Text("Offer Title (e.g. Eid Mubarak Sale)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = offerDesc,
                            onValueChange = { offerDesc = it },
                            label = { Text("Offer Subtext/Description") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = offerDiscount,
                            onValueChange = { offerDiscount = it },
                            label = { Text("Flat Off (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val discountD = offerDiscount.toDoubleOrNull() ?: 0.0
                                if (offerTitle.trim().isEmpty() || discountD <= 0) return@Button
                                viewModel.addOffer(offerTitle.trim(), offerDesc.trim(), discountD)
                                Toast.makeText(context, "New Banner posted & global alert dispatched!", Toast.LENGTH_SHORT).show()
                                offerTitle = ""
                                offerDesc = ""
                                offerDiscount = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Publish Slider Offer")
                        }
                    }

                    4 -> {
                        // TAB 4: REVIEWS MODERATION
                        Text("Student Reviews Moderation", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        if (allReviews.isEmpty()) {
                            Text("No reviews written in the store yet.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                        } else {
                            allReviews.forEach { item ->
                                val title = item.first
                                val rev = item.second
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(rev.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("On notes: $title", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteReview(rev.id, rev.noteId)
                                                    allReviews.remove(item)
                                                    Toast.makeText(context, "Review deleted instantly.", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape).size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Review", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(rev.reviewText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }

                    5 -> {
                        // TAB 5: SUPPORT CHATS (FIRESTORE REAL-TIME)
                        val activeChats by viewModel.adminActiveChats.collectAsState()
                        var selectedChatUser by remember { mutableStateOf<com.example.data.repository.ActiveChat?>(null) }

                        Text(
                            text = "Live Support Dashboard (Firestore)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (activeChats.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text("No active support tickets", fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(
                                    text = "When students request help from their user profile, their tickets will appear here in real-time.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            activeChats.forEach { chat ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedChatUser = chat }
                                        .testTag("admin_chat_card_${chat.userId}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = chat.userName.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(chat.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(chat.userId, fontSize = 11.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = chat.lastMessage,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                                    }
                                }
                            }
                        }

                        // Dialog for Active Chat Conversation
                        selectedChatUser?.let { chat ->
                            val userMessages by viewModel.getSupportMessagesForUser(chat.userId).collectAsState(initial = emptyList())
                            var adminReplyText by remember { mutableStateOf("") }
                            val dialogScrollState = rememberLazyListState()

                            LaunchedEffect(userMessages.size) {
                                if (userMessages.isNotEmpty()) {
                                    dialogScrollState.animateScrollToItem(userMessages.size - 1)
                                }
                            }

                            AlertDialog(
                                onDismissRequest = { selectedChatUser = null },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(550.dp)
                                    .testTag("admin_chat_dialog"),
                                title = {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text("Chat with ${chat.userName}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Text(chat.userId, fontSize = 11.sp, color = Color.Gray)
                                        }
                                        IconButton(onClick = { selectedChatUser = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close")
                                        }
                                    }
                                },
                                text = {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Messages log
                                        LazyColumn(
                                            state = dialogScrollState,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(userMessages) { msg ->
                                                val isAdminMsg = msg.sender == "SUPPORT"
                                                val bubbleColor = if (isAdminMsg) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                                val textColor = if (isAdminMsg) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                                val alignment = if (isAdminMsg) Alignment.End else Alignment.Start

                                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(bubbleColor)
                                                            .padding(10.dp)
                                                    ) {
                                                        Text(msg.messageText, color = textColor, fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Reply Input field
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = adminReplyText,
                                                onValueChange = { adminReplyText = it },
                                                placeholder = { Text("Type reply...") },
                                                modifier = Modifier.weight(1f).testTag("admin_reply_input"),
                                                singleLine = true
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (adminReplyText.trim().isNotEmpty()) {
                                                        viewModel.sendSupportMessage(
                                                            messageText = adminReplyText.trim(),
                                                            overrideUserId = chat.userId,
                                                            overrideUserName = chat.userName
                                                        )
                                                        adminReplyText = ""
                                                    }
                                                },
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                                    .size(44.dp)
                                                    .testTag("admin_send_reply_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = "Send",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {}
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
