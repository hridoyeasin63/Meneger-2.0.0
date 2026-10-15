package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class OperationType { CREATE, READ, UPDATE, DELETE, LIST }

fun handleFirestoreError(error: Exception, operation: OperationType, path: String) {
    try {
        val json = JSONObject().apply {
            put("timestamp", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date()))
            put("operation", operation.name)
            put("path", path)
            put("errorType", error.javaClass.simpleName)
            put("message", error.message ?: "Unknown error")
        }
        Log.e("FirestoreError", json.toString())
    } catch (e: Exception) {
        Log.e("FirestoreError", "Error during $operation on $path: ${error.message}")
    }
}

class FirestoreSyncRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        resolveFirestore(context),
        FirebaseAuth.getInstance()
    )

    companion object {
        private fun resolveFirestore(context: Context): FirebaseFirestore {
            val app = FirebaseApp.getInstance()
            return try {
                val dbId = context.applicationContext.getString(R.string.firestore_database_id)
                if (dbId.isNotBlank() && dbId != "(default)") {
                    FirebaseFirestore.getInstance(app, dbId)
                } else {
                    FirebaseFirestore.getInstance(app)
                }
            } catch (e: Exception) {
                FirebaseFirestore.getInstance(app)
            }
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    suspend fun pushUserData(dataJson: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = firestore.collection("users").document(uid).collection("userData").document("sync")
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                val data = mapOf(
                    "userId" to uid,
                    "dataJson" to dataJson,
                    "version" to 1L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data).await()
            } else {
                val updateData = mapOf(
                    "dataJson" to dataJson,
                    "version" to 1L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateData).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val path = try { "users/${requireUserId()}/userData/sync" } catch (_: Exception) { "users/unknown/userData/sync" }
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchUserData(): Result<String?> {
        return try {
            val uid = requireUserId()
            val docRef = firestore.collection("users").document(uid).collection("userData").document("sync")
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val dataJson = snapshot.getString("dataJson")
                Result.success(dataJson)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            val path = try { "users/${requireUserId()}/userData/sync" } catch (_: Exception) { "users/unknown/userData/sync" }
            handleFirestoreError(e, OperationType.READ, path)
            Result.failure(e)
        }
    }

    fun observeUserData(): Flow<String?> = flow {
        val uid = requireUserId()
        val docRef = firestore.collection("users").document(uid).collection("userData").document("sync")
        docRef.snapshots()
            .map { it.getString("dataJson") }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.READ, docRef.path)
                }
                throw error
            }
            .collect { emit(it) }
    }
}
