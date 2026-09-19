package com.ironlog.app.data.db

import androidx.room.TypeConverter
import com.ironlog.app.data.model.DayType
import com.ironlog.app.data.model.EquipmentType
import com.ironlog.app.data.model.RepUnit
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * All string→typed converters return null on parse failure rather than throwing.
 * A single corrupted row will no longer crash every query that touches it; callers
 * already handle null entity fields via Kotlin nullability.
 */
class Converters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? =
        value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    @TypeConverter
    fun localDateTimeToString(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDateTime(value: String?): LocalDateTime? =
        value?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    @TypeConverter
    fun dayTypeToString(value: DayType?): String? = value?.name

    @TypeConverter
    fun stringToDayType(value: String?): DayType? =
        value?.let { runCatching { DayType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun equipmentTypeToString(value: EquipmentType?): String? = value?.name

    @TypeConverter
    fun stringToEquipmentType(value: String?): EquipmentType? =
        value?.let { runCatching { EquipmentType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun repUnitToString(value: RepUnit?): String? = value?.name

    @TypeConverter
    fun stringToRepUnit(value: String?): RepUnit? =
        value?.let { runCatching { RepUnit.valueOf(it) }.getOrNull() }
}
