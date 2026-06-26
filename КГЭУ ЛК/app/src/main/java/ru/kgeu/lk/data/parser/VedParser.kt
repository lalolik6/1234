package ru.kgeu.lk.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.kgeu.lk.data.model.GradePoint

object VedParser {
    /**
     * Матчит заголовки столбцов вида «Итоги по КТ 1», «Итоги по КТ 2», «Итоги по КТ 3».
     * На экране предмета должны остаться только эти строки и баллы за них.
     */
    private val ktPartRegex = Regex("итог[а-я]*\\s*(?:по\\s*)?кт\\s*(\\d+)", RegexOption.IGNORE_CASE)

    data class VedResult(
        val ktPoints: List<GradePoint>,
        val ratingKt: String?,
    )

    /** Только строки «Итоги по КТ 1‑3» (экран предмета). */
    fun parse(html: String): List<GradePoint> = parseFull(html).ktPoints

    /** Балл из столбца «Итоговый рейтинг по КТ» (для карточки предмета). */
    fun parseRatingKt(html: String): String? = parseFull(html).ratingKt

    fun parseFull(html: String): VedResult {
        val doc = Jsoup.parse(html)
        val grid = selectMarksGrid(doc)
        val headerRows = grid?.select("tr[id*=DXHeadersRow]").orEmpty()
        val dataRow = grid?.select("tr[id*=DXDataRow]")?.firstOrNull()

        if (grid == null || headerRows.isEmpty() || dataRow == null) {
            return VedResult(ktFallback(doc), null)
        }

        val values = directCells(dataRow).map { it.text().trim() }
        val columnHeaders = buildColumnHeaders(headerRows)

        val ktPoints = sortedMapOf<Int, GradePoint>()
        var ratingKt: String? = null
        var ratingScore = Int.MIN_VALUE

        for ((column, headers) in columnHeaders) {
            if (column >= values.size) continue
            val value = values[column]
            val bottom = headers.lastOrNull { it.isNotBlank() }.orEmpty()
            val combined = headers.filter { it.isNotBlank() }.joinToString(" ")

            val ktNumber = ktPartRegex.find(bottom)?.groupValues?.get(1)?.toIntOrNull()
            if (ktNumber != null && ktNumber in 1..20) {
                if (value.isNotBlank()) {
                    ktPoints.putIfAbsent(ktNumber, GradePoint("Итоги по КТ $ktNumber", value))
                }
                continue
            }

            if (isRatingHeader(combined) && value.isNotBlank()) {
                val score = value.replace(',', '.').toFloatOrNull()?.toInt() ?: 0
                if (score >= ratingScore) {
                    ratingScore = score
                    ratingKt = value
                }
            }
        }

        if (ktPoints.isEmpty()) {
            return VedResult(ktFallback(doc), ratingKt)
        }
        return VedResult(ktPoints.values.toList(), ratingKt)
    }

    /** Столбец «Итоговый рейтинг по КТ» (но не сами «Итоги по КТ N»). */
    private fun isRatingHeader(header: String): Boolean =
        header.contains("рейтинг", ignoreCase = true) &&
            header.contains("кт", ignoreCase = true) &&
            ktPartRegex.find(header) == null

    /** Выбираем основную таблицу с баллами DevExpress (с наибольшим числом столбцов). */
    private fun selectMarksGrid(doc: Document): Element? {
        val grids = doc.select("table[id*=DXMainTable]")
        if (grids.isEmpty()) return null
        return grids.maxByOrNull { grid ->
            grid.select("tr[id*=DXDataRow]").firstOrNull()
                ?.let { directCells(it).size } ?: 0
        }
    }

    private fun directCells(row: Element): List<Element> =
        row.children().filter { it.tagName() == "td" || it.tagName() == "th" }

    /**
     * Разбирает заголовок таблицы DevExpress (несколько строк, colspan/rowspan)
     * в карту: индекс столбца → список текстов заголовка сверху вниз.
     */
    private fun buildColumnHeaders(headerRows: List<Element>): Map<Int, List<String>> {
        val result = sortedMapOf<Int, MutableList<String>>()
        // Активные rowspan-ы: столбец → (текст, сколько строк ещё занимает).
        val pending = HashMap<Int, Pair<String, Int>>()

        for (row in headerRows) {
            var column = 0
            for (cell in directCells(row)) {
                while ((pending[column]?.second ?: 0) > 0) {
                    val (text, remaining) = pending[column]!!
                    result.getOrPut(column) { mutableListOf() }.add(text)
                    pending[column] = text to (remaining - 1)
                    column++
                }
                val text = cell.text().trim()
                val colspan = cell.attr("colspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
                val rowspan = cell.attr("rowspan").toIntOrNull()?.coerceAtLeast(1) ?: 1
                repeat(colspan) {
                    result.getOrPut(column) { mutableListOf() }.add(text)
                    if (rowspan > 1) pending[column] = text to (rowspan - 1)
                    column++
                }
            }
        }
        return result
    }

    /** Возвращает номер КТ (1, 2, 3…), если заголовок относится к итогам по КТ. */
    private fun ktNumberOf(title: String): Int? =
        ktPartRegex.find(title)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * Запасной разбор: ищем по всей таблице ячейки с заголовком «Итоги по КТ N»
     * и берём соседнее значение.
     */
    private fun ktFallback(doc: Document): List<GradePoint> {
        val points = sortedMapOf<Int, GradePoint>()
        doc.select("table tr").forEach { row ->
            val cells = row.select("td, th").map { it.text().trim() }
            cells.forEachIndexed { index, cell ->
                val ktNumber = ktNumberOf(cell) ?: return@forEachIndexed
                val value = cells.drop(index + 1).firstOrNull { it.isNotBlank() } ?: return@forEachIndexed
                points.putIfAbsent(ktNumber, GradePoint(title = "Итоги по КТ $ktNumber", value = value))
            }
        }
        return points.values.toList()
    }
}
