package dev.redfox.alarmzy.presentation.livealerts

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.alarm.BackgroundRestrictions
import dev.redfox.alarmzy.clock.LiveAlertPermissions
import dev.redfox.alarmzy.clock.LiveAlertStatus
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.BrCard
import dev.redfox.bedrock.components.BrDialogFrame
import dev.redfox.bedrock.components.ButtonSize
import dev.redfox.bedrock.components.ButtonVariant
import dev.redfox.bedrock.components.IconTile
import dev.redfox.bedrock.components.ListGroup
import dev.redfox.bedrock.components.ListRow
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrSpacing

/** Live permission status plus the actions to fix each piece. Shared by the home prompt and Settings. */
class LiveAlertsController internal constructor(
    private val statusState: MutableState<LiveAlertStatus>,
    private val onRequestNotifications: () -> Unit,
    private val context: Context,
) {
    val status: LiveAlertStatus get() = statusState.value

    fun fixNotifications() {
        if (!status.notificationsAllowed && Build.VERSION.SDK_INT >= 33) onRequestNotifications()
        else LiveAlertPermissions.openNotificationSettings(context) // allowed but channel muted
    }

    fun fixLiveUpdates() = LiveAlertPermissions.openLiveUpdatesSettings(context)
    fun fixFullScreen() = LiveAlertPermissions.openFullScreenSettings(context)
    fun openBbkSettings() = LiveAlertPermissions.openNotificationSettings(context)
}

/**
 * Reads [LiveAlertStatus] and re-reads it on every ON_RESUME: every step except the runtime
 * notification dialog happens in a system Settings screen that returns no result.
 * [onPermissionsChanged] fires whenever it changes, so running timers get (re-)promoted.
 */
@Composable
fun rememberLiveAlertsController(onPermissionsChanged: () -> Unit): LiveAlertsController {
    val context = LocalContext.current
    val activity = context.findActivity()
    val onChanged by rememberUpdatedState(onPermissionsChanged)

    val statusState = remember { mutableStateOf(LiveAlertPermissions.status(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Permanently denied (no rationale on a denial): the dialog won't show again, go to Settings.
        if (!granted && activity != null && Build.VERSION.SDK_INT >= 33 &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        ) LiveAlertPermissions.openNotificationSettings(context)
        statusState.value = LiveAlertPermissions.status(context)
        onChanged()
    }
    val controller = remember {
        LiveAlertsController(
            statusState = statusState,
            onRequestNotifications = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            context = context,
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val fresh = LiveAlertPermissions.status(context)
                if (fresh != statusState.value) {
                    statusState.value = fresh
                    onChanged()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return controller
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val BbkCopy =
    "On this phone, also turn on \"Show Live Updates on Live Alerts\" in notification settings so timers appear as a Live Alert."

// ---------------------------------------------------------------------------------------------
// Home prompt (Alarms tab) — asked once; Settings → Live alerts is the way back.
// ---------------------------------------------------------------------------------------------

/** One checklist step of the home prompt. [done] = null: the state can't be read (BBK toggle). */
private class PromptStep(
    val icon: Painter,
    val title: String,
    val subtitle: String?,
    val done: Boolean?,
    val onClick: () -> Unit,
)

@Composable
fun LiveAlertsHomePrompt(
    asked: Boolean,
    onMarkAsked: () -> Unit,
    onPermissionsChanged: () -> Unit,
) {
    val controller = rememberLiveAlertsController(onPermissionsChanged)
    // Decide once per composition entry, so the dialog doesn't vanish mid-flow as steps get ticked.
    var visible by remember { mutableStateOf(!asked && controller.status.worthPrompting) }
    val status = controller.status
    val dismiss = {
        visible = false
        onMarkAsked()
    }
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography

    val alarmIcon = painterResource(R.drawable.ic_alarm)
    val bellIcon = painterResource(R.drawable.ic_bell)
    val bellRingIcon = painterResource(R.drawable.ic_bell_ring)
    val phoneIcon = rememberVectorPainter(Icons.Rounded.PhoneAndroid)
    val steps = buildList {
        if (Build.VERSION.SDK_INT >= 34) {
            add(
                PromptStep(
                    icon = alarmIcon,
                    title = "Allow full-screen alarms",
                    subtitle = "So a ringing alarm takes over the lock screen",
                    done = status.fullScreenAllowed,
                    onClick = controller::fixFullScreen
                )
            )
        }
        if (Build.VERSION.SDK_INT >= 33 || !status.channelEnabled) {
            add(
                PromptStep(
                    icon = bellIcon,
                    title = "Allow notifications",
                    subtitle = null,
                    done = status.notificationsAllowed && status.channelEnabled,
                    onClick = controller::fixNotifications
                )
            )
        }
        status.liveUpdatesAllowed?.let { allowed ->
            add(
                PromptStep(
                    icon = bellRingIcon,
                    title = "Allow Live Updates",
                    subtitle = null,
                    done = allowed,
                    onClick = controller::fixLiveUpdates
                )
            )
        }
        if (status.showBbkStep) {
            add(
                PromptStep(
                    icon = phoneIcon,
                    title = "Turn on \"Show Live Updates on Live Alerts\"",
                    subtitle = "In notification settings: needed on this phone for the Live Alert pill",
                    done = null,
                    onClick = controller::openBbkSettings
                )
            )
        }
    }

    // Always composed and driven by [visible], so closing plays the dialog's calm exit.
    BrDialogFrame(visible = visible, onDismiss = dismiss) {
        IconTile(
            painterResource(R.drawable.ic_nav_timer),
            tint = colors.accentSoft,
            container = colors.accentContainer,
            size = 48.dp,
            iconSize = 22.dp
        )
        Spacer(Modifier.height(BrSpacing.L))
        Text("See your timers without opening the app", style = type.headline, color = colors.textPrimary)
        Spacer(Modifier.height(BrSpacing.S))
        Text(
            "Running timers and the stopwatch can live on your lock screen and status bar. A few settings are needed:",
            style = type.body,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(BrSpacing.L))

        ListGroup {
            steps.forEachIndexed { index, step ->
                PromptStepRow(step, divider = index < steps.lastIndex)
            }
        }

        Spacer(Modifier.height(BrSpacing.XL))
        BrButton(
            text = if (status.somethingMissing) "Not now" else "Done",
            onClick = dismiss,
            modifier = Modifier.fillMaxWidth(),
            variant = if (status.somethingMissing) ButtonVariant.Tonal else ButtonVariant.Primary,
            size = ButtonSize.Medium
        )
    }
}

/**
 * One checklist row. Done steps show a quiet tick and stop responding; open steps show an
 * "Allow" button, and the unreadable BBK step a softer "Check" (never a tick). The whole row is
 * the target too, so the small button never has to be hit exactly.
 */
@Composable
private fun PromptStepRow(step: PromptStep, divider: Boolean) {
    val colors = BedrockTheme.colors
    ListRow(
        title = step.title,
        subtitle = step.subtitle,
        modifier = Modifier.semantics(mergeDescendants = true) {},
        leading = { IconTile(step.icon, tint = colors.textSecondary) },
        trailing = {
            when (step.done) {
                true -> IconTile(
                    rememberVectorPainter(Icons.Rounded.Check),
                    modifier = Modifier.semantics { contentDescription = "Done" },
                    tint = colors.accentSoft,
                    container = colors.accentContainerStrong,
                    size = 28.dp,
                    iconSize = 16.dp
                )
                false -> BrButton("Allow", onClick = step.onClick, variant = ButtonVariant.Tonal, size = ButtonSize.Small)
                null -> BrButton("Check", onClick = step.onClick, variant = ButtonVariant.Plain, size = ButtonSize.Small)
            }
        },
        divider = divider,
        onClick = if (step.done == true) null else step.onClick
    )
}

// ---------------------------------------------------------------------------------------------
// Settings → "Live alerts" section content (rows go inside the Settings ListGroup)
// ---------------------------------------------------------------------------------------------

private enum class RowStatus { Allowed, Off, Check }

/** The Live alerts rows, for a caller's `ListGroup { }`: one per permission, each opening its fix. */
@Composable
fun LiveAlertsSettingsRows(onPermissionsChanged: () -> Unit) {
    val controller = rememberLiveAlertsController(onPermissionsChanged)
    val status = controller.status
    val liveUpdatesAllowed = status.liveUpdatesAllowed
    if (Build.VERSION.SDK_INT >= 34) {
        StatusRow(
            title = "Full-screen alarms",
            subtitle = "Ringing alarm over the lock screen",
            icon = painterResource(R.drawable.ic_alarm),
            status = if (status.fullScreenAllowed) RowStatus.Allowed else RowStatus.Off,
            divider = true, // Notifications always follows
            onClick = controller::fixFullScreen
        )
    }
    StatusRow(
        title = "Notifications",
        subtitle = "Timer and stopwatch notifications",
        icon = painterResource(R.drawable.ic_bell),
        status = if (status.notificationsAllowed && status.channelEnabled) RowStatus.Allowed else RowStatus.Off,
        divider = liveUpdatesAllowed != null || status.showBbkStep,
        onClick = controller::fixNotifications
    )
    if (liveUpdatesAllowed != null) {
        StatusRow(
            title = "Live Updates",
            subtitle = "Lock screen, status bar and Now Bar",
            icon = painterResource(R.drawable.ic_bell_ring),
            status = if (liveUpdatesAllowed) RowStatus.Allowed else RowStatus.Off,
            divider = status.showBbkStep,
            onClick = controller::fixLiveUpdates
        )
    }
    if (status.showBbkStep) {
        StatusRow(
            title = "Show on Live Alerts",
            subtitle = "\"Show Live Updates on Live Alerts\" toggle",
            icon = painterResource(R.drawable.ic_nav_timer),
            status = RowStatus.Check,
            divider = false,
            onClick = controller::openBbkSettings
        )
    }
}

/** Footer under the Live alerts group. */
@Composable
fun LiveAlertsSettingsFooter(modifier: Modifier = Modifier) {
    FooterText(
        "Running timers and the stopwatch show on your lock screen and status bar." +
            if (LiveAlertPermissions.isBbkDevice()) " $BbkCopy" else "",
        modifier
    )
}

/** A row that opens a system setting, with where that setting stands at its end. */
@Composable
private fun StatusRow(
    title: String,
    subtitle: String,
    icon: Painter,
    status: RowStatus,
    divider: Boolean,
    onClick: () -> Unit,
) {
    ListRow(
        title = title,
        subtitle = subtitle,
        leading = { IconTile(icon) },
        trailing = { StatusText(status) },
        chevron = true,
        divider = divider,
        onClick = onClick
    )
}

@Composable
private fun StatusText(status: RowStatus) {
    val colors = BedrockTheme.colors
    val (text, color) = when (status) {
        RowStatus.Allowed -> "Allowed" to colors.textTertiary
        RowStatus.Off -> "Off" to colors.caution // caution, not grey, so it stands out
        RowStatus.Check -> "Check" to colors.accentSoft // unknowable — never "Enabled"
    }
    Text(text, style = BedrockTheme.typography.body, color = color, maxLines = 1)
}

@Composable
private fun FooterText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = BedrockTheme.typography.caption,
        color = BedrockTheme.colors.textTertiary,
        modifier = modifier.padding(start = BrSpacing.L, end = BrSpacing.L, top = BrSpacing.S)
    )
}

/**
 * Persistent Alarms-tab warning while full-screen alarms are blocked (Android 14+). Without that
 * access a ringing alarm is only a banner — easy to miss or lose in the morning — so unlike the
 * one-time prompt this stays until it's fixed. Re-checks whenever the app resumes.
 */
@Composable
fun FullScreenAlarmWarning(modifier: Modifier = Modifier) {
    val controller = rememberLiveAlertsController(onPermissionsChanged = {})
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    BrCard(
        modifier = modifier.fillMaxWidth(),
        onClick = controller::fixFullScreen
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrSpacing.M)
        ) {
            IconTile(
                rememberVectorPainter(Icons.Rounded.WarningAmber),
                tint = colors.caution,
                container = colors.caution.copy(alpha = 0.16f)
            )
            Column(Modifier.weight(1f)) {
                Text("Alarms won't open full screen", style = type.label, color = colors.textPrimary)
                Spacer(Modifier.height(BrSpacing.XXS))
                Text(
                    "Allow full-screen notifications so ringing alarms appear over the lock screen.",
                    style = type.caption,
                    color = colors.textSecondary
                )
            }
            BrButton("Fix", onClick = controller::fixFullScreen, variant = ButtonVariant.Tonal, size = ButtonSize.Small)
        }
    }
}

/** Whether full-screen alarms are allowed; re-read every time the app resumes. */
@Composable
fun rememberFullScreenAlarmsAllowed(): Boolean =
    rememberLiveAlertsController(onPermissionsChanged = {}).status.fullScreenAllowed

// ---------------------------------------------------------------------------------------------
// Background running (Xiaomi / Redmi / POCO): Autostart + Battery saver. Shared by Settings and
// the widget page. Only call these when [BackgroundRestrictions.isXiaomi].
// ---------------------------------------------------------------------------------------------

/** The Autostart and Battery saver rows, for a caller's `ListGroup { }`. */
@Composable
fun BackgroundRunRows() {
    val context = LocalContext.current
    var unrestricted by remember { mutableStateOf(BackgroundRestrictions.isUnrestricted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) unrestricted = BackgroundRestrictions.isUnrestricted(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    StatusRow(
        title = "Autostart",
        subtitle = "Turn on so Alarmzy can restart after the system closes it",
        icon = painterResource(R.drawable.ic_alarm),
        status = RowStatus.Check, // unreadable on HyperOS
        divider = true,
        onClick = { BackgroundRestrictions.openAutostart(context) }
    )
    StatusRow(
        title = "Battery saver",
        subtitle = "Set to \"No restrictions\"",
        icon = painterResource(R.drawable.ic_info),
        // Unrestricted is reliable when true; HyperOS doesn't always mirror it, so never "Off".
        status = if (unrestricted) RowStatus.Allowed else RowStatus.Check,
        divider = false,
        onClick = { BackgroundRestrictions.openBatterySaver(context) }
    )
}

@Composable
fun BackgroundRunFooter(modifier: Modifier = Modifier) {
    FooterText(
        "HyperOS closes apps in the background, which stops the home-screen widget from updating and can " +
            "delay alarms. Turn on Autostart, set Battery saver to \"No restrictions\", and lock Alarmzy in " +
            "Recents (long-press its card → lock).",
        modifier
    )
}
