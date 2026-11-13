package io.github.aedev.flow.youtubesync.account

import io.github.aedev.flow.data.local.KeystoreSecretBox
import javax.inject.Inject

/** Encrypts the token bundle before it is written. A seam so tests run without the Android Keystore. */
interface SecretSealer {
    fun seal(plain: String): String

    /** The plain text, or empty when [sealed] cannot be opened any more. */
    fun open(sealed: String?): String
}

class KeystoreSecretSealer
    @Inject
    constructor() : SecretSealer {
        override fun seal(plain: String): String = KeystoreSecretBox.seal(plain)

        override fun open(sealed: String?): String = KeystoreSecretBox.open(sealed)
    }
