package com.hdlee73.everylaw.core.domain

import com.hdlee73.everylaw.core.model.Article
import com.hdlee73.everylaw.core.model.ArticleItem
import com.hdlee73.everylaw.core.model.ArticleNo
import com.hdlee73.everylaw.core.model.ItemPath
import com.hdlee73.everylaw.core.model.LawLevel
import com.hdlee73.everylaw.core.model.LawNode

/**
 * 시행령 한 조의 여러 항이 법률 조문의 서로 다른 위치를 위임하면(예: 시행령 제87조 제1항은 법 제85조 단서, 제2항은 법 제85조제2호)
 * 그 조를 항별 노드로 쪼갠다. 그래야 각각을 법률 조문의 해당 위치 바로 아래에 둘 수 있다.
 * 쪼개도 위치가 하나뿐이거나 항이 둘 미만이면 그대로 둔다. 노드 id는 "원래id#첫항번호".
 */
fun LawNode.withParagraphPieces(): LawNode {
    val lawNo = article.number ?: return this
    return copy(children = children.flatMap { splitChild(it, lawNo) })
}

private fun isParagraph(i: ArticleItem) = i.path.paragraph != null && i.path.subparagraph == null && i.path.item == null

private fun splitChild(c: LawNode, lawNo: ArticleNo): List<LawNode> {
    val parsed = c.article.parsed
    val paras = parsed.items.filter(::isParagraph)
    if (paras.size < 2) return listOf(c)

    class Piece(val paras: MutableList<ArticleItem>, val anchor: ItemPath)

    val pieces = mutableListOf<Piece>()
    paras.forEach { p ->
        val text = p.flatten().joinToString("\n") { it.text }
        val a = DelegationTreeBuilder.anchorOf(text, DelegationTreeBuilder.LAW_PREFIX, lawNo)
        // 법을 인용하지 않은 항은 앞 항의 이어지는 내용으로 본다
        if (a == null && pieces.isNotEmpty()) pieces.last().paras += p
        else pieces += Piece(mutableListOf(p), a ?: c.anchor ?: ItemPath())
    }
    if (pieces.map { it.anchor }.distinct().size < 2) return listOf(c)

    val prefix = when (c.level) {
        LawLevel.DECREE -> DelegationTreeBuilder.DECREE_PREFIX
        LawLevel.RULE -> DelegationTreeBuilder.RULE_PREFIX
        LawLevel.SUPERVISORY_REGULATION -> DelegationTreeBuilder.SUPERVISORY_PREFIX
        else -> null
    }
    val own = c.article.number
    // 하위 노드는 "영 제87조제2항"처럼 항을 인용했으면 그 항이 든 조각 밑으로, 아니면 첫 조각 밑으로
    fun pieceOf(child: LawNode): Int {
        val para = if (prefix != null && own != null) DelegationTreeBuilder.anchorOf(child.article.content, prefix, own)?.paragraph else null
        return pieces.indexOfFirst { pc -> pc.paras.any { it.path.paragraph == para } }.takeIf { para != null && it >= 0 } ?: 0
    }

    return pieces.mapIndexed { i, pc ->
        val nums = pc.paras.mapNotNull { it.path.paragraph }
        val part = if (nums.size == 1) "제${nums.first()}항" else "제${nums.first()}항~제${nums.last()}항"
        val body = pc.paras.joinToString("\n") { p -> p.flatten().joinToString("\n") { it.text } }
        val content = if (i == 0 && parsed.lead.isNotBlank()) parsed.lead + "\n" + body else body
        c.copy(
            id = "${c.id}#${nums.first()}",
            article = c.article.copy(content = content),
            children = c.children.filter { pieceOf(it) == i },
            anchor = pc.anchor,
            part = part,
        )
    }
}

/** 비교표 한 줄: 법률 조문의 한 부분과, 그 부분을 위임 근거로 삼은 단계별 규정들. */
data class CompareRow(val lawText: String, val cells: Map<LawLevel, List<LawNode>>)

/** 3단·4단·5단 비교표. 법률 조문의 위임 위치별로 시행령·시행규칙·감독규정·시행세칙을 같은 줄에 나란히 놓는다. */
object CompareTable {
    fun levels(tiers: Int): List<LawLevel> = LawLevel.entries.take(tiers.coerceIn(3, 5))

    fun build(root: LawNode, tiers: Int): List<CompareRow> {
        val levels = levels(tiers)
        val split = root.withParagraphPieces()
        val attached = ItemAttachment.attach(split.article, split.children)
        val parsed = split.article.parsed

        fun cells(nodes: List<LawNode>): Map<LawLevel, List<LawNode>> {
            val all = nodes.flatMap { it.flatten() }
            return levels.drop(1).associateWith { lv -> all.filter { it.level == lv } }.filterValues { it.isNotEmpty() }
        }

        val rows = mutableListOf<CompareRow>()
        val pending = StringBuilder()
        fun add(text: String) {
            if (text.isBlank()) return
            if (pending.isNotEmpty()) pending.append('\n')
            pending.append(text)
        }
        fun emit(path: ItemPath, blankLabel: String? = null) {
            val nodes = attached[path] ?: return
            val text = pending.toString().ifBlank { blankLabel.orEmpty() }
            rows += CompareRow(text, cells(nodes))
            pending.clear()
        }

        if (parsed.items.isEmpty() && parsed.lead.isBlank()) add(split.article.content) else add(parsed.lead)
        emit(ItemPath.LEAD)
        fun item(i: ArticleItem, depth: Int) {
            add("  ".repeat(depth) + i.text)
            emit(i.path)
            i.children.forEach { item(it, depth + 1) }
        }
        parsed.items.forEach { item(it, 0) }
        emit(ItemPath(), blankLabel = "(조 전체)")
        if (pending.isNotBlank()) rows += CompareRow(pending.toString(), emptyMap())
        if (rows.isEmpty()) rows += CompareRow(split.article.body, emptyMap())
        return rows
    }
}
