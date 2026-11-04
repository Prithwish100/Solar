package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.cloud.CloudAdminBroadcast
import com.example.ui.theme.SolarBurntOrange
import com.example.ui.theme.SolarCardBackground
import com.example.ui.theme.SolarDeepBlack
import com.example.ui.theme.SolarDivider
import com.example.ui.theme.SolarElevatedSurface
import com.example.ui.theme.SolarMutedGrey
import com.example.ui.theme.SolarPrimaryOrange
import com.example.ui.theme.SolarWarmAmber
import com.example.ui.theme.SolarWhite
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread
import org.json.JSONObject

object SolarSystemNotificationHelper {
    private const val CHANNEL_ID = "solar_admin_broadcasts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SOLAR Admin Announcements & Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct notifications and app updates from SOLAR Admin"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun showSystemNotification(context: Context, title: String, message: String, isUpdate: Boolean = false) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        runCatching {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(if (isUpdate) "🚀 $title" else "🔔 $title")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
        }
    }
}

/**
 * Lightweight embedded HTTP server on port 8765 so the Web Admin Panel can also be opened
 * in any browser at http://127.0.0.1:8765 and dispatch live notifications/updates to the app.
 */
class SolarAdminWebServer(
    private val context: Context,
    private val onNotificationReceived: (title: String, message: String) -> Unit,
    private val onUpdateReceived: (version: String, title: String, notes: String, url: String, mandatory: Boolean) -> Unit
) {
    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var running = false

    fun start(port: Int = 8765) {
        if (running) return
        running = true
        thread(name = "SolarAdminWebServer", isDaemon = true) {
            try {
                val socket = ServerSocket(port, 10, InetAddress.getByName("127.0.0.1"))
                serverSocket = socket
                while (running) {
                    val client = socket.accept() ?: continue
                    thread(isDaemon = true) {
                        try {
                            client.use { c ->
                                val reader = BufferedReader(InputStreamReader(c.getInputStream()))
                                val requestLine = reader.readLine() ?: return@use
                                var contentLength = 0
                                var line = reader.readLine()
                                while (!line.isNullOrEmpty()) {
                                    if (line.lowercase().startsWith("content-length:")) {
                                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                                    }
                                    line = reader.readLine()
                                }
                                val out = c.getOutputStream()
                                if (requestLine.startsWith("POST /api/broadcast")) {
                                    val bodyChars = CharArray(contentLength)
                                    var readTotal = 0
                                    while (readTotal < contentLength) {
                                        val r = reader.read(bodyChars, readTotal, contentLength - readTotal)
                                        if (r == -1) break
                                        readTotal += r
                                    }
                                    val json = JSONObject(String(bodyChars, 0, readTotal.coerceAtLeast(0)))
                                    val type = json.optString("type", "NOTIFICATION")
                                    val title = json.optString("title", "SOLAR Notification")
                                    val msg = json.optString("message", "")
                                    val ver = json.optString("targetVersion", "1.7")
                                    val url = json.optString("actionUrl", "")
                                    val mandatory = json.optBoolean("isMandatory", false)
                                    if (type == "APP_UPDATE") {
                                        onUpdateReceived(ver, title, msg, url, mandatory)
                                    } else {
                                        onNotificationReceived(title, msg)
                                    }
                                    val resp = """{"status":"ok"}"""
                                    val headers = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${resp.toByteArray().size}\r\nConnection: close\r\n\r\n"
                                    out.write(headers.toByteArray())
                                    out.write(resp.toByteArray())
                                    out.flush()
                                } else {
                                    val html = context.assets.open("admin_panel.html").bufferedReader().use { it.readText() }
                                    val htmlBytes = html.toByteArray(Charsets.UTF_8)
                                    val headers = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${htmlBytes.size}\r\nConnection: close\r\n\r\n"
                                    out.write(headers.toByteArray())
                                    out.write(htmlBytes)
                                    out.flush()
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun stop() {
        running = false
        runCatching { serverSocket?.close() }
    }
}

private class SolarAdminJsBridge(
    private val onSendNotif: (String, String) -> Unit,
    private val onSendUpdate: (String, String, String, String, Boolean) -> Unit
) {
    @JavascriptInterface
    fun sendNotification(title: String, message: String) {
        onSendNotif(title, message)
    }

    @JavascriptInterface
    fun sendAppUpdate(version: String, title: String, notes: String, url: String, mandatory: Boolean) {
        onSendUpdate(version, title, notes, url, mandatory)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SolarWebAdminScreen(
    broadcasts: List<CloudAdminBroadcast>,
    onBack: () -> Unit,
    onSendNotification: (title: String, message: String) -> Unit,
    onSendAppUpdate: (version: String, title: String, notes: String, url: String, mandatory: Boolean) -> Unit,
    onDeleteBroadcast: (String) -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("WEB_CONSOLE") } // WEB_CONSOLE, HISTORY

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarDeepBlack)
            .testTag("solar_web_admin_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SolarWhite)
                }
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = SolarBurntOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Web Admin Panel",
                        style = MaterialTheme.typography.titleMedium,
                        color = SolarWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "http://127.0.0.1:8765 • Cloud Sync v1.7",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarWarmAmber
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = activeTab == "WEB_CONSOLE",
                    onClick = { activeTab = "WEB_CONSOLE" },
                    label = { Text("Web Panel") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SolarBurntOrange,
                        selectedLabelColor = SolarWhite,
                        containerColor = SolarCardBackground,
                        labelColor = SolarMutedGrey
                    )
                )
                FilterChip(
                    selected = activeTab == "HISTORY",
                    onClick = { activeTab = "HISTORY" },
                    label = { Text("Sent (${broadcasts.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SolarBurntOrange,
                        selectedLabelColor = SolarWhite,
                        containerColor = SolarCardBackground,
                        labelColor = SolarMutedGrey
                    )
                )
            }
        }

        if (activeTab == "WEB_CONSOLE") {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        addJavascriptInterface(
                            SolarAdminJsBridge(
                                onSendNotif = { t, m -> onSendNotification(t, m) },
                                onSendUpdate = { v, t, n, u, mand -> onSendAppUpdate(v, t, n, u, mand) }
                            ),
                            "SolarAdminBridge"
                        )
                        loadUrl("file:///android_asset/admin_panel.html")
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Open in External Browser", color = SolarWhite, style = MaterialTheme.typography.titleSmall)
                                Text("http://127.0.0.1:8765", color = SolarWarmAmber, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:8765"))
                                        context.startActivity(intent)
                                    }
                                }
                            ) {
                                Text("Launch Browser", color = SolarWarmAmber)
                            }
                        }
                    }
                }

                if (broadcasts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No broadcasts sent yet. Use the Web Panel tab to send notifications or updates.",
                                color = SolarMutedGrey,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(broadcasts, key = { it.id }) { bc ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SolarCardBackground),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = SolarBurntOrange.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (bc.type == "APP_UPDATE") "APP UPDATE • v${bc.targetVersion}" else "NOTIFICATION",
                                            color = SolarWarmAmber,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(bc.title, color = SolarWhite, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(bc.message, color = SolarMutedGrey, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onDeleteBroadcast(bc.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Broadcast", tint = SolarMutedGrey)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SolarLiveNotificationBanner(
    broadcast: CloudAdminBroadcast,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .border(1.dp, SolarBurntOrange, RoundedCornerShape(18.dp))
            .testTag("live_admin_notification_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SolarElevatedSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            SolarBurntOrange.copy(alpha = 0.24f),
                            SolarElevatedSurface
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SolarBurntOrange.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = SolarWarmAmber,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = broadcast.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = SolarWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = broadcast.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = SolarWarmAmber
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss Notification", tint = SolarMutedGrey)
            }
        }
    }
}

@Composable
fun SolarAppUpdateDialog(
    updateBroadcast: CloudAdminBroadcast,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {
            if (!updateBroadcast.isMandatory) onDismiss()
        },
        containerColor = SolarCardBackground,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SolarBurntOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = SolarBurntOrange,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = updateBroadcast.title.ifBlank { "SOLAR Update Available" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = SolarWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = SolarBurntOrange.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Version ${updateBroadcast.targetVersion}",
                        style = MaterialTheme.typography.labelMedium,
                        color = SolarWarmAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "What's New:",
                    style = MaterialTheme.typography.labelLarge,
                    color = SolarWarmAmber,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = updateBroadcast.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SolarWhite
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (updateBroadcast.actionUrl.startsWith("http")) {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateBroadcast.actionUrl))
                            context.startActivity(intent)
                        }
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SolarBurntOrange)
            ) {
                Text("Update Now", color = SolarWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!updateBroadcast.isMandatory) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Later", color = SolarMutedGrey)
                }
            }
        }
    )
}
