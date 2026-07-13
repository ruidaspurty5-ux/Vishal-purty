package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyNotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    noteId: String,
    isPreviewMode: Boolean = false,
    viewModel: StudyNotesViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Find the note
    val note = viewModel.publishedNotes.collectAsState().value.firstOrNull { it.id == noteId }
    val purchases by viewModel.purchases.collectAsState()
    
    val isPurchased = remember(purchases, noteId) {
        purchases.any { it.noteId == noteId }
    }

    // Security Gate Check
    val canAccessFull = note != null && (isPurchased || note.price <= 0)
    val canAccessPreview = note != null && isPreviewMode && !canAccessFull

    if (note == null || (!canAccessFull && !canAccessPreview)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Text("Secure Portal Blocked", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        text = "Purchase this note to continue. Reading is restricted strictly to authorized purchasers.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Button(onClick = onNavigateBack) {
                        Text("Back to Details")
                    }
                }
            }
        }
        return
    }

    // 1. Android FLAG_SECURE Screenshot & Screen Recording Protection
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        Toast.makeText(context, "Anti-screenshot protection active", Toast.LENGTH_SHORT).show()
        onDispose {
            // Restore window flags when leaving the reader
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Retrieve streamed/decrypted note pages
    val pages = remember(noteId) { viewModel.getSecurePdfPages(noteId) }

    val displayedPages = remember(pages, canAccessPreview) {
        if (canAccessPreview) pages.take(2) else pages
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(note.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                        Text(
                            text = if (canAccessPreview) "Free Preview Mode" else "High Security Reader",
                            fontSize = 11.sp,
                            color = if (canAccessPreview) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Reader")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color(0xFF121824) // Deep high contrast dark background for reading
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 2. High Security Header / Preview Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (canAccessPreview) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (canAccessPreview) Icons.Default.Visibility else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (canAccessPreview) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (canAccessPreview) "Free preview of first 2 pages. Buy to unlock all pages." else "Watermarked & Screen-Locked. Copying / Downloading disabled.",
                        color = if (canAccessPreview) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Document Canvas Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                displayedPages.forEachIndexed { index, pageContent ->
                    // Beautiful styled watermarked page
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.707f) // Dynamic standard A4 aspect ratio representation
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .drawBehind {
                                // Draw dynamic repeated watermark on canvas safely
                                val watermarkText = "STUDY NOTES • +91 ${currentUser?.mobileNumber} • ${currentUser?.name}"
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.argb(30, 200, 200, 200) // Super faint gray
                                    textSize = 36f
                                    isAntiAlias = true
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }
                                
                                drawContext.canvas.nativeCanvas.save()
                                drawContext.canvas.nativeCanvas.rotate(-35f, size.width / 2, size.height / 2)
                                
                                // Draw three watermark lines across the page diagonal
                                drawContext.canvas.nativeCanvas.drawText(
                                    watermarkText,
                                    -size.width * 0.2f,
                                    size.height * 0.3f,
                                    paint
                                )
                                drawContext.canvas.nativeCanvas.drawText(
                                    watermarkText,
                                    -size.width * 0.1f,
                                    size.height * 0.5f,
                                    paint
                                )
                                drawContext.canvas.nativeCanvas.drawText(
                                    watermarkText,
                                    -size.width * 0.0f,
                                    size.height * 0.7f,
                                    paint
                                )
                                
                                drawContext.canvas.nativeCanvas.restore()
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            // Page Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (canAccessPreview) "FREE PREVIEW ACCESS" else "STUDY NOTES DIRECT ACCESS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canAccessPreview) MaterialTheme.colorScheme.secondary else Color.Gray,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "PAGE ${index + 1} OF ${pages.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Page contents - strictly regular Text composables with no selection, copy, or sharing
                            Text(
                                text = pageContent,
                                color = Color(0xFF1E293B), // Sleek Charcoal reading text
                                fontSize = 14.sp,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 22.sp,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Footer watermarking in text form as well
                            Text(
                                text = "Licensed to ${currentUser?.name} (${currentUser?.mobileNumber}). Unauthorised distribution is highly punishable.",
                                fontSize = 8.sp,
                                color = Color.Red.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // CTA card at the end of preview
                if (canAccessPreview) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .testTag("preview_cta_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "End of Free Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "You have read the first 2 pages of this premium compilation. Purchase the full note package to access all ${pages.size} pages securely, with copy/download restrictions preserved.",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            
                            Button(
                                onClick = {
                                    // Navigate back to Note Detail to proceed with payment
                                    onNavigateBack()
                                },
                                modifier = Modifier.fillMaxWidth().testTag("preview_purchase_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null)
                                    Text("Buy Complete eNotes Now (₹${note.price.toInt()})", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
