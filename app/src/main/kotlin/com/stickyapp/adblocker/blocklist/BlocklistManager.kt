package com.stickyapp.adblocker.blocklist

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object BlocklistManager {
    private val defaultBlocklist = setOf(
        // Common ad networks
        "ad.doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "pagead2.googlesyndication.com",
        "ads.google.com",
        "adservice.google.com",
        
        // Facebook/Meta
        "facebook.com",
        "fbcdn.net",
        "fbsbx.com",
        "m.facebook.com",
        
        // YouTube ads
        "youtube-nocookie.com",
        "yt-ad-metrics.ggpht.com",
        "s.youtube.com",
        "www.youtube.com/get_midroll_info",
        
        // Other major ad networks
        "criteo.com",
        "criteo.net",
        "teads.tv",
        "rubiconproject.com",
        "openx.net",
        "pubmatic.com",
        "xandr.com",
        "appnexus.com",
        
        // Analytics
        "google-analytics.com",
        "analytics.google.com",
        "metrics.google.com",
        
        // Tracking
        "tracking.google.com",
        "safebrowsing.google.com",
    )

    private var customDomains = mutableSetOf<String>()
    private var remoteBlocklists = mutableMapOf<String, Set<String>>()

    fun getBlocklist(context: Context): Set<String> {
        val prefs = context.getSharedPreferences("blocklist", Context.MODE_PRIVATE)
        
        customDomains = prefs.getStringSet("custom_domains", setOf())?.toMutableSet() ?: mutableSetOf()
        
        val combined = defaultBlocklist.toMutableSet()
        combined.addAll(customDomains)
        remoteBlocklists.forEach { (_, domains) ->
            combined.addAll(domains)
        }
        
        return combined
    }

    fun addCustomDomain(context: Context, domain: String): Boolean {
        if (!isValidDomain(domain)) return false
        
        customDomains.add(domain.lowercase())
        saveCustomDomains(context)
        return true
    }

    fun removeCustomDomain(context: Context, domain: String) {
        customDomains.remove(domain.lowercase())
        saveCustomDomains(context)
    }

    suspend fun addRemoteBlocklist(context: Context, url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val domains = fetchBlocklist(url)
                if (domains.isNotEmpty()) {
                    remoteBlocklists[url] = domains
                    saveRemoteBlocklists(context)
                    return@withContext true
                }
                false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun refreshRemoteBlocklists(context: Context) {
        withContext(Dispatchers.IO) {
            remoteBlocklists.forEach { (url, _) ->
                try {
                    val domains = fetchBlocklist(url)
                    remoteBlocklists[url] = domains
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            saveRemoteBlocklists(context)
        }
    }

    private suspend fun fetchBlocklist(url: String): Set<String> {
        return withContext(Dispatchers.IO) {
            val domains = mutableSetOf<String>()
            try {
                val connection = URL(url).openConnection()
                connection.readTimeout = 10000
                connection.connectTimeout = 10000
                
                connection.getInputStream().bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        val domain = line.trim().lowercase()
                        if (domain.isNotEmpty() && !domain.startsWith("#") && isValidDomain(domain)) {
                            domains.add(domain)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            domains
        }
    }

    private fun isValidDomain(domain: String): Boolean {
        val domainRegex = Regex(
            "^(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)*[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$",
            RegexOption.IGNORE_CASE
        )
        return domainRegex.matches(domain) && domain.length <= 255
    }

    private fun saveCustomDomains(context: Context) {
        val prefs = context.getSharedPreferences("blocklist", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("custom_domains", customDomains).apply()
    }

    private fun saveRemoteBlocklists(context: Context) {
        val prefs = context.getSharedPreferences("blocklist", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("remote_blocklists", remoteBlocklists.keys).apply()
        remoteBlocklists.forEach { (url, domains) ->
            prefs.edit().putStringSet("blocklist_$url", domains).apply()
        }
    }

    fun loadBlocklists(context: Context) {
        val prefs = context.getSharedPreferences("blocklist", Context.MODE_PRIVATE)
        customDomains = prefs.getStringSet("custom_domains", setOf())?.toMutableSet() ?: mutableSetOf()
        
        val remoteUrls = prefs.getStringSet("remote_blocklists", setOf()) ?: setOf()
        remoteBlocklists.clear()
        remoteUrls.forEach { url ->
            val domains = prefs.getStringSet("blocklist_$url", setOf()) ?: setOf()
            if (domains.isNotEmpty()) {
                remoteBlocklists[url] = domains
            }
        }
    }
}
