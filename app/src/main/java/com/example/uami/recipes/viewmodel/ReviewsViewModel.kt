package com.example.uami.recipes.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uami.recipes.data.RecipeRepository
import com.example.uami.recipes.models.ReviewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class ReviewsViewModel(private val repository: RecipeRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var userDocListener: ListenerRegistration? = null

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    data class UserProfileData(val uid: String, val displayName: String, val photoUrl: String)

    private val _userProfile = MutableStateFlow<UserProfileData?>(null)
    val userProfile: StateFlow<UserProfileData?> = _userProfile.asStateFlow()

    private val _userFavorites = MutableStateFlow<List<Int>>(emptyList())
    val userFavorites: StateFlow<List<Int>> = _userFavorites.asStateFlow()

    private val _reviews = MutableStateFlow<List<ReviewModel>>(emptyList())
    val reviews: StateFlow<List<ReviewModel>> = _reviews.asStateFlow()

    private val _isLoadingReviews = MutableStateFlow(false)
    val isLoadingReviews: StateFlow<Boolean> = _isLoadingReviews.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    init {
        // Escuchar cambios de autenticación
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user == null) {
                userDocListener?.remove()
                userDocListener = null
                _userProfile.value = null
                _userFavorites.value = emptyList()
            } else {
                listenToUserProfile(user)
            }
        }
        
        // Sincronizar reactivamente los favoritos locales hacia Firestore
        viewModelScope.launch {
            repository.favoritos.collect { localFavs ->
                if (_currentUser.value != null) {
                    val firestoreFavs = _userFavorites.value
                    if (localFavs != firestoreFavs) {
                        saveFavoritesToFirestore(localFavs)
                    }
                }
            }
        }

        // Cargar reseñas iniciales
        loadReviews()
    }

    private fun listenToUserProfile(user: FirebaseUser) {
        userDocListener?.remove()
        userDocListener = firestore.collection("users").document(user.uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e("FIRESTORE", "Error al escuchar perfil del usuario: ${error.message}")
                    _userProfile.value = UserProfileData(user.uid, user.displayName ?: "Chef", user.photoUrl?.toString() ?: "avatar_1")
                    return@addSnapshotListener
                }

                if (doc != null && doc.exists()) {
                    val displayName = doc.getString("displayName") ?: user.displayName ?: "Chef"
                    val photoUrl = doc.getString("photoUrl") ?: user.photoUrl?.toString() ?: "avatar_1"
                    _userProfile.value = UserProfileData(user.uid, displayName, photoUrl)

                    val rawFavs = doc.get("favorites") as? List<*>
                    val firestoreFavs = rawFavs?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
                    _userFavorites.value = firestoreFavs

                    // Sync favorites: Union local and cloud
                    val localFavs = repository.favoritos.value
                    if (localFavs != firestoreFavs) {
                        val union = (localFavs.toSet() + firestoreFavs.toSet()).toList()
                        repository.saveFavorites(union)
                        saveFavoritesToFirestore(union)
                    }
                } else {
                    // Create document for user
                    val displayName = user.displayName ?: "Chef"
                    val photoUrl = user.photoUrl?.toString() ?: "avatar_1"
                    val localFavs = repository.favoritos.value
                    val data = hashMapOf(
                        "displayName" to displayName,
                        "photoUrl" to photoUrl,
                        "favorites" to localFavs
                    )
                    firestore.collection("users").document(user.uid).set(data)
                    _userProfile.value = UserProfileData(user.uid, displayName, photoUrl)
                    _userFavorites.value = localFavs
                }
            }
    }

    fun signInWithEmailAndPassword(email: String, password: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                    onSuccess()
                } else {
                    onFailure(task.exception ?: Exception("Error al iniciar sesión"))
                }
            }
    }

    fun registerWithEmailAndPassword(email: String, password: String, displayName: String, avatarId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userData = hashMapOf(
                            "displayName" to displayName,
                            "photoUrl" to avatarId
                        )
                        firestore.collection("users").document(user.uid).set(userData)
                            .addOnCompleteListener { firestoreTask ->
                                if (firestoreTask.isSuccessful) {
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName)
                                        .build()
                                    user.updateProfile(profileUpdates).addOnCompleteListener {
                                        _currentUser.value = auth.currentUser
                                        _userProfile.value = UserProfileData(user.uid, displayName, avatarId)
                                        onSuccess()
                                    }
                                } else {
                                    onFailure(firestoreTask.exception ?: Exception("Error al guardar perfil en Firestore"))
                                }
                            }
                    } else {
                        onSuccess()
                    }
                } else {
                    onFailure(task.exception ?: Exception("Error al registrar usuario"))
                }
            }
    }

    private fun compressUriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Resize to max 120x120 pixels to keep it small and fast
            val maxDimension = 120
            val width = originalBitmap.width
            val height = originalBitmap.height
            val (newWidth, newHeight) = if (width > height) {
                val ratio = height.toFloat() / width
                maxDimension to (maxDimension * ratio).toInt()
            } else {
                val ratio = width.toFloat() / height
                (maxDimension * ratio).toInt() to maxDimension
            }

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream) // 70% quality
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("BASE64_COMPRESS", "Error compressing image: ${e.message}")
            null
        }
    }

    fun registerWithEmailAndPasswordAndCustomPhoto(
        context: Context,
        email: String,
        password: String,
        displayName: String,
        imageUri: Uri,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        _isPosting.value = true
        val base64Image = compressUriToBase64(context, imageUri)
        if (base64Image == null) {
            _isPosting.value = false
            onFailure(Exception("Error al procesar la imagen seleccionada"))
            return
        }

        val photoUrlValue = "data:image/jpeg;base64,$base64Image"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userData = hashMapOf(
                            "displayName" to displayName,
                            "photoUrl" to photoUrlValue
                        )
                        firestore.collection("users").document(user.uid).set(userData)
                            .addOnCompleteListener { firestoreTask ->
                                if (firestoreTask.isSuccessful) {
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName)
                                        .build()
                                    user.updateProfile(profileUpdates).addOnCompleteListener {
                                        _isPosting.value = false
                                        _currentUser.value = auth.currentUser
                                        _userProfile.value = UserProfileData(user.uid, displayName, photoUrlValue)
                                        onSuccess()
                                    }
                                } else {
                                    _isPosting.value = false
                                    onFailure(firestoreTask.exception ?: Exception("Error al guardar perfil en Firestore"))
                                }
                            }
                    } else {
                        _isPosting.value = false
                        onFailure(Exception("Usuario nulo tras registro exitoso"))
                    }
                } else {
                    _isPosting.value = false
                    onFailure(task.exception ?: Exception("Error al registrar usuario"))
                }
            }
    }

    fun updateProfile(displayName: String, avatarId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val user = auth.currentUser ?: run {
            onFailure(Exception("Usuario no autenticado"))
            return
        }

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userData = hashMapOf(
                        "displayName" to displayName,
                        "photoUrl" to avatarId
                    )
                    firestore.collection("users").document(user.uid).set(userData)
                        .addOnCompleteListener { firestoreTask ->
                            if (firestoreTask.isSuccessful) {
                                _currentUser.value = auth.currentUser
                                _userProfile.value = UserProfileData(user.uid, displayName, avatarId)
                                onSuccess()
                            } else {
                                onFailure(firestoreTask.exception ?: Exception("Error al actualizar Firestore"))
                            }
                        }
                } else {
                    onFailure(task.exception ?: Exception("Error al actualizar perfil"))
                }
            }
    }

    fun uploadProfileImageAndEdit(context: Context, displayName: String, imageUri: Uri, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val user = auth.currentUser ?: run {
            onFailure(Exception("Usuario no autenticado"))
            return
        }

        _isPosting.value = true
        val base64Image = compressUriToBase64(context, imageUri)
        if (base64Image == null) {
            _isPosting.value = false
            onFailure(Exception("Error al procesar la imagen seleccionada"))
            return
        }

        val photoUrlValue = "data:image/jpeg;base64,$base64Image"

        updateProfile(
            displayName = displayName,
            avatarId = photoUrlValue,
            onSuccess = {
                _isPosting.value = false
                onSuccess()
            },
            onFailure = { e ->
                _isPosting.value = false
                onFailure(e)
            }
        )
    }

    fun signOut() {
        userDocListener?.remove()
        userDocListener = null
        auth.signOut()
        _currentUser.value = null
        _userProfile.value = null
        _userFavorites.value = emptyList()
    }

    fun saveFavoritesToFirestore(favorites: List<Int>) {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid)
            .update("favorites", favorites)
            .addOnFailureListener {
                val data = hashMapOf("favorites" to favorites)
                firestore.collection("users").document(user.uid)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
            }
    }

    fun toggleLikeReview(reviewId: String, userId: String) {
        val reviewDoc = firestore.collection("reviews").document(reviewId)
        reviewDoc.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val likedBy = doc.get("likedBy") as? List<*> ?: emptyList<Any>()
                val isLiked = likedBy.contains(userId)

                if (isLiked) {
                    reviewDoc.update(
                        "likedBy", FieldValue.arrayRemove(userId),
                        "likesCount", FieldValue.increment(-1)
                    )
                } else {
                    reviewDoc.update(
                        "likedBy", FieldValue.arrayUnion(userId),
                        "likesCount", FieldValue.increment(1)
                    )
                }
            }
        }
    }

    fun loadReviews() {
        _isLoadingReviews.value = true
        firestore.collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoadingReviews.value = false
                if (error != null) {
                    Log.e("FIRESTORE", "Error cargando reseñas: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val reviewsList = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val userId = doc.getString("userId") ?: ""
                        val userName = doc.getString("userName") ?: ""
                        val userPhotoUrl = doc.getString("userPhotoUrl") ?: ""
                        val comment = doc.getString("comment") ?: ""
                        val rating = doc.getLong("rating")?.toInt() ?: 5
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
                        val likedBy = doc.get("likedBy") as? List<*> ?: emptyList<Any>()
                        val likedByList = likedBy.mapNotNull { it?.toString() }
                        ReviewModel(id, userId, userName, userPhotoUrl, comment, rating, timestamp, likesCount, likedByList)
                    }
                    _reviews.value = reviewsList
                }
            }
    }

    fun postReview(comment: String, rating: Int, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val user = _currentUser.value ?: run {
            onFailure(Exception("Usuario no autenticado"))
            return
        }
        
        _isPosting.value = true
        
        val currentProfile = _userProfile.value
        val userNameValue = currentProfile?.displayName ?: user.displayName ?: "Chef de Uami"
        val userPhotoUrlValue = currentProfile?.photoUrl ?: user.photoUrl?.toString() ?: ""
        
        val reviewData = hashMapOf(
            "userId" to user.uid,
            "userName" to userNameValue,
            "userPhotoUrl" to userPhotoUrlValue,
            "comment" to comment,
            "rating" to rating,
            "timestamp" to System.currentTimeMillis(),
            "likesCount" to 0,
            "likedBy" to emptyList<String>()
        )

        firestore.collection("reviews")
            .add(reviewData)
            .addOnSuccessListener {
                _isPosting.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                _isPosting.value = false
                onFailure(e)
            }
    }

    fun deleteReview(reviewId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("reviews").document(reviewId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}
