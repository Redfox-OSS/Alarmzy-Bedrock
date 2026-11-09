package dev.redfox.alarmzy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.redfox.alarmzy.widget.ClockWidgetStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AlarmzyApplication : Application() {

    @Inject lateinit var clockWidgetStore: ClockWidgetStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Aggressive OEMs (HyperOS/MIUI above all) kill the app in the background, which also drops
        // the clock widget's pending minute tick. Whatever starts the process next (an alarm, a
        // notification action, boot, a widget update, a tap) re-arms it and catches the time up.
        appScope.launch { runCatching { clockWidgetStore.refreshWidgets() } }
    }
}
