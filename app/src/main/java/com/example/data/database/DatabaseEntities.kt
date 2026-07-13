package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val mobileNumber: String,
    val name: String,
    val profilePhotoUrl: String = "",
    val isAdmin: Boolean = false,
    val isLoggedIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val thumbnail: String, // Can be a URL or local draw resource identifier
    val price: Double,
    val pdfUrl: String, // Path to file or storage reference
    val uploadDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val category: String,
    val subject: String,
    val semester: String,
    val isNew: Boolean = true,
    val isPublished: Boolean = true,
    val totalRatings: Int = 0,
    val averageRating: Float = 0f
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val description: String = ""
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val name: String,
    val categoryName: String
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val purchaseId: Int = 0,
    val userId: String, // Mobile number
    val noteId: String,
    val paymentId: String,
    val transactionDate: Long = System.currentTimeMillis(),
    val purchasePrice: Double
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val paymentId: String,
    val userId: String,
    val noteId: String,
    val amount: Double,
    val status: String, // "SUCCESS" or "FAILED"
    val orderId: String,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val code: String,
    val discountPercent: Double,
    val expiryDate: Long,
    val usageLimit: Int,
    val usageCount: Int = 0
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: String,
    val userName: String,
    val rating: Int,
    val reviewText: String,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val date: Long = System.currentTimeMillis(),
    val type: String = "INFO" // "NEW_NOTE", "DISCOUNT", "OFFER"
)

@Entity(tableName = "wishlist")
data class WishlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val noteId: String
)

@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val discountPercent: Double,
    val isLimitedTime: Boolean = false,
    val bannerUrl: String = ""
)
