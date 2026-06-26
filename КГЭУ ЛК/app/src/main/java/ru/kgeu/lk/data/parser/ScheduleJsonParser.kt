package ru.kgeu.lk.data.parser

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import ru.kgeu.lk.data.model.ScheduleGroup
import ru.kgeu.lk.data.model.ScheduleInfo
import ru.kgeu.lk.data.model.ScheduleLesson
import ru.kgeu.lk.data.model.WeekType

object ScheduleJsonParser {
    fun parseInfo(data: JsonObject): ScheduleInfo? {
        val info = data.obj("info") ?: return null
        val groupObj = info.obj("group")
        return ScheduleInfo(
            curNumNed = info.int("curNumNed", "CurNumNed"),
            curSem = info.int("curSem", "CurSem"),
            group = groupObj?.let {
                ScheduleGroup(
                    groupID = it.int("groupID", "GroupID", "id"),
                    name = it.text("name", "Name"),
                )
            },
            typesWeek = info.array("typesWeek", "TypesWeek")?.mapNotNull { parseWeekType(it) },
        )
    }

    fun parseLessons(data: JsonObject): List<ScheduleLesson> =
        data.array("rasp", "Rasp")
            ?.mapNotNull { element -> runCatching { parseLesson(element) }.getOrNull() }
            .orEmpty()

    fun parseLesson(element: JsonElement): ScheduleLesson {
        val obj = element.jsonObject
        return ScheduleLesson(
            date = obj.text("Дата", "date", "Date"),
            dayOfWeek = obj.text("День_недели", "dayOfWeek", "DayOfWeek"),
            discipline = obj.text("Дисциплина", "discipline", "Discipline", "name", "Name"),
            teacher = obj.text("Преподаватель", "teacher", "Teacher", "prepod", "Prepod"),
            room = obj.text("Аудитория", "room", "Room", "aud", "Aud"),
            start = obj.text("Начало", "start", "Start", "timeStart"),
            end = obj.text("Конец", "end", "End", "timeEnd"),
            startDateTime = obj.text("ДатаНачала", "НачалоЗанятия", "startDateTime", "StartDateTime"),
            endDateTime = obj.text("ДатаОкончания", "ОкончаниеЗанятия", "endDateTime", "EndDateTime"),
            lessonNumber = obj.int("НомерЗанятия", "lessonNumber", "LessonNumber", "number"),
            cancelled = obj.bool("ОтмененоЗанятие", "cancelled", "Cancelled", "isCancelled"),
            color = obj.text("Цвет", "color", "Color"),
        )
    }

    private fun parseWeekType(element: JsonElement): WeekType? {
        val obj = element.jsonObject
        return WeekType(
            typeWeekID = obj.int("typeWeekID", "TypeWeekID"),
            shortName = obj.text("shortName", "ShortName"),
            name = obj.text("name", "Name"),
        )
    }
}
