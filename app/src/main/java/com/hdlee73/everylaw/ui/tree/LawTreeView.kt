package com.hdlee73.everylaw.ui.tree

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hdlee73.everylaw.core.model.DelegationTree
import com.hdlee73.everylaw.core.model.LawLevel
import com.hdlee73.everylaw.core.model.LawNode
import com.hdlee73.everylaw.core.model.PrecedentSummary
import com.hdlee73.everylaw.core.model.formatYmd
import com.hdlee73.everylaw.ui.search.SearchUiState
import com.hdlee73.everylaw.ui.theme.LawBodyStyle
import com.hdlee73.everylaw.ui.theme.LevelColors

/**
 * 법조문과 하위 규정을 계층으로 보여주는 뷰.
 * 화면 너비가 넓으면(폴더블 펼침·태블릿, 600dp 이상) 좌우 분할, 아니면 아코디언.
 */
@Composable
fun LawTreeView(
    state: SearchUiState.TreeSuccess,
    isWide: Boolean,
    header: @Composable ColumnScope.() -> Unit,
    onToggle: (String) -> Unit,
    onSelect: (String) -> Unit,
    onOpenPrecedent: (PrecedentSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isWide) SplitTreeView(state, header, onSelect, onOpenPrecedent, modifier)
    else AccordionTreeView(state, header, onToggle, onOpenPrecedent, modifier)
}

// ------------------------------------------------------------------ 아코디언 (폰)

private data class VisibleNode(val node: LawNode, val depth: Int)

private fun visibleNodes(root: LawNode, expanded: Set<String>): List<VisibleNode> {
    val out = mutableListOf<VisibleNode>()
    fun walk(n: LawNode, d: Int) {
        out += VisibleNode(n, d)
        if (n.id in expanded) n.children.forEach { walk(it, d + 1) }
    }
    walk(root, 0)
    return out
}

@Composable
private fun AccordionTreeView(
    state: SearchUiState.TreeSuccess,
    header: @Composable ColumnScope.() -> Unit,
    onToggle: (String) -> Unit,
    onOpenPrecedent: (PrecedentSummary) -> Unit,
    modifier: Modifier,
) {
    val nodes = visibleNodes(state.tree.root, state.expanded)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") { Column { header() } }
        items(nodes, key = { it.node.id }) { v ->
            AccordionNodeCard(
                node = v.node,
                depth = v.depth,
                expanded = v.node.id in state.expanded,
                onToggle = { onToggle(v.node.id) },
                modifier = Modifier.animateItem(),
            )
        }
        precedentItems(state.tree, onOpenPrecedent)
    }
}

@Composable
private fun AccordionNodeCard(
    node: LawNode,
    depth: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = LevelColors.current[node.level]
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .animateContentSize(),
        shape = if (depth == 0) MaterialTheme.shapes.large else MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = tone.container, contentColor = tone.onContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = if (expanded) "펼침" else "접힘" }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LevelBadge(node.level)
            Column(Modifier.weight(1f)) {
                Text(
                    node.article.heading.ifBlank { node.sourceName },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    node.sourceName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (node.children.isNotEmpty()) {
                Text("${node.descendantCount}", style = MaterialTheme.typography.labelMedium)
            }
            Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotation))
        }
        AnimatedVisibility(expanded) {
            SelectionContainer {
                Text(
                    node.article.content,
                    style = LawBodyStyle,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------ 분할 (폴더블)

@Composable
private fun SplitTreeView(
    state: SearchUiState.TreeSuccess,
    header: @Composable ColumnScope.() -> Unit,
    onSelect: (String) -> Unit,
    onOpenPrecedent: (PrecedentSummary) -> Unit,
    modifier: Modifier,
) {
    val all = state.tree.root.flatten()
    val depthOf = remember(state.tree) { depthMap(state.tree.root) }
    Row(modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // 왼쪽: 상위 조문 + 위임 트리
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") { Column { header() } }
            items(all, key = { it.id }) { node ->
                SplitTreeRow(
                    node = node,
                    depth = depthOf[node.id] ?: 0,
                    selected = node.id == state.selectedId,
                    onClick = { onSelect(node.id) },
                )
            }
            item(key = "prec-entry") {
                val selected = state.selectedId == SearchUiState.PRECEDENTS_PANE
                Surface(
                    onClick = { onSelect(SearchUiState.PRECEDENTS_PANE) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Gavel, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("관련 판례 ${state.tree.precedents.size}건", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
        // 오른쪽: 선택한 하위 규정 본문 (또는 판례 목록)
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(bottom = 12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            if (state.selectedId == SearchUiState.PRECEDENTS_PANE) {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    precedentItems(state.tree, onOpenPrecedent)
                }
            } else {
                val node = state.tree.root.find(state.selectedId) ?: state.tree.root
                NodeDetail(state.tree, node, onSelect)
            }
        }
    }
}

private fun depthMap(root: LawNode): Map<String, Int> {
    val m = mutableMapOf<String, Int>()
    fun walk(n: LawNode, d: Int) {
        m[n.id] = d
        n.children.forEach { walk(it, d + 1) }
    }
    walk(root, 0)
    return m
}

@Composable
private fun SplitTreeRow(node: LawNode, depth: Int, selected: Boolean, onClick: () -> Unit) {
    val tone = LevelColors.current[node.level]
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = tone.container,
        contentColor = tone.onContainer,
        border = if (selected) BorderStroke(3.dp, tone.accent) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp)
            .semantics { if (selected) stateDescription = "선택됨" },
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LevelBadge(node.level)
                Text(
                    node.article.heading.ifBlank { node.sourceName },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (depth == 0) {
                Text(
                    node.article.content,
                    style = LawBodyStyle,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeDetail(tree: DelegationTree, node: LawNode, onSelect: (String) -> Unit) {
    val path = tree.root.pathTo(node.id)
    val parent = path.dropLast(1).lastOrNull()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            path.joinToString(" › ") { it.level.shortLabel + " " + (it.article.number?.display ?: "") },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LevelBadge(node.level, large = true)
            node.issuer?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
        }
        Text(node.sourceName, style = MaterialTheme.typography.titleMedium)
        Text(node.article.heading, style = MaterialTheme.typography.headlineSmall)
        SelectionContainer { Text(node.article.content, style = LawBodyStyle) }

        if (node.children.isNotEmpty()) {
            HorizontalDivider()
            Text("이 조문이 위임한 하위 규정", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                node.children.forEach { c ->
                    AssistChip(
                        onClick = { onSelect(c.id) },
                        label = { Text("${c.level.shortLabel} ${c.article.number?.display ?: c.article.title}") },
                    )
                }
            }
        }
        if (parent != null) {
            TextButton(onClick = { onSelect(parent.id) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("상위 ${parent.level.label} ${parent.article.number?.display ?: ""} 보기")
            }
        }
    }
}

// ------------------------------------------------------------------ 공통

@Composable
fun LevelBadge(level: LawLevel, large: Boolean = false) {
    val tone = LevelColors.current[level]
    Box(
        Modifier
            .clip(CircleShape)
            .background(tone.accent)
            .padding(horizontal = if (large) 12.dp else 10.dp, vertical = if (large) 4.dp else 3.dp)
            .semantics { contentDescription = level.label },
    ) {
        Text(
            if (large) level.label else level.shortLabel,
            color = tone.onAccent,
            style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun LazyListScope.precedentItems(tree: DelegationTree, onOpen: (PrecedentSummary) -> Unit) {
    item(key = "prec-title") {
        Row(Modifier.padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("관련 판례", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Text("${tree.precedents.size}건", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (tree.precedents.isEmpty()) {
        item(key = "prec-empty") {
            Text("이 조문을 다룬 판례를 찾지 못했어요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    items(tree.precedents, key = { "prec-" + it.serialNo + it.caseNumber }) { p -> PrecedentRow(p, onClick = { onOpen(p) }) }
}

@Composable
fun PrecedentRow(p: PrecedentSummary, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(p.caseName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(p.court, p.caseNumber, p.decisionDate?.let(::formatYmd), p.caseType).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
