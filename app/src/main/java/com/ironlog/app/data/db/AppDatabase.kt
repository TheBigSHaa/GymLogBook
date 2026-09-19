package com.ironlog.app.data.db

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ironlog.app.data.db.dao.DailyNutritionLogDao
import com.ironlog.app.data.db.dao.ExerciseDao
import com.ironlog.app.data.db.dao.ExerciseLogDao
import com.ironlog.app.data.db.dao.IngredientCheckDao
import com.ironlog.app.data.db.dao.MealCompletionDao
import com.ironlog.app.data.db.dao.MealIngredientDao
import com.ironlog.app.data.db.dao.MealOptionDao
import com.ironlog.app.data.db.dao.MealSlotDao
import com.ironlog.app.data.db.dao.ScheduledWorkoutDao
import com.ironlog.app.data.db.dao.SetLogDao
import com.ironlog.app.data.db.dao.WorkoutDayDao
import com.ironlog.app.data.db.dao.WorkoutSessionDao
import com.ironlog.app.data.model.DailyNutritionLog
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.data.model.ExerciseLog
import com.ironlog.app.data.model.IngredientCheck
import com.ironlog.app.data.model.MealCompletion
import com.ironlog.app.data.model.MealIngredient
import com.ironlog.app.data.model.MealOption
import com.ironlog.app.data.model.MealSlot
import com.ironlog.app.data.model.ScheduledWorkout
import com.ironlog.app.data.model.SetLog
import com.ironlog.app.data.model.WorkoutDay
import com.ironlog.app.data.model.WorkoutSession
import com.ironlog.app.ui.theme.dayColorHexForId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WorkoutDay::class,
        Exercise::class,
        WorkoutSession::class,
        ExerciseLog::class,
        SetLog::class,
        ScheduledWorkout::class,
        MealSlot::class,
        MealOption::class,
        MealIngredient::class,
        DailyNutritionLog::class,
        MealCompletion::class,
        IngredientCheck::class,
    ],
    version = 11,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDayDao(): WorkoutDayDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun setLogDao(): SetLogDao
    abstract fun scheduledWorkoutDao(): ScheduledWorkoutDao
    abstract fun mealSlotDao(): MealSlotDao
    abstract fun mealOptionDao(): MealOptionDao
    abstract fun mealIngredientDao(): MealIngredientDao
    abstract fun dailyNutritionLogDao(): DailyNutritionLogDao
    abstract fun mealCompletionDao(): MealCompletionDao
    abstract fun ingredientCheckDao(): IngredientCheckDao

    companion object {
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE Exercise ADD COLUMN section TEXT")
                    db.execSQL("ALTER TABLE Exercise ADD COLUMN circuitRounds INTEGER")
                    db.execSQL("ALTER TABLE Exercise ADD COLUMN circuitDurationSeconds INTEGER")

                    db.beginTransaction()
                    try {
                        db.execSQL(
                            "INSERT OR REPLACE INTO WorkoutDay(id, name, dayType, colorHex) " +
                                "VALUES(5, 'Bonus · Conditioning & Core', 'CONDITIONING', '${dayColorHexForId(5)}')",
                        )
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Easy Run', 1, 10, 10, 1, 'NONE', 'SECONDS', '10 min Z2 pace. Sub: jump rope or shadow boxing if running not possible.', 'WARMUP', NULL, NULL)")
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'DB Thrusters (2×10kg)', 1, 12, 12, 2, 'DUMBBELL', 'REPS', NULL, 'CIRCUIT', NULL, 900)")
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'DB Renegade Rows', 1, 8, 8, 3, 'DUMBBELL', 'PER_SIDE', NULL, 'CIRCUIT', NULL, 900)")
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Goblet Squats (1×10kg)', 1, 15, 15, 4, 'DUMBBELL', 'REPS', NULL, 'CIRCUIT', NULL, 900)")
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'DB Push Press', 1, 10, 10, 5, 'DUMBBELL', 'REPS', NULL, 'CIRCUIT', NULL, 900)")
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Burpees', 1, 8, 8, 6, 'BODYWEIGHT', 'REPS', NULL, 'CIRCUIT', NULL, 900)")
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'DB Russian Twists', 3, 20, 20, 7, 'DUMBBELL', 'REPS', NULL, 'CORE', NULL, NULL)")
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Hollow Body Hold', 3, 30, 30, 8, 'BODYWEIGHT', 'SECONDS', NULL, 'CORE', NULL, NULL)")
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'DB Side Bend', 3, 12, 12, 9, 'DUMBBELL', 'PER_SIDE', NULL, 'CORE', NULL, NULL)")
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Plank', 3, 45, 45, 10, 'BODYWEIGHT', 'SECONDS', NULL, 'CORE', NULL, NULL)")
                        db.execSQL("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Cooldown Walk/Run', 1, 5, 5, 11, 'NONE', 'SECONDS', '5 min easy + stretching', 'COOLDOWN', NULL, NULL)")
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            }

        val MIGRATION_2_3: Migration =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS meal_slot (
                            id INTEGER NOT NULL PRIMARY KEY,
                            name TEXT NOT NULL,
                            timeLabel TEXT NOT NULL,
                            orderIndex INTEGER NOT NULL,
                            mealType TEXT NOT NULL
                        )""",
                    )

                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS meal_option (
                            id INTEGER NOT NULL PRIMARY KEY,
                            mealSlotId INTEGER NOT NULL,
                            label TEXT NOT NULL,
                            shortLabel TEXT NOT NULL,
                            description TEXT NOT NULL,
                            orderIndex INTEGER NOT NULL,
                            calories INTEGER NOT NULL,
                            proteinG REAL NOT NULL,
                            carbsG REAL NOT NULL,
                            fatG REAL NOT NULL,
                            note TEXT,
                            usesWholeEggs INTEGER NOT NULL,
                            FOREIGN KEY(mealSlotId) REFERENCES meal_slot(id) ON DELETE CASCADE
                        )""",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_option_mealSlotId` ON `meal_option` (`mealSlotId`)")

                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS meal_ingredient (
                            id INTEGER NOT NULL PRIMARY KEY,
                            mealOptionId INTEGER NOT NULL,
                            name TEXT NOT NULL,
                            amount TEXT NOT NULL,
                            orderIndex INTEGER NOT NULL,
                            proteinG REAL NOT NULL,
                            carbsG REAL NOT NULL,
                            fatG REAL NOT NULL,
                            FOREIGN KEY(mealOptionId) REFERENCES meal_option(id) ON DELETE CASCADE
                        )""",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_ingredient_mealOptionId` ON `meal_ingredient` (`mealOptionId`)")

                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS daily_nutrition_log (
                            date TEXT NOT NULL PRIMARY KEY,
                            waterMl INTEGER NOT NULL,
                            waterTargetMl INTEGER NOT NULL
                        )""",
                    )

                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS meal_completion (
                            date TEXT NOT NULL,
                            mealSlotId INTEGER NOT NULL,
                            selectedOptionId INTEGER NOT NULL,
                            completed INTEGER NOT NULL,
                            PRIMARY KEY(date, mealSlotId)
                        )""",
                    )

                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS ingredient_check (
                            date TEXT NOT NULL,
                            mealSlotId INTEGER NOT NULL,
                            ingredientId INTEGER NOT NULL,
                            checked INTEGER NOT NULL,
                            PRIMARY KEY(date, mealSlotId, ingredientId)
                        )""",
                    )

                }
            }

        val MIGRATION_3_4: Migration =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE meal_ingredient ADD COLUMN wholeEggCount INTEGER NOT NULL DEFAULT 0")
                    db.beginTransaction()
                    try {
                        nutritionSeedStatements().forEach(db::execSQL)
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            }

        val MIGRATION_4_5: Migration =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.beginTransaction()
                    try {
                        // Delete old evening snack options (cascades to meal_ingredient via FK)
                        db.execSQL("DELETE FROM meal_option WHERE mealSlotId = 7")

                        // Option A: Cottage Cheese + Cinnamon
                        db.execSQL("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(701,7,'Option A: Cottage Cheese + Cinnamon','A','Cottage Cheese + Cinnamon',1,180,22.0,6.0,8.0,NULL,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7011,701,'Cottage cheese 5%','200g',1,22.0,6.0,8.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7012,701,'Cinnamon','pinch',2,0.0,0.0,0.0,0)")

                        // Option B: Tofu Stir-Fry
                        db.execSQL("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(702,7,'Option B: Tofu Stir-Fry','B','Tofu Stir-Fry',2,220,20.0,10.0,12.0,NULL,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7021,702,'Firm tofu','150g',1,18.0,3.0,9.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7022,702,'Sesame oil','1 tsp',2,0.0,0.0,3.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7023,702,'Soy sauce','1 tbsp',3,1.0,1.0,0.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7024,702,'Cherry tomatoes + cucumber','50g',4,1.0,6.0,0.0,0)")

                        // Option C: Protein Pudding
                        db.execSQL("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(703,7,'Option C: Protein Pudding','C','Protein Pudding',3,200,28.0,10.0,4.0,NULL,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7031,703,'Casein protein powder','25g',1,22.0,3.0,1.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7032,703,'Milk','150ml',2,6.0,7.0,3.0,0)")

                        // Option D: Pro Yogurt + Walnuts
                        db.execSQL("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(704,7,'Option D: Pro Yogurt + Walnuts','D','Pro Yogurt + Walnuts',4,200,24.0,8.0,9.0,NULL,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7041,704,'Pro yogurt','200g',1,22.0,7.0,0.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7042,704,'Walnuts','10g',2,2.0,1.0,9.0,0)")

                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            }

        val MIGRATION_5_6: Migration =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.beginTransaction()
                    try {
                        // Update Dinner Option C (Beef Kebab) — new macros and ingredients
                        db.execSQL("UPDATE meal_option SET calories=450, proteinG=32.0, carbsG=32.0, fatG=20.0 WHERE id=603")
                        db.execSQL("DELETE FROM meal_ingredient WHERE mealOptionId=603")
                        db.execSQL("INSERT INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6031,603,'Ground beef kebabs (~20%)','120g',1,22.0,0.0,18.0,0)")
                        db.execSQL("INSERT INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6032,603,'Chopped salad + pickles','150g',2,2.0,8.0,0.0,0)")
                        db.execSQL("INSERT INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6033,603,'Whole wheat pita (small)','1',3,4.0,22.0,2.0,0)")
                        db.execSQL("INSERT INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6034,603,'Lemon squeeze','1',4,0.0,1.0,0.0,0)")
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            }

        val MIGRATION_6_7: Migration =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.beginTransaction()
                    try {
                        // Fix 1: Remove duplicate meal_ingredient rows (keep lowest id per mealOptionId+name)
                        db.execSQL(
                            "DELETE FROM meal_ingredient WHERE id NOT IN (" +
                                "SELECT MIN(id) FROM meal_ingredient GROUP BY mealOptionId, name)",
                        )

                        // Fix 2: Add Dinner Option E — Cottage Cheese Salad Bowl (no eggs, no red meat)
                        db.execSQL("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(605,6,'Option E: Cottage Cheese Salad Bowl','E','Cottage Cheese Salad Bowl',5,420,35.0,30.0,16.0,NULL,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6051,605,'Cottage cheese 5%','250g',1,28.0,8.0,10.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6052,605,'Cherry tomatoes','100g',2,1.0,6.0,0.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6053,605,'Cucumber','100g',3,1.0,3.0,0.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6054,605,'Avocado','50g',4,1.0,4.0,6.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6055,605,'Olive oil + lemon dressing','1 tbsp',5,0.0,0.0,5.0,0)")
                        db.execSQL("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6056,605,'Whole grain bread','1 slice',6,4.0,14.0,2.0,0)")

                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            }

        val MIGRATION_7_8: Migration =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.beginTransaction()
                    try {
                        // Replace nutrition reference data with the canonical plan, then prune
                        // any user state whose FKs no longer point at a valid seeded row.
                        // This preserves meal_completion / ingredient_check rows that still
                        // reference rows present in the new seed (IDs are stable across the
                        // seed list, so most user history survives intact).
                        db.execSQL("DELETE FROM meal_ingredient")
                        db.execSQL("DELETE FROM meal_option")
                        db.execSQL("DELETE FROM meal_slot")
                        nutritionSeedStatements().forEach(db::execSQL)
                        db.execSQL(
                            "DELETE FROM ingredient_check " +
                                "WHERE ingredientId NOT IN (SELECT id FROM meal_ingredient) " +
                                "OR mealSlotId NOT IN (SELECT id FROM meal_slot)",
                        )
                        db.execSQL(
                            "DELETE FROM meal_completion " +
                                "WHERE selectedOptionId NOT IN (SELECT id FROM meal_option) " +
                                "OR mealSlotId NOT IN (SELECT id FROM meal_slot)",
                        )
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            }

        val MIGRATION_8_9: Migration =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE SetLog ADD COLUMN restSeconds INTEGER")
                }
            }

        val MIGRATION_9_10: Migration =
            object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        DELETE FROM SetLog WHERE exerciseLogId IN (
                            SELECT el.id FROM ExerciseLog el
                            INNER JOIN Exercise e ON e.id = el.exerciseId
                            WHERE e.workoutDayId = 5
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "DELETE FROM ExerciseLog WHERE exerciseId IN (SELECT id FROM Exercise WHERE workoutDayId = 5)",
                    )
                    db.execSQL("DELETE FROM Exercise WHERE workoutDayId = 5")
                    day5ExerciseInsertStatements().forEach(db::execSQL)
                }
            }

        val MIGRATION_10_11: Migration =
            object : Migration(10, 11) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("UPDATE WorkoutDay SET name = 'Calisthenics' WHERE id = 5")
                }
            }

        fun day5ExerciseInsertStatements(): List<String> = listOf(
            "INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Arm Circles & Torso Twists', 1, 60, 60, 1, 'BODYWEIGHT', 'SECONDS', '60s work, no rest.', 'WARMUP', NULL, NULL)",
            "INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Bodyweight Squats', 1, 60, 60, 2, 'BODYWEIGHT', 'SECONDS', '60s work, no rest.', 'WARMUP', NULL, NULL)",
            "INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Scapular Pull-ups', 1, 60, 60, 3, 'BODYWEIGHT', 'SECONDS', '60s work, no rest.', 'WARMUP', NULL, NULL)",
            "INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Pull-ups', 4, 40, 40, 4, 'BODYWEIGHT', 'SECONDS', '40s work / 20s rest.', 'CIRCUIT', 4, 40)",
            "INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Parallel Bar Dips', 4, 40, 40, 5, 'BODYWEIGHT', 'SECONDS', '40s work / 20s rest.', 'CIRCUIT', 4, 40)",
            "INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Bulgarian Split Squats', 4, 40, 40, 6, 'BODYWEIGHT', 'SECONDS', 'Bodyweight. 40s work / 20s rest.', 'CIRCUIT', 4, 40)",
            "INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Inverted Rows', 4, 40, 40, 7, 'BODYWEIGHT', 'SECONDS', '40s work / 20s rest.', 'CIRCUIT', 4, 40)",
            "INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Hanging Leg Raises', 4, 40, 40, 8, 'BODYWEIGHT', 'SECONDS', '40s work / 20s rest.', 'CIRCUIT', 4, 40)",
            "INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Dead Hang', 1, 60, 60, 9, 'BODYWEIGHT', 'SECONDS', '60s work, no rest.', 'COOLDOWN', NULL, NULL)",
            "INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes, section, circuitRounds, circuitDurationSeconds) VALUES(5, 'Chest Stretch', 1, 60, 60, 10, 'BODYWEIGHT', 'SECONDS', '60s work, no rest.', 'COOLDOWN', NULL, NULL)",
        )

        fun nutritionSeedStatements(): List<String> = buildList {
            // ── Meal Slots ─────────────────────────────────────────────────────────
            add("INSERT OR REPLACE INTO meal_slot(id,name,timeLabel,orderIndex,mealType) VALUES(1,'Pre-Workout','~6:00 AM',1,'PRE_WORKOUT')")
            add("INSERT OR REPLACE INTO meal_slot(id,name,timeLabel,orderIndex,mealType) VALUES(2,'Meal 1 - Breakfast','~7:30 AM',2,'MEAL')")
            add("INSERT OR REPLACE INTO meal_slot(id,name,timeLabel,orderIndex,mealType) VALUES(3,'Snack 1','~10:00 AM',3,'SNACK')")
            add("INSERT OR REPLACE INTO meal_slot(id,name,timeLabel,orderIndex,mealType) VALUES(4,'Meal 2 - Lunch','12:00 PM',4,'MEAL')")
            add("INSERT OR REPLACE INTO meal_slot(id,name,timeLabel,orderIndex,mealType) VALUES(5,'Snack 2','~3:30 PM',5,'SNACK')")
            add("INSERT OR REPLACE INTO meal_slot(id,name,timeLabel,orderIndex,mealType) VALUES(6,'Meal 3 - Dinner','~7:00 PM',6,'MEAL')")
            add("INSERT OR REPLACE INTO meal_slot(id,name,timeLabel,orderIndex,mealType) VALUES(7,'Evening Snack','~9:00 PM',7,'EVENING_SNACK')")

            // ── Slot 1: Pre-Workout ─────────────────────────────────────────────────
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(101,1,'Option A: Banana + Coffee','A','Banana + Coffee',1,150,1.0,35.0,0.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(102,1,'Option B: Rice Cakes + Almond Butter','B','Rice Cakes + Almond Butter',2,180,4.0,28.0,7.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(103,1,'Option C: Instant Oats','C','Instant Oats',3,170,5.0,30.0,3.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(1011,101,'Banana','1 medium',1,1.0,27.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(1012,101,'Black coffee','1 cup',2,0.0,0.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(1021,102,'Rice cakes','2',1,1.0,22.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(1022,102,'Almond butter','1 tbsp',2,3.0,3.0,7.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(1023,102,'Black coffee','1 cup',3,0.0,0.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(1031,103,'Instant oats','40g',1,5.0,28.0,3.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(1032,103,'Cinnamon','pinch',2,0.0,0.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(1033,103,'Black coffee','1 cup',3,0.0,0.0,0.0,0)")

            // ── Slot 2: Meal 1 - Breakfast ──────────────────────────────────────────
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(201,2,'Option A: Yogurt Bowl','A','Yogurt Bowl',1,420,35.0,30.0,18.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(202,2,'Option B: Whole Egg Omelet','B','Whole Egg Omelet',2,400,28.0,20.0,22.0,'Limit whole-egg omelets to 3x/week for LDL management.',3)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(203,2,'Option C: Egg White Omelet + Oats','C','Egg White Omelet + Oats',3,380,32.0,25.0,14.0,NULL,1)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2011,201,'Pro yogurt','200g',1,20.0,12.0,6.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2012,201,'Walnuts','15g',2,2.0,1.0,10.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2013,201,'Pumpkin seeds','10g',3,3.0,1.0,5.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2014,201,'Creatine','5g',4,0.0,0.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2015,201,'Mixed berries','80g',5,1.0,16.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2021,202,'Whole eggs','3',1,18.0,1.0,15.0,3)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2022,202,'Spinach','50g',2,1.0,2.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2023,202,'Red bell pepper','30g',3,0.0,3.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2024,202,'Whole grain bread','1 slice',4,4.0,14.0,2.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2025,202,'Creatine (in water)','5g',5,0.0,0.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2031,203,'Whole egg','1',1,6.0,0.0,5.0,1)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2032,203,'Egg whites','3',2,10.0,0.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2033,203,'Feta cheese','30g',3,5.0,1.0,7.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2034,203,'Cherry tomatoes + za''atar','50g',4,1.0,4.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(2035,203,'Instant oats','30g',5,4.0,20.0,2.0,0)")

            // ── Slot 3: Snack 1 ─────────────────────────────────────────────────────
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(301,3,'Option A: Almonds + Apple','A','Almonds + Apple',1,220,7.0,25.0,14.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(302,3,'Option B: Cottage Cheese + Seeds','B','Cottage Cheese + Seeds',2,180,22.0,5.0,8.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(303,3,'Option C: Rice Cakes + Tahini','C','Rice Cakes + Tahini',3,190,5.0,18.0,12.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(3011,301,'Almonds','30g',1,6.0,6.0,14.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(3012,301,'Apple','1 medium',2,1.0,19.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(3021,302,'Cottage cheese 5%','150g',1,20.0,4.0,6.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(3022,302,'Sunflower seeds','10g',2,2.0,1.0,5.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(3031,303,'Rice cakes','2',1,1.0,14.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(3032,303,'Tahini','20g',2,3.0,1.0,12.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(3033,303,'Cucumber slices','50g',3,1.0,3.0,0.0,0)")

            // ── Slot 4: Meal 2 - Lunch ──────────────────────────────────────────────
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(401,4,'Option A: Chicken Breast + Rice','A','Chicken Breast + Rice',1,550,50.0,45.0,14.0,'Weights are RAW. 200g raw chicken ≈ 150g cooked.',0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(402,4,'Option B: Lean Beef + Sweet Potato','B','Lean Beef + Sweet Potato',2,530,42.0,45.0,16.0,'Lean cuts only: shoulder (כתף) or sirloin (סינטה). Max 2x/week.',0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(403,4,'Option C: Baked Schnitzel','C','Baked Schnitzel',3,520,46.0,48.0,12.0,'Baked at 200°C with cooking spray — NOT deep fried.',0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4011,401,'Chicken breast (no skin, raw)','200g',1,42.0,0.0,4.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4012,401,'Brown rice or quinoa (cooked)','150g',2,4.0,33.0,1.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4013,401,'Roasted vegetables','200g',3,3.0,10.0,1.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4014,401,'Extra virgin olive oil','1 tbsp',4,0.0,0.0,14.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4015,401,'Lemon squeeze','1',5,0.0,1.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4021,402,'Lean beef shoulder/sirloin (raw)','150g',1,36.0,0.0,8.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4022,402,'Sweet potato (raw)','150g',2,2.0,30.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4023,402,'Green salad (spinach, arugula, tomato)','150g',3,3.0,6.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4024,402,'Olive oil + lemon dressing','1 tbsp',4,0.0,1.0,14.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4031,403,'Chicken breast (pounded, panko coated, raw)','200g',1,42.0,12.0,4.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4032,403,'Israeli salad','150g',2,2.0,8.0,1.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(4033,403,'Bulgur (cooked)','100g',3,3.0,26.0,0.0,0)")

            // ── Slot 5: Snack 2 ─────────────────────────────────────────────────────
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(501,5,'Option A: Protein Shake','A','Protein Shake',1,250,30.0,8.0,10.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(502,5,'Option B: Greek Yogurt + Chia + Silan','B','Greek Yogurt + Chia + Silan',2,180,22.0,14.0,4.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(503,5,'Option C: Hard-Boiled Eggs + Veggies','C','Hard-Boiled Eggs + Veggies',3,180,14.0,8.0,11.0,'Count toward weekly egg budget.',2)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(5011,501,'Whey protein','30g',1,24.0,3.0,1.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(5012,501,'Water','200ml',2,0.0,0.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(5013,501,'Peanut butter','1 tbsp',3,4.0,3.0,8.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(5021,502,'Greek yogurt 0-2%','200g',1,20.0,8.0,2.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(5022,502,'Chia seeds','5g',2,1.0,2.0,2.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(5023,502,'Silan (date syrup)','drizzle',3,0.0,5.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(5031,503,'Hard-boiled eggs','2',1,12.0,1.0,10.0,2)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(5032,503,'Raw vegetables (carrots, cucumber, pepper)','100g',2,2.0,7.0,0.0,0)")

            // ── Slot 6: Meal 3 - Dinner ─────────────────────────────────────────────
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(601,6,'Option A: Chicken + Legumes Salad','A','Chicken + Legumes Salad',1,480,42.0,25.0,20.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(602,6,'Option B: Shakshuka','B','Shakshuka',2,420,30.0,28.0,18.0,'On low-egg days: use 1 whole + 3 whites.',2)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(603,6,'Option C: Beef Kebab Plate','C','Beef Kebab Plate',3,450,32.0,32.0,20.0,'Regular ground beef (~20% fat). Max 1x/week. No tahini.',0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(604,6,'Option D: Stuffed Bell Peppers','D','Stuffed Bell Peppers',4,470,36.0,40.0,14.0,'Uses lean ground beef.',0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(605,6,'Option E: Cottage Cheese Salad Bowl','E','Cottage Cheese Salad Bowl',5,420,35.0,30.0,16.0,'Dairy option — no eggs, no red meat. Always available.',0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6011,601,'Grilled chicken breast (raw)','150g',1,32.0,0.0,3.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6012,601,'Mixed salad (romaine, tomato, cucumber, red cabbage)','200g',2,3.0,8.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6013,601,'Avocado','50g',3,1.0,4.0,8.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6014,601,'Cooked lentils or chickpeas','100g',4,8.0,15.0,1.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6015,601,'Olive oil + balsamic dressing','1 tbsp',5,0.0,1.0,14.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6021,602,'Eggs (poached in tomato sauce)','2',1,12.0,1.0,10.0,2)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6022,602,'Tomato sauce with onion, garlic, pepper, cumin, paprika','200g',2,3.0,12.0,2.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6023,602,'Feta cheese','30g',3,5.0,1.0,7.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6024,602,'Whole grain bread','1 slice',4,4.0,14.0,2.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6031,603,'Ground beef kebabs (~20% fat, raw)','120g',1,22.0,0.0,18.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6032,603,'Chopped salad + pickled vegetables','150g',2,2.0,8.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6033,603,'Whole wheat pita (small)','1',3,4.0,22.0,2.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6034,603,'Lemon squeeze','1',4,0.0,1.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6041,604,'Bell peppers (large, stuffed)','2',1,2.0,10.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6042,604,'Lean ground beef + rice + onion + spices (filling, raw)','150g',2,28.0,20.0,12.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6043,604,'Side salad with olive oil','100g',3,2.0,5.0,5.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6051,605,'Cottage cheese 5%','250g',1,28.0,8.0,10.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6052,605,'Cherry tomatoes','100g',2,1.0,6.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6053,605,'Cucumber','100g',3,1.0,3.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6054,605,'Avocado','50g',4,1.0,4.0,6.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6055,605,'Olive oil + lemon dressing','1 tbsp',5,0.0,0.0,5.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(6056,605,'Whole grain bread','1 slice',6,4.0,14.0,2.0,0)")

            // ── Slot 7: Evening Snack ───────────────────────────────────────────────
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(701,7,'Option A: Cottage Cheese + Cinnamon','A','Cottage Cheese + Cinnamon',1,180,22.0,6.0,8.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(702,7,'Option B: Tofu Stir-Fry','B','Tofu Stir-Fry',2,220,20.0,10.0,12.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(703,7,'Option C: Protein Pudding','C','Protein Pudding',3,200,28.0,10.0,4.0,'Set in fridge 30 minutes before eating.',0)")
            add("INSERT OR REPLACE INTO meal_option(id,mealSlotId,label,shortLabel,description,orderIndex,calories,proteinG,carbsG,fatG,note,usesWholeEggs) VALUES(704,7,'Option D: Pro Yogurt + Walnuts','D','Pro Yogurt + Walnuts',4,200,24.0,8.0,9.0,NULL,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7011,701,'Cottage cheese 5%','200g',1,22.0,6.0,8.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7012,701,'Cinnamon','pinch',2,0.0,0.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7021,702,'Firm tofu','150g',1,18.0,3.0,9.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7022,702,'Sesame oil','1 tsp',2,0.0,0.0,3.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7023,702,'Soy sauce','1 tbsp',3,1.0,1.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7024,702,'Cherry tomatoes + cucumber','50g',4,1.0,6.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7031,703,'Casein protein powder','25g',1,22.0,3.0,1.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7032,703,'Milk','150ml',2,6.0,7.0,3.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7041,704,'Pro yogurt','200g',1,22.0,7.0,0.0,0)")
            add("INSERT OR REPLACE INTO meal_ingredient(id,mealOptionId,name,amount,orderIndex,proteinG,carbsG,fatG,wholeEggCount) VALUES(7042,704,'Walnuts','10g',2,2.0,1.0,9.0,0)")
        }
    }
}

class IronLogSeedCallback(
    private val applicationScope: CoroutineScope,
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        applicationScope.launch(Dispatchers.IO) {
            seedReferenceData(db)
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        applicationScope.launch(Dispatchers.IO) {
            updateWorkoutDayColors(db)
        }
    }

    private fun seedReferenceData(db: SupportSQLiteDatabase) {
        val sqlStatements = buildList {
            // WorkoutDay (id is fixed 1-5)
            add("INSERT OR REPLACE INTO WorkoutDay(id, name, dayType, colorHex) VALUES(1, 'Upper Power', 'POWER', '${dayColorHexForId(1)}')")
            add("INSERT OR REPLACE INTO WorkoutDay(id, name, dayType, colorHex) VALUES(2, 'Lower Power', 'POWER', '${dayColorHexForId(2)}')")
            add("INSERT OR REPLACE INTO WorkoutDay(id, name, dayType, colorHex) VALUES(3, 'Upper Hypertrophy', 'HYPERTROPHY', '${dayColorHexForId(3)}')")
            add("INSERT OR REPLACE INTO WorkoutDay(id, name, dayType, colorHex) VALUES(4, 'Lower Hypertrophy', 'HYPERTROPHY', '${dayColorHexForId(4)}')")
            add("INSERT OR REPLACE INTO WorkoutDay(id, name, dayType, colorHex) VALUES(5, 'Calisthenics', 'CONDITIONING', '${dayColorHexForId(5)}')")

            // Day 1 – Upper Power
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(1, 'Barbell Bench Press', 5, 5, 5, 1, 'BARBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(1, 'Weighted Pull-ups', 5, 5, 5, 2, 'BODYWEIGHT', 'REPS', 'Track added weight')")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(1, 'Barbell OH Press', 4, 6, 8, 3, 'BARBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(1, 'Barbell Rows', 4, 6, 8, 4, 'BARBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(1, 'Close Grip Bench Press', 3, 8, 10, 5, 'BARBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(1, 'Farmer''s Walks', 3, 40, 40, 6, 'DUMBBELL', 'STEPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(1, 'Dead Bugs', 3, 10, 10, 7, 'BODYWEIGHT', 'PER_SIDE', NULL)")

            // Day 2 – Lower Power
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(2, 'Back Squat', 5, 5, 5, 1, 'BARBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(2, 'Romanian Deadlifts', 4, 6, 8, 2, 'BARBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(2, 'Goblet Squats', 3, 8, 10, 3, 'DUMBBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(2, 'Walking Lunges', 3, 10, 10, 4, 'DUMBBELL', 'PER_LEG', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(2, 'DB Calf Raises', 4, 12, 15, 5, 'DUMBBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(2, 'Barbell Rollouts', 3, 10, 15, 6, 'BARBELL', 'REPS', NULL)")

            // Day 3 – Upper Hypertrophy
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(3, 'Incline Dumbbell Press', 4, 8, 12, 1, 'DUMBBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(3, 'DB Rows', 4, 10, 12, 2, 'DUMBBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(3, 'DB Shoulder Press', 4, 10, 12, 3, 'DUMBBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(3, 'Chin-ups', 4, 8, 10, 4, 'BODYWEIGHT', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(3, 'DB Flyes', 3, 12, 15, 5, 'DUMBBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(3, 'Lateral Raises', 4, 12, 15, 6, 'DUMBBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(3, 'Barbell Curls', 3, 10, 12, 7, 'BARBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(3, 'OH DB Extension', 4, 10, 12, 8, 'DUMBBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(3, 'Hanging Leg Raises', 3, 0, 0, 9, 'BODYWEIGHT', 'REPS', 'To failure')")

            // Day 4 – Lower Hypertrophy
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(4, 'Bulgarian Split Squats', 4, 10, 12, 1, 'DUMBBELL', 'REPS', 'Hold DBs at sides')")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(4, 'Goblet Squats', 3, 12, 15, 2, 'DUMBBELL', 'REPS', 'Slow 3-1-3 tempo')")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(4, 'Barbell Hip Thrusts', 4, 10, 12, 3, 'BARBELL', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(4, 'Nordic Curls', 3, 6, 10, 4, 'BODYWEIGHT', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(4, 'Banded Spanish Squats', 3, 15, 20, 5, 'BAND', 'REPS', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(4, 'Single-Leg Calf Raises', 4, 15, 20, 6, 'DUMBBELL', 'PER_LEG', NULL)")
            add("INSERT INTO Exercise(workoutDayId, name, targetSets, targetRepsMin, targetRepsMax, orderIndex, equipmentType, repUnit, notes) VALUES(4, 'Dead Bugs', 3, 10, 10, 7, 'BODYWEIGHT', 'PER_SIDE', NULL)")

            // Day 5 – Calisthenics circuit
            addAll(AppDatabase.day5ExerciseInsertStatements())

            // Nutrition seed data (7 slots + 22 options + 73 ingredients)
            addAll(AppDatabase.nutritionSeedStatements())
        }

        db.beginTransaction()
        try {
            sqlStatements.forEach(db::execSQL)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun updateWorkoutDayColors(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            db.execSQL("UPDATE WorkoutDay SET colorHex = '${dayColorHexForId(1)}' WHERE id = 1")
            db.execSQL("UPDATE WorkoutDay SET colorHex = '${dayColorHexForId(2)}' WHERE id = 2")
            db.execSQL("UPDATE WorkoutDay SET colorHex = '${dayColorHexForId(3)}' WHERE id = 3")
            db.execSQL("UPDATE WorkoutDay SET colorHex = '${dayColorHexForId(4)}' WHERE id = 4")
            db.execSQL("UPDATE WorkoutDay SET colorHex = '${dayColorHexForId(5)}' WHERE id = 5")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
