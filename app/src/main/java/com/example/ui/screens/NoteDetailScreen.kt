package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.NoteEntity
import com.example.data.database.ReviewEntity
import com.example.ui.viewmodel.StudyNotesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    viewModel: StudyNotesViewModel,
    onNavigateToPdfViewer: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val noteState = viewModel.publishedNotes.collectAsState().value.firstOrNull { it.id == noteId }
    val wishlist by viewModel.wishlist.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val couponError by viewModel.couponError.collectAsState()

    val isPurchased = remember(purchases, noteId) {
        purchases.any { it.noteId == noteId }
    }

    var couponCodeInput by remember { mutableStateOf("") }
    var showRazorpaySheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Details, 1: Reviews

    // Review creation states
    var ratingStars by remember { mutableStateOf(5) }
    var reviewText by remember { mutableStateOf("") }

    val reviewsFlow = remember(noteId) { viewModel.getReviewsForNote(noteId) }
    val reviews by reviewsFlow.collectAsState(initial = emptyList())

    if (noteState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Notes not found")
        }
        return
    }

    val finalPrice = remember(noteState.price, appliedCoupon) {
        val coupon = appliedCoupon
        if (coupon != null) {
            val discount = noteState.price * (coupon.discountPercent / 100.0)
            noteState.price - discount
        } else {
            noteState.price
        }
    }

    val isWishlisted = wishlist.any { it.id == noteId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(noteState.subject, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWishlist(noteId) }) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) Color.Red else MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Action button bar
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isPurchased && noteState.price > 0) {
                        Column(modifier = Modifier.padding(end = 4.dp)) {
                            Text("Total Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            Row(verticalAlignment = Alignment.Bottom) {
                                if (appliedCoupon != null) {
                                    Text(
                                        text = "₹${noteState.price.toInt()}",
                                        fontSize = 13.sp,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(end = 4.dp, bottom = 2.dp)
                                    )
                                }
                                Text(
                                    text = "₹${finalPrice.toInt()}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Preview button
                        OutlinedButton(
                            onClick = {
                                onNavigateToPdfViewer("$noteId?preview=true")
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(52.dp)
                                .testTag("action_preview_notes"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Preview",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Purchase/Open Button
                    Button(
                        onClick = {
                            if (isPurchased || noteState.price <= 0) {
                                // Unlock and read
                                if (noteState.price <= 0 && !isPurchased) {
                                    viewModel.recordSuccessfulPayment(noteId, "pay_free_unlocked", 0.0)
                                    Toast.makeText(context, "Free note unlocked!", Toast.LENGTH_SHORT).show()
                                }
                                onNavigateToPdfViewer(noteId)
                            } else {
                                showRazorpaySheet = true
                            }
                        },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(52.dp)
                            .testTag("action_purchase_or_view"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPurchased || noteState.price <= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPurchased || noteState.price <= 0) Icons.Default.MenuBook else Icons.Default.Payment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isPurchased) "Open Notes" else if (noteState.price <= 0) "Unlock Free" else "Buy Now",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Image & Essential Details Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            ) {
                AsyncImage(
                    model = noteState.thumbnail,
                    contentDescription = noteState.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
                
                // Overlay text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.tertiary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = noteState.semester,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = noteState.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tabs for Sectioning (Details, Reviews)
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Description", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Reviews (${reviews.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Details Screen Section
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Stats Block
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Course", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(noteState.category, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Format", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Secure PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Watermark", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Yes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Description text
                    Text(
                        text = "About these Notes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = noteState.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )

                    // Secure Protection warning
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Anti-Piracy Safeguards Enabled",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Screenshots, recording, copying, printing, sharing and downloading are fully disabled. Each page contains dynamic watermarks of your credentials.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Coupon Code Input (Only visible if not purchased yet and price > 0)
                    if (!isPurchased && noteState.price > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Apply Coupon Code",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = couponCodeInput,
                                onValueChange = { couponCodeInput = it },
                                placeholder = { Text("e.g. STUDY25") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                enabled = appliedCoupon == null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("coupon_input")
                            )

                            if (appliedCoupon == null) {
                                Button(
                                    onClick = {
                                        viewModel.applyCouponCode(couponCodeInput.trim())
                                        focusManager.clearFocus()
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Apply")
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.clearCoupon()
                                        couponCodeInput = ""
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Text("Remove", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // Success/Failure Coupon indicators
                        appliedCoupon?.let { coupon ->
                            Text(
                                text = "Code applied successfully! Extra ${coupon.discountPercent.toInt()}% off.",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        couponError?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                // Reviews & Ratings tab
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Create Review Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Submit Your Review", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            
                            // Select Star Rating
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { star ->
                                    val isSelected = star <= ratingStars
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Star $star",
                                        tint = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { ratingStars = star }
                                    )
                                }
                            }

                            // Review message
                            OutlinedTextField(
                                value = reviewText,
                                onValueChange = { reviewText = it },
                                placeholder = { Text("Write your honest feedback here...") },
                                maxLines = 3,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (currentUser == null) {
                                        Toast.makeText(context, "Please login first to submit review", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (reviewText.trim().isEmpty()) {
                                        Toast.makeText(context, "Review text cannot be empty", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.addReview(noteId, ratingStars, reviewText.trim())
                                    reviewText = ""
                                    focusManager.clearFocus()
                                    Toast.makeText(context, "Review submitted. Thank you!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Post Review")
                            }
                        }
                    }

                    // Reviews List
                    if (reviews.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No reviews written yet. Be the first!", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    } else {
                        reviews.forEach { rev ->
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
                                        Text(rev.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Row {
                                            (1..5).forEach { star ->
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (star <= rev.rating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(rev.reviewText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }

        // Razorpay Checkout and Verification Dialogs
        val verificationState by viewModel.verificationState.collectAsState()

        if (verificationState !is com.example.ui.viewmodel.VerificationUIState.Idle) {
            Dialog(
                onDismissRequest = {
                    if (verificationState is com.example.ui.viewmodel.VerificationUIState.Success || 
                        verificationState is com.example.ui.viewmodel.VerificationUIState.Error) {
                        viewModel.resetVerificationState()
                    }
                },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (val state = verificationState) {
                            is com.example.ui.viewmodel.VerificationUIState.Loading -> {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Secure Backend Verification",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Verifying Razorpay payment signature securely on backend proxy and unlocking notes in Firestore...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            is com.example.ui.viewmodel.VerificationUIState.Success -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "Verification Success!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Signature successfully verified and purchase registered in Firestore! Your study materials are unlocked.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.resetVerificationState()
                                        showRazorpaySheet = false
                                        onNavigateToPdfViewer(noteId)
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("open_study_notes_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Open Study Notes")
                                }
                            }
                            is com.example.ui.viewmodel.VerificationUIState.Error -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "Verification Failed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = state.message,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.resetVerificationState()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("dismiss_verification_error_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Dismiss")
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        if (showRazorpaySheet && verificationState is com.example.ui.viewmodel.VerificationUIState.Idle) {
            Dialog(
                onDismissRequest = { showRazorpaySheet = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1736)) // Dark Purple Razorpay Theme
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Razorpay Header Brand
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF0F75FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("R", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                            Text("Razorpay Secure", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        
                        Text(
                            text = noteState.title,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Amount to Pay: ₹${finalPrice.toInt()}",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Student ID: ${currentUser?.mobileNumber}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                Text("Full Name: ${currentUser?.name}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                if (appliedCoupon != null) {
                                    Text("Discount Applied: ${appliedCoupon?.discountPercent?.toInt()}% (${appliedCoupon?.code})", color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Trigger actual Razorpay Checkout integration
                        val activity = remember(context) { context as? android.app.Activity }
                        Button(
                            onClick = {
                                if (activity != null) {
                                    launchRazorpayPayment(
                                        activity = activity,
                                        noteId = noteId,
                                        noteTitle = noteState.title,
                                        amount = finalPrice,
                                        userMobile = currentUser?.mobileNumber ?: "",
                                        viewModel = viewModel
                                    )
                                } else {
                                    Toast.makeText(context, "Hosting Activity not found.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("pay_razorpay_sdk_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F75FF))
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                                Text("Pay via Razorpay SDK Gateway", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Simulation button with secure verification
                        OutlinedButton(
                            onClick = {
                                val simulatedOrderId = "order_" + java.util.UUID.randomUUID().toString().take(12)
                                val simulatedPaymentId = "pay_rzp_sim_" + java.util.UUID.randomUUID().toString().take(12)
                                
                                // Compute signature locally for simulated verification
                                val keySecret = com.example.BuildConfig.RAZORPAY_KEY_SECRET
                                val payload = "$simulatedOrderId|$simulatedPaymentId"
                                val simulatedSignature = try {
                                    val mac = javax.crypto.Mac.getInstance("HmacSHA256")
                                    val secretKeySpec = javax.crypto.spec.SecretKeySpec(keySecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
                                    mac.init(secretKeySpec)
                                    val rawHmac = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
                                    rawHmac.joinToString("") { byte -> String.format("%02x", byte) }
                                } catch (e: Exception) {
                                    "failed_simulated_sig"
                                }

                                viewModel.startPaymentSession(noteId, finalPrice)
                                viewModel.verifySecurePayment(
                                    noteId = noteId,
                                    paymentId = simulatedPaymentId,
                                    orderId = simulatedOrderId,
                                    signature = simulatedSignature,
                                    paidAmount = finalPrice
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("simulate_hmac_signature_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Simulate Secure HMAC Signature", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.recordFailedPayment(noteId, "User cancelled Razorpay checkout flow", finalPrice)
                                showRazorpaySheet = false
                                Toast.makeText(context, "Payment Failed. Note remained locked.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("cancel_payment_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Red),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Cancel / Decline Payment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Invokes the actual Razorpay SDK checkout activity
 */
fun launchRazorpayPayment(
    activity: android.app.Activity,
    noteId: String,
    noteTitle: String,
    amount: Double,
    userMobile: String,
    viewModel: StudyNotesViewModel
) {
    // 1. Establish the session identifiers
    viewModel.startPaymentSession(noteId, amount)

    // 2. Instantiate and configure Razorpay Checkout
    val checkout = com.razorpay.Checkout()
    
    // Retrieve the Key ID configured dynamically via build configuration secrets
    val keyId = com.example.BuildConfig.RAZORPAY_KEY_ID
    val activeKey = if (keyId.isNotEmpty() && keyId != "rzp_test_placeholder_key_id") {
        keyId
    } else {
        "rzp_test_3f78u31N83y57h" // Default fallback Sandbox Key for previewing Razorpay UI
    }
    
    checkout.setKeyID(activeKey)

    try {
        val options = org.json.JSONObject()
        options.put("name", "Study Notes")
        options.put("description", noteTitle)
        options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
        options.put("theme.color", "#1E1736")
        options.put("currency", "INR")
        options.put("amount", (amount * 100).toInt()) // Amount must be specified in Paise (e.g. 199.00 INR = 19900 paise)
        options.put("prefill.contact", userMobile)
        options.put("prefill.email", "student@studynotes.com")
        
        // Provide a freshly generated unique Order ID for this transaction session
        val orderId = "order_rzp_" + java.util.UUID.randomUUID().toString().take(12)
        options.put("order_id", orderId)

        // Launch the SDK checkout interface
        checkout.open(activity, options)
    } catch (e: Exception) {
        android.util.Log.e("RazorpayCheckout", "Error initiating Razorpay Gateway: ${e.message}")
        android.widget.Toast.makeText(activity, "Error launching payment: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
