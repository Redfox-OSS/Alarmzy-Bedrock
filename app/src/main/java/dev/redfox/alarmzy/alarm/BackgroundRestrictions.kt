package dev.redfox.alarmzy.alarm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Xiaomi's HyperOS/MIUI (Xiaomi, Redmi, POCO) kills apps in the background and blocks them from
 * being restarted unless the user turns on **Autostart** and sets **Battery saver → No
 * restrictions**. A killed app loses its pending AlarmManager alarms, so the clock widget stops
 * ticking (and alarms are at risk). Neither switch can be flipped by the app, and Autostart can't
 * even be read, so the UI deep-links to both and shows Autostart as "Check".
 */
object BackgroundRestrictions {
    private val XiaomiBrands = setOf("xiaomi", "redmi", "poco")

    fun isXiaomi(): Boolean =
        Build.MANUFACTURER.lowercase() in XiaomiBrands || Build.BRAND.lowercase() in XiaomiBrands

    /** AOSP battery-optimization exemption; HyperOS's "No restrictions" also sets it. */
    fun isUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    // Every deep link is an OEM-private screen that may move between HyperOS versions: fall back
    // to the app's info page, which on HyperOS lists both Autostart and Battery saver.

    fun openAutostart(context: Context) {
        val intent = Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        )
        if (!tryStart(context, intent)) openAppInfo(context)
    }

    fun openBatterySaver(context: Context) {
        val label = context.applicationInfo.loadLabel(context.packageManager).toString()
        val miui = Intent("miui.intent.action.HIDDEN_APPS_CONFIG_ACTIVITY")
            .setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"))
            .putExtra("package_name", context.packageName)
            .putExtra("package_label", label)
        if (tryStart(context, miui)) return
        if (tryStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) return
        openAppInfo(context)
    }

    fun openAppInfo(context: Context) {
        tryStart(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
    }

    private fun tryStart(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}
