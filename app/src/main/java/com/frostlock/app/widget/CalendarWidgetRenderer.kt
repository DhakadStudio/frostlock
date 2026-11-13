package com.frostlock.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.frostlock.app.ui.util.focusIntensity
import java.time.LocalDate
import java.time.YearMonth

/**
 * Renders the month heat-map into a bitmap because RemoteViews cannot host a
 * dynamic 7×6 grid. The image is drawn locally at 720×520 and scaled by the
 * widget's ImageView, so it stays crisp without any extra permission or data.
 */
internal fun buildCalendarBitmap(
    data: WidgetData,
    today: LocalDate = LocalDate.now()
): Bitmap {
    val width = 720
    val height = 520
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val month = YearMonth.from(today)
    val first = month.atDay(1)
    val gridStart = first.minusDays((first.dayOfWeek.value - 1).toLong())

    val margin = 6f
    val headerHeight = 44f
    val cellWidth = (width - margin * 2) / 7f
    val cellHeight = (height - margin * 2 - headerHeight) / 6f
    val gap = 6f

    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }

    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    text.textSize = 22f
    text.color = WIDGET_TEXT_SECONDARY
    labels.forEachIndexed { index, label ->
        val centerX = margin + cellWidth * index + cellWidth / 2f
        canvas.drawText(label, centerX, margin + 30f, text)
    }

    val cell = RectF()
    for (index in 0 until WidgetData.DAYS_IN_MONTH_GRID) {
        val row = index / 7
        val column = index % 7
        val date = gridStart.plusDays(index.toLong())
        val inMonth = YearMonth.from(date) == month
        val seconds = data.monthSeconds.getOrElse(index) { 0L }
        val level = focusIntensity(seconds)

        val left = margin + column * cellWidth + gap / 2f
        val top = margin + headerHeight + row * cellHeight + gap / 2f
        val right = left + cellWidth - gap
        val bottom = top + cellHeight - gap
        cell.set(left, top, right, bottom)

        fill.color = cellColor(level, inMonth)
        val radius = 14f
        canvas.drawRoundRect(cell, radius, radius, fill)

        if (date == today) {
            stroke.color = WIDGET_ACCENT
            canvas.drawRoundRect(cell, radius, radius, stroke)
        }

        text.textSize = 23f
        text.typeface = if (date == today) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        text.color = when {
            !inMonth -> WIDGET_TEXT_MUTED
            level >= 3 -> WIDGET_INK
            else -> WIDGET_TEXT_PRIMARY
        }
        val baseline = (top + bottom) / 2f + 8f
        canvas.drawText(date.dayOfMonth.toString(), (left + right) / 2f, baseline, text)
    }

    return bitmap
}

private fun cellColor(level: Int, inMonth: Boolean): Int {
    if (!inMonth) return 0x2E152838
    return when (level) {
        0 -> 0x59152838
        1 -> 0x2938BDF8
        2 -> 0x4D38BDF8
        3 -> 0x8538BDF8
        else -> 0xCC38BDF8
    }.toInt()
}

private const val WIDGET_ACCENT = 0xFF7DD3FC.toInt()
private const val WIDGET_TEXT_PRIMARY = 0xFFEAF7FF.toInt()
private const val WIDGET_TEXT_SECONDARY = 0xFF93AEC4.toInt()
private const val WIDGET_TEXT_MUTED = 0x6693AEC4
private const val WIDGET_INK = 0xFF04202E.toInt()
