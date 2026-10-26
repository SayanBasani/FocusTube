package com.focustube.app.util

import android.util.Log

object SafeLogger {
    private const val TAG_PREFIX = "FocusTube_"

    fun d(tag: String, message: String) {
        try {
            Log.d("$TAG_PREFIX$tag", sanitize(message))
        } catch (_: Exception) {}
    }

    fun i(tag: String, message: String) {
        try {
            Log.i("$TAG_PREFIX$tag", sanitize(message))
        } catch (_: Exception) {}
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.w("$TAG_PREFIX$tag", sanitize(message), throwable)
            } else {
                Log.w("$TAG_PREFIX$tag", sanitize(message))
            }
        } catch (_: Exception) {}
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.e("$TAG_PREFIX$tag", sanitize(message), throwable)
            } else {
                Log.e("$TAG_PREFIX$tag", sanitize(message))
            }
        } catch (_: Exception) {}
    }

    /**
     * Strips potential sensitive authentication headers or token strings from output.
     */
    private fun sanitize(input: String): String {
        return input.replace(Regex("(?i)(key|token|auth|password)=[^&\\s]+"), "$1=REDACTED")
    }
}
