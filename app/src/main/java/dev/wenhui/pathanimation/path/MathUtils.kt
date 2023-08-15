package dev.wenhui.pathanimation.path

import kotlin.math.roundToInt

/** Map a float value from a given range to another range. */
fun mapValueFromRangeToRange(
    value: Float,
    fromLow: Float,
    fromHigh: Float,
    toLow: Float,
    toHigh: Float,
    clamp: Boolean = true,
): Float {
    return mapValueFromRangeToRange(
        value.toDouble(),
        fromLow.toDouble(),
        fromHigh.toDouble(),
        toLow.toDouble(),
        toHigh.toDouble(),
        clamp,
    ).toFloat()
}

/** Map an int value from a given range to another range. */
fun mapValueFromRangeToRange(
    value: Int,
    fromLow: Int,
    fromHigh: Int,
    toLow: Int,
    toHigh: Int,
    clamp: Boolean = true,
): Int {
    return mapValueFromRangeToRange(
        value.toDouble(),
        fromLow.toDouble(),
        fromHigh.toDouble(),
        toLow.toDouble(),
        toHigh.toDouble(),
        clamp,
    ).roundToInt()
}

/** Map a double value from a given range to another range. */
fun mapValueFromRangeToRange(
    value: Double,
    fromLow: Double,
    fromHigh: Double,
    toLow: Double,
    toHigh: Double,
    clamp: Boolean = true,
): Double {
    val fromRangeSize = fromHigh - fromLow
    val toRangeSize = toHigh - toLow
    val valueScale = (value - fromLow) / fromRangeSize
    val result = toLow + valueScale * toRangeSize
    return if (clamp) result.coerceIn(minOf(toLow, toHigh), maxOf(toLow, toHigh)) else result
}
