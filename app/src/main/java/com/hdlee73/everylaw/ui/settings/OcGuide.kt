package com.hdlee73.everylaw.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** 국가법령정보 공동활용 OpenAPI 안내·신청 페이지 */
const val OC_GUIDE_URL = "https://open.law.go.kr/LSO/openApi/guideList.do"

/** 인증키(OC) 사용 방식 안내 문구. 설정 화면과 첫 실행 안내에서 함께 쓴다. */
@Composable
fun OcGuideText(hasBuiltInKey: Boolean, usingOwnKey: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            when {
                usingOwnKey -> "지금은 직접 입력한 OC로 조회하고 있어요. 입력을 지우면 " +
                    (if (hasBuiltInKey) "앱의 기본 인증키로 돌아가요." else "데모 데이터로 돌아가요.")
                hasBuiltInKey -> "따로 입력하지 않아도 앱에 들어 있는 기본 인증키로 실제 법령을 조회해요. " +
                    "다만 기본 키는 모든 사용자가 함께 쓰는 공용 키라 하루 호출 한도를 나눠 쓰고, 사용자가 몰리면 조회가 잠시 막힐 수 있어요."
                else -> "이 빌드에는 기본 인증키가 없어 데모 데이터로 동작해요. 본인 OC를 입력하면 실제 법령을 조회해요."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!usingOwnKey) {
            Text(
                "본인 OC를 무료로 발급받아 입력하면 더 안정적으로 쓸 수 있어요.\n" +
                    "① open.law.go.kr 회원가입·로그인 → ② OPEN API › 활용 신청 → ③ 승인되면 가입 이메일의 @ 앞부분(ID)이 OC예요 → ④ 설정 › 내 OC로 바꾸기에 입력",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun OpenOcGuideButton() {
    val context = LocalContext.current
    TextButton(onClick = {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OC_GUIDE_URL))) }
    }) { Text("OC 발급 방법 보기") }
}

/** 첫 실행 때 한 번만 띄우는 인증키 안내 */
@Composable
fun FirstRunOcGuide(vm: SettingsViewModel, onOpenSettings: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    if (!settings.loaded || settings.ocGuideSeen || settings.userOc.isNotBlank()) return
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { vm.markOcGuideSeen() },
        icon = { Icon(Icons.Outlined.Key, contentDescription = null) },
        title = { Text("인증키 안내") },
        text = { OcGuideText(hasBuiltInKey = vm.hasBuiltInKey, usingOwnKey = false) },
        confirmButton = {
            TextButton(onClick = { vm.markOcGuideSeen() }) { Text(if (vm.hasBuiltInKey) "기본 키로 시작" else "데모로 시작") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = { vm.markOcGuideSeen(); onOpenSettings() }) { Text("내 OC 입력") }
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OC_GUIDE_URL))) }
                }) { Text("발급 방법") }
            }
        },
    )
}
