package com.kunzisoft.keepass.view

interface ProtectedFieldView {

    var onRevealChanged: ((isRevealed: Boolean) -> Unit)?

    fun setProtection(
        isProtected: Boolean,
        isRevealedByDefault: Boolean,
        needUserVerificationToReveal: Boolean
    )

    fun isRevealed(): Boolean
    fun mask()
    fun reveal()
}