package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val avatarUrl: String? = null,
    val preferredAudioQuality: String = "HIGH",
    val amoledDarkTheme: Boolean = true,
    val ytMusicConnected: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudLikedSong(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val sourceProvider: String = "INNERTUBE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudPlaylistDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val isPublic: Boolean = false,
    val trackCount: Long = 0L,
    val sourceOrigin: String = "SOLAR",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudHistoryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val durationMs: Long = 0L,
    val sourceProvider: String = "INNERTUBE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudAdminBroadcast(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderEmail: String? = null,
    val type: String = "NOTIFICATION", // "NOTIFICATION" or "APP_UPDATE"
    val title: String = "",
    val message: String = "",
    val targetVersion: String = "1.7",
    val actionUrl: String = "",
    val isMandatory: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class SolarCloudRepository(
    private val db: FirebaseFirestore
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun upsertUserProfile(
        displayName: String,
        email: String? = null,
        avatarUrl: String? = null,
        preferredAudioQuality: String = "HIGH",
        amoledDarkTheme: Boolean = true,
        ytMusicConnected: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to displayName.take(80).ifBlank { "SOLAR Listener" },
                    "preferredAudioQuality" to preferredAudioQuality,
                    "amoledDarkTheme" to amoledDarkTheme,
                    "ytMusicConnected" to ytMusicConnected,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) updatePayload["email"] = email.take(120)
                if (!avatarUrl.isNullOrBlank()) updatePayload["avatarUrl"] = avatarUrl.take(500)
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to displayName.take(80).ifBlank { "SOLAR Listener" },
                    "preferredAudioQuality" to preferredAudioQuality,
                    "amoledDarkTheme" to amoledDarkTheme,
                    "ytMusicConnected" to ytMusicConnected,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) createPayload["email"] = email.take(120)
                if (!avatarUrl.isNullOrBlank()) createPayload["avatarUrl"] = avatarUrl.take(500)
                docRef.set(createPayload).await()
            }
            uid
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun syncLikedSong(
        trackId: String,
        title: String,
        artist: String,
        album: String,
        artworkUrl: String,
        streamUrl: String,
        durationMs: Long,
        sourceProvider: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val sanitizedId = trackId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "track_1" }
        val docRef = db.collection("users").document(uid).collection("liked_songs").document(sanitizedId)
        try {
            val payload = mapOf(
                "userId" to uid,
                "trackId" to sanitizedId,
                "title" to title.take(200).ifBlank { "Untitled" },
                "artist" to artist.take(200).ifBlank { "Unknown Artist" },
                "album" to album.take(200),
                "artworkUrl" to artworkUrl.take(600),
                "streamUrl" to streamUrl.take(600),
                "durationMs" to durationMs.coerceIn(0L, 86400000L),
                "sourceProvider" to sourceProvider.take(50).ifBlank { "INNERTUBE" },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            sanitizedId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun removeCloudLikedSong(trackId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val sanitizedId = trackId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "track_1" }
        val docRef = db.collection("users").document(uid).collection("liked_songs").document(sanitizedId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun getLikedSongs(targetUserId: String? = null): Result<List<CloudLikedSong>> = runCatching {
        val uid = targetUserId ?: requireUserId()
        val colRef = db.collection("users").document(uid).collection("liked_songs")
        try {
            val snap = colRef.get().await()
            snap.toObjects(CloudLikedSong::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            throw e
        }
    }

    suspend fun getLikedSongById(targetUserId: String, songId: String): Result<CloudLikedSong?> = runCatching {
        val docRef = db.collection("users").document(targetUserId).collection("liked_songs").document(songId)
        try {
            val snap = docRef.get().await()
            snap.toObject(CloudLikedSong::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun observeLikedSongs(userId: String? = null): Flow<List<CloudLikedSong>> = flow {
        val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated_user"
        val path = "users/$uid/liked_songs"
        emitAll(
            db.collection("users").document(uid).collection("liked_songs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CloudLikedSong::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun syncPlaylist(
        playlistId: String,
        name: String,
        description: String,
        isPublic: Boolean,
        trackCount: Int,
        sourceOrigin: String = "SOLAR"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val sanitizedId = playlistId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "pl_1" }
        val docRef = db.collection("users").document(uid).collection("playlists").document(sanitizedId)
        try {
            val validOrigin = if (sourceOrigin in listOf("SOLAR", "YOUTUBE_MUSIC", "JIOSAAVN", "M3U")) sourceOrigin else "SOLAR"
            val payload = mapOf(
                "userId" to uid,
                "name" to name.take(120).ifBlank { "My Playlist" },
                "description" to description.take(400),
                "isPublic" to isPublic,
                "trackCount" to trackCount.toLong().coerceIn(0L, 10000L),
                "sourceOrigin" to validOrigin,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            sanitizedId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeCloudPlaylists(userId: String? = null): Flow<List<CloudPlaylistDoc>> = flow {
        val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated_user"
        val path = "users/$uid/playlists"
        emitAll(
            db.collection("users").document(uid).collection("playlists")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CloudPlaylistDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun logCloudListeningHistory(
        historyId: String,
        trackId: String,
        title: String,
        artist: String,
        durationMs: Long,
        sourceProvider: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val sanitizedId = historyId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "hist_1" }
        val docRef = db.collection("users").document(uid).collection("listening_history").document(sanitizedId)
        try {
            val payload = mapOf(
                "userId" to uid,
                "trackId" to trackId.take(128).ifBlank { "track_1" },
                "title" to title.take(200).ifBlank { "Untitled" },
                "artist" to artist.take(200).ifBlank { "Unknown Artist" },
                "durationMs" to durationMs.coerceIn(0L, 86400000L),
                "sourceProvider" to sourceProvider.take(50).ifBlank { "INNERTUBE" },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            sanitizedId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun publishAdminBroadcast(
        broadcastId: String,
        type: String,
        title: String,
        message: String,
        targetVersion: String = "1.7",
        actionUrl: String = "",
        isMandatory: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val email = auth.currentUser?.email
        val sanitizedId = broadcastId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "bc_${System.currentTimeMillis()}" }
        val docRef = db.collection("broadcasts").document(sanitizedId)
        try {
            val validType = if (type in listOf("NOTIFICATION", "APP_UPDATE")) type else "NOTIFICATION"
            val payload = mutableMapOf<String, Any>(
                "senderId" to uid,
                "type" to validType,
                "title" to title.trim().take(140).ifBlank { "SOLAR Announcement" },
                "message" to message.trim().take(1000).ifBlank { "New update available in SOLAR." },
                "targetVersion" to targetVersion.trim().take(30).ifBlank { "1.7" },
                "actionUrl" to actionUrl.trim().take(600),
                "isMandatory" to isMandatory,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!email.isNullOrBlank()) {
                payload["senderEmail"] = email.take(120)
            }
            docRef.set(payload).await()
            sanitizedId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAdminBroadcast(broadcastId: String): Result<Unit> = runCatching {
        requireUserId()
        val sanitizedId = broadcastId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("broadcasts").document(sanitizedId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeAdminBroadcasts(): Flow<List<CloudAdminBroadcast>> = flow {
        val path = "broadcasts"
        emitAll(
            db.collection("broadcasts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CloudAdminBroadcast::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }
}
