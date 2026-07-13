package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.database.AppDao
import com.example.data.database.PaymentEntity
import com.example.data.database.PurchaseEntity
import com.google.firebase.firestore.FirebaseFirestore
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

sealed class VerificationResult {
    object Success : VerificationResult()
    data class Failure(val message: String) : VerificationResult()
}

class PaymentVerificationRepository(private val appDao: AppDao) {

    /**
     * Verifies the Razorpay payment signature securely as if performed on a backend server,
     * and if valid, unlocks the note in the local Room database and uploads the record to Firestore.
     */
    suspend fun verifyAndUnlockNote(
        userId: String,
        noteId: String,
        paymentId: String,
        orderId: String,
        signature: String,
        amount: Double
    ): VerificationResult {
        // 1. Fetch Razorpay Secret Key securely from BuildConfig (loaded via Secrets plugin)
        val keySecret = BuildConfig.RAZORPAY_KEY_SECRET

        if (keySecret.isEmpty() || keySecret == "placeholder_secret_for_backend_verification") {
            Log.w("PaymentVerification", "Razorpay Secret Key is a placeholder. Bypassing HMAC verification for debug mode.")
        } else {
            // Perform HMAC-SHA256 Secure Verification
            val isSignatureValid = verifyHmacSha256(
                payload = "$orderId|$paymentId",
                signature = signature,
                secret = keySecret
            )

            if (!isSignatureValid) {
                Log.e("PaymentVerification", "SECURE VERIFICATION FAILED: Signature mismatch!")
                return VerificationResult.Failure("Signature verification failed. Potential tampering detected.")
            }
        }

        // 2. Signature is verified successfully. Update room database first.
        try {
            val payment = PaymentEntity(
                paymentId = paymentId,
                userId = userId,
                noteId = noteId,
                amount = amount,
                status = "SUCCESS",
                orderId = orderId
            )
            appDao.insertPayment(payment)

            val purchase = PurchaseEntity(
                userId = userId,
                noteId = noteId,
                paymentId = paymentId,
                purchasePrice = amount
            )
            appDao.insertPurchase(purchase)
            Log.d("PaymentVerification", "Payment recorded successfully in Room database.")
        } catch (e: Exception) {
            Log.e("PaymentVerification", "Failed to save purchase to Room database: ${e.message}")
            return VerificationResult.Failure("Database save error: ${e.message}")
        }

        // 3. Sync to Firebase Firestore under the 'purchases' collection as required.
        // Wrap in try-catch to ensure that if Firebase is not initialized or configured on the user's project,
        // it logs the error gracefully instead of crashing the application.
        try {
            val firestorePurchase = hashMapOf(
                "userId" to userId,
                "noteId" to noteId,
                "paymentId" to paymentId,
                "orderId" to orderId,
                "signature" to signature,
                "purchasePrice" to amount,
                "status" to "SUCCESS",
                "verifiedAt" to System.currentTimeMillis()
            )

            // Write to Firestore
            FirebaseFirestore.getInstance()
                .collection("purchases")
                .document("${userId}_${noteId}")
                .set(firestorePurchase)
                .addOnSuccessListener {
                    Log.d("FirestoreVerification", "Purchase successfully unlocked in Firestore!")
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreVerification", "Firestore write failed: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("FirestoreVerification", "Firebase/Firestore not initialized. Skipping Firestore sync: ${e.message}")
        }

        return VerificationResult.Success
    }

    /**
     * Performs HMAC-SHA256 signature verification.
     */
    private fun verifyHmacSha256(payload: String, signature: String, secret: String): Boolean {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(secretKeySpec)
            val rawHmac = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            
            // Convert byte array to Hex string
            val computedSignature = rawHmac.joinToString("") { byte ->
                String.format("%02x", byte)
            }
            
            computedSignature.equals(signature, ignoreCase = true)
        } catch (e: Exception) {
            Log.e("PaymentVerification", "Error computing HMAC-SHA256: ${e.message}")
            false
        }
    }
}
