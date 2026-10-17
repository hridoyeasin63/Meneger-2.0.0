package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test

class FirestoreSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun pushAndFetchUserData_authenticatedUser_succeeds() = runBlocking {
        assumeTrue("Firestore emulator is not running", isEmulatorRunning())
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreSyncRepository(firestore, auth)

        val sampleJson = """{"transactions":[{"id":1,"amount":500.0}],"version":1}"""
        val pushResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.pushUserData(sampleJson)
        }
        assertTrue(pushResult.isSuccess)

        val fetchResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.fetchUserData()
        }
        assertTrue(fetchResult.isSuccess)
        assertEquals(sampleJson, fetchResult.getOrThrow())
    }

    @Test
    fun fetchUserData_unauthenticatedUser_fails() = runBlocking {
        auth.signOut()
        val repo = FirestoreSyncRepository(firestore, auth)

        val fetchResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.fetchUserData()
        }
        assertTrue(fetchResult.isFailure)
    }

    @Test
    fun crossUserAccess_cannotReadOtherUserData() = runBlocking {
        assumeTrue("Firestore emulator is not running", isEmulatorRunning())
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreSyncRepository(firestore, auth)
        val sampleJson = """{"transactions":[{"id":2,"amount":1000.0}],"version":1}"""
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.pushUserData(sampleJson)
        }

        val bobUid = signInTestUser(BOB_EMAIL)
        // Direct attempt by Bob to read Alice's sync data
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("userData").document("sync").get().await()
            }
            fail("Expected FirebaseFirestoreException for cross-user access")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@example.com"
        const val BOB_EMAIL = "bob@example.com"
        const val DEFAULT_TIMEOUT_MS = 10000L
    }
}
