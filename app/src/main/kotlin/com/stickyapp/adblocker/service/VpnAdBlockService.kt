package com.stickyapp.adblocker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.stickyapp.adblocker.R
import com.stickyapp.adblocker.blocklist.BlocklistRepository
import com.stickyapp.adblocker.dns.DnsHandler
import com.stickyapp.adblocker.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

class VpnAdBlockService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var dnsSocket: DatagramSocket? = null
    private var isRunning = false
    private val appFilterManager by lazy { AppFilterManager(this) }
    private val blocklistRepository by lazy { BlocklistRepository(this) }
    private val scope = CoroutineScope(Dispatchers.Default)
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "vpn_service"
        private const val DNS_PORT = 53
        private const val UPSTREAM_DNS = "8.8.8.8"
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            "com.stickyapp.adblocker.START_VPN" -> {
                startVpn()
                START_STICKY
            }
            "com.stickyapp.adblocker.STOP_VPN" -> {
                stopVpn()
                START_NOT_STICKY
            }
            else -> START_STICKY
        }
    }

    private fun startVpn() {
        if (isRunning) return
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        scope.launch {
            try {
                setupVpn()
                isRunning = true
                startDnsInterception()
            } catch (e: Exception) {
                e.printStackTrace()
                stopVpn()
            }
        }
    }

    private fun stopVpn() {
        isRunning = false
        cleanupVpn()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun setupVpn() {
        withContext(Dispatchers.IO) {
            val builder = Builder()
            
            // Set DNS
            builder.addDnsServer("127.0.0.1")
            
            // Set routing
            builder.setSession("AdBlocker VPN")
            builder.setMtu(1500)
            
            // Add selected apps
            val selectedApps = appFilterManager.getSelectedApps()
            selectedApps.forEach { packageName ->
                try {
                    builder.addAllowedApplication(packageName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Add IPv4 route
            builder.addRoute("0.0.0.0", 0)
            
            vpnInterface = builder.establish()
            
            // Setup DNS socket for interception
            dnsSocket = DatagramSocket(DNS_PORT)
        }
    }

    private fun startDnsInterception() {
        thread(isDaemon = true) {
            val buffer = ByteArray(512)
            
            try {
                while (isRunning && dnsSocket != null) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    dnsSocket?.receive(packet)
                    
                    val domain = DnsHandler.parseDnsQuery(buffer.take(packet.length).toByteArray())
                    val isBlocked = domain?.let { blocklistRepository.isDomainBlocked(it) } ?: false
                    
                    val response = DnsHandler.buildDnsResponse(
                        buffer.take(packet.length).toByteArray(),
                        domain ?: "unknown",
                        isBlocked
                    )
                    
                    if (response != null) {
                        val responsePacket = DatagramPacket(
                            response,
                            response.size,
                            packet.address,
                            packet.port
                        )
                        dnsSocket?.send(responsePacket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun cleanupVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            dnsSocket?.close()
            dnsSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_vpn),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): NotificationCompat.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
