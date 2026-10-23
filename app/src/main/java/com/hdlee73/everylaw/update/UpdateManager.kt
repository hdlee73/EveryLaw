package com.hdlee73.everylaw.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.hdlee73.everylaw.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val version: String,
    val notes: String,
    val publishedAt: String,
    val apkUrl: String?,
    val pageUrl: String,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val latest: ReleaseInfo?) : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: ReleaseInfo, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * GitHub Releases 기반 자동 업데이트.
 *
 * 다른 사람이 만든 APK로 "업데이트"되지 않도록 두 겹으로 막는다.
 * 1) 업데이트 확인 주소는 빌드에 고정된 공식 저장소(BuildConfig.UPDATE_REPO)뿐이다.
 * 2) 받은 APK의 서명 인증서(SHA-256)가 지금 설치된 앱과 다르면 설치하지 않는다.
 *    (안드로이드도 서명이 다른 APK의 덮어쓰기 설치를 거부하며, 서명 키는 GitHub Secrets에만 있다.)
 */
class UpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    suspend fun check(): UpdateState {
        _state.value = UpdateState.Checking
        val result = runCatching { fetchLatest() }.fold(
            onSuccess = { latest ->
                if (latest != null && isNewer(latest.version, currentVersion)) UpdateState.Available(latest)
                else UpdateState.UpToDate(latest)
            },
            onFailure = { UpdateState.Failed("업데이트 확인 실패: ${it.message}") },
        )
        _state.value = result
        return result
    }

    private suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(req).execute().use { res ->
            if (res.code == 404) return@withContext null // 아직 릴리스 없음
            if (!res.isSuccessful) error("HTTP ${res.code}")
            val json = JSONObject(res.body!!.string())
            val assets = json.optJSONArray("assets")
            var apk: String? = null
            if (assets != null) for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) {
                    apk = a.optString("browser_download_url"); break
                }
            }
            ReleaseInfo(
                version = json.optString("tag_name").removePrefix("v"),
                notes = json.optString("body"),
                publishedAt = json.optString("published_at").take(10),
                apkUrl = apk,
                pageUrl = json.optString("html_url"),
            )
        }
    }

    suspend fun download(release: ReleaseInfo) {
        val url = release.apkUrl ?: run {
            _state.value = UpdateState.Failed("릴리스에 APK 파일이 없습니다.")
            return
        }
        _state.value = UpdateState.Downloading(release, 0f)
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val out = File(dir, "Everylaw-${release.version}.apk")
                client.newCall(Request.Builder().url(url).build()).execute().use { res ->
                    if (!res.isSuccessful) error("HTTP ${res.code}")
                    val body = res.body!!
                    val total = body.contentLength().takeIf { it > 0 }
                    body.byteStream().use { input ->
                        out.outputStream().use { output ->
                            val buf = ByteArray(64 * 1024)
                            var read: Int
                            var done = 0L
                            while (input.read(buf).also { read = it } >= 0) {
                                output.write(buf, 0, read)
                                done += read
                                if (total != null) _state.value = UpdateState.Downloading(release, done.toFloat() / total)
                            }
                        }
                    }
                }
                if (!isSignedLikeMe(out)) {
                    out.delete()
                    error("공식 서명과 다른 APK라서 설치하지 않았습니다.")
                }
                out
            }
        }
        _state.value = result.fold(
            onSuccess = { UpdateState.ReadyToInstall(release, it) },
            onFailure = { UpdateState.Failed(it.message ?: "다운로드 실패") },
        )
    }

    /** 설치 화면을 연다. "출처를 알 수 없는 앱" 허용이 필요하면 해당 설정 화면으로 보낸다. */
    fun install(file: File) {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !pm.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun reset() {
        _state.value = UpdateState.Idle
    }

    private fun isSignedLikeMe(apk: File): Boolean {
        val pm = context.packageManager
        val archive = archiveInfo(pm, apk.absolutePath) ?: return false
        if (archive.packageName != context.packageName) return false
        val mine = certDigests(installedInfo(pm))
        val theirs = certDigests(archive)
        return mine.isNotEmpty() && mine == theirs
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(pm: PackageManager, path: String): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
        else pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES)

    @Suppress("DEPRECATION")
    private fun installedInfo(pm: PackageManager): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        else pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)

    @Suppress("DEPRECATION")
    private fun certDigests(info: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners ?: info.signatures
        } else info.signatures
        val md = MessageDigest.getInstance("SHA-256")
        return sigs.orEmpty().map { s -> md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }

    companion object {
        /** "1.10.0" > "1.9.3" 처럼 숫자 단위로 비교 */
        fun isNewer(candidate: String, current: String): Boolean {
            fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val a = parts(candidate)
            val b = parts(current)
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
