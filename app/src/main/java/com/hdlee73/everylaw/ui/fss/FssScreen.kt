package com.hdlee73.everylaw.ui.fss

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hdlee73.everylaw.core.parser.ArticleCitationExtractor
import com.hdlee73.everylaw.core.parser.ExtractedCitation
import com.hdlee73.everylaw.ui.library.BackTopBar
import org.json.JSONTokener

/** 금융감독원 공개정보 목록. 상세 화면의 주소는 사이트가 정하는 대로 따라간다. */
const val FSS_OPEN_INFO_URL = "https://www.fss.or.kr/fss/job/openInfo/list.do?menuNo=200476"

/**
 * 금감원 공개정보 사이트를 앱 안에서 열고, 지금 보고 있는 글에 인용된 조문을 이 앱의 조문으로 연결한다.
 * 사이트의 HTML 구조에 기대지 않고 화면에 보이는 글자(document.body.innerText)만 읽는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FssScreen(onBack: () -> Unit, onCite: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("금감원 공개정보") }
    var found by remember { mutableStateOf<List<ExtractedCitation>?>(null) }
    var readFailed by remember { mutableStateOf(false) }

    // 뒤로가기: 사이트 안에서 이전 페이지가 있으면 그쪽으로, 없으면 앱의 이전 화면으로
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Scaffold(
        topBar = { BackTopBar(title, onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    webView?.evaluateJavascript("(function(){return document.body ? document.body.innerText : ''})()") { raw ->
                        val text = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull()
                        if (text == null) readFailed = true else { readFailed = false; found = ArticleCitationExtractor.extract(text) }
                    }
                },
                icon = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
                text = { Text("인용 조문 찾기") },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val uri = request.url
                                val host = uri.host.orEmpty()
                                if (uri.scheme == "https" && (host == "fss.or.kr" || host.endsWith(".fss.or.kr"))) return false
                                // 다른 사이트나 첨부 파일은 브라우저·다른 앱에 맡긴다
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                return true
                            }
                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) { canGoBack = view.canGoBack() }
                            override fun onPageFinished(view: WebView, url: String?) {
                                canGoBack = view.canGoBack()
                                view.title?.takeIf { it.isNotBlank() }?.let { title = it }
                            }
                        }
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress }
                        }
                        loadUrl(FSS_OPEN_INFO_URL)
                        webView = this
                    }
                },
                onRelease = { it.destroy() },
            )
        }
    }

    if (found != null || readFailed) {
        ModalBottomSheet(onDismissRequest = { found = null; readFailed = false }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("이 글에 인용된 조문", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp))
                val list = found.orEmpty()
                when {
                    readFailed -> Note("화면의 글을 읽지 못했어요. 페이지가 다 열린 뒤 다시 눌러 주세요.")
                    list.isEmpty() -> Note("법령명과 함께 적힌 조문 인용을 찾지 못했어요. 목록 화면이면 개별 글을 연 다음에 눌러 주세요. '자본시장법 제55조'처럼 법령명이 앞에 붙은 인용만 찾아요.")
                    else -> {
                        Note("누르면 그 조문의 위임 구조로 이동해요. 글에 적힌 법령명 그대로 검색하므로 약칭이 다르면 결과가 없을 수 있어요.")
                        LazyColumn {
                            items(list, key = { it.query }) { c ->
                                ListItem(
                                    headlineContent = { Text(c.query) },
                                    supportingContent = { Text(if (c.count > 1) "${c.text} · ${c.count}회 인용" else c.text) },
                                    modifier = Modifier.clickable { found = null; onCite(c.query) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
}
