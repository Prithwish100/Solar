package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AudioQuality
import com.example.domain.model.ProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.solarDataStore by preferencesDataStore(name = "solar_user_preferences")

data class SolarAppSettings(
    val amoledBlackTheme: Boolean = true,
    val accentIntensity: Float = 1.0f,
    val dynamicArtworkBackground: Boolean = true,
    val enableAnimations: Boolean = true,
    val streamingQuality: AudioQuality = AudioQuality.HIGH,
    val downloadQuality: AudioQuality = AudioQuality.LOSSLESS,
    val crossfadeSeconds: Int = 4,
    val gaplessPlayback: Boolean = true,
    val normalizeVolume: Boolean = true,
    val autoplaySimilar: Boolean = true,
    val defaultProvider: ProviderType = ProviderType.INNERTUBE,
    val wifiOnlyDownloads: Boolean = true,
    val mobileDataSaver: Boolean = false,
    val personalizationEnabled: Boolean = true,
    val ytMusicConnected: Boolean = false,
    val ytMusicAccountName: String = "",
    val jioSaavnEnabled: Boolean = true,
    val hiddenLocalFolders: Set<String> = emptySet(),
    val eqEnabled: Boolean = true,
    val eqPreset: String = "Solar Warmth",
    val bassBoostPercent: Int = 35,
    val virtualizerPercent: Int = 25
)

class SolarPreferencesManager(private val context: Context) {

    private object Keys {
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val ACCENT_INTENSITY = floatPreferencesKey("accent_intensity")
        val DYNAMIC_ARTWORK_BG = booleanPreferencesKey("dynamic_artwork_bg")
        val ENABLE_ANIMATIONS = booleanPreferencesKey("enable_animations")
        val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val NORMALIZE_VOLUME = booleanPreferencesKey("normalize_volume")
        val AUTOPLAY_SIMILAR = booleanPreferencesKey("autoplay_similar")
        val DEFAULT_PROVIDER = stringPreferencesKey("default_provider")
        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        val MOBILE_DATA_SAVER = booleanPreferencesKey("mobile_data_saver")
        val PERSONALIZATION_ENABLED = booleanPreferencesKey("personalization_enabled")
        val YT_MUSIC_CONNECTED = booleanPreferencesKey("yt_music_connected")
        val YT_MUSIC_ACCOUNT = stringPreferencesKey("yt_music_account")
        val JIOSAAVN_ENABLED = booleanPreferencesKey("jiosaavn_enabled")
        val HIDDEN_FOLDERS = stringSetPreferencesKey("hidden_folders")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
    }

    val settingsFlow: Flow<SolarAppSettings> = context.solarDataStore.data.map { prefs ->
        SolarAppSettings(
            amoledBlackTheme = prefs[Keys.AMOLED_BLACK] ?: true,
            accentIntensity = prefs[Keys.ACCENT_INTENSITY] ?: 1.0f,
            dynamicArtworkBackground = prefs[Keys.DYNAMIC_ARTWORK_BG] ?: true,
            enableAnimations = prefs[Keys.ENABLE_ANIMATIONS] ?: true,
            streamingQuality = prefs[Keys.STREAMING_QUALITY]?.let {
                runCatching { AudioQuality.valueOf(it) }.getOrNull()
            } ?: AudioQuality.HIGH,
            downloadQuality = prefs[Keys.DOWNLOAD_QUALITY]?.let {
                runCatching { AudioQuality.valueOf(it) }.getOrNull()
            } ?: AudioQuality.LOSSLESS,
            crossfadeSeconds = prefs[Keys.CROSSFADE_SECONDS] ?: 4,
            gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
            normalizeVolume = prefs[Keys.NORMALIZE_VOLUME] ?: true,
            autoplaySimilar = prefs[Keys.AUTOPLAY_SIMILAR] ?: true,
            defaultProvider = prefs[Keys.DEFAULT_PROVIDER]?.let {
                runCatching { ProviderType.valueOf(it) }.getOrNull()
            } ?: ProviderType.INNERTUBE,
            wifiOnlyDownloads = prefs[Keys.WIFI_ONLY_DOWNLOADS] ?: true,
            mobileDataSaver = prefs[Keys.MOBILE_DATA_SAVER] ?: false,
            personalizationEnabled = prefs[Keys.PERSONALIZATION_ENABLED] ?: true,
            ytMusicConnected = prefs[Keys.YT_MUSIC_CONNECTED] ?: false,
            ytMusicAccountName = prefs[Keys.YT_MUSIC_ACCOUNT] ?: "",
            jioSaavnEnabled = prefs[Keys.JIOSAAVN_ENABLED] ?: true,
            hiddenLocalFolders = prefs[Keys.HIDDEN_FOLDERS] ?: emptySet(),
            eqEnabled = prefs[Keys.EQ_ENABLED] ?: true,
            eqPreset = prefs[Keys.EQ_PRESET] ?: "Solar Warmth",
            bassBoostPercent = prefs[Keys.BASS_BOOST] ?: 35,
            virtualizerPercent = prefs[Keys.VIRTUALIZER] ?: 25
        )
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        context.solarDataStore.edit { it[Keys.AMOLED_BLACK] = enabled }
    }

    suspend fun setAccentIntensity(intensity: Float) {
        context.solarDataStore.edit { it[Keys.ACCENT_INTENSITY] = intensity.coerceIn(0.5f, 1.0f) }
    }

    suspend fun setDynamicArtworkBg(enabled: Boolean) {
        context.solarDataStore.edit { it[Keys.DYNAMIC_ARTWORK_BG] = enabled }
    }

    suspend fun setEnableAnimations(enabled: Boolean) {
        context.solarDataStore.edit { it[Keys.ENABLE_ANIMATIONS] = enabled }
    }

    suspend fun setStreamingQuality(quality: AudioQuality) {
        context.solarDataStore.edit { it[Keys.STREAMING_QUALITY] = quality.name }
    }

    suspend fun setDownloadQuality(quality: AudioQuality) {
        context.solarDataStore.edit { it[Keys.DOWNLOAD_QUALITY] = quality.name }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.solarDataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.coerceIn(0, 12) }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.solarDataStore.edit { it[Keys.GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setNormalizeVolume(enabled: Boolean) {
        context.solarDataStore.edit { it[Keys.NORMALIZE_VOLUME] = enabled }
    }

    suspend fun setAutoplaySimilar(enabled: Boolean) {
        context.solarDataStore.edit { it[Keys.AUTOPLAY_SIMILAR] = enabled }
    }

    suspend fun setDefaultProvider(provider: ProviderType) {
        context.solarDataStore.edit { it[Keys.DEFAULT_PROVIDER] = provider.name }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.solarDataStore.edit { it[Keys.WIFI_ONLY_DOWNLOADS] = enabled }
    }

    suspend fun setPersonalizationEnabled(enabled: Boolean) {
        context.solarDataStore.edit { it[Keys.PERSONALIZATION_ENABLED] = enabled }
    }

    suspend fun setYtMusicConnection(connected: Boolean, accountName: String) {
        context.solarDataStore.edit {
            it[Keys.YT_MUSIC_CONNECTED] = connected
            it[Keys.YT_MUSIC_ACCOUNT] = accountName
        }
    }

    suspend fun setJioSaavnEnabled(enabled: Boolean) {
        context.solarDataStore.edit { it[Keys.JIOSAAVN_ENABLED] = enabled }
    }

    suspend fun toggleHiddenFolder(folderPath: String) {
        context.solarDataStore.edit { prefs ->
            val current = prefs[Keys.HIDDEN_FOLDERS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(folderPath)) current.remove(folderPath) else current.add(folderPath)
            prefs[Keys.HIDDEN_FOLDERS] = current
        }
    }

    suspend fun setEqualizerState(enabled: Boolean, preset: String, bassBoost: Int, virtualizer: Int) {
        context.solarDataStore.edit {
            it[Keys.EQ_ENABLED] = enabled
            it[Keys.EQ_PRESET] = preset
            it[Keys.BASS_BOOST] = bassBoost
            it[Keys.VIRTUALIZER] = virtualizer
        }
    }
}
