package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Meneger 2.0", appName)
  }

  @Test
  fun `verify FirebaseApp initialization and safe helpers`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = MenegerApplication.initFirebase(context)
    assertNotNull("FirebaseApp must be initialized", app)
    assertEquals("[DEFAULT]", app?.name)

    val safeApp = MenegerApplication.getFirebaseAppSafe(context)
    assertNotNull("Safe FirebaseApp must not be null", safeApp)

    val auth = MenegerApplication.getFirebaseAuthSafe(context)
    assertNotNull("Safe FirebaseAuth must not be null", auth)

    val firestore = MenegerApplication.getFirebaseFirestoreSafe(context)
    assertNotNull("Safe FirebaseFirestore must not be null", firestore)

    val webClientId = MenegerApplication.getGoogleWebClientId(context)
    assertTrue("Web Client ID must not be blank", webClientId.isNotBlank())
    assertEquals(
      "130566504336-0atgbvgvnc4o1og1tanifnl8ot2a1od5.apps.googleusercontent.com",
      webClientId
    )
  }

  @Test
  fun `launch MainActivity successfully`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    controller.create().start().resume()
  }
}
