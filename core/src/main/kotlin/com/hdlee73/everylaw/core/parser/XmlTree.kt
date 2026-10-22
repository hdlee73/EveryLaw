package com.hdlee73.everylaw.core.parser

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/**
 * 국가법령정보 DRF 응답 XML을 다루기 위한 가벼운 트리.
 *
 * DRF 응답은 한글 태그명(<조문내용>, <행정규칙명> 등)을 쓰고, 대상(target)마다 래퍼 구조가 조금씩 다르다.
 * 고정 스키마 대신 "이름이 X인/X를 포함하는 하위 요소"로 찾도록 해서 구조 변화에 덜 민감하게 만든다.
 * javax DOM 파서는 JVM과 안드로이드 모두에 있어 :core 모듈을 순수 Kotlin으로 유지할 수 있다.
 */
class XmlNode(
    val name: String,
    val text: String,
    val attributes: Map<String, String>,
    val children: List<XmlNode>,
) {
    /** 이름이 정확히 [tag]인 직계 자식 */
    fun child(tag: String): XmlNode? = children.firstOrNull { it.name == tag }

    fun childText(vararg tags: String): String? =
        tags.firstNotNullOfOrNull { tag -> child(tag)?.text?.takeIf { it.isNotBlank() } }

    /** 이름이 [tag]인 모든 하위 요소(깊이 우선, 자신 포함) */
    fun findAll(tag: String): List<XmlNode> = findAll { it.name == tag }

    fun findAll(predicate: (XmlNode) -> Boolean): List<XmlNode> {
        val out = mutableListOf<XmlNode>()
        fun walk(n: XmlNode) {
            if (predicate(n)) out += n
            n.children.forEach(::walk)
        }
        walk(this)
        return out
    }

    fun first(tag: String): XmlNode? = findAll(tag).firstOrNull()

    fun firstText(vararg tags: String): String? =
        tags.firstNotNullOfOrNull { tag -> first(tag)?.text?.takeIf { it.isNotBlank() } }

    /** 자신과 모든 하위 요소의 텍스트를 줄바꿈으로 이어 붙인다. (항·호·목이 따로 들어오는 조문용) */
    fun deepText(): String {
        val parts = mutableListOf<String>()
        fun walk(n: XmlNode) {
            if (n.text.isNotBlank()) parts += n.text
            n.children.forEach(::walk)
        }
        walk(this)
        return parts.joinToString("\n")
    }

    companion object {
        fun parse(xml: String): XmlNode {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isExpandEntityReferences = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }
            val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml.trimStart('﻿'))))
            return convert(doc.documentElement)
        }

        private fun convert(el: Element): XmlNode {
            val kids = mutableListOf<XmlNode>()
            val text = StringBuilder()
            val nodes = el.childNodes
            for (i in 0 until nodes.length) {
                val n = nodes.item(i)
                when (n.nodeType) {
                    Node.ELEMENT_NODE -> kids += convert(n as Element)
                    Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> text.append(n.nodeValue)
                }
            }
            val attrs = buildMap {
                val a = el.attributes
                for (i in 0 until a.length) put(a.item(i).nodeName, a.item(i).nodeValue)
            }
            return XmlNode(el.tagName, text.toString().trim(), attrs, kids)
        }
    }
}
