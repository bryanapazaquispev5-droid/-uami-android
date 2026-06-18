package com.example.uami.recipes.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.uami.recipes.models.ReviewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class FirebaseRepository {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    fun compressUriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

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
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("BASE64_COMPRESS", "Error compressing image: ${e.message}")
            null
        }
    }
}
