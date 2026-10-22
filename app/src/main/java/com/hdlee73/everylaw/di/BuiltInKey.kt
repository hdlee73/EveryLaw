package com.hdlee73.everylaw.di

import android.util.Base64
import com.hdlee73.everylaw.BuildConfig

/** 빌드에 내장된 기본 OC (app/build.gradle.kts 의 obfuscate 와 짝). */
object BuiltInKey {
    private const val SALT = "everylaw-oc"

    val oc: String by lazy {
        val enc = BuildConfig.LAW_OC_ENC
        if (enc.isEmpty()) return@lazy ""
        val salt = SALT.toByteArray()
        val bytes = Base64.decode(enc, Base64.DEFAULT)
        String(ByteArray(bytes.size) { i -> (bytes[i].toInt() xor salt[i % salt.size].toInt()).toByte() }, Charsets.UTF_8)
    }
}
