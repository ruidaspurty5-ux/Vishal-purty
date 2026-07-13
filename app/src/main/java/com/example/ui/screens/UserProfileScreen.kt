package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.data.database.UserEntity
import com.example.ui.viewmodel.StudyNotesViewModel
import java.text.SimpleDateFormat
import java.util.*

// Avatar Definition
data class AvatarPreset(
    val id: String,
    val emoji: String,
    val name: String,
    val gradientColors: List<Color>
)

val AVATAR_PRESETS = listOf(
    AvatarPreset("avatar_grad", "🎓", "Academic Scholar", listOf(Color(0xFF6366F1), Color(0xFFA855F7))),
    AvatarPreset("avatar_sc", "🔬", "Lab Researcher", listOf(Color(0xFF0D9488), Color(0xFF10B981))),
    AvatarPreset("avatar_art", "🎨", "Creative Designer", listOf(Color(0xFFF43F5E), Color(0xFFFB923C))),
    AvatarPreset("avatar_space", "🚀", "Cosmic Explorer", listOf(Color(0xFF1E1B4B), Color(0xFF4F46E5))),
    AvatarPreset("avatar_book", "📚", "Avid Reader", listOf(Color(0xFF2563EB), Color(0xFF38BDF8)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: StudyNotesViewModel,
    onNavigateToAdmin: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSupportChat: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val notes by viewModel.allNotes.collectAsState()
    val wishlist by viewModel.wishlist.collectAsState()

    var showTncDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Dynamic User Statistics Calculations
    val userPurchasedNotes = remember(payments) {
        payments.filter { it.status == "SUCCESS" }
    }
    
    val totalAmountSpent = remember(userPurchasedNotes) {
        userPurchasedNotes.sumOf { it.amount }
    }

    val wishlistCount = wishlist.size

    // Match payment notes
    val paymentListWithNoteTitle = remember(payments, notes) {
        payments.map { p ->
            val note = notes.firstOrNull { it.id == p.noteId }
            Pair(p, note?.title ?: "Study Notes Compilation")
        }
    }

    // Determine current avatar details or fallback
    val currentAvatar = remember(currentUser) {
        AVATAR_PRESETS.firstOrNull { it.id == currentUser?.profilePhotoUrl }
            ?: AvatarPreset("default", "🧑‍🎓", "Student", listOf(Color(0xFF4F46E5), Color(0xFF06B6D4)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("profile_screen_title")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("profile_back_button")) {
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // 1. Polished Profile block with custom background gradient & avatar preset
            Card(
                modifier = Modifier.fillMaxWidth().testTag("profile_details_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Image placeholder with gorgeous custom selected gradient
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(currentAvatar.gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentAvatar.emoji,
                            fontSize = 44.sp,
                            modifier = Modifier.testTag("profile_avatar_emoji")
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentUser?.name ?: "Student User",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("profile_user_name")
                            )
                            if (currentUser?.isAdmin == true) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "+91 ${currentUser?.mobileNumber ?: "N/A"}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.testTag("profile_user_mobile")
                        )

                        Text(
                            text = "Aesthetic Persona: ${currentAvatar.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Button to open "Manage Personal Details" / Edit profile dialog
                    Button(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("edit_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Personal Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // 2. High-contrast User Statistics Panel
            Text(
                text = "My Study Statistics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().testTag("profile_stats_row"),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Purchases Stat Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = userPurchasedNotes.size.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Purchased",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Total Invested Stat Card
                Card(
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${totalAmountSpent.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total Spent",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Wishlist Stat Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF43F5E))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = wishlistCount.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Saved",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 3. Admin Portal Option
            if (currentUser?.isAdmin == true) {
                Button(
                    onClick = onNavigateToAdmin,
                    modifier = Modifier.fillMaxWidth().testTag("enter_admin_console_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Secure Admin Console", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // 4. More Profile and Support Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Terms & Conditions") },
                        leadingContent = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { showTncDialog = true }.testTag("tnc_list_item")
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    ListItem(
                        headlineContent = { Text("Privacy Policy") },
                        leadingContent = { Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { showPrivacyDialog = true }.testTag("privacy_list_item")
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    ListItem(
                        headlineContent = { Text("Contact Support") },
                        leadingContent = { Icon(Icons.Default.ContactSupport, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        supportingContent = { Text("support@studynotes.com • +91 98765 43210") },
                        modifier = Modifier.clickable {
                            onNavigateToSupportChat()
                        }.testTag("support_list_item")
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    ListItem(
                        headlineContent = { Text("Logout Account", color = Color.Red) },
                        leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red) },
                        modifier = Modifier.clickable {
                            viewModel.logout()
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                            onNavigateToLogin()
                        }.testTag("logout_list_item")
                    )
                }
            }

            // 5. Razorpay Transaction Log
            Text(
                text = "Razorpay Transaction History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (paymentListWithNoteTitle.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions yet.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("transactions_history_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        paymentListWithNoteTitle.forEach { pair ->
                            val payment = pair.first
                            val title = pair.second
                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            val dateString = sdf.format(Date(payment.date))
                            val isSuccess = payment.status == "SUCCESS"

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Ref: ${payment.paymentId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text(dateString, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹${payment.amount.toInt()}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSuccess) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isSuccess) "SUCCESS" else "FAILED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSuccess) Color(0xFF137333) else Color(0xFFC5221F)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                            }
                        }
                    }
                }
            }

            // 6. Polished App Versioning block (Monospace font, distinctive look)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("app_version_block"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "STUDY NOTES COMPILER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "App Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("version_text")
                )
                Text(
                    text = "Package: ${BuildConfig.APPLICATION_ID}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("package_text")
                )
                Text(
                    text = "© ${Calendar.getInstance().get(Calendar.YEAR)} Study Notes Inc. All rights reserved.",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // Terms & Conditions Dialog
    if (showTncDialog) {
        AlertDialog(
            onDismissRequest = { showTncDialog = false },
            confirmButton = {
                TextButton(onClick = { showTncDialog = false }, modifier = Modifier.testTag("dismiss_tnc")) { Text("Dismiss") }
            },
            title = { Text("Terms & Conditions") },
            text = {
                Text(
                    "Welcome to Study Notes. By using this app, you agree strictly to comply with our anti-piracy covenants. All documents accessed inside this app are protected under Intellectual Property laws. Unauthorized sharing, recording, screenshot distribution, or modification of the secure PDF files will immediately terminate your account and will invoke direct legal actions."
                )
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }, modifier = Modifier.testTag("dismiss_privacy")) { Text("Dismiss") }
            },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    "Study Notes is committed to protecting your privacy. We store only your mobile phone number, name, purchase listings, and Razorpay transaction IDs securely in localized, encrypted configurations. We do not expose or share your personal identities with third-party networks or service providers."
                )
            }
        )
    }

    // Personal Details Editor Dialog with Custom Avatar Selection
    if (showEditProfileDialog) {
        var editedName by remember { mutableStateOf(currentUser?.name ?: "") }
        var selectedAvatarId by remember { mutableStateOf(currentUser?.profilePhotoUrl ?: "avatar_grad") }

        Dialog(
            onDismissRequest = { showEditProfileDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("edit_profile_dialog"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit Personal Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("edit_profile_dialog_title")
                    )

                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("Enter your name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Choose Your Avatar Persona",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Avatar Selector Grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AVATAR_PRESETS.forEach { preset ->
                            val isSelected = selectedAvatarId == preset.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedAvatarId = preset.id }
                                    .padding(8.dp)
                                    .testTag("avatar_option_${preset.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(preset.gradientColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(preset.emoji, fontSize = 20.sp)
                                }
                                Column {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditProfileDialog = false },
                            modifier = Modifier.weight(1f).testTag("cancel_profile_edit_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (editedName.trim().isEmpty()) {
                                    Toast.makeText(context, "Name cannot be empty!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updateUserProfile(editedName.trim(), selectedAvatarId)
                                    showEditProfileDialog = false
                                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("save_profile_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
