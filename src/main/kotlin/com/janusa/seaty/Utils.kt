package com.janusa.seaty

import java.security.MessageDigest

object Utils {
    /**
     * Compares two strings for equality.
     *
     * The calculation time depends only on the length of [a].
     * It does not depend on the length of [b] or the contents
     * of [a] or [b].
     */
    fun constantTimeEquals(
        a: String,
        b: String,
    ): Boolean = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}
