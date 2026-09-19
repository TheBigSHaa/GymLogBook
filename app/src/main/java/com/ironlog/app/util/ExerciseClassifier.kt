package com.ironlog.app.util

/**
 * Heuristics for classifying exercises.
 *
 * Abs/core in the seeded plan:
 *   Day 1 Dead Bugs, Day 2 Barbell Rollouts, Day 3 Hanging Leg Raises,
 *   Day 4 Dead Bugs. Day 5 hanging leg raises sit in the timed circuit.
 *
 * Everything else (compounds, accessories, circuit, warmup/cooldown) is
 * treated as non-abs for progression. Abs/core also always rest 60 seconds.
 */
object ExerciseClassifier {
    const val ABS_REST_SECONDS = 60

    private val ABS_KEYWORDS = listOf(
        "dead bug",
        "plank",
        "hollow",
        "russian twist",
        "side bend",
        "rollout",
        "leg raise",
        "crunch",
        "sit-up",
        "situp",
        "ab wheel",
        "l-sit",
        "mountain climber",
    )

    fun isAbsExercise(name: String, section: String? = null): Boolean {
        if (section != null && section.equals("CORE", ignoreCase = true)) return true
        val normalized = name.lowercase()
        return ABS_KEYWORDS.any(normalized::contains)
    }

    /**
     * Weighted pull-ups / chin-ups use smaller plates (1.25 kg) than barbell compounds.
     */
    fun isPullUpExercise(name: String): Boolean {
        val normalized = name.lowercase().replace('-', ' ').replace('_', ' ')
        return normalized.contains("pull up") ||
            normalized.contains("pullup") ||
            normalized.contains("chin up") ||
            normalized.contains("chinup")
    }
}
