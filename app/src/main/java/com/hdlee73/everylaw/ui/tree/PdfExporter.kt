package com.hdlee73.everylaw.ui.tree

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.hdlee73.everylaw.BuildConfig
import com.hdlee73.everylaw.core.model.DelegationTree
import com.hdlee73.everylaw.core.model.LawLevel
import com.hdlee73.everylaw.core.model.LawNode
import com.hdlee73.everylaw.core.model.formatYmd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 검색 결과(조문·하위 규정 트리·판례)를 A4 PDF로 만든다.
 * 안드로이드 기본 글꼴(Noto Sans CJK)로 그리므로 한글이 깨지지 않는다.
 */
object PdfExporter {

    private const val PAGE_W = 595 // A4, 1/72 inch
    private const val PAGE_H = 842
    private const val MARGIN = 48f

    private val levelColor = mapOf(
        LawLevel.LAW to 0xFF6750A4.toInt(),
        LawLevel.DECREE to 0xFF0B5CAD.toInt(),
        LawLevel.RULE to 0xFF0F6B50.toInt(),
        LawLevel.SUPERVISORY_REGULATION to 0xFF9A4400.toInt(),
        LawLevel.ENFORCEMENT_DETAIL to 0xFF9C2452.toInt(),
    )

    fun fileName(tree: DelegationTree): String {
        val base = "${tree.law.abbreviation ?: tree.law.name}_${tree.query.article.display}"
        val asOf = tree.asOf?.let { "_${it}기준" }.orEmpty()
        return (base + asOf).replace(Regex("""[\\/:*?"<>|\s]"""), "_") + ".pdf"
    }

    suspend fun write(tree: DelegationTree, out: OutputStream) = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val w = Writer(doc)
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date())

        w.text("Everylaw 위임 법령 조회 결과", 18f, bold = true, color = 0xFF4F378B.toInt())
        w.text("${tree.law.name} ${tree.query.article.display}", 14f, bold = true)
        w.text(
            buildString {
                append("기준: ")
                append(tree.version?.let { "${it.effectiveDateLabel} 시행 (${it.status ?: "연혁"})" } ?: "현행")
                append("   ·   출력: $now")
                if (tree.isSample) append("   ·   데모 데이터(실제 조문 아님)")
            },
            9f, color = Color.DKGRAY,
        )
        tree.warnings.forEach { w.text("※ $it", 8.5f, color = Color.GRAY) }
        w.gap(10f)

        fun node(n: LawNode, depth: Int) {
            val indent = depth * 14f
            w.text("[${n.level.label}] ${n.sourceName}", 9f, bold = true, color = levelColor.getValue(n.level), indent = indent)
            w.text(n.article.heading, 11f, bold = true, indent = indent)
            w.text(n.article.content, 9.5f, indent = indent, extraSpacing = 2f)
            w.gap(8f)
            n.children.forEach { node(it, depth + 1) }
        }
        node(tree.root, 0)

        w.gap(8f)
        w.text("관련 판례 (${tree.precedents.size}건)", 13f, bold = true, color = 0xFF4F378B.toInt())
        if (tree.precedents.isEmpty()) w.text("찾은 판례가 없습니다.", 9.5f, color = Color.GRAY)
        tree.precedents.forEach { p ->
            w.text("• ${p.caseName}", 10f, bold = true)
            w.text(listOfNotNull(p.court, p.caseNumber, p.decisionDate?.let(::formatYmd)).joinToString(" · "), 9f, color = Color.DKGRAY, indent = 10f)
            w.gap(3f)
        }
        w.gap(12f)
        w.text(
            "출처: 국가법령정보센터 공동활용 API (law.go.kr). 법적 판단의 근거로는 반드시 원문을 확인하세요.\n" +
                "Everylaw ${BuildConfig.VERSION_NAME} · 만든이 ${BuildConfig.AUTHOR_NAME} (${BuildConfig.AUTHOR_EMAIL})",
            8f, color = Color.GRAY,
        )
        w.finish()
        doc.writeTo(out)
        doc.close()
    }

    /** 공유 시트용: 캐시에 저장한 뒤 다른 앱으로 보낸다. */
    suspend fun share(context: Context, tree: DelegationTree) {
        val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val file = File(dir, fileName(tree))
        file.outputStream().use { write(tree, it) }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "PDF 공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** 문단을 줄 단위로 쪼개 페이지를 넘기며 그린다. */
    private class Writer(private val doc: PdfDocument) {
        private var pageNo = 0
        private var page: PdfDocument.Page? = null
        private var y = MARGIN

        private val canvas: Canvas get() = (page ?: newPage()).canvas

        private fun newPage(): PdfDocument.Page {
            page?.let { finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            page = p
            y = MARGIN
            return p
        }

        private fun finishPage(p: PdfDocument.Page) {
            val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.GRAY }
            p.canvas.drawText("- $pageNo -", PAGE_W / 2f - 10f, PAGE_H - 24f, paint)
            doc.finishPage(p)
        }

        fun gap(h: Float) {
            y += h
        }

        fun text(
            value: String,
            size: Float,
            bold: Boolean = false,
            color: Int = Color.BLACK,
            indent: Float = 0f,
            extraSpacing: Float = 1f,
        ) {
            if (value.isBlank()) return
            val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            }
            val width = (PAGE_W - MARGIN * 2 - indent).toInt().coerceAtLeast(80)
            val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(extraSpacing, 1f)
                .build()
            for (line in 0 until layout.lineCount) {
                val top = layout.getLineTop(line)
                val bottom = layout.getLineBottom(line)
                val h = (bottom - top).toFloat()
                if (page == null || y + h > PAGE_H - MARGIN) newPage()
                val c = canvas
                c.save()
                c.translate(MARGIN + indent, y - top)
                c.clipRect(0, top, width, bottom)
                layout.draw(c)
                c.restore()
                y += h
            }
            y += 2f
        }

        fun finish() {
            (page ?: newPage()).let { finishPage(it) }
            page = null
        }
    }
}
