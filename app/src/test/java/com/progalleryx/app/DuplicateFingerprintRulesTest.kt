package com.progalleryx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.MessageDigest

class DuplicateFingerprintRulesTest {
    @Test fun identicalBytesHaveIdenticalKeys() {
        val bytes = "sample image data".toByteArray()
        val first = MessageDigest.getInstance("SHA-256").digest(bytes)
        val second = MessageDigest.getInstance("SHA-256").digest(bytes)
        assertEquals(DuplicateFingerprintRules.key(bytes.size.toLong(), first),
            DuplicateFingerprintRules.key(bytes.size.toLong(), second))
    }
    @Test fun sameSizeDifferentContentIsNotDuplicate() {
        val a = MessageDigest.getInstance("SHA-256").digest("abc".toByteArray())
        val b = MessageDigest.getInstance("SHA-256").digest("xyz".toByteArray())
        assertNotEquals(DuplicateFingerprintRules.key(3, a), DuplicateFingerprintRules.key(3, b))
    }
    @Test fun countsOnlyAdditionalCopies() {
        assertEquals(400L, DuplicateFingerprintRules.reclaimableBytes(listOf(200,200,200)))
        assertEquals(0L, DuplicateFingerprintRules.reclaimableBytes(listOf(200)))
    }
    @Test fun invalidDigestAndSizesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { DuplicateFingerprintRules.key(0, ByteArray(32)) }
        assertThrows(IllegalArgumentException::class.java) { DuplicateFingerprintRules.key(100, ByteArray(8)) }
    }
}
