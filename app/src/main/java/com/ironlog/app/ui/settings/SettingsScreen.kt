package com.ironlog.app.ui.settings

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.ironlog.app.ui.components.foundation.AnimatedStatText
import com.ironlog.app.ui.components.foundation.HairlineDivider
import com.ironlog.app.ui.components.foundation.ProYouTopBar
import com.ironlog.app.ui.components.foundation.SectionHeader
import com.ironlog.app.ui.components.foundation.StaggeredEntrance
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.components.foundation.TopBarIconButton
import com.ironlog.app.ui.components.foundation.pressScale
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.MotionTokens
import com.ironlog.app.util.SettingsStore
import com.ironlog.app.util.SettingsStore.ThemeConfig
import com.ironlog.app.util.SettingsStore.WorkoutScreenTimeout
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.onSignInSuccess(account)
            } catch (e: ApiException) {
                viewModel.onSignInFailed()
            }
        }
    }
    val onGoogleSignIn = { signInLauncher.launch(viewModel.getSignInIntent()) }
    var restEdit by remember { mutableStateOf<RestTimerEditTarget?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProYouTopBar(
                overline = "ProYou",
                title = "Settings",
                navigationIcon = {
                    TopBarIconButton(
                        icon = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        onClick = onNavigateBack,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = Dimens.ScreenPaddingH,
                end = Dimens.ScreenPaddingH,
                top = 8.dp,
                bottom = Dimens.ScreenPaddingBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
        ) {
            // ─── Rest Timer Feedback ───
            item {
                StaggeredEntrance(index = 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        SectionHeader(title = "Rest Timer Feedback")
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            SwitchSettingRow(
                                title = "Countdown Vibration",
                                subtitle = "Vibrate with the 10-second warning and the 5-second countdown beeps",
                                checked = state.countdownVibrationEnabled,
                                onCheckedChange = { enabled -> viewModel.setCountdownVibrationEnabled(enabled) },
                            )

                            HairlineDivider()

                            var localVolume by remember(state.beepVolume) { mutableFloatStateOf(state.beepVolume) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Dimens.CardPadding, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column {
                                    Text(
                                        "Beep Volume",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "${(localVolume * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Slider(
                                    value = localVolume,
                                    onValueChange = { localVolume = it },
                                    onValueChangeFinished = { viewModel.setBeepVolume(localVolume) },
                                    valueRange = 0.0f..1.0f,
                                )
                            }
                        }
                    }
                }
            }

            // ─── Workout Screen Timeout ───
            item {
                StaggeredEntrance(index = 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        SectionHeader(title = "Workout Screen")
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(Dimens.CardPadding),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Screen Timeout",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "Keeps the screen awake only while a workout is in progress. Off uses the device default.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    WorkoutScreenTimeout.entries.forEach { option ->
                                        val selected = state.workoutScreenTimeout == option
                                        val chipColor by animateColorAsState(
                                            targetValue = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                            },
                                            animationSpec = MotionTokens.standardSpec(),
                                            label = "timeoutChipBg-${option.name}",
                                        )
                                        val textColor by animateColorAsState(
                                            targetValue = if (selected) {
                                                Color.White
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            animationSpec = MotionTokens.standardSpec(),
                                            label = "timeoutChipText-${option.name}",
                                        )
                                        Box(
                                            modifier = Modifier
                                                .pressScale()
                                                .clip(RoundedCornerShape(50))
                                                .background(chipColor)
                                                .clickable { viewModel.setWorkoutScreenTimeout(option) }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                        ) {
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = textColor,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── Theme ───
            item {
                StaggeredEntrance(index = 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        SectionHeader(title = "Theme")
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.CardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "App Theme",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "Choose dark or light mode",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                val isDark = themeConfig == ThemeConfig.DARK
                                SegmentedToggle(
                                    options = listOf("DARK", "LIGHT"),
                                    selectedIndex = if (isDark) 0 else 1,
                                    onSelect = { index ->
                                        viewModel.updateTheme(if (index == 0) ThemeConfig.DARK else ThemeConfig.LIGHT)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ─── Cloud Backup ───
            item {
                StaggeredEntrance(index = 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        SectionHeader(title = "Cloud Backup")
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            if (state.isSignedIn) {
                                // Account row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimens.CardPadding),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            state.googleEmail ?: "",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            if (state.isSyncing) "Syncing…" else "Auto-sync enabled",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                }

                                HairlineDivider()

                                // Last backup row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimens.CardPadding),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "Last backup",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        state.lastBackupTimeStr ?: "Never",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                HairlineDivider()

                                // Backup Now
                                SettingActionRow(
                                    icon = Icons.Outlined.CloudUpload,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    title = "Backup Now",
                                    enabled = !state.isSyncing,
                                    onClick = { viewModel.backupNow() },
                                )

                                HairlineDivider()

                                // Restore — triggers a confirmation dialog before any destructive action.
                                SettingActionRow(
                                    icon = Icons.Outlined.CloudDownload,
                                    iconTint = MaterialTheme.colorScheme.secondary,
                                    title = "Restore from Backup",
                                    enabled = !state.isSyncing,
                                    onClick = { viewModel.requestRestoreConfirmation() },
                                )

                                HairlineDivider()

                                // Sign out
                                SettingActionRow(
                                    icon = Icons.Outlined.Logout,
                                    iconTint = MaterialTheme.colorScheme.error,
                                    title = "Sign Out",
                                    titleColor = MaterialTheme.colorScheme.error,
                                    onClick = { viewModel.signOut() },
                                )
                            } else {
                                // Sign in row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pressScale()
                                        .clickable { onGoogleSignIn() }
                                        .padding(Dimens.CardPadding),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.Cloud,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Sign in with Google",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            "Backup your data to Google Drive",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── Program Start Date + Calendar Settings ───
            item {
                StaggeredEntrance(index = 3) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        SectionHeader(title = "Program Lifecycle")
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            val date = state.programStartDate ?: LocalDate.now()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale()
                                    .clickable {
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d -> viewModel.setProgramStartDate(LocalDate.of(y, m + 1, d)) },
                                            date.year,
                                            date.monthValue - 1,
                                            date.dayOfMonth,
                                        ).show()
                                    }
                                    .padding(Dimens.CardPadding),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    "Program Start Date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = state.programStartDate
                                            ?.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.ENGLISH))
                                            ?: "Tap to set date",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }

                            HairlineDivider()

                            // First Day of Week toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.CardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "First Day of Week",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "Calendar start day",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                val isSunday = state.firstDayOfWeek == "SUNDAY"
                                SegmentedToggle(
                                    options = listOf("SUN", "MON"),
                                    selectedIndex = if (isSunday) 0 else 1,
                                    onSelect = { index ->
                                        viewModel.setFirstDayOfWeek(if (index == 0) "SUNDAY" else "MONDAY")
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ─── Equipment Configuration ───
            item {
                var showAddDialog by remember { mutableStateOf(false) }
                var newWeight by remember { mutableStateOf("") }

                if (showAddDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddDialog = false; newWeight = "" },
                        title = { Text("Add Dumbbell Weight") },
                        text = {
                            OutlinedTextField(
                                value = newWeight,
                                onValueChange = { newWeight = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                newWeight.toFloatOrNull()?.let { viewModel.addDumbbellWeight(it) }
                                newWeight = ""
                                showAddDialog = false
                            }) { Text("Add") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddDialog = false; newWeight = "" }) { Text("Cancel") }
                        },
                    )
                }

                StaggeredEntrance(index = 4) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        SectionHeader(
                            title = "Equipment Configuration",
                            action = {
                                Row(
                                    modifier = Modifier
                                        .pressScale()
                                        .clip(RoundedCornerShape(Dimens.RadiusS))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { showAddDialog = true }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        "Add Weight",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                    )
                                }
                            },
                        )
                        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "Tap × to remove a weight. Dumbbell exercises can only use weights from this list.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (state.dumbbellWeights.isEmpty()) {
                                    Text(
                                        "No weights configured. Tap \"Add Weight\" to add one.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    )
                                } else {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        state.dumbbellWeights.forEach { weight ->
                                            Row(
                                                modifier = Modifier
                                                    .pressScale()
                                                    .clip(RoundedCornerShape(50))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    text = "${weight.toInt()} kg",
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                    color = Color.White,
                                                )
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = "Remove ${weight.toInt()} kg",
                                                    tint = Color.White.copy(alpha = 0.7f),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { viewModel.removeDumbbellWeight(weight) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── Rest Timer Defaults ───
            item {
                StaggeredEntrance(index = 5) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        SectionHeader(title = "Rest Timer Defaults")
                        Text(
                            "Each value is used only on that workout. Tap the number to type, or use + / −.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                            RestTimerCard(
                                label = "POWER",
                                subtitle = "Days 1–2",
                                seconds = state.restPowerSeconds,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                onSecondsChange = viewModel::setPowerRestSeconds,
                                onEditClick = {
                                    restEdit = RestTimerEditTarget(
                                        title = "Power rest",
                                        subtitle = "Upper Power and Lower Power",
                                        seconds = state.restPowerSeconds,
                                        onSave = viewModel::setPowerRestSeconds,
                                    )
                                },
                            )
                            RestTimerCard(
                                label = "HYPERTROPHY",
                                subtitle = "Days 3–4",
                                seconds = state.restHypertrophySeconds,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f),
                                onSecondsChange = viewModel::setHypertrophyRestSeconds,
                                onEditClick = {
                                    restEdit = RestTimerEditTarget(
                                        title = "Hypertrophy rest",
                                        subtitle = "Upper Hypertrophy and Lower Hypertrophy",
                                        seconds = state.restHypertrophySeconds,
                                        onSave = viewModel::setHypertrophyRestSeconds,
                                    )
                                },
                            )
                        }
                        RestTimerCard(
                            label = "CALISTHENICS",
                            subtitle = "Day 5",
                            seconds = state.restBonusSeconds,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth(),
                            onSecondsChange = viewModel::setBonusRestSeconds,
                            onEditClick = {
                                restEdit = RestTimerEditTarget(
                                    title = "Calisthenics rest",
                                    subtitle = "Rest between circuit sets",
                                    seconds = state.restBonusSeconds,
                                    onSave = viewModel::setBonusRestSeconds,
                                )
                            },
                        )
                    }
                }
            }

            // ─── Notifications ───
            item {
                StaggeredEntrance(index = 6) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        SectionHeader(title = "Engagement")
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            SwitchSettingRow(
                                title = "Reminder Notification",
                                subtitle = "Daily nudge to hit your workout goal",
                                checked = state.reminderEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setReminder(enabled, state.reminderTime)
                                },
                            )

                            HairlineDivider()

                            val reminderTimeStr = state.reminderTime
                                .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (state.reminderEnabled) Modifier.pressScale() else Modifier)
                                    .clickable(enabled = state.reminderEnabled) {
                                        val t = state.reminderTime
                                        TimePickerDialog(
                                            context,
                                            { _, h, m -> viewModel.setReminder(true, LocalTime.of(h, m)) },
                                            t.hour,
                                            t.minute,
                                            false,
                                        ).show()
                                    }
                                    .padding(Dimens.CardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Notification Time",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Dimens.RadiusS))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = reminderTimeStr,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = if (state.reminderEnabled) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── Export ───
            item {
                StaggeredEntrance(index = 8) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                        SectionHeader(title = "Data Export")
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (!state.exportInProgress) Modifier.pressScale() else Modifier)
                                    .clickable(enabled = !state.exportInProgress) { viewModel.exportToCsv() }
                                    .padding(Dimens.CardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (state.exportInProgress) "Exporting…" else "Export My Data",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "Generate a CSV of all workout history",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            // Share last export (if available)
                            if (state.lastExportFile != null) {
                                HairlineDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pressScale()
                                        .clickable { viewModel.shareLastExport() }
                                        .padding(Dimens.CardPadding),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Share last export",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── Version footer ───
            item {
                StaggeredEntrance(index = 8) {
                    Text(
                        text = "ProYou V1.0.0",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    restEdit?.let { target ->
        RestTimerEditDialog(
            target = target,
            onDismiss = { restEdit = null },
            onSave = { seconds ->
                target.onSave(seconds)
                restEdit = null
            },
        )
    }

    // ─── Destructive-restore confirmation ───
    if (state.restorePendingConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelRestore() },
            title = { Text("Replace all local data?") },
            text = {
                Text(
                    "Restoring will overwrite all workouts, schedules and nutrition data on this " +
                        "device with the latest cloud backup. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore() }) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRestore() }) { Text("Cancel") }
            },
        )
    }

    state.restoreError?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearRestoreError() },
            title = { Text("Restore failed") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearRestoreError() }) { Text("OK") }
            },
        )
    }

    if (state.restoreWarnings.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.clearRestoreError() },
            title = { Text("Restore complete (with warnings)") },
            text = {
                Text(state.restoreWarnings.joinToString("\n• ", prefix = "• "))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearRestoreError() }) { Text("OK") }
            },
        )
    }

    state.syncError?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSyncError() },
            title = { Text("Sync issue") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSyncError() }) { Text("OK") }
            },
        )
    }

    state.exportError?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExportError() },
            title = { Text("Export failed") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearExportError() }) { Text("OK") }
            },
        )
    }
}

// ─── Helper composables ──────────────────────────────────────────────────────

/** Two-option pill toggle with an animated selected segment. */
@Composable
private fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusM))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val segmentColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = MotionTokens.standardSpec(),
                label = "segmentBg",
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = MotionTokens.standardSpec(),
                label = "segmentText",
            )
            Box(
                modifier = Modifier
                    .pressScale()
                    .clip(RoundedCornerShape(Dimens.RadiusS))
                    .background(segmentColor)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = textColor,
                )
            }
        }
    }
}

/** Setting row with title + subtitle and a trailing switch; 20dp internal padding. */
@Composable
private fun SwitchSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = Color.White,
            ),
        )
    }
}

/** Tappable icon + label row inside a zero-padding SurfaceCard. */
@Composable
private fun SettingActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.pressScale() else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(Dimens.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = titleColor,
        )
    }
}

@Composable
private fun RestTimerCard(
    label: String,
    subtitle: String,
    seconds: Int,
    color: Color,
    onSecondsChange: (Int) -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RestStepButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Decrease rest",
                onClick = {
                    onSecondsChange((seconds - REST_STEP_SECONDS).coerceAtLeast(SettingsStore.REST_SECONDS_MIN))
                },
            )
            Column(
                modifier = Modifier
                    .pressScale()
                    .clickable(onClick = onEditClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedStatText(
                        text = "$seconds",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = color,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "SEC",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 2.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(
                    text = "Tap to edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            RestStepButton(
                icon = Icons.Filled.Add,
                contentDescription = "Increase rest",
                onClick = {
                    onSecondsChange((seconds + REST_STEP_SECONDS).coerceAtMost(SettingsStore.REST_SECONDS_MAX))
                },
            )
        }
    }
}

@Composable
private fun RestStepButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .pressScale()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

private data class RestTimerEditTarget(
    val title: String,
    val subtitle: String,
    val seconds: Int,
    val onSave: (Int) -> Unit,
)

@Composable
private fun RestTimerEditDialog(
    target: RestTimerEditTarget,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var text by remember(target.seconds) { mutableStateOf(target.seconds.toString()) }
    val parsed = text.toIntOrNull()?.coerceIn(SettingsStore.REST_SECONDS_MIN, SettingsStore.REST_SECONDS_MAX)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(target.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    target.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { incoming ->
                        text = incoming.filter { it.isDigit() }.take(3)
                    },
                    label = { Text("Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = {
                        Text("${SettingsStore.REST_SECONDS_MIN}–${SettingsStore.REST_SECONDS_MAX} seconds")
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null,
                onClick = { parsed?.let(onSave) },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private const val REST_STEP_SECONDS = 5

