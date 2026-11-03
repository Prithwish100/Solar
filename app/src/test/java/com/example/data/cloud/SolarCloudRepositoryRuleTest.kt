package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SolarCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncLikedSong_authenticatedOwner_createsAndReadsSong() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = SolarCloudRepository(firestore)
        val uniqueTrackId = "track_${UUID.randomUUID().toString().take(8)}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncLikedSong(
                trackId = uniqueTrackId,
                title = "Solar Flare",
                artist = "Prithwish",
                album = "Helios",
                artworkUrl = "https://example.com/art.jpg",
                streamUrl = "https://example.com/stream.mp3",
                durationMs = 205000L,
                sourceProvider = "INNERTUBE"
            )
        }
        assertTrue("Expected syncLikedSong to succeed", createResult.isSuccess)

        val emittedSongs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLikedSongs().first { list -> list.any { it.trackId == uniqueTrackId } }
        }
        assertTrue(emittedSongs.any { it.trackId == uniqueTrackId })
    }

    @Test
    fun getLikedSongById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SolarCloudRepository(firestore)
        val uniqueTrackId = "track_${UUID.randomUUID().toString().take(8)}"

        val songId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.syncLikedSong(
                trackId = uniqueTrackId,
                title = "Alice Private Song",
                artist = "Alice",
                album = "Private",
                artworkUrl = "",
                streamUrl = "",
                durationMs = 180000L,
                sourceProvider = "INNERTUBE"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = SolarCloudRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getLikedSongById(aliceUid, songId).getOrThrow()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeLikedSongs_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SolarCloudRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeLikedSongs("alice_unauth_test").first()
            }
            fail("Expected unauthenticated observeLikedSongs to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@solar.test"
        const val BOB_EMAIL = "bob@solar.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
