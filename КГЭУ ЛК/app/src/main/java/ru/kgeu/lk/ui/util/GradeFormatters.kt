package ru.kgeu.lk.ui.util

import ru.kgeu.lk.data.model.DisciplineGrade

fun DisciplineGrade.displayName(): String =
    name?.takeIf { it.isNotBlank() } ?: "Предмет №${ratingID ?: "?"}"

fun DisciplineGrade.ktRating(): String? =
    avg?.takeIf { it.isNotBlank() }
