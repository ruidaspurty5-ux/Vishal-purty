package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // User Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE mobileNumber = :mobileNumber LIMIT 1")
    suspend fun getUser(mobileNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()

    @Query("SELECT COUNT(*) FROM users")
    fun getTotalUsersCountFlow(): Flow<Int>

    // Notes Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("SELECT * FROM notes WHERE isPublished = 1 ORDER BY uploadDate DESC")
    fun getAllPublishedNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY uploadDate DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteByIdFlow(id: String): Flow<NoteEntity?>

    @Query("SELECT COUNT(*) FROM notes")
    fun getTotalNotesCountFlow(): Flow<Int>

    @Query("SELECT * FROM notes WHERE category = :category AND isPublished = 1")
    fun getNotesByCategoryFlow(category: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE category = :category AND subject = :subject AND isPublished = 1")
    fun getNotesBySubjectFlow(category: String, subject: String): Flow<List<NoteEntity>>

    // Categories Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    // Subjects Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Query("SELECT * FROM subjects WHERE categoryName = :categoryName")
    fun getSubjectsByCategoryFlow(categoryName: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects")
    fun getAllSubjectsFlow(): Flow<List<SubjectEntity>>

    // Purchases Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Query("SELECT * FROM purchases WHERE userId = :userId")
    fun getPurchasesForUserFlow(userId: String): Flow<List<PurchaseEntity>>

    @Query("SELECT COUNT(*) > 0 FROM purchases WHERE userId = :userId AND noteId = :noteId")
    fun isNotePurchasedFlow(userId: String, noteId: String): Flow<Boolean>

    @Query("SELECT COUNT(*) > 0 FROM purchases WHERE userId = :userId AND noteId = :noteId")
    suspend fun isNotePurchased(userId: String, noteId: String): Boolean

    @Query("SELECT COUNT(*) FROM purchases")
    fun getTotalSalesCountFlow(): Flow<Int>

    @Query("SELECT SUM(purchasePrice) FROM purchases")
    fun getTotalRevenueFlow(): Flow<Double?>

    // Payments Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    // Coupons Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupon(coupon: CouponEntity)

    @Query("SELECT * FROM coupons WHERE code = :code LIMIT 1")
    suspend fun getCoupon(code: String): CouponEntity?

    @Query("SELECT * FROM coupons")
    fun getAllCouponsFlow(): Flow<List<CouponEntity>>

    @Update
    suspend fun updateCoupon(coupon: CouponEntity)

    // Reviews Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Query("SELECT * FROM reviews WHERE noteId = :noteId ORDER BY date DESC")
    fun getReviewsForNoteFlow(noteId: String): Flow<List<ReviewEntity>>

    @Query("DELETE FROM reviews WHERE id = :reviewId")
    suspend fun deleteReviewById(reviewId: Int)

    // Notifications Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY date DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    // Wishlist Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlist(wishlist: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE userId = :userId AND noteId = :noteId")
    suspend fun deleteWishlist(userId: String, noteId: String): Int

    @Query("SELECT COUNT(*) > 0 FROM wishlist WHERE userId = :userId AND noteId = :noteId")
    fun isWishlistedFlow(userId: String, noteId: String): Flow<Boolean>

    @Query("SELECT * FROM notes WHERE id IN (SELECT noteId FROM wishlist WHERE userId = :userId)")
    fun getWishlistedNotesFlow(userId: String): Flow<List<NoteEntity>>

    // Offers Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: OfferEntity)

    @Query("SELECT * FROM offers")
    fun getAllOffersFlow(): Flow<List<OfferEntity>>
}
