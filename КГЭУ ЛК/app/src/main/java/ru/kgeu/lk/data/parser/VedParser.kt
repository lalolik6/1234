package ru.kgeu.lk.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.kgeu.lk.data.model.GradePoint

object VedParser {
    /**
     * Матчит заголовки столбцов вида «Итоги по КТ 1», «Итоги по КТ 2», «Итоги по КТ 3».
     * На экране предмета должны остаться только эти строки и баллы за них.
     */
    private val ktPartRegex = Regex("итог[а-я]*\\s*(?:по\\s*)?кт\\s*(\\d+)", RegexOption.IGNORE_CASE)

    fun parse(html: String): List<GradePoint> {
        val doc = Jsoup.parse(html)
        val table = doc.select("table").maxByOrNull { it.select("tr").size } ?: return ktFallback(doc)
        val rows = table.select("tr")
        if (rows.isEmpty()) return ktFallback(doc)

        val headerRows = rows.take(4)
        val dataRow = rows.drop(headerRows.size).firstOrNull { row ->
            row.select("td").size >= 3
        } ?: rows.lastOrNull()

        if (dataRow == null) return ktFallback(doc)

        val columnTitles = buildColumnTitles(headerRows)
        val cells = dataRow.select("td, th").map { it.text().trim() }
        val points = sortedMapOf<Int, GradePoint>()

        columnTitles.forEachIndexed { index, title ->
            if (index >= cells.size) return@forEachIndexed
            val value = cells[index]
            if (value.isBlank()) return@forEachIndexed
            val ktNumber = ktNumberOf(title) ?: return@forEachIndexed
            points.putIfAbsent(ktNumber, GradePoint(title = "Итоги по КТ $ktNumber", value = value))
        }

        if (points.isEmpty()) return ktFallback(doc)

        return points.values.toList()
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

    /** Возвращает номер КТ (1, 2, 3…), если заголовок относится к итогам по КТ. */
    private fun ktNumberOf(title: String): Int? =
        ktPartRegex.find(title)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * Запасной разбор: ищем по всей таблице ячейки с заголовком «Итоги по КТ N»
     * и берём соседнее значение.
     */
    private fun ktFallback(doc: org.jsoup.nodes.Document): List<GradePoint> {
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
