package com.ironlog.app.di

import android.content.Context
import androidx.room.Room
import com.ironlog.app.data.db.AppDatabase
import com.ironlog.app.data.db.IronLogSeedCallback
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
import com.ironlog.app.data.repository.NutritionRepository
import com.ironlog.app.data.repository.ScheduleRepository
import com.ironlog.app.data.repository.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        applicationScope: CoroutineScope,
    ): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "ironlog.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11)
            .addCallback(IronLogSeedCallback(applicationScope))
            .build()
    }

    // ── Existing DAOs ──────────────────────────────────────────────────────────
    @Provides fun provideWorkoutDayDao(db: AppDatabase): WorkoutDayDao = db.workoutDayDao()
    @Provides fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()
    @Provides fun provideWorkoutSessionDao(db: AppDatabase): WorkoutSessionDao = db.workoutSessionDao()
    @Provides fun provideExerciseLogDao(db: AppDatabase): ExerciseLogDao = db.exerciseLogDao()
    @Provides fun provideSetLogDao(db: AppDatabase): SetLogDao = db.setLogDao()
    @Provides fun provideScheduledWorkoutDao(db: AppDatabase): ScheduledWorkoutDao = db.scheduledWorkoutDao()

    // ── Nutrition DAOs ─────────────────────────────────────────────────────────
    @Provides fun provideMealSlotDao(db: AppDatabase): MealSlotDao = db.mealSlotDao()
    @Provides fun provideMealOptionDao(db: AppDatabase): MealOptionDao = db.mealOptionDao()
    @Provides fun provideMealIngredientDao(db: AppDatabase): MealIngredientDao = db.mealIngredientDao()
    @Provides fun provideDailyNutritionLogDao(db: AppDatabase): DailyNutritionLogDao = db.dailyNutritionLogDao()
    @Provides fun provideMealCompletionDao(db: AppDatabase): MealCompletionDao = db.mealCompletionDao()
    @Provides fun provideIngredientCheckDao(db: AppDatabase): IngredientCheckDao = db.ingredientCheckDao()

    // ── Repositories ───────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideWorkoutRepository(
        workoutDayDao: WorkoutDayDao,
        exerciseDao: ExerciseDao,
        workoutSessionDao: WorkoutSessionDao,
        exerciseLogDao: ExerciseLogDao,
        setLogDao: SetLogDao,
    ): WorkoutRepository = WorkoutRepository(
        workoutDayDao = workoutDayDao,
        exerciseDao = exerciseDao,
        workoutSessionDao = workoutSessionDao,
        exerciseLogDao = exerciseLogDao,
        setLogDao = setLogDao,
    )

    @Provides
    @Singleton
    fun provideScheduleRepository(
        scheduledWorkoutDao: ScheduledWorkoutDao,
    ): ScheduleRepository = ScheduleRepository(scheduledWorkoutDao)

    @Provides
    @Singleton
    fun provideNutritionRepository(
        db: AppDatabase,
        mealSlotDao: MealSlotDao,
        mealOptionDao: MealOptionDao,
        mealIngredientDao: MealIngredientDao,
        dailyNutritionLogDao: DailyNutritionLogDao,
        mealCompletionDao: MealCompletionDao,
        ingredientCheckDao: IngredientCheckDao,
    ): NutritionRepository = NutritionRepository(
        db = db,
        mealSlotDao = mealSlotDao,
        mealOptionDao = mealOptionDao,
        mealIngredientDao = mealIngredientDao,
        dailyNutritionLogDao = dailyNutritionLogDao,
        mealCompletionDao = mealCompletionDao,
        ingredientCheckDao = ingredientCheckDao,
    )
}
