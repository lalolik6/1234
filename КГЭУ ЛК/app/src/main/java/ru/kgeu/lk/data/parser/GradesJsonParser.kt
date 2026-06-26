package ru.kgeu.lk.data.parser

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import ru.kgeu.lk.data.model.DisciplineGrade
import ru.kgeu.lk.data.model.SemesterRating

object GradesJsonParser {
    fun parseRating(data: JsonObject): List<SemesterRating> =
        data.array("rating", "Rating")
            ?.mapNotNull { element -> runCatching { parseSemester(element) }.getOrNull() }
            .orEmpty()

    private fun parseSemester(element: JsonElement): SemesterRating {
        val obj = element.jsonObject
        return SemesterRating(
            sem = obj.int("sem", "Sem", "semester", "Semester"),
            year = obj.text("year", "Year"),
            disciplines = obj.array("disciplines", "Disciplines")
                ?.mapNotNull { item -> runCatching { parseDiscipline(item) }.getOrNull() }
                .orEmpty(),
        )
    }

    fun parseDiscipline(element: JsonElement): DisciplineGrade {
        val obj = element.jsonObject
        return DisciplineGrade(
            name = obj.text(
                "name",
                "Name",
                "discipline",
                "Discipline",
                "disciplineName",
                "DisciplineName",
            ),
            teacher = obj.text("teacher", "Teacher", "prepod", "Prepod"),
            controlForm = obj.text("controlForm", "ControlForm", "typeControl", "TypeControl"),
            mark = obj.text("mark", "Mark", "grade", "Grade"),
            avg = obj.text("avg", "Avg", "average", "Average", "rating", "Rating"),
            ratingID = obj.int("ratingID", "RatingID", "ratingId", "id", "ID", "vedID", "VedID"),
            closed = obj.bool("closed", "Closed", "isClosed", "IsClosed"),
            type = obj.text("type", "Type"),
        )
    }
}
