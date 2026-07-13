package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import java.util.UUID

class StudyNotesRepository(private val appDao: AppDao) {

    // Auth & User Flow
    val currentUserFlow: Flow<UserEntity?> = appDao.getCurrentUserFlow()
    val totalUsersCountFlow: Flow<Int> = appDao.getTotalUsersCountFlow()

    suspend fun loginUser(mobile: String, name: String, isAdmin: Boolean = false): UserEntity {
        appDao.logoutAllUsers() // Ensure single logged in user locally
        val existing = appDao.getUser(mobile)
        val user = existing?.copy(name = name, isLoggedIn = true, isAdmin = isAdmin)
            ?: UserEntity(mobileNumber = mobile, name = name, isAdmin = isAdmin, isLoggedIn = true)
        appDao.insertUser(user)
        return user
    }

    suspend fun logout() {
        appDao.logoutAllUsers()
    }

    suspend fun getCurrentUser(): UserEntity? = appDao.getCurrentUser()

    suspend fun updateUser(user: UserEntity) {
        appDao.insertUser(user)
    }

    // Notes Flow
    val publishedNotesFlow: Flow<List<NoteEntity>> = appDao.getAllPublishedNotesFlow()
    val allNotesFlow: Flow<List<NoteEntity>> = appDao.getAllNotesFlow()
    val totalNotesCountFlow: Flow<Int> = appDao.getTotalNotesCountFlow()
    val totalSalesCountFlow: Flow<Int> = appDao.getTotalSalesCountFlow()
    val totalRevenueFlow: Flow<Double?> = appDao.getTotalRevenueFlow()

    fun getNotesByCategory(category: String): Flow<List<NoteEntity>> = appDao.getNotesByCategoryFlow(category)
    fun getNotesBySubject(category: String, subject: String): Flow<List<NoteEntity>> = appDao.getNotesBySubjectFlow(category, subject)
    fun getNoteByIdFlow(id: String): Flow<NoteEntity?> = appDao.getNoteByIdFlow(id)
    suspend fun getNoteById(id: String): NoteEntity? = appDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity) = appDao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = appDao.updateNote(note)
    suspend fun deleteNote(id: String) = appDao.deleteNoteById(id)

    // Categories & Subjects
    val categoriesFlow: Flow<List<CategoryEntity>> = appDao.getAllCategoriesFlow()
    val allSubjectsFlow: Flow<List<SubjectEntity>> = appDao.getAllSubjectsFlow()

    fun getSubjectsByCategory(categoryName: String): Flow<List<SubjectEntity>> = appDao.getSubjectsByCategoryFlow(categoryName)

    suspend fun insertCategory(category: CategoryEntity) = appDao.insertCategory(category)
    suspend fun insertSubject(subject: SubjectEntity) = appDao.insertSubject(subject)

    // Purchases & Payments
    fun getPurchasesForUser(userId: String): Flow<List<PurchaseEntity>> = appDao.getPurchasesForUserFlow(userId)
    fun isNotePurchasedFlow(userId: String, noteId: String): Flow<Boolean> = appDao.isNotePurchasedFlow(userId, noteId)
    suspend fun isNotePurchased(userId: String, noteId: String): Boolean = appDao.isNotePurchased(userId, noteId)
    val allPaymentsFlow: Flow<List<PaymentEntity>> = appDao.getAllPaymentsFlow()

    suspend fun recordPurchase(
        userId: String,
        noteId: String,
        paymentId: String,
        amount: Double,
        status: String,
        orderId: String
    ) {
        val payment = PaymentEntity(
            paymentId = paymentId,
            userId = userId,
            noteId = noteId,
            amount = amount,
            status = status,
            orderId = orderId
        )
        appDao.insertPayment(payment)

        if (status == "SUCCESS") {
            val purchase = PurchaseEntity(
                userId = userId,
                noteId = noteId,
                paymentId = paymentId,
                purchasePrice = amount
            )
            appDao.insertPurchase(purchase)
        }
    }

    // Coupons
    val allCouponsFlow: Flow<List<CouponEntity>> = appDao.getAllCouponsFlow()
    suspend fun getCoupon(code: String): CouponEntity? = appDao.getCoupon(code)
    suspend fun insertCoupon(coupon: CouponEntity) = appDao.insertCoupon(coupon)
    suspend fun updateCoupon(coupon: CouponEntity) = appDao.updateCoupon(coupon)

    // Reviews & Ratings
    fun getReviewsForNote(noteId: String): Flow<List<ReviewEntity>> = appDao.getReviewsForNoteFlow(noteId)
    suspend fun insertReview(review: ReviewEntity) {
        appDao.insertReview(review)
        // Recalculate average rating for Note
        val reviews = appDao.getReviewsForNoteFlow(review.noteId).firstOrNull() ?: emptyList()
        val note = appDao.getNoteById(review.noteId)
        if (note != null) {
            val updatedReviews = reviews + review
            val count = updatedReviews.size
            val avg = updatedReviews.map { it.rating }.average().toFloat()
            appDao.updateNote(note.copy(totalRatings = count, averageRating = avg))
        }
    }
    suspend fun deleteReview(reviewId: Int, noteId: String) {
        appDao.deleteReviewById(reviewId)
        // Recalculate
        val reviews = appDao.getReviewsForNoteFlow(noteId).firstOrNull() ?: emptyList()
        val note = appDao.getNoteById(noteId)
        if (note != null) {
            val count = reviews.size
            val avg = if (count > 0) reviews.map { it.rating }.average().toFloat() else 0f
            appDao.updateNote(note.copy(totalRatings = count, averageRating = avg))
        }
    }

    // Wishlist
    fun getWishlistedNotes(userId: String): Flow<List<NoteEntity>> = appDao.getWishlistedNotesFlow(userId)
    fun isWishlisted(userId: String, noteId: String): Flow<Boolean> = appDao.isWishlistedFlow(userId, noteId)
    suspend fun toggleWishlist(userId: String, noteId: String, shouldWishlist: Boolean) {
        if (shouldWishlist) {
            appDao.insertWishlist(WishlistEntity(userId = userId, noteId = noteId))
        } else {
            appDao.deleteWishlist(userId, noteId)
        }
    }

    // Notifications
    val allNotificationsFlow: Flow<List<NotificationEntity>> = appDao.getAllNotificationsFlow()
    suspend fun insertNotification(notification: NotificationEntity) = appDao.insertNotification(notification)

    // Offers
    val allOffersFlow: Flow<List<OfferEntity>> = appDao.getAllOffersFlow()
    suspend fun insertOffer(offer: OfferEntity) = appDao.insertOffer(offer)

    // Secure PDF Pages Content Provider
    fun getSecurePdfPages(noteId: String): List<String> {
        val customNote = kotlinx.coroutines.runBlocking { appDao.getNoteById(noteId) }
        if (customNote != null) {
            val content = customNote.pdfUrl
            if (content.isNotEmpty() && 
                content != "streaming_secure_encrypted_pdf_link_storage" && 
                !content.endsWith(".pdf") && 
                !content.endsWith(".txt")
            ) {
                // Parse pages split by a known marker
                val delimiters = listOf("[PAGE_BREAK]", "---", "[PAGE]")
                var pages: List<String>? = null
                for (delim in delimiters) {
                    if (content.contains(delim)) {
                        pages = content.split(delim).map { it.trim() }.filter { it.isNotEmpty() }
                        break
                    }
                }
                if (pages != null) {
                    return pages
                }
                
                // If no page break exists but the note is long, split by double-newline paragraphs up to a limit
                val paragraphs = content.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (paragraphs.size > 2) {
                    val pageList = mutableListOf<String>()
                    var currentPageText = StringBuilder()
                    paragraphs.forEach { p ->
                        if (currentPageText.length + p.length > 800) {
                            pageList.add(currentPageText.toString().trim())
                            currentPageText = StringBuilder(p)
                        } else {
                            if (currentPageText.isNotEmpty()) currentPageText.append("\n\n")
                            currentPageText.append(p)
                        }
                    }
                    if (currentPageText.isNotEmpty()) {
                        pageList.add(currentPageText.toString().trim())
                    }
                    return pageList
                }
                return listOf(content)
            }
        }

        return when (noteId) {
            "note_dsa" -> listOf(
                "DATA STRUCTURES & ALGORITHMS - CHAPTER 1: INTRO\n\nData Structure is a systematic way to organize data in order to use it efficiently.\nTypes of DS:\n1. Linear: Arrays, Linked List, Stack, Queue.\n2. Non-Linear: Trees, Graphs.\n\nTime Complexity is measured using Big-O Notation:\n- O(1): Constant Time\n- O(n): Linear Time\n- O(log n): Logarithmic Time\n- O(n^2): Quadratic Time",
                "DATA STRUCTURES & ALGORITHMS - CHAPTER 2: LINKED LISTS\n\nA Linked List is a dynamic data structure consisting of nodes where each node contains data and a reference (link) to the next node.\n\nStructure:\nstruct Node {\n    int data;\n    struct Node* next;\n};\n\nOperations:\n1. Insertion (At beginning, end, middle) - O(1) or O(n)\n2. Deletion - O(1) or O(n)\n3. Traversal - O(n)",
                "DATA STRUCTURES & ALGORITHMS - CHAPTER 3: STACKS & QUEUES\n\nStack operates on Last-In-First-Out (LIFO) principle.\nCore operations: push(), pop(), peek().\nApplications: Undo mechanisms, recursion call stacks, balancing parentheses.\n\nQueue operates on First-In-First-Out (FIFO) principle.\nCore operations: enqueue(), dequeue().\nApplications: CPU Scheduling, Buffers."
            )
            "note_dbms" -> listOf(
                "DATABASE MANAGEMENT SYSTEMS - NORMALIZATION STUDY\n\nNormalization is the process of organizing data to reduce redundancy and dependency.\n\n1NF: Atomic values only. No repeating groups.\n2NF: In 1NF + every non-prime attribute is fully functionally dependent on primary key.\n3NF: In 2NF + no transitive dependency exists.\nBCNF: In 3NF + for every functional dependency X -> Y, X must be a super key.",
                "DATABASE MANAGEMENT SYSTEMS - TRANSACTION CONTROL\n\nACID Properties:\n- Atomicity: Entire transaction succeeds or entire fails.\n- Consistency: Database state remains valid after transaction.\n- Isolation: Transactions run concurrently without interfering.\n- Durability: Saved changes are permanent even on crash."
            )
            "note_chem" -> listOf(
                "ORGANIC CHEMISTRY - MAJOR NAMED REACTIONS\n\n1. ALDOL CONDENSATION:\nInvolves nucleophilic addition of an enolate ion to a carbonyl compound to form a beta-hydroxy aldehyde/ketone, followed by dehydration to yield conjugated enone.\n\n2. CANNIZZARO REACTION:\nBase-induced disproportionation of non-enolizable aldehydes into a primary alcohol and carboxylic acid derivative.",
                "ORGANIC CHEMISTRY - GRIGNARD REAGENTS\n\nFormula: R-Mg-X\nGrignard reagent acts as a strong nucleophile reacting with carbonyl compounds (aldehydes/ketones) to form primary, secondary, or tertiary alcohols after acid workup.\nExample: HCHO + RMgX -> R-CH2-OH"
            )
            "note_physics" -> listOf(
                "QUANTUM PHYSICS - WAVE EQUATION DERIVATION\n\nSchrödinger Equation (Time Independent):\n\n- (hbar^2 / 2m) * (d^2/dx^2) psi(x) + V(x) psi(x) = E psi(x)\n\nWhere:\n- hbar: Reduced Planck's constant\n- m: Mass of the particle\n- psi(x): Wavefunction of the particle\n- V(x): Potential energy function\n- E: Total energy of the particle",
                "QUANTUM PHYSICS - WAVEFUNCTION INTERPRETATION\n\nBorn Interpretation:\n|psi(x)|^2 dx represents the probability of finding the particle between x and x + dx.\n\nNormalization Condition:\nIntegral from -infinity to +infinity of |psi(x)|^2 dx = 1\n(The particle must exist somewhere in space)."
            )
            "note_polity" -> listOf(
                "INDIAN POLITY - REVISION CAPSULE\n\nFUNDAMENTAL RIGHTS (Part III, Articles 12-35):\n\n1. Right to Equality (Articles 14-18)\n2. Right to Freedom (Articles 19-22)\n3. Right against Exploitation (Articles 23-24)\n4. Right to Freedom of Religion (Articles 25-28)\n5. Cultural and Educational Rights (Articles 29-30)\n6. Right to Constitutional Remedies (Article 32 - Heart and Soul according to Dr. B.R. Ambedkar)"
            )
            else -> listOf(
                "STUDY NOTES - PREMIUM STUDY MATERIALS\n\nThank you for purchasing this note! This document is protected with high-security features. Sharing or printing is disabled, and screenshots are locked.\n\nSummary:\nThis is a premium notes compilation covering the entire course syllabus, structured for rapid revision, clean concept learning, and complete exam preparation.",
                "STUDY NOTES - REVISION PAGE 2\n\nHighlights:\n- Detailed concept definitions\n- Key graphs and tabular layouts\n- Selected previous year solved questions\n- Visual frameworks for better memory retention."
            )
        }
    }

    // Populate Initial Sample Data
    suspend fun populateSampleDataIfNeeded() {
        val count = appDao.getTotalNotesCountFlow().firstOrNull() ?: 0
        if (count > 0) return // Already populated

        // Populate Categories
        val cats = listOf(
            CategoryEntity("BA English Honours", "Focus on core English Literature, criticism, and linguistics."),
            CategoryEntity("BCA", "Bachelor of Computer Applications - programming, databases, and algorithms."),
            CategoryEntity("B.Sc", "Bachelor of Science - detailed Physics, Chemistry, and Mathematics."),
            CategoryEntity("B.Com", "Bachelor of Commerce - Corporate Accounts, Finance, and Economics."),
            CategoryEntity("Class 11", "High-school eleventh grade revision notes for Science, Commerce & Arts."),
            CategoryEntity("Class 12", "Board examination specialized study guides and formulas."),
            CategoryEntity("Competitive Exams", "UPSC, SSC, Banking, Railways, and state government exams.")
        )
        cats.forEach { appDao.insertCategory(it) }

        // Populate Subjects
        val subs = listOf(
            SubjectEntity("Shakespearean Drama", "BA English Honours"),
            SubjectEntity("Victorian Prose", "BA English Honours"),
            SubjectEntity("Data Structures", "BCA"),
            SubjectEntity("DBMS", "BCA"),
            SubjectEntity("Organic Chemistry", "B.Sc"),
            SubjectEntity("Quantum Physics", "B.Sc"),
            SubjectEntity("Corporate Accounts", "B.Com"),
            SubjectEntity("Macroeconomics", "B.Com"),
            SubjectEntity("Physics Formulae", "Class 12"),
            SubjectEntity("Indian Polity", "Competitive Exams")
        )
        subs.forEach { appDao.insertSubject(it) }

        // Populate Notes
        val notes = listOf(
            NoteEntity(
                id = "note_dsa",
                title = "Data Structures & Algorithms (DSA)",
                description = "Complete BCA 3rd Sem DSA notes. Includes detailed pseudocode, step-by-step Dry Runs, complexity analysis, and solved exam papers. Best for quick exams.",
                thumbnail = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=500&q=80",
                price = 199.0,
                pdfUrl = "note_dsa_file.pdf",
                category = "BCA",
                subject = "Data Structures",
                semester = "Semester 3",
                isNew = true,
                isPublished = true
            ),
            NoteEntity(
                id = "note_dbms",
                title = "Database Management Systems (DBMS)",
                description = "Simplified DBMS concepts for BCA. SQL queries, Normalization (1NF, 2NF, 3NF, BCNF) with clear diagrams, ER Models, and transaction protocols.",
                thumbnail = "https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=500&q=80",
                price = 149.0,
                pdfUrl = "note_dbms_file.pdf",
                category = "BCA",
                subject = "DBMS",
                semester = "Semester 4",
                isNew = false,
                isPublished = true
            ),
            NoteEntity(
                id = "note_chem",
                title = "Organic Chemistry Reactions",
                description = "Detailed reaction mechanisms, synthesis equations, and named reactions (Aldol, Cannizzaro, Grignard) for B.Sc Chemistry.",
                thumbnail = "https://images.unsplash.com/photo-1532187863486-abf9d39d66e8?w=500&q=80",
                price = 249.0,
                pdfUrl = "note_chem_file.pdf",
                category = "B.Sc",
                subject = "Organic Chemistry",
                semester = "Semester 2",
                isNew = true,
                isPublished = true
            ),
            NoteEntity(
                id = "note_physics",
                title = "Quantum Physics Fundamentals",
                description = "Wave-particle duality, Schrödinger wave equation derivation, operator theory, and 1D potential barriers with complete step-by-step proofs.",
                thumbnail = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&q=80",
                price = 299.0,
                pdfUrl = "note_physics_file.pdf",
                category = "B.Sc",
                subject = "Quantum Physics",
                semester = "Semester 5",
                isNew = true,
                isPublished = true
            ),
            NoteEntity(
                id = "note_polity",
                title = "Indian Polity Revision Capsule",
                description = "Laxmikanth simplified summary for UPSC & SSC. Fundamental Rights, Directive Principles, and constitutional amendments organized in tabular visual form.",
                thumbnail = "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?w=500&q=80",
                price = 299.0,
                pdfUrl = "note_polity_file.pdf",
                category = "Competitive Exams",
                subject = "Indian Polity",
                semester = "Year 2026",
                isNew = true,
                isPublished = true
            )
        )
        notes.forEach { appDao.insertNote(it) }

        // Populate Coupons
        val coupons = listOf(
            CouponEntity("STUDY25", 25.0, System.currentTimeMillis() + 86400000 * 7, 100),
            CouponEntity("EEXAM50", 50.0, System.currentTimeMillis() + 86400000 * 3, 50),
            CouponEntity("WELCOME10", 10.0, System.currentTimeMillis() + 86400000 * 30, 1000)
        )
        coupons.forEach { appDao.insertCoupon(it) }

        // Populate Offers
        val offers = listOf(
            OfferEntity("offer_1", "Flat 25% Off Exam Special!", "Apply code STUDY25 at checkout to unlock direct 25% discount on all Notes.", 25.0, true),
            OfferEntity("offer_2", "Festival Offer - 50% Off!", "Limited time festival sale on BCA & B.Sc computer notes.", 50.0, true)
        )
        offers.forEach { appDao.insertOffer(it) }

        // Populate Notifications
        val notifications = listOf(
            NotificationEntity(title = "BCA 3rd Semester DSA Notes Released", message = "Highly requested Data Structures & Algorithms notes are now live. Unlock at 25% discount using code STUDY25.", type = "NEW_NOTE"),
            NotificationEntity(title = "Flat 50% Weekend Discount", message = "Special weekend discount of 50% available for Indian Polity capsule. Use code EEXAM50.", type = "DISCOUNT"),
            NotificationEntity(title = "Welcome Discount Live!", message = "New to Study Notes? Use code WELCOME10 on any purchase for an instant 10% discount.", type = "OFFER")
        )
        notifications.forEach { appDao.insertNotification(it) }

        // Populate Reviews
        val reviews = listOf(
            ReviewEntity(noteId = "note_dsa", userName = "Amit Kumar", rating = 5, reviewText = "Life saver for exam prep! Normalization and Trees are simplified beautifully."),
            ReviewEntity(noteId = "note_dsa", userName = "Nisha Patel", rating = 4, reviewText = "Excellent Dry Runs for Algorithms. Easy to read and understand."),
            ReviewEntity(noteId = "note_dbms", userName = "Rahul Sharma", rating = 5, reviewText = "Simple and neat diagrams. The relational algebra chapter is exceptionally good."),
            ReviewEntity(noteId = "note_chem", userName = "Sneha Roy", rating = 4, reviewText = "Named reactions are explained beautifully with proper mechanics.")
        )
        reviews.forEach { appDao.insertReview(it) }

        // Prepopulate standard Admin
        val admin = UserEntity(
            mobileNumber = "9999999999",
            name = "Admin Principal",
            isAdmin = true,
            isLoggedIn = false
        )
        appDao.insertUser(admin)
    }
}
