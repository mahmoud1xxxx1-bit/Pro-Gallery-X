package com.progalleryx.app

/** Groups only exact matching size-and-SHA256 fingerprints. */
object DuplicateFingerprintRules {
    fun key(size: Long, digest: ByteArray): String {
        require(size > 0L && digest.size == 32)
        return size.toString() + ":" + digest.joinToString("") { "%02x".format(it) }
    }
    fun reclaimableBytes(sizes: List<Long>): Long {
        require(sizes.all { it > 0 })
        return sizes.drop(1).sum()
    }
}
