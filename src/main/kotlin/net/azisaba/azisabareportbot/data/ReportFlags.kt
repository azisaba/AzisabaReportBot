package net.azisaba.azisabareportbot.data

object ReportFlags {
    const val OPEN = 1 shl 0
    const val CLOSED = 1 shl 1
    const val RESOLVED = 1 shl 2
    const val INVALID = 1 shl 3
    const val NEED_MORE_PROOF = 1 shl 4
}
