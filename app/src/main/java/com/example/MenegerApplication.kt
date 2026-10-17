package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MenegerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        val app = initFirebase(this)
        if (app != null) {
            Log.i(TAG, "Meneger 2.0 Firebase initialized successfully in Application onCreate: ${app.name}")
        } else {
            Log.w(TAG, "Meneger 2.0 Firebase could not be initialized in Application onCreate.")
        }
    }

    companion object {
        private const val TAG = "MenegerApp"
        private var instance: MenegerApplication? = null

        // Verified project configuration constants matching google-services.json for package "com.aistudio.meneger2.hridoy"
        const val FIREBASE_PROJECT_ID = "master-63"
        const val FIREBASE_APPLICATION_ID = "1:130566504336:android:ed9c3d833d6426ae587d15"
        const val FIREBASE_API_KEY = "AIzaSyBrj7GIIcSWZE0LTaT_qKXGinU1LECaiHc"
        const val FIREBASE_GCM_SENDER_ID = "130566504336"
        const val FIREBASE_STORAGE_BUCKET = "master-63.firebasestorage.app"
        const val DEFAULT_WEB_CLIENT_ID = "130566504336-0atgbvgvnc4o1og1tanifnl8ot2a1od5.apps.googleusercontent.com"

        fun getAppInstance(): MenegerApplication? = instance

        @Synchronized
        fun initFirebase(context: Context): FirebaseApp? {
            val appContext = context.applicationContext ?: context
            try {
                // 1. Check if the default app is already initialized in this process
                val existingDefaultApp = FirebaseApp.getApps(appContext).firstOrNull {
                    it.name == FirebaseApp.DEFAULT_APP_NAME
                }
                if (existingDefaultApp != null) {
                    Log.d(TAG, "Default FirebaseApp already initialized: ${existingDefaultApp.name}")
                    return existingDefaultApp
                }

                // 2. Strategy 1: Standard auto-initialization from merged google-services resources
                Log.d(TAG, "Attempting standard FirebaseApp.initializeApp(context)...")
                try {
                    val autoApp = FirebaseApp.initializeApp(appContext)
                    if (autoApp != null) {
                        Log.i(TAG, "FirebaseApp initialized via auto configuration.")
                        return autoApp
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Auto initialization threw: ${e.message}, falling back to explicit configuration.")
                }

                // 3. Strategy 2: Explicit FirebaseOptions build with dynamic resource resolution (no direct R.string references)
                Log.d(TAG, "Building FirebaseOptions from dynamic application resources and verified defaults...")
                val res = appContext.resources
                val pkg = appContext.packageName

                fun resolveString(resName: String, fallback: String): String {
                    // Try identifier lookup in application package name
                    try {
                        val id = res.getIdentifier(resName, "string", pkg)
                        if (id != 0) {
                            val str = res.getString(id)
                            if (str.isNotBlank()) return str
                        }
                    } catch (_: Exception) {}

                    // Try identifier lookup in project namespace "com.example"
                    try {
                        val id = res.getIdentifier(resName, "string", "com.example")
                        if (id != 0) {
                            val str = res.getString(id)
                            if (str.isNotBlank()) return str
                        }
                    } catch (_: Exception) {}

                    return fallback
                }

                val appId = resolveString("google_app_id", FIREBASE_APPLICATION_ID)
                val apiKey = resolveString("google_api_key", FIREBASE_API_KEY)
                val projectId = resolveString("project_id", FIREBASE_PROJECT_ID)
                val gcmSenderId = resolveString("gcm_defaultSenderId", FIREBASE_GCM_SENDER_ID)
                val storageBucket = resolveString("google_storage_bucket", FIREBASE_STORAGE_BUCKET)

                val builder = FirebaseOptions.Builder()
                    .setApplicationId(appId)
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setGcmSenderId(gcmSenderId)
                    .setStorageBucket(storageBucket)

                val options = builder.build()
                val explicitApp = try {
                    FirebaseApp.initializeApp(appContext, options)
                } catch (ise: IllegalStateException) {
                    // Already initialized concurrently or default app name already registered
                    FirebaseApp.getApps(appContext).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                        ?: FirebaseApp.getInstance()
                }

                Log.i(TAG, "FirebaseApp explicitly initialized with project: $projectId (appId: $appId)")
                return explicitApp
            } catch (e: Throwable) {
                Log.e(TAG, "Exception during FirebaseApp initialization: ${e.message}", e)
                return null
            }
        }

        fun getFirebaseAppSafe(context: Context? = null): FirebaseApp? {
            val ctx = context?.applicationContext ?: instance ?: return null
            return try {
                val existingApp = FirebaseApp.getApps(ctx).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                existingApp ?: initFirebase(ctx)
            } catch (e: Throwable) {
                Log.w(TAG, "Unable to get FirebaseApp: ${e.message}")
                initFirebase(ctx)
            }
        }

        fun getFirebaseAuthSafe(context: Context? = null): FirebaseAuth? {
            return try {
                val ctx = context?.applicationContext ?: instance ?: return null
                val app = getFirebaseAppSafe(ctx) ?: return null
                FirebaseAuth.getInstance(app)
            } catch (e: Throwable) {
                Log.w(TAG, "FirebaseAuth unavailable: ${e.message}")
                null
            }
        }

        fun getFirebaseFirestoreSafe(context: Context? = null): FirebaseFirestore? {
            return try {
                val ctx = context?.applicationContext ?: instance ?: return null
                val app = getFirebaseAppSafe(ctx) ?: return null
                val dbId = try {
                    val id = ctx.resources.getIdentifier("firestore_database_id", "string", ctx.packageName)
                    val resolvedId = if (id != 0) id else ctx.resources.getIdentifier("firestore_database_id", "string", "com.example")
                    if (resolvedId != 0) ctx.getString(resolvedId) else null
                } catch (_: Exception) {
                    null
                }
                if (!dbId.isNullOrBlank() && dbId != "(default)") {
                    try {
                        FirebaseFirestore.getInstance(app, dbId)
                    } catch (t: Throwable) {
                        Log.w(TAG, "Named database $dbId unavailable, falling back to default: ${t.message}")
                        FirebaseFirestore.getInstance(app)
                    }
                } else {
                    FirebaseFirestore.getInstance(app)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "FirebaseFirestore unavailable: ${e.message}")
                null
            }
        }

        fun getGoogleWebClientId(context: Context? = null): String {
            val ctx = context?.applicationContext ?: instance
            if (ctx != null) {
                try {
                    val id = ctx.resources.getIdentifier("default_web_client_id", "string", ctx.packageName)
                    if (id != 0) {
                        val str = ctx.getString(id)
                        if (str.isNotBlank()) return str
                    }
                } catch (_: Exception) {}

                try {
                    val id = ctx.resources.getIdentifier("default_web_client_id", "string", "com.example")
                    if (id != 0) {
                        val str = ctx.getString(id)
                        if (str.isNotBlank()) return str
                    }
                } catch (_: Exception) {}
            }
            return DEFAULT_WEB_CLIENT_ID
        }
    }
}
