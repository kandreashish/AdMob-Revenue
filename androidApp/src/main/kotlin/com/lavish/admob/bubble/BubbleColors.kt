package com.lavish.admob.bubble

/**
 * Bubble gradient based on progress toward the daily goal (today / goal).
 * Shared by the overlay bubble and the in-app size preview.
 */
object BubbleColors {
    /** Locked = call to action, so brand red rather than "disabled" grey. */
    val LOCKED = intArrayOf(0xFFD93025.toInt(), 0xFFEA4335.toInt())
    val NO_GOAL = intArrayOf(0xFF3367D6.toInt(), 0xFF4285F4.toInt())  // blue #3367D6

    /** @param progress today / dailyGoal; null when there's no goal. */
    fun forProgress(progress: Double?): IntArray = when {
        progress == null || progress.isNaN() -> NO_GOAL
        progress >= 1.0 -> intArrayOf(0xFF1E8E3E.toInt(), 0xFF34A853.toInt())   // goal hit: green
        progress >= 0.66 -> intArrayOf(0xFF3367D6.toInt(), 0xFF4285F4.toInt())  // close: blue #3367D6
        progress >= 0.33 -> intArrayOf(0xFFE37400.toInt(), 0xFFFBBC04.toInt())  // halfway: amber
        else -> intArrayOf(0xFFD93025.toInt(), 0xFFEA4335.toInt())               // behind: red (app's CoralCrimson)
    }

    fun progress(today: Double, goal: Double): Double? = if (goal > 0.0) today / goal else null
}
