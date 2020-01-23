package org.mozilla.fenix.detektrules

object MozillaLogger {
    fun logError(msg: String) {
        System.err.println(msg)
    }

    private fun logNonError(msg: String) {
        System.out.println(msg)
    }

    fun logWarning(msg: String) = logNonError(msg)
    fun logInfo(msg: String) = logNonError(msg)
}
