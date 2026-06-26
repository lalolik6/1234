package ru.kgeu.lk.ui.util

import ru.kgeu.lk.data.model.DisciplineGrade

fun DisciplineGrade.displayName(): String =
    name?.takeIf { it.isNotBlank() } ?: "Предмет №${ratingID ?: "?"}"

fun DisciplineGrade.ktRating(): String? =
    ktRatingKt?.takeIf { it.isNotBlank() }
        ?: avg?.takeIf { it.isNotBlank() }

/** Итоговая оценка («Итоги»: Отлично/Зачтено/5…) для карточки без захода в предмет. */
fun DisciplineGrade.finalGrade(): String? =
    summary?.takeIf { it.isNotBlank() }
        ?: avg?.takeIf { it.isNotBlank() }
        ?: mark?.takeIf { it.isNotBlank() }
