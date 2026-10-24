package com.hdlee73.everylaw.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hdlee73.everylaw.BuildConfig
import com.hdlee73.everylaw.R
import com.hdlee73.everylaw.ui.library.BackTopBar
import com.hdlee73.everylaw.update.Changelog
import com.hdlee73.everylaw.update.UpdateState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onOpenAbout: () -> Unit, onOpenFss: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val update by vm.updateState.collectAsStateWithLifecycle()
    var oc by rememberSaveable(settings.userOc) { mutableStateOf(settings.userOc) }
    var reveal by rememberSaveable { mutableStateOf(false) }
    // 기본 키가 내장된 빌드에서는 입력란을 접어 두고, 원할 때만 내 OC로 덮어쓴다
    var showOverride by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = { LargeTopAppBar(title = { Text("설정") }) }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("국가법령정보 API 인증(OC)", style = MaterialTheme.typography.titleMedium)
                    }
                    val overrideOpen = !vm.hasBuiltInKey || showOverride || settings.userOc.isNotEmpty()
                    OcGuideText(hasBuiltInKey = vm.hasBuiltInKey, usingOwnKey = settings.userOc.isNotBlank())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!overrideOpen) TextButton(onClick = { showOverride = true }) { Text("내 OC로 바꾸기") }
                        OpenOcGuideButton()
                    }
                    if (overrideOpen) Text(
                        "입력한 OC는 이 기기에만 저장되고, 기본 키보다 우선해서 쓰여요.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (overrideOpen) OutlinedTextField(
                        value = oc,
                        onValueChange = { oc = it },
                        label = { Text("OC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { reveal = !reveal }) {
                                Icon(if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (reveal) "숨기기" else "보기")
                            }
                        },
                    )
                    if (overrideOpen) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.saveOc(oc) }, enabled = oc != settings.userOc) { Text("저장") }
                        if (settings.userOc.isNotEmpty()) {
                            OutlinedButton(onClick = { oc = ""; vm.saveOc(""); showOverride = false }) {
                                Text(if (vm.hasBuiltInKey) "기본 키로 되돌리기" else "지우기")
                            }
                        }
                    }
                    val mode = when {
                        settings.userOc.isNotBlank() -> "직접 입력한 키로 실제 API 조회 중"
                        vm.hasBuiltInKey -> "기본 내장 키로 실제 API 조회 중"
                        else -> "데모 모드"
                    }
                    Text("현재 상태: $mode", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("분할 화면", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "위임 구조를 왼쪽 목록·오른쪽 본문으로 나눠 볼지 정해요.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    com.hdlee73.everylaw.data.SplitMode.entries.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { vm.setSplitMode(m) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(selected = settings.splitMode == m, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Text(m.label, style = MaterialTheme.typography.bodyLarge)
                                Text(m.desc, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    ListItem(
                        headlineContent = { Text("자동 업데이트 확인") },
                        supportingContent = { Text("앱을 열 때 하루 한 번 새 버전을 확인해요") },
                        leadingContent = { Icon(Icons.Outlined.SystemUpdate, null) },
                        trailingContent = { Switch(checked = settings.autoUpdate, onCheckedChange = vm::setAutoUpdate) },
                    )
                    UpdateStatusRow(update, onCheck = { vm.checkUpdate() })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("금감원 공개정보") },
                        supportingContent = { Text("공개정보 글에 인용된 조문을 이 앱의 조문으로 연결") },
                        leadingContent = { Icon(Icons.Outlined.Info, null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Outlined.ArrowForward, null) },
                        modifier = Modifier.clickable(role = Role.Button, onClick = onOpenFss),
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("앱 정보") },
                        supportingContent = { Text("버전 ${vm.currentVersion} · 만든이 · 업데이트 내역") },
                        leadingContent = { Icon(Icons.Outlined.Info, null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Outlined.ArrowForward, null) },
                        modifier = Modifier.clickable(role = Role.Button, onClick = onOpenAbout),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UpdateStatusRow(state: UpdateState, onCheck: () -> Unit) {
    val text = when (state) {
        UpdateState.Idle -> "마지막 확인 결과 없음"
        UpdateState.Checking -> "확인 중…"
        is UpdateState.UpToDate -> "최신 버전이에요" + (state.latest?.let { " (릴리스 ${it.version})" } ?: "")
        is UpdateState.Available -> "새 버전 ${state.release.version} 이 있어요"
        is UpdateState.Downloading -> "내려받는 중 ${(state.progress * 100).toInt()}%"
        is UpdateState.ReadyToInstall -> "설치 준비 완료"
        is UpdateState.Failed -> state.message
    }
    ListItem(
        headlineContent = { Text("지금 업데이트 확인") },
        supportingContent = { Text(text) },
        trailingContent = { TextButton(onClick = onCheck, enabled = state != UpdateState.Checking && state !is UpdateState.Downloading) { Text("확인") } },
    )
}

/** 앱 이름 · 버전 · 만든이 · 업데이트 내역 */
@Composable
fun AboutScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val update by vm.updateState.collectAsStateWithLifecycle()
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Scaffold(topBar = { BackTopBar("앱 정보", onBack) }) { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp).widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 적응형 아이콘(XML)은 Compose painter로 못 그려서 전경 벡터를 배경색 원 위에 얹는다
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(72.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFF0F5C4D)),
                    ) {
                        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Everylaw", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text("버전 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text("법조문 하나로 법률·시행령·시행규칙·감독규정·시행세칙의 위임 관계와 관련 판례를 찾아주는 앱", style = MaterialTheme.typography.bodyMedium)

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        ListItem(headlineContent = { Text("만든이") }, supportingContent = { Text("${BuildConfig.AUTHOR_NAME} (${BuildConfig.AUTHOR_EMAIL})") },
                            modifier = Modifier.clickable(role = Role.Button) {
                                runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${BuildConfig.AUTHOR_EMAIL}"))) }
                            })
                        ListItem(headlineContent = { Text("데이터 출처") }, supportingContent = { Text("국가법령정보센터 공동활용 Open API (law.go.kr)") })
                        ListItem(headlineContent = { Text("소스·릴리스") }, supportingContent = { Text("github.com/${BuildConfig.UPDATE_REPO}") },
                            modifier = Modifier.clickable(role = Role.Button) {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${BuildConfig.UPDATE_REPO}/releases"))) }
                            })
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("업데이트", style = MaterialTheme.typography.titleMedium)
                        UpdateBody(update, onCheck = { vm.checkUpdate() }, onDownload = { vm.download(it) }, onInstall = { vm.install(it) })
                    }
                }

                Text("업데이트 내역", style = MaterialTheme.typography.titleMedium)
                Changelog.entries.forEach { e ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${e.version} · ${e.date}", style = MaterialTheme.typography.titleSmall)
                            e.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                Text(
                    "이 앱의 정보는 참고용이며 법적 판단의 근거로는 국가법령정보센터의 원문을 확인하세요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UpdateBody(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: (com.hdlee73.everylaw.update.ReleaseInfo) -> Unit,
    onInstall: (UpdateState.ReadyToInstall) -> Unit,
) {
    when (state) {
        is UpdateState.Available -> {
            Text("새 버전 ${state.release.version} (${state.release.publishedAt})")
            if (state.release.notes.isNotBlank()) Text(state.release.notes, style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onDownload(state.release) }) { Text("내려받고 설치") }
        }
        is UpdateState.Downloading -> {
            Text("내려받는 중…")
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
        }
        is UpdateState.ReadyToInstall -> Button(onClick = { onInstall(state) }) { Text("${state.release.version} 설치") }
        is UpdateState.UpToDate -> {
            Text("현재 최신 버전이에요.")
            state.latest?.notes?.takeIf { it.isNotBlank() }?.let { Text("최근 릴리스 노트\n$it", style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = onCheck) { Text("다시 확인") }
        }
        is UpdateState.Failed -> {
            Text(state.message, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onCheck) { Text("다시 확인") }
        }
        UpdateState.Checking -> LinearProgressIndicator(Modifier.fillMaxWidth())
        UpdateState.Idle -> Button(onClick = onCheck) { Text("업데이트 확인") }
    }
}

/** 자동 확인에서 새 버전을 찾았을 때 띄우는 대화상자 */
@Composable
fun UpdateDialog(vm: SettingsViewModel) {
    val state by vm.updateState.collectAsStateWithLifecycle()
    val s = state
    if (s !is UpdateState.Available && s !is UpdateState.Downloading && s !is UpdateState.ReadyToInstall) return
    AlertDialog(
        onDismissRequest = { if (s !is UpdateState.Downloading) vm.dismissUpdate() },
        icon = { Icon(Icons.Outlined.SystemUpdate, null) },
        title = { Text("새 버전이 있어요") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UpdateBody(s, onCheck = {}, onDownload = { vm.download(it) }, onInstall = { vm.install(it) })
            }
        },
        confirmButton = {},
        dismissButton = { if (s !is UpdateState.Downloading) TextButton(onClick = vm::dismissUpdate) { Text("나중에") } },
    )
}
