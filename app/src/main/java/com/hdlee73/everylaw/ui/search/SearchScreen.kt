package com.hdlee73.everylaw.ui.search

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hdlee73.everylaw.core.model.AdminRuleSummary
import com.hdlee73.everylaw.core.model.DelegationTree
import com.hdlee73.everylaw.core.model.KeywordSearchResult
import com.hdlee73.everylaw.core.model.LawSummary
import com.hdlee73.everylaw.core.model.PrecedentSummary
import com.hdlee73.everylaw.core.model.formatYmd
import com.hdlee73.everylaw.ui.tree.LawTreeView
import com.hdlee73.everylaw.ui.tree.PdfExporter
import com.hdlee73.everylaw.ui.tree.PrecedentRow
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    vm: SearchViewModel,
    isWide: Boolean,
    onOpenPrecedent: (PrecedentSummary) -> Unit,
    onOpenLaw: (LawSummary) -> Unit,
    onOpenAdminRule: (AdminRuleSummary) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val asOf by vm.asOf.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val versions by vm.versions.collectAsStateWithLifecycle()

    val searchBar: @Composable ColumnScope.() -> Unit = {
        LawSearchBar(
            query = query,
            asOf = asOf,
            onQueryChange = vm::onQueryChange,
            onSearch = { vm.submit() },
            onAsOfChange = vm::setAsOf,
            modifier = Modifier.padding(horizontal = if (isWide) 0.dp else 14.dp, vertical = 12.dp),
        )
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is SearchUiState.TreeSuccess -> LawTreeView(
                state = s,
                isWide = isWide,
                header = {
                    searchBar()
                    TreeHeader(
                        tree = s.tree,
                        isFavorite = s.tree.query.raw in favorites,
                        isWide = isWide,
                        onToggleFavorite = { vm.toggleFavorite(s.tree.query.raw) },
                        onExpandAll = vm::expandAll,
                        onVersions = vm::openVersions,
                        onOpenSettings = onOpenSettings,
                    )
                },
                onToggle = vm::toggle,
                onSelect = vm::select,
                onOpenPrecedent = onOpenPrecedent,
            )

            else -> LazyColumn(
                Modifier.fillMaxSize().let { if (isWide) it.padding(horizontal = 24.dp) else it },
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item { Column { searchBar() } }
                when (s) {
                    SearchUiState.Idle -> item {
                        IdleContent(
                            recent = recent,
                            favorites = favorites,
                            onPick = { vm.submit(it) },
                            onClearRecent = vm::clearRecent,
                        )
                    }
                    is SearchUiState.Loading -> item {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(s.message, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    is SearchUiState.Error -> item {
                        Card(
                            Modifier.fillMaxWidth().padding(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SelectionContainer { Text(s.message, color = MaterialTheme.colorScheme.onErrorContainer) }
                                if (s.canRetry) Button(onClick = vm::retry) { Text("다시 시도") }
                            }
                        }
                    }
                    is SearchUiState.KeywordSuccess -> keywordResults(
                        result = s.result,
                        onLaw = onOpenLaw,
                        onAdminRule = onOpenAdminRule,
                        onPrecedent = onOpenPrecedent,
                    )
                    is SearchUiState.TreeSuccess -> Unit
                }
            }
        }
    }

    if (versions != VersionSheetState.Hidden) {
        VersionSheet(
            state = versions,
            asOf = asOf,
            onPick = { vm.closeVersions(); vm.setAsOf(it) },
            onDismiss = vm::closeVersions,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TreeHeader(
    tree: DelegationTree,
    isFavorite: Boolean,
    isWide: Boolean,
    onToggleFavorite: () -> Unit,
    onExpandAll: (Boolean) -> Unit,
    onVersions: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { PdfExporter.write(tree, it) } ?: error("파일을 열 수 없어요")
            }.onSuccess { Toast.makeText(context, "PDF를 저장했어요", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "PDF 저장 실패: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    Column(
        Modifier.padding(horizontal = if (isWide) 0.dp else 14.dp).padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(tree.law.abbreviation ?: tree.law.name, style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${tree.query.target} 위임 구조",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SuggestionChip(onClick = {}, label = { Text("하위 규정 ${tree.root.descendantCount}개") })
            tree.version?.let { v -> SuggestionChip(onClick = onVersions, label = { Text("${v.effectiveDateLabel} 시행 기준") }) }
            if (!isWide) {
                AssistChip(onClick = { onExpandAll(true) }, label = { Text("전체 펼치기") }, leadingIcon = { Icon(Icons.Outlined.UnfoldMore, null) })
                AssistChip(onClick = { onExpandAll(false) }, label = { Text("접기") }, leadingIcon = { Icon(Icons.Outlined.UnfoldLess, null) })
            }
            AssistChip(onClick = onVersions, label = { Text("시행 연혁") }, leadingIcon = { Icon(Icons.Outlined.History, null) })
            AssistChip(
                onClick = { saveLauncher.launch(PdfExporter.fileName(tree)) },
                label = { Text("PDF 저장") },
                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) },
            )
            AssistChip(
                onClick = { scope.launch { runCatching { PdfExporter.share(context, tree) } } },
                label = { Text("공유") },
                leadingIcon = { Icon(Icons.Outlined.Share, null) },
            )
        }
        if (tree.isSample) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("데모 데이터입니다. 설정에서 국가법령정보 OC를 입력하면 실제 법령을 조회해요.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onOpenSettings) { Text("설정") }
                }
            }
        }
        tree.warnings.forEach {
            Text("※ $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleContent(
    recent: List<String>,
    favorites: List<String>,
    onPick: (String) -> Unit,
    onClearRecent: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(4.dp))
        Text("법조문 하나로\n위임 규정을 한눈에", style = MaterialTheme.typography.headlineMedium)
        Text(
            "법률 → 시행령 → 시행규칙 → 감독규정 → 시행세칙을 거슬러 찾아 계층으로 보여드려요. 조문 번호 없이 입력하면 법령·행정규칙·판례 키워드 검색이 돼요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("이렇게 찾아보세요", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("자본시장법 제12조", "은행법 제34조", "보험업법 제95조의2", "내부통제").forEach {
                SuggestionChip(onClick = { onPick(it) }, label = { Text(it) })
            }
        }
        if (favorites.isNotEmpty()) {
            Text("즐겨찾기", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                favorites.take(8).forEach { AssistChip(onClick = { onPick(it) }, label = { Text(it) }, leadingIcon = { Icon(Icons.Filled.Star, null, Modifier.size(18.dp)) }) }
            }
        }
        if (recent.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("최근 검색", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onClearRecent) { Text("지우기") }
            }
            recent.take(6).forEach { q ->
                ListItem(
                    headlineContent = { Text(q) },
                    leadingContent = { Icon(Icons.Outlined.History, null) },
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(q) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.keywordResults(
    result: KeywordSearchResult,
    onLaw: (LawSummary) -> Unit,
    onAdminRule: (AdminRuleSummary) -> Unit,
    onPrecedent: (PrecedentSummary) -> Unit,
) {
    if (result.warnings.isNotEmpty()) item {
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                result.warnings.forEach {
                    SelectionContainer { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer) }
                }
            }
        }
    }
    item { SectionTitle("법령", result.laws.size, Icons.AutoMirrored.Outlined.MenuBook) }
    items(result.laws, key = { "law-${it.mst}-${it.name}" }) { law ->
        ResultCard(
            title = law.name,
            subtitle = listOfNotNull(law.kind, law.abbreviation, law.effectiveDate?.let { "시행 ${formatYmd(it)}" }).joinToString(" · "),
            onClick = { onLaw(law) },
        )
    }
    item { SectionTitle("행정규칙 (감독규정·세칙 등)", result.adminRules.size, Icons.Outlined.Description) }
    items(result.adminRules, key = { "adm-${it.serialNo}-${it.name}" }) { r ->
        ResultCard(
            title = r.name,
            subtitle = listOfNotNull(r.kind, r.agency).joinToString(" · "),
            onClick = { onAdminRule(r) },
        )
    }
    item { SectionTitle("판례", result.precedents.size, Icons.Outlined.History) }
    items(result.precedents, key = { "kp-${it.serialNo}-${it.caseNumber}" }) { p ->
        Box(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) { PrecedentRow(p) { onPrecedent(p) } }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text("${count}건", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionSheet(
    state: VersionSheetState,
    asOf: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).widthIn(max = 640.dp)) {
            Text("시행 연혁", style = MaterialTheme.typography.titleLarge)
            Text("시점을 고르면 그날 시행 중이던 조문 기준으로 다시 조회해요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { onPick(null) }, enabled = asOf != null) { Text("현행 기준으로 보기") }
            when (state) {
                VersionSheetState.Loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is VersionSheetState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 16.dp))
                is VersionSheetState.Shown -> LazyColumn {
                    items(state.versions, key = { it.mst + it.effectiveDate }) { v ->
                        val selected = asOf == v.effectiveDate
                        ListItem(
                            headlineContent = { Text("${v.effectiveDateLabel} 시행", fontWeight = if (selected) FontWeight.Black else null) },
                            supportingContent = {
                                Text(listOfNotNull(v.revisionType, v.promulgationDate?.let { "공포 ${formatYmd(it)}" }).joinToString(" · "))
                            },
                            trailingContent = { v.status?.let { Text(it, color = MaterialTheme.colorScheme.primary) } },
                            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(v.effectiveDate) },
                        )
                    }
                }
                VersionSheetState.Hidden -> Unit
            }
        }
    }
}
