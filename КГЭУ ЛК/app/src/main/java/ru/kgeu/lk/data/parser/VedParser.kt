package ru.kgeu.lk.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.kgeu.lk.data.model.GradePoint

object VedParser {
    private val ktTotalHeaders = listOf(
        "Итоговый рейтинг по КТ",
        "Итоговый рейтинг",
        "рейтинг по КТ",
    )
    private val ktPartHeaders = listOf(
        "Итоги по КТ 1",
        "Итоги по КТ 2",
        "Итоги по КТ 3",
        "Итоги по КТ 4",
    )

    fun parse(html: String): List<GradePoint> {
        val doc = Jsoup.parse(html)
        val table = doc.select("table").maxByOrNull { it.select("tr").size } ?: return emptyList()
        val rows = table.select("tr")
        if (rows.isEmpty()) return emptyList()

        val headerRows = rows.take(4)
        val dataRow = rows.drop(headerRows.size).firstOrNull { row ->
            row.select("td").size >= 3
        } ?: rows.lastOrNull()

        if (dataRow == null) return fallbackParse(doc)

        val columnTitles = buildColumnTitles(headerRows)
        val cells = dataRow.select("td, th").map { it.text().trim() }
        val points = linkedMapOf<String, String>()

        columnTitles.forEachIndexed { index, title ->
            if (index >= cells.size) return@forEachIndexed
            val value = cells[index]
            if (value.isBlank()) return@forEachIndexed
            if (isImportantHeader(title)) {
                points[normalizeTitle(title)] = value
            }
        }

        ktTotalHeaders.firstNotNullOfOrNull { header ->
            findColumnValue(columnTitles, cells, header)?.let { value ->
                points.putIfAbsent("Итоговый рейтинг по КТ", value)
            }
        }

        ktPartHeaders.forEach { header ->
            findColumnValue(columnTitles, cells, header)?.let { value ->
                points.putIfAbsent(normalizeTitle(header), value)
            }
        }

        if (points.isEmpty()) {
            return fallbackParse(doc)
        }

        return points.map { (title, value) -> GradePoint(title = title, value = value) }
    }

    private fun buildColumnTitles(headerRows: List<Element>): List<String> {
        val maxColumns = headerRows.maxOfOrNull { it.select("td, th").size } ?: 0
        val titles = MutableList(maxColumns) { "" }

        headerRows.forEach { row ->
            var column = 0
            row.select("td, th").forEach { cell ->
                while (column < titles.size && titles[column].isNotBlank()) {
                    column++
                }
                if (column >= titles.size) return@forEach
                val text = cell.text().trim()
                val colspan = cell.attr("colspan").toIntOrNull() ?: 1
                repeat(colspan) { offset ->
                    val target = column + offset
                    if (target < titles.size && text.isNotBlank()) {
                        titles[target] = if (titles[target].isBlank()) {
                            text
                        } else {
                            "${titles[target]} / $text"
                        }
                    }
                }
                column += colspan
            }
        }

        return titles
    }

    private fun findColumnValue(
        titles: List<String>,
        cells: List<String>,
        needle: String,
    ): String? {
        val index = titles.indexOfFirst { title ->
            title.contains(needle, ignoreCase = true) ||
                needle.contains(title, ignoreCase = true)
        }
        if (index < 0 || index >= cells.size) return null
        return cells[index].takeIf { it.isNotBlank() }
    }

    private fun isImportantHeader(title: String): Boolean {
        val normalized = title.lowercase()
        return ktTotalHeaders.any { normalized.contains(it.lowercase()) } ||
            ktPartHeaders.any { normalized.contains(it.lowercase()) } ||
            normalized.contains("итог")
    }

    private fun normalizeTitle(title: String): String =
        title.replace(Regex("\\s+"), " ").trim()

    private fun fallbackParse(doc: org.jsoup.nodes.Document): List<GradePoint> {
        val points = mutableListOf<GradePoint>()
        doc.select("table tr").forEach { row ->
            val cells = row.select("td, th").map { it.text().trim() }.filter { it.isNotBlank() }
            if (cells.size >= 2) {
                points += GradePoint(title = cells[0], value = cells.drop(1).joinToString(" "))
            }
        }
        return points.distinctBy { it.title + it.value }
    }
}
