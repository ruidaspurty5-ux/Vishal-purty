package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudyNotesViewModel
import com.example.ui.viewmodel.StudyNotesViewModelFactory

import com.razorpay.Checkout
import com.razorpay.PaymentResultWithDataListener
import com.razorpay.PaymentData
import android.widget.Toast

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    // Obtain the central ViewModel using our custom Factory
    private val viewModel: StudyNotesViewModel by viewModels {
        val app = application as StudyNotesApplication
        StudyNotesViewModelFactory(app.repository, app.paymentVerificationRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Preload Razorpay checkout for optimized checkout experience
        Checkout.preload(applicationContext)

        setContent {
            MyApplicationTheme {
                StudyNotesApp(viewModel = viewModel)
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val noteId = viewModel.activeNoteId ?: return
        val price = viewModel.activePrice
        val payId = paymentData?.paymentId ?: razorpayPaymentId ?: ""
        val orderId = paymentData?.orderId ?: ""
        val signature = paymentData?.signature ?: ""

        viewModel.verifySecurePayment(
            noteId = noteId,
            paymentId = payId,
            orderId = orderId,
            signature = signature,
            paidAmount = price
        )
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        val noteId = viewModel.activeNoteId ?: return
        val price = viewModel.activePrice
        val errorDesc = response ?: "Razorpay payment cancelled or failed. (Code: $code)"
        
        viewModel.recordFailedPayment(noteId, errorDesc, price)
        Toast.makeText(this, "Payment Failed: $errorDesc", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun StudyNotesApp(viewModel: StudyNotesViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // Splash Screen
        composable("splash") {
            SplashScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // Login Screen
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate("admin_panel") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Home / Explore Screen
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate("note_detail/$noteId")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToPurchases = {
                    navController.navigate("my_purchases")
                },
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                },
                onNavigateToDashboard = {
                    navController.navigate("dashboard")
                }
            )
        }

        // Search Screen
        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onNavigateToNoteDetail = { noteId ->
                    navController.navigate("note_detail/$noteId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Notes Detail / Checkout Screen
        composable(
            route = "note_detail/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            NoteDetailScreen(
                noteId = noteId,
                viewModel = viewModel,
                onNavigateToPdfViewer = { nid ->
                    navController.navigate("pdf_viewer/$nid")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Secure PDF Reader Screen
        composable(
            route = "pdf_viewer/{noteId}?preview={preview}",
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType },
                navArgument("preview") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            val isPreview = backStackEntry.arguments?.getBoolean("preview") ?: false
            PdfViewerScreen(
                noteId = noteId,
                isPreviewMode = isPreview,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // My Purchases Screen
        composable("my_purchases") {
            MyPurchasesScreen(
                viewModel = viewModel,
                onNavigateToPdfViewer = { noteId ->
                    navController.navigate("pdf_viewer/$noteId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // User Profile & Settings Screen
        composable("dashboard") {
            UserProfileScreen(
                viewModel = viewModel,
                onNavigateToAdmin = {
                    navController.navigate("admin_panel")
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSupportChat = {
                    navController.navigate("support_chat")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Support Chat Screen
        composable("support_chat") {
            SupportChatScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Secure Admin Panel Screen
        composable("admin_panel") {
            AdminPanelScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    // Route back safely based on user login state
                    navController.navigate("home") {
                        popUpTo("admin_panel") { inclusive = true }
                    }
                }
            )
        }

        // Notifications Screen
        composable("notifications") {
            NotificationsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
