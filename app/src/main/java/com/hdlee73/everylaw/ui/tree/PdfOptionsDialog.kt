package com.hdlee73.everylaw.ui.tree

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hdlee73.everylaw.core.model.LawLevel

/** PDF에 담을 범위(단계별 규정, 판례 목록·본문, 인용 조문)를 고르는 창. */
@Composable
fun PdfOptionsDialog(
    initial: PdfOptions,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (PdfOptions) -> Unit,
) {
    var levels by remember { mutableStateOf(initial.levels) }
    var precList by remember { mutableStateOf(initial.precedentList) }
    var precBody by remember { mutableStateOf(initial.precedentBodies) }
    var related by remember { mutableStateOf(initial.relatedArticles) }
    var tiers by remember { mutableStateOf(initial.compareTiers) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PDF에 담을 범위") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("보기 방식", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                listOf(
                    0 to "현행 (조문 아래에 하위 규정을 이어서)",
                    3 to "3단 비교 (법률·시행령·시행규칙, 가로 A4)",
                    4 to "4단 비교 (+감독규정, 가로 A4)",
                    5 to "5단 비교 (+시행세칙, 가로 A3)",
                ).forEach { (n, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { tiers = n },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tiers == n, onClick = null)
                        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp))
                    }
                }
                if (tiers >= 3) Text(
                    "비교 방식은 법률 조문의 위임 위치마다 시행령 등을 같은 줄에 나란히 놓아요. 아래 '법령' 단계 선택은 현행 방식에만 적용돼요.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("법령", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OptionRow("법률 조문 (항상 포함)", checked = true, enabled = false) {}
                LawLevel.entries.filter { it != LawLevel.LAW }.forEach { lv ->
                    OptionRow(
                        when (lv) {
                            LawLevel.DECREE -> "시행령"
                            LawLevel.RULE -> "시행규칙"
                            LawLevel.SUPERVISORY_REGULATION -> "감독규정"
                            else -> "감독업무 시행세칙"
                        },
                        checked = lv in levels,
                    ) { levels = if (it) levels + lv else levels - lv }
                }
                Text("판례", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
                OptionRow("판례 목록", precList) { precList = it }
                OptionRow("판례 본문 (판시사항·판결요지·참조조문, 최대 10건)", precBody) { precBody = it }
                Text("그 밖에", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
                OptionRow("본문이 인용한 같은 법의 다른 조문 (최대 8건)", related) { related = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(PdfOptions(levels + LawLevel.LAW, precList, precBody, related, tiers))
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun OptionRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Checkbox) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp))
    }
}
