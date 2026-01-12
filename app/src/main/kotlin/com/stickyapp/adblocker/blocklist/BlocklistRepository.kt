package com.stickyapp.adblocker.blocklist

import android.content.Context

class BlocklistRepository(private val context: Context) {
    
    fun getBlocklist(): Set<String> {
        BlocklistManager.loadBlocklists(context)
        return BlocklistManager.getBlocklist(context)
    }

    fun isDomainBlocked(domain: String): Boolean {
        val blocklist = getBlocklist()
        val domainLower = domain.lowercase()
        
        // Check for exact match
        if (blocklist.contains(domainLower)) {
            return true
        }
        
        // Check for wildcard match (e.g., ads.example.com matches if *.example.com is in list)
        val parts = domainLower.split(".")
        for (i in 1 until parts.size) {
            val wildcard = "*." + parts.subList(i, parts.size).joinToString(".")
            if (blocklist.contains(wildcard)) {
                return true
            }
        }
        
        return false
    }

    fun addCustomDomain(domain: String): Boolean {
        return BlocklistManager.addCustomDomain(context, domain)
    }

    fun removeCustomDomain(domain: String) {
        BlocklistManager.removeCustomDomain(context, domain)
    }

    suspend fun addRemoteBlocklist(url: String): Boolean {
        return BlocklistManager.addRemoteBlocklist(context, url)
    }

    suspend fun refreshRemoteBlocklists() {
        BlocklistManager.refreshRemoteBlocklists(context)
    }
}
