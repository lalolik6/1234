package ru.kgeu.lk.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Разбирает страницу со списком ведомостей (https://kabinet.kgeu.ru/Ved/).
 * В отличие от ответа API `IntermediatePerfomance`, в этом списке присутствуют
 * ВСЕ предметы семестра, в том числе незакрытые (колонка «Закрыто» = «нет»).
 */
object VedListParser {
    data class VedListItem(
        val ratingID: Int,
        val name: String?,
        val controlForm: String?,
        val closed: Boolean,
    )

    private val idRegex = Regex("Ved\\.aspx\\?id=(\\d+)", RegexOption.IGNORE_CASE)

    fun parse(html: String): List<VedListItem> {
        val doc = Jsoup.parse(html)
        val grid = selectListGrid(doc) ?: return emptyList()
        val dataRows = grid.select("tr[id*=DXDataRow]")
        return dataRows.mapNotNull { parseRow(it) }
    }

    private fun parseRow(row: Element): VedListItem? {
        val cells = directCells(row)
        if (cells.isEmpty()) return null
        val ratingId = cells.asSequence()
            .mapNotNull { cell -> cell.select("a[href*=Ved.aspx]").firstOrNull()?.attr("href") }
            .mapNotNull { href -> idRegex.find(href)?.groupValues?.get(1)?.toIntOrNull() }
            .firstOrNull() ?: return null

        val texts = cells.map { it.text().trim() }
        val name = texts.firstOrNull { it.isNotBlank() }
        // Колонка «Закрыто» — последняя; значение «да»/«нет».
        val closedText = texts.lastOrNull().orEmpty()
        val closed = closedText.equals("да", ignoreCase = true)
        // Форма контроля — обычно средний столбец (между названием и «Закрыто»).
        val controlForm = if (texts.size >= 3) texts[texts.size - 2].takeIf { it.isNotBlank() } else null

        return VedListItem(
            ratingID = ratingId,
            name = name,
            controlForm = controlForm,
            closed = closed,
        )
    }

    private fun selectListGrid(doc: Document): Element? {
        val grids = doc.select("table[id*=DXMainTable]")
        if (grids.isEmpty()) return null
        // Берём таблицу, строки которой содержат ссылки на Ved.aspx?id=.
        return grids.firstOrNull { grid ->
            grid.select("tr[id*=DXDataRow] a[href*=Ved.aspx]").isNotEmpty()
        } ?: grids.first()
    }

    private fun directCells(row: Element): List<Element> =
        row.children().filter { it.tagName() == "td" || it.tagName() == "th" }
}
