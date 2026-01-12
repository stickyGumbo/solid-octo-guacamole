package com.stickyapp.adblocker.dns

import java.nio.ByteBuffer

object DnsHandler {
    
    private const val DNS_QUERY_HEADER_SIZE = 12
    private const val TYPE_A = 1
    private const val TYPE_AAAA = 28
    private const val CLASS_IN = 1
    
    fun parseDnsQuery(buffer: ByteArray): String? {
        try {
            if (buffer.size < DNS_QUERY_HEADER_SIZE) return null
            
            val bbuffer = ByteBuffer.wrap(buffer)
            val transactionId = bbuffer.short
            val flags = bbuffer.short
            val qdCount = bbuffer.short.toInt() and 0xFFFF
            val anCount = bbuffer.short.toInt() and 0xFFFF
            val nsCount = bbuffer.short.toInt() and 0xFFFF
            val arCount = bbuffer.short.toInt() and 0xFFFF
            
            // Check if it's a query (not response)
            if ((flags.toInt() and 0x8000) != 0) return null
            if (qdCount == 0) return null
            
            var offset = DNS_QUERY_HEADER_SIZE
            val domain = parseDomainName(buffer, offset) ?: return null
            
            return domain.lowercase()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun buildDnsResponse(
        originalQuery: ByteArray,
        domain: String,
        isBlocked: Boolean
    ): ByteArray? {
        try {
            if (originalQuery.size < DNS_QUERY_HEADER_SIZE) return null
            
            val bbuffer = ByteBuffer.wrap(originalQuery)
            val transactionId = bbuffer.short
            val flags = bbuffer.short
            
            // Extract query type
            var offset = DNS_QUERY_HEADER_SIZE
            val parsedDomain = parseDomainName(originalQuery, offset) ?: return null
            offset += calculateDomainNameLength(originalQuery, DNS_QUERY_HEADER_SIZE)
            
            if (offset + 4 > originalQuery.size) return null
            val queryType = ByteBuffer.wrap(originalQuery, offset, 2).short.toInt() and 0xFFFF
            
            // Build response
            val response = ByteBuffer.allocate(512)
            response.putShort(transactionId)
            response.putShort((0x8000 or 0x0000).toShort()) // Standard response, no error
            response.putShort(1) // Questions
            response.putShort(if (isBlocked) 1 else 0) // Answers
            response.putShort(0) // Authority
            response.putShort(0) // Additional
            
            // Write question
            writeDomainName(response, domain)
            response.putShort(queryType.toShort())
            response.putShort(CLASS_IN.toShort())
            
            // Write answer if blocked
            if (isBlocked) {
                writeDomainName(response, domain)
                response.putShort(queryType.toShort())
                response.putShort(CLASS_IN.toShort())
                response.putInt(300) // TTL
                
                when (queryType) {
                    TYPE_A -> {
                        response.putShort(4) // Data length
                        response.put(127)
                        response.put(0)
                        response.put(0)
                        response.put(1)
                    }
                    TYPE_AAAA -> {
                        response.putShort(16) // Data length
                        for (i in 0 until 16) {
                            response.put(0)
                        }
                    }
                    else -> {
                        response.putShort(0)
                    }
                }
            }
            
            val result = ByteArray(response.position())
            response.rewind()
            response.get(result)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun parseDomainName(buffer: ByteArray, offset: Int): String? {
        return try {
            val labels = mutableListOf<String>()
            var currentOffset = offset
            
            while (currentOffset < buffer.size) {
                val length = buffer[currentOffset].toInt() and 0xFF
                
                if (length == 0) {
                    break
                }
                
                if ((length and 0xC0) == 0xC0) {
                    // Pointer
                    currentOffset += 2
                    break
                }
                
                currentOffset++
                if (currentOffset + length > buffer.size) return null
                
                val label = buffer.slice(currentOffset until currentOffset + length).toByteArray().decodeToString()
                labels.add(label)
                currentOffset += length
            }
            
            labels.joinToString(".")
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateDomainNameLength(buffer: ByteArray, offset: Int): Int {
        var currentOffset = offset
        
        while (currentOffset < buffer.size) {
            val length = buffer[currentOffset].toInt() and 0xFF
            currentOffset++
            
            if (length == 0) break
            if ((length and 0xC0) == 0xC0) {
                currentOffset++
                break
            }
            
            currentOffset += length
        }
        
        return currentOffset - offset
    }

    private fun writeDomainName(buffer: ByteBuffer, domain: String) {
        domain.split(".").forEach { label ->
            buffer.put(label.length.toByte())
            buffer.put(label.toByteArray())
        }
        buffer.put(0)
    }
}
