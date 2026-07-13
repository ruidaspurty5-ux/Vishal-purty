package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.PaymentVerificationRepository
import com.example.data.repository.StudyNotesRepository
import com.example.data.repository.VerificationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class VerificationUIState {
    object Idle : VerificationUIState()
    object Loading : VerificationUIState()
    object Success : VerificationUIState()
    data class Error(val message: String) : VerificationUIState()
}

class StudyNotesViewModel(
    private val repository: StudyNotesRepository,
    private val paymentVerificationRepository: PaymentVerificationRepository
) : ViewModel() {

    // Current User
    val currentUser: StateFlow<UserEntity?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Notes lists
    val publishedNotes: StateFlow<List<NoteEntity>> = repository.publishedNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.categoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubjects: StateFlow<List<SubjectEntity>> = repository.allSubjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Wishlist for current user
    val wishlist: StateFlow<List<NoteEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getWishlistedNotes(user.mobileNumber)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Purchases for current user
    val purchases: StateFlow<List<PurchaseEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getPurchasesForUser(user.mobileNumber)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Offers
    val offers: StateFlow<List<OfferEntity>> = repository.allOffersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Coupons
    val coupons: StateFlow<List<CouponEntity>> = repository.allCouponsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Payments History
    val payments: StateFlow<List<PaymentEntity>> = repository.allPaymentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Checkout / Secure Verification State
    private val _verificationState = MutableStateFlow<VerificationUIState>(VerificationUIState.Idle)
    val verificationState: StateFlow<VerificationUIState> = _verificationState.asStateFlow()

    fun resetVerificationState() {
        _verificationState.value = VerificationUIState.Idle
    }

    // Active Razorpay checkout session state
    var activeNoteId: String? = null
    var activePrice: Double = 0.0

    fun startPaymentSession(noteId: String, price: Double) {
        activeNoteId = noteId
        activePrice = price
    }

    // Admin Dashboard Statistics
    val totalUsersCount: StateFlow<Int> = repository.totalUsersCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalNotesCount: StateFlow<Int> = repository.totalNotesCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSalesCount: StateFlow<Int> = repository.totalSalesCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalRevenue: StateFlow<Double> = repository.totalRevenueFlow
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Active Checkout State
    private val _appliedCoupon = MutableStateFlow<CouponEntity?>(null)
    val appliedCoupon: StateFlow<CouponEntity?> = _appliedCoupon.asStateFlow()

    private val _couponError = MutableStateFlow<String?>(null)
    val couponError: StateFlow<String?> = _couponError.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSemester = MutableStateFlow<String?>(null)
    val selectedSemester: StateFlow<String?> = _selectedSemester.asStateFlow()

    // Filtered Notes based on search
    val searchResults: StateFlow<List<NoteEntity>> = combine(
        publishedNotes,
        _searchQuery,
        _selectedSemester
    ) { notes, query, semester ->
        notes.filter { note ->
            val matchesQuery = query.isEmpty() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.subject.contains(query, ignoreCase = true) ||
                    note.category.contains(query, ignoreCase = true)
            val matchesSemester = semester == null || note.semester.contains(semester, ignoreCase = true)
            matchesQuery && matchesSemester
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Authentication methods
    fun login(mobile: String, name: String, isAdmin: Boolean = false) {
        viewModelScope.launch {
            repository.loginUser(mobile, name, isAdmin)
            _appliedCoupon.value = null
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _appliedCoupon.value = null
        }
    }

    fun updateUserProfile(name: String, profilePhotoUrl: String) {
        val current = currentUser.value ?: return
        viewModelScope.launch {
            val updated = current.copy(name = name, profilePhotoUrl = profilePhotoUrl)
            repository.updateUser(updated)
        }
    }

    // Wishlist Actions
    fun toggleWishlist(noteId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val isCurrentlyWishlisted = wishlist.value.any { it.id == noteId }
            repository.toggleWishlist(user.mobileNumber, noteId, !isCurrentlyWishlisted)
        }
    }

    fun isWishlisted(noteId: String): Flow<Boolean> {
        val user = currentUser.value ?: return flowOf(false)
        return repository.isWishlisted(user.mobileNumber, noteId)
    }

    // Coupon & Checkout Operations
    fun applyCouponCode(code: String) {
        if (code.isEmpty()) {
            _appliedCoupon.value = null
            _couponError.value = null
            return
        }
        viewModelScope.launch {
            val coupon = repository.getCoupon(code.uppercase())
            if (coupon == null) {
                _couponError.value = "Invalid Coupon Code"
                _appliedCoupon.value = null
            } else if (coupon.expiryDate < System.currentTimeMillis()) {
                _couponError.value = "Coupon Expired"
                _appliedCoupon.value = null
            } else if (coupon.usageCount >= coupon.usageLimit) {
                _couponError.value = "Coupon Usage Limit Reached"
                _appliedCoupon.value = null
            } else {
                _appliedCoupon.value = coupon
                _couponError.value = null
            }
        }
    }

    fun clearCoupon() {
        _appliedCoupon.value = null
        _couponError.value = null
    }

    // Payment & Razorpay Unlock Workflows
    fun verifySecurePayment(
        noteId: String,
        paymentId: String,
        orderId: String,
        signature: String,
        paidAmount: Double
    ) {
        val user = currentUser.value ?: run {
            _verificationState.value = VerificationUIState.Error("User session not found.")
            return
        }

        _verificationState.value = VerificationUIState.Loading
        viewModelScope.launch {
            val result = paymentVerificationRepository.verifyAndUnlockNote(
                userId = user.mobileNumber,
                noteId = noteId,
                paymentId = paymentId,
                orderId = orderId,
                signature = signature,
                amount = paidAmount
            )

            when (result) {
                is VerificationResult.Success -> {
                    _verificationState.value = VerificationUIState.Success
                    // Update Coupon count if coupon was applied
                    appliedCoupon.value?.let { coupon ->
                        repository.updateCoupon(coupon.copy(usageCount = coupon.usageCount + 1))
                        _appliedCoupon.value = null
                    }
                }
                is VerificationResult.Failure -> {
                    _verificationState.value = VerificationUIState.Error(result.message)
                }
            }
        }
    }

    fun recordSuccessfulPayment(noteId: String, paymentId: String, paidAmount: Double) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val orderId = "order_${UUID.randomUUID().toString().take(8)}"
            repository.recordPurchase(
                userId = user.mobileNumber,
                noteId = noteId,
                paymentId = paymentId,
                amount = paidAmount,
                status = "SUCCESS",
                orderId = orderId
            )
            // Update Coupon count if coupon was applied
            appliedCoupon.value?.let { coupon ->
                repository.updateCoupon(coupon.copy(usageCount = coupon.usageCount + 1))
                _appliedCoupon.value = null
            }
        }
    }

    fun recordFailedPayment(noteId: String, errorDescription: String, amount: Double) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val orderId = "order_${UUID.randomUUID().toString().take(8)}"
            val paymentId = "pay_failed_${UUID.randomUUID().toString().take(8)}"
            repository.recordPurchase(
                userId = user.mobileNumber,
                noteId = noteId,
                paymentId = paymentId,
                amount = amount,
                status = "FAILED: $errorDescription",
                orderId = orderId
            )
        }
    }

    // Review Actions
    fun addReview(noteId: String, rating: Int, reviewText: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.insertReview(
                ReviewEntity(
                    noteId = noteId,
                    userName = user.name,
                    rating = rating,
                    reviewText = reviewText
                )
            )
        }
    }

    fun deleteReview(reviewId: Int, noteId: String) {
        viewModelScope.launch {
            repository.deleteReview(reviewId, noteId)
        }
    }

    fun getReviewsForNote(noteId: String): Flow<List<ReviewEntity>> {
        return repository.getReviewsForNote(noteId)
    }

    // Search & Filter Operations
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSemesterFilter(semester: String?) {
        _selectedSemester.value = semester
    }

    // Admin Operations
    fun addNote(
        title: String,
        description: String,
        thumbnailUrl: String,
        price: Double,
        pdfUrl: String,
        category: String,
        subject: String,
        semester: String,
        isNew: Boolean
    ) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = "note_${UUID.randomUUID().toString().take(6)}",
                title = title,
                description = description,
                thumbnail = if (thumbnailUrl.isEmpty()) "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=500&q=80" else thumbnailUrl,
                price = price,
                pdfUrl = pdfUrl,
                category = category,
                subject = subject,
                semester = semester,
                isNew = isNew,
                isPublished = true
            )
            repository.insertNote(note)

            // Auto notify users about the new note
            repository.insertNotification(
                NotificationEntity(
                    title = "New Notes Available: $title",
                    message = "Premium study materials released for $category ($semester - $subject). Check it out!",
                    type = "NEW_NOTE"
                )
            )
        }
    }

    fun editNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(updatedDate = System.currentTimeMillis()))
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun togglePublishNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPublished = !note.isPublished))
        }
    }

    fun addCategory(name: String, description: String) {
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name, description))
        }
    }

    fun addSubject(name: String, categoryName: String) {
        viewModelScope.launch {
            repository.insertSubject(SubjectEntity(name, categoryName))
        }
    }

    fun addCoupon(code: String, discountPercent: Double, expiryDate: Long, usageLimit: Int) {
        viewModelScope.launch {
            repository.insertCoupon(
                CouponEntity(
                    code = code.uppercase(),
                    discountPercent = discountPercent,
                    expiryDate = expiryDate,
                    usageLimit = usageLimit
                )
            )
        }
    }

    fun addOffer(title: String, description: String, discountPercent: Double) {
        viewModelScope.launch {
            repository.insertOffer(
                OfferEntity(
                    id = "offer_${UUID.randomUUID().toString().take(6)}",
                    title = title,
                    description = description,
                    discountPercent = discountPercent,
                    isLimitedTime = true
                )
            )
            // Auto notify
            repository.insertNotification(
                NotificationEntity(
                    title = "New Offer: $title",
                    message = "$description Get incredible discounts on your favorite eNotes today!",
                    type = "OFFER"
                )
            )
        }
    }

    // Note PDF reader access helper
    fun getSecurePdfPages(noteId: String): List<String> {
        return repository.getSecurePdfPages(noteId)
    }

    // Support Chat Flows
    val currentUserIdFlow: StateFlow<String> = currentUser
        .map { it?.mobileNumber ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val supportMessages: StateFlow<List<com.example.data.repository.SupportMessage>> = currentUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isNotEmpty()) com.example.data.repository.SupportChatRepository.getMessagesFlow(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminActiveChats: StateFlow<List<com.example.data.repository.ActiveChat>> = com.example.data.repository.SupportChatRepository.getActiveChatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendSupportMessage(messageText: String, overrideUserId: String? = null, overrideUserName: String? = null) {
        viewModelScope.launch {
            val user = currentUser.value
            val userId = overrideUserId ?: user?.mobileNumber ?: "guest"
            val userName = overrideUserName ?: user?.name ?: "Guest User"
            val sender = if (user?.isAdmin == true && overrideUserId != null) "SUPPORT" else "USER"
            com.example.data.repository.SupportChatRepository.sendMessage(userId, userName, messageText, sender)
        }
    }

    fun getSupportMessagesForUser(userId: String): Flow<List<com.example.data.repository.SupportMessage>> {
        return com.example.data.repository.SupportChatRepository.getMessagesFlow(userId)
    }
}

class StudyNotesViewModelFactory(
    private val repository: StudyNotesRepository,
    private val paymentVerificationRepository: PaymentVerificationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyNotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyNotesViewModel(repository, paymentVerificationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
