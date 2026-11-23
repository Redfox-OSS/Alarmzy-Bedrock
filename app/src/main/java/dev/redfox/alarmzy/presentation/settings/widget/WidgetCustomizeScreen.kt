package dev.redfox.alarmzy.presentation.settings.widget

import android.content.ComponentName
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.alarm.BackgroundRestrictions
import dev.redfox.alarmzy.domain.model.ClockWidgetConfig
import dev.redfox.alarmzy.domain.model.WeatherInterval
import dev.redfox.alarmzy.domain.model.WeatherProvider
import dev.redfox.alarmzy.domain.model.WidgetHorizontalAlign
import dev.redfox.alarmzy.domain.model.WidgetLayout
import dev.redfox.alarmzy.domain.model.WidgetPart
import dev.redfox.alarmzy.domain.model.WidgetVerticalAlign
import dev.redfox.alarmzy.domain.model.WidgetWeather
import dev.redfox.alarmzy.presentation.livealerts.BackgroundRunFooter
import dev.redfox.alarmzy.presentation.livealerts.BackgroundRunRows
import dev.redfox.alarmzy.widget.ClockWidgetRenderer
import dev.redfox.alarmzy.widget.LaunchableApp
import dev.redfox.alarmzy.widget.WeatherError
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.BrChip
import dev.redfox.bedrock.components.BrDivider
import dev.redfox.bedrock.components.BrLargeTitle
import dev.redfox.bedrock.components.BrSearchField
import dev.redfox.bedrock.components.BrSheet
import dev.redfox.bedrock.components.BrSlider
import dev.redfox.bedrock.components.BrSwitch
import dev.redfox.bedrock.components.BrTextField
import dev.redfox.bedrock.components.BrTopBar
import dev.redfox.bedrock.components.ButtonSize
import dev.redfox.bedrock.components.ButtonVariant
import dev.redfox.bedrock.components.IconTile
import dev.redfox.bedrock.components.ListGroup
import dev.redfox.bedrock.components.ListRow
import dev.redfox.bedrock.components.SectionHeader
import dev.redfox.bedrock.components.rememberTopBarCollapse
import dev.redfox.bedrock.components.topBarClearance
import dev.redfox.bedrock.effects.glassSource
import dev.redfox.bedrock.effects.pressable
import dev.redfox.bedrock.motion.BrTransitions
import dev.redfox.bedrock.motion.RevealColumn
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrShapes
import dev.redfox.bedrock.theme.BrSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import kotlin.math.roundToInt

@Composable
fun WidgetCustomizeScreen(
    uiState: WidgetCustomizeUiState,
    onIntent: (WidgetCustomizeIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val config = uiState.config
    val scrollState = rememberScrollState()
    val collapse = rememberTopBarCollapse(scrollState)
    val title = "Home-screen widget"

    // Widgets may have been placed/removed (or apps installed) while we were away.
    LifecycleResumeEffect(Unit) {
        onIntent(WidgetCustomizeIntent.Refresh)
        onPauseOrDispose { }
    }

    Box(modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .glassSource()
                .verticalScroll(scrollState)
                .padding(
                    top = topBarClearance(),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + BrSpacing.XXL
                )
        ) {
            BrLargeTitle(title = title, collapse = collapse)
            RevealColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BrSpacing.Gutter),
                verticalArrangement = Arrangement.spacedBy(BrSpacing.Section)
            ) {
                Column {
                    SectionHeader("Preview")
                    WidgetPreview(
                        config = config,
                        weather = uiState.weather,
                        onPartClick = { onIntent(WidgetCustomizeIntent.OpenPicker(it)) }
                    )
                    Spacer(Modifier.height(BrSpacing.M))
                    Text(
                        if (config.showWeather) "Tap the date, time or weather to choose the app it opens."
                        else "Tap the date or the time to choose the app it opens.",
                        style = type.body,
                        color = colors.textSecondary
                    )
                }

                Column {
                    SectionHeader("On your home screen")
                    ListGroup {
                        ListRow(
                            title = when (uiState.placedCount) {
                                0 -> "Not added yet"
                                1 -> "1 widget placed"
                                else -> "${uiState.placedCount} widgets placed"
                            },
                            subtitle = if (uiState.canPinWidget) "Edits apply instantly"
                            else "Long-press your home screen → Widgets → Alarmzy",
                            leading = { IconTile(painterResource(R.drawable.ic_widgets)) },
                            trailing = if (uiState.canPinWidget) {
                                {
                                    BrButton(
                                        text = "Add",
                                        onClick = { onIntent(WidgetCustomizeIntent.AddToHomeScreen) },
                                        variant = ButtonVariant.Tonal,
                                        size = ButtonSize.Small
                                    )
                                }
                            } else null
                        )
                    }
                }

                // HyperOS kills the app in the background, and with it the widget's minute tick.
                if (uiState.placedCount > 0 && BackgroundRestrictions.isXiaomi()) {
                    Column {
                        SectionHeader("Keep it updating")
                        ListGroup {
                            BackgroundRunRows()
                        }
                        Spacer(Modifier.height(BrSpacing.M))
                        BackgroundRunFooter()
                    }
                }

                Column {
                    SectionHeader("Tap actions")
                    ListGroup {
                        val parts = WidgetPart.entries.filter { it != WidgetPart.WEATHER || config.showWeather }
                        parts.forEachIndexed { index, part ->
                            PartAppRow(
                                part = part,
                                app = uiState.partApps[part],
                                isDefault = part in uiState.defaultParts,
                                divider = index < parts.lastIndex,
                                onClick = { onIntent(WidgetCustomizeIntent.OpenPicker(part)) }
                            )
                        }
                    }
                }

                Column {
                    val percent = (config.scale * 100).roundToInt()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = BrSpacing.S, bottom = BrSpacing.M),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("Size", style = type.label, color = colors.textSecondary)
                        // Tapping the value puts the size back to 100%.
                        Text(
                            "$percent%",
                            style = type.figure,
                            color = colors.textSecondary,
                            modifier = Modifier
                                .pressable(pressedScale = 0.94f) { onIntent(WidgetCustomizeIntent.ResetScale) }
                                .semantics { contentDescription = "$percent%, reset size" }
                        )
                    }
                    BrSlider(
                        value = config.scale,
                        onValueChange = { onIntent(WidgetCustomizeIntent.DragScale((it * 20).roundToInt() / 20f)) },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = ClockWidgetConfig.MIN_SCALE..ClockWidgetConfig.MAX_SCALE,
                        onValueChangeFinished = { onIntent(WidgetCustomizeIntent.CommitScale) }
                    )
                }

                Column {
                    SectionHeader("Layout")
                    Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)) {
                        WidgetLayout.entries.forEach { layout ->
                            BrChip(
                                label = layout.displayName,
                                selected = layout == config.layout,
                                onClick = { onIntent(WidgetCustomizeIntent.SetLayout(layout)) }
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = config.layout == WidgetLayout.VERTICAL,
                        enter = BrTransitions.expandIn,
                        exit = BrTransitions.collapseOut
                    ) {
                        Text(
                            "Date, time and weather on separate lines. Resize the widget taller on your home screen to fit.",
                            style = type.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = BrSpacing.M)
                        )
                    }
                }

                Column {
                    SectionHeader("Horizontal position")
                    Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)) {
                        WidgetHorizontalAlign.entries.forEach { align ->
                            BrChip(
                                label = align.displayName,
                                selected = align == config.horizontalAlign,
                                onClick = { onIntent(WidgetCustomizeIntent.SetHorizontalAlign(align)) }
                            )
                        }
                    }
                }

                Column {
                    SectionHeader("Vertical position")
                    Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)) {
                        WidgetVerticalAlign.entries.forEach { align ->
                            BrChip(
                                label = align.displayName,
                                selected = align == config.verticalAlign,
                                onClick = { onIntent(WidgetCustomizeIntent.SetVerticalAlign(align)) }
                            )
                        }
                    }
                }

                Column {
                    SectionHeader("Style")
                    ListGroup {
                        ListRow(
                            title = "24-hour clock",
                            subtitle = if (config.use24Hour) "00:02" else "12:02",
                            leading = { IconTile(painterResource(R.drawable.ic_alarm)) },
                            trailing = {
                                BrSwitch(
                                    checked = config.use24Hour,
                                    onCheckedChange = { onIntent(WidgetCustomizeIntent.SetUse24Hour(it)) }
                                )
                            },
                            divider = true,
                            onClick = { onIntent(WidgetCustomizeIntent.SetUse24Hour(!config.use24Hour)) }
                        )
                        ListRow(
                            title = "Bold",
                            subtitle = "Heavier text, dividers and weather icon",
                            leading = { IconTile(painterResource(R.drawable.ic_format_paint)) },
                            trailing = {
                                BrSwitch(
                                    checked = config.bold,
                                    onCheckedChange = { onIntent(WidgetCustomizeIntent.SetBold(it)) }
                                )
                            },
                            onClick = { onIntent(WidgetCustomizeIntent.SetBold(!config.bold)) }
                        )
                    }
                }

                WeatherSection(uiState = uiState, onIntent = onIntent)
            }
        }

        BrTopBar(
            title = title,
            collapse = collapse,
            modifier = Modifier.align(Alignment.TopCenter),
            onBack = onNavigateBack
        )
    }

    // Sheets stay composed and are driven by `visible`, so their exits play in full.
    CitySearchSheet(
        visible = uiState.citySearch.open,
        search = uiState.citySearch,
        provider = uiState.config.weatherProvider,
        onIntent = onIntent
    )

    ApiKeySheet(
        visible = uiState.apiKeySheet.open,
        sheet = uiState.apiKeySheet,
        hasSavedKey = uiState.apiKey != null,
        onIntent = onIntent
    )

    // The picker keeps showing the part it was opened for while it slides away.
    val pickerPart = uiState.pickerPart
    val lastPickerPart = remember { arrayOfNulls<WidgetPart>(1) }
    if (pickerPart != null) lastPickerPart[0] = pickerPart
    (pickerPart ?: lastPickerPart[0])?.let { part ->
        AppPickerSheet(
            visible = pickerPart != null,
            part = part,
            uiState = uiState,
            onIntent = onIntent
        )
    }
}

/**
 * The real widget [android.widget.RemoteViews] inflated in-app over a wallpaper-ish backdrop,
 * so the preview is pixel-identical to the home screen. Its parts open the app picker.
 */
@Composable
private fun WidgetPreview(config: ClockWidgetConfig, weather: WidgetWeather?, onPartClick: (WidgetPart) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val latestClick by rememberUpdatedState(onPartClick)
    // The widget is drawn as bitmaps, so tick the preview at each minute boundary like the real one.
    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            val millis = System.currentTimeMillis()
            delay(60_000L - millis % 60_000L)
            value = ZonedDateTime.now()
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            // A 4x1 cell for the line; roughly 4x2 for the stack.
            .height(if (config.layout == WidgetLayout.VERTICAL) 200.dp else 132.dp)
            .clip(shape)
            .background(
                // A stand-in wallpaper, so the widget's own colours read as they would at home.
                Brush.linearGradient(listOf(Color(0xFF2B3A55), Color(0xFF1B1F2B), Color(0xFF3A2B45)))
            )
            .border(1.dp, BedrockTheme.colors.lineStrong, shape)
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        // The widget's cell bounds, so alignment changes are visible.
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
        )
        AndroidView(
            factory = { ctx -> FrameLayout(ctx) },
            update = { frame ->
                val ctx = frame.context
                val views = ClockWidgetRenderer.build(ctx, config, weather, now, withClicks = false)
                val layout = ClockWidgetRenderer.layoutFor(config)
                val child = frame.getChildAt(0)
                // reapply() needs the same layout; a layout switch inflates afresh.
                if (child == null || frame.tag != layout) {
                    frame.removeAllViews()
                    frame.addView(views.apply(ctx, frame))
                    frame.tag = layout
                } else {
                    views.reapply(ctx, child)
                }
                WidgetPart.entries.forEach { part ->
                    ClockWidgetRenderer.partViews(part).forEach { id ->
                        frame.findViewById<View>(id)?.setOnClickListener { latestClick(part) }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun WeatherSection(uiState: WidgetCustomizeUiState, onIntent: (WidgetCustomizeIntent) -> Unit) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val config = uiState.config
    val provider = config.weatherProvider
    Column {
        SectionHeader("Weather")
        ListGroup {
            ListRow(
                title = "Show weather",
                subtitle = weatherStatusText(uiState),
                leading = { IconTile(painterResource(R.drawable.ic_weather_cloud)) },
                trailing = {
                    BrSwitch(
                        checked = config.showWeather,
                        onCheckedChange = { onIntent(WidgetCustomizeIntent.SetShowWeather(it)) }
                    )
                },
                divider = true,
                onClick = {
                    when {
                        !config.showWeather -> onIntent(WidgetCustomizeIntent.SetShowWeather(true))
                        uiState.weatherError == WeatherError.NO_KEY -> onIntent(WidgetCustomizeIntent.OpenApiKeySheet)
                        uiState.weather == null || uiState.weatherError != null -> onIntent(WidgetCustomizeIntent.RefreshWeather)
                        else -> onIntent(WidgetCustomizeIntent.SetShowWeather(false))
                    }
                }
            )
            ListRow(
                title = "City",
                subtitle = config.weatherCity?.displayName ?: "Choose a city",
                leading = { IconTile(painterResource(R.drawable.ic_label)) },
                chevron = true,
                divider = true,
                onClick = { onIntent(WidgetCustomizeIntent.OpenCitySearch) }
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = BrSpacing.L, vertical = BrSpacing.M)) {
                Text("Provider", style = type.label, color = colors.textPrimary)
                Spacer(Modifier.height(BrSpacing.M))
                Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)) {
                    WeatherProvider.entries.forEach { option ->
                        BrChip(
                            label = option.displayName,
                            selected = option == provider,
                            onClick = { onIntent(WidgetCustomizeIntent.SetProvider(option)) }
                        )
                    }
                }
            }
            if (provider.needsApiKey || config.showWeather) BrDivider(inset = BrSpacing.L)
            if (provider.needsApiKey) {
                ListRow(
                    title = "API key",
                    subtitle = uiState.apiKey?.let { "••••${it.takeLast(4)}" + if (uiState.weatherError == WeatherError.AUTH) " · rejected" else "" }
                        ?: "Not set · tap to add",
                    leading = { IconTile(painterResource(R.drawable.ic_tune)) },
                    chevron = true,
                    divider = config.showWeather,
                    onClick = { onIntent(WidgetCustomizeIntent.OpenApiKeySheet) }
                )
            }
            if (config.showWeather) {
                Column(Modifier.fillMaxWidth().padding(horizontal = BrSpacing.L, vertical = BrSpacing.M)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Update every", style = type.label, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        BrButton(
                            text = if (uiState.weatherRefreshing) "Updating…" else "Update now",
                            onClick = { onIntent(WidgetCustomizeIntent.RefreshWeather) },
                            variant = ButtonVariant.Tonal,
                            size = ButtonSize.Small
                        )
                    }
                    Spacer(Modifier.height(BrSpacing.M))
                    Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)) {
                        WeatherInterval.entries.forEach { interval ->
                            BrChip(
                                label = interval.displayName,
                                selected = interval == config.weatherInterval,
                                onClick = { onIntent(WidgetCustomizeIntent.SetWeatherInterval(interval)) }
                            )
                        }
                    }
                }
                BrDivider(inset = BrSpacing.L)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .padding(horizontal = BrSpacing.L, vertical = BrSpacing.M),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Units", style = type.label, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)) {
                        BrChip(label = "°C", selected = !config.useFahrenheit, onClick = { onIntent(WidgetCustomizeIntent.SetFahrenheit(false)) })
                        BrChip(label = "°F", selected = config.useFahrenheit, onClick = { onIntent(WidgetCustomizeIntent.SetFahrenheit(true)) })
                    }
                }
            }
        }
        Spacer(Modifier.height(BrSpacing.M))
        Text(
            when (provider) {
                WeatherProvider.OPEN_METEO ->
                    "Weather comes from Open-Meteo (open-meteo.com), free and without an account. "
                WeatherProvider.OPENWEATHER ->
                    "Weather comes from OpenWeather (openweathermap.org) with your own API key, which " +
                        "stays on this phone and is never backed up. "
            } + "Only the city you search for and its coordinates leave your phone — Alarmzy never " +
                "reads your location. Weather updates on your chosen interval while the screen is on " +
                "(never waking the phone); if an update fails, the last reading stays.",
            style = type.caption,
            color = colors.textTertiary
        )
    }
}

private fun weatherStatusText(uiState: WidgetCustomizeUiState): String {
    val config = uiState.config
    val weather = uiState.weather
    val provider = config.weatherProvider.displayName
    val problem = when (uiState.weatherError) {
        WeatherError.NO_KEY -> "add your API key"
        WeatherError.AUTH -> "$provider rejected the API key"
        WeatherError.NETWORK -> "couldn't reach $provider"
        null -> null
    }
    return when {
        !config.showWeather -> "Temperature and an icon after the time"
        uiState.weatherRefreshing && weather == null -> "Updating…"
        weather == null && problem != null -> problem.replaceFirstChar { it.uppercase() } + " · tap to retry"
        weather == null -> "Waiting for the first update · tap to retry"
        else -> {
            val minutes = ((System.currentTimeMillis() - weather.fetchedAtMillis) / 60_000L).coerceAtLeast(0)
            val age = when {
                minutes < 1 -> "just now"
                minutes < 60 -> "$minutes min ago"
                minutes < 24 * 60 -> "${minutes / 60} h ago"
                else -> "${minutes / (24 * 60)} d ago"
            }
            val reading = "${weather.temperature(config.useFahrenheit)}° · ${weather.condition.displayName} · updated $age"
            if (problem != null) "$reading · $problem" else reading
        }
    }
}

@Composable
private fun CitySearchSheet(
    visible: Boolean,
    search: CitySearchState,
    provider: WeatherProvider,
    onIntent: (WidgetCustomizeIntent) -> Unit
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    BrSheet(visible = visible, onDismiss = { onIntent(WidgetCustomizeIntent.CloseCitySearch) }, title = "Weather city") {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(visible) { if (visible) focusRequester.requestFocus() }
        // The sheet sits over the keyboard: lift its content clear of it.
        Column(Modifier.fillMaxWidth().imePadding()) {
            BrSearchField(
                query = search.query,
                onQueryChange = { onIntent(WidgetCustomizeIntent.SetCityQuery(it)) },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = "Search for a city"
            )
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 420.dp),
                contentPadding = PaddingValues(vertical = BrSpacing.S)
            ) {
                val message = when {
                    search.searching -> "Searching…"
                    search.error == WeatherError.NO_KEY -> "Add your ${provider.displayName} API key first."
                    search.error == WeatherError.AUTH -> "${provider.displayName} rejected the API key."
                    search.error == WeatherError.NETWORK -> "Couldn't reach ${provider.displayName}. Check your connection."
                    search.query.trim().length < 2 -> "Type at least two letters"
                    search.results.isEmpty() -> "No places match “${search.query.trim()}”"
                    else -> null
                }
                if (message != null) {
                    item(key = "message") {
                        Text(
                            message,
                            style = type.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = BrSpacing.M, vertical = BrSpacing.L)
                        )
                    }
                }
                items(search.results, key = { "${it.latitude},${it.longitude}" }) { city ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .pressable(pressedScale = 0.99f) { onIntent(WidgetCustomizeIntent.PickCity(city)) }
                            .clip(BrShapes.Medium)
                            .padding(horizontal = BrSpacing.M, vertical = BrSpacing.M)
                    ) {
                        Text(city.name, style = type.label, color = colors.textPrimary)
                        if (city.detail.isNotBlank()) Text(city.detail, style = type.caption, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

/** Paste, verify and save the OpenWeather API key. */
@Composable
private fun ApiKeySheet(
    visible: Boolean,
    sheet: ApiKeySheetState,
    hasSavedKey: Boolean,
    onIntent: (WidgetCustomizeIntent) -> Unit
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val uriHandler = LocalUriHandler.current
    BrSheet(visible = visible, onDismiss = { onIntent(WidgetCustomizeIntent.CloseApiKeySheet) }, title = "OpenWeather API key") {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = BrSpacing.S),
            verticalArrangement = Arrangement.spacedBy(BrSpacing.L)
        ) {
            Text(
                "Use your own free key from openweathermap.org (My account → API keys). It's stored " +
                    "only on this phone and never included in backups.",
                style = type.body,
                color = colors.textSecondary
            )
            BrTextField(
                value = sheet.draft,
                onValueChange = { onIntent(WidgetCustomizeIntent.SetApiKeyDraft(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Paste your API key",
                supportingText = when (sheet.check) {
                    WeatherError.AUTH -> "OpenWeather rejected this key. New keys can take up to 2 hours to activate — you can save it anyway."
                    WeatherError.NETWORK -> "Couldn't reach OpenWeather to check the key. You can save it anyway."
                    else -> null
                },
                isError = sheet.check == WeatherError.AUTH,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onIntent(WidgetCustomizeIntent.SaveApiKey()) })
            )
            BrButton(
                text = if (sheet.check != null) "Save anyway" else "Check & save",
                onClick = { onIntent(WidgetCustomizeIntent.SaveApiKey(skipCheck = sheet.check != null)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = sheet.draft.isNotBlank(),
                loading = sheet.checking
            )
            Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)) {
                BrButton(
                    text = "Get a free key",
                    onClick = { uriHandler.openUri("https://home.openweathermap.org/api_keys") },
                    variant = ButtonVariant.Tonal,
                    size = ButtonSize.Small
                )
                if (hasSavedKey) {
                    BrButton(
                        text = "Remove key",
                        onClick = { onIntent(WidgetCustomizeIntent.RemoveApiKey) },
                        variant = ButtonVariant.Tonal,
                        size = ButtonSize.Small
                    )
                }
            }
        }
    }
}

@Composable
private fun PartAppRow(
    part: WidgetPart,
    app: LaunchableApp?,
    isDefault: Boolean,
    divider: Boolean,
    onClick: () -> Unit
) {
    ListRow(
        title = part.displayName,
        subtitle = when {
            app == null -> "No app found · tap to choose"
            isDefault -> "Opens ${app.label} (default)"
            else -> "Opens ${app.label}"
        },
        leading = { AppIconTile(app?.component, 36.dp) },
        chevron = true,
        divider = divider,
        onClick = onClick
    )
}

@Composable
private fun AppPickerSheet(
    visible: Boolean,
    part: WidgetPart,
    uiState: WidgetCustomizeUiState,
    onIntent: (WidgetCustomizeIntent) -> Unit
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val current = uiState.partApps[part]?.key.takeIf { part !in uiState.defaultParts }
    BrSheet(visible = visible, onDismiss = { onIntent(WidgetCustomizeIntent.ClosePicker) }, title = "${part.displayName} opens…") {
        // The sheet sits over the keyboard: lift its content clear of it.
        Column(Modifier.fillMaxWidth().imePadding()) {
            BrSearchField(
                query = uiState.query,
                onQueryChange = { onIntent(WidgetCustomizeIntent.SetQuery(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Search apps"
            )
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 520.dp),
                contentPadding = PaddingValues(vertical = BrSpacing.S)
            ) {
                if (uiState.query.isBlank()) {
                    item(key = "default") {
                        PickerRow(
                            title = "Default",
                            subtitle = when (part) {
                                WidgetPart.DATE -> "Your calendar app"
                                WidgetPart.CLOCK -> "Alarmzy"
                                WidgetPart.WEATHER -> "A weather app, if one is installed"
                            },
                            component = null,
                            selected = current == null,
                            onClick = { onIntent(WidgetCustomizeIntent.AssignApp(null)) }
                        )
                    }
                }
                if (uiState.appsLoading) {
                    item(key = "loading") {
                        Text(
                            "Loading apps…",
                            style = type.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = BrSpacing.M, vertical = BrSpacing.L)
                        )
                    }
                } else if (uiState.filteredApps.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "No apps match “${uiState.query.trim()}”",
                            style = type.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = BrSpacing.M, vertical = BrSpacing.L)
                        )
                    }
                }
                items(uiState.filteredApps, key = { it.key }) { app ->
                    PickerRow(
                        title = app.label,
                        subtitle = null,
                        component = app.component,
                        selected = app.key == current,
                        onClick = { onIntent(WidgetCustomizeIntent.AssignApp(app.key)) }
                    )
                }
            }
        }
    }
}

/** An app choice in the picker; the current one sits in a quiet accent fill with a tick. */
@Composable
private fun PickerRow(title: String, subtitle: String?, component: ComponentName?, selected: Boolean, onClick: () -> Unit) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.99f, onClick = onClick)
            .semantics { this.selected = selected }
            .clip(BrShapes.Medium)
            .background(if (selected) colors.accentContainer else Color.Transparent)
            .heightIn(min = 56.dp)
            .padding(horizontal = BrSpacing.M, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconTile(component, 36.dp)
        Spacer(Modifier.width(BrSpacing.M))
        Column(Modifier.weight(1f)) {
            Text(title, style = type.label, color = if (selected) colors.accentSoft else colors.textPrimary)
            if (subtitle != null) Text(subtitle, style = type.caption, color = colors.textSecondary)
        }
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_check_bold),
                contentDescription = "Selected",
                tint = colors.accentSoft,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** An app's launcher icon, loaded off the main thread; the widgets glyph when [component] is null. */
@Composable
private fun AppIconTile(component: ComponentName?, size: Dp) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, component, px) {
        value = component?.let {
            withContext(Dispatchers.IO) {
                runCatching { context.packageManager.getActivityIcon(it).toBitmap(px, px).asImageBitmap() }.getOrNull()
            }
        }
    }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        val bitmap = icon
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(BrShapes.concentric(size / 2 + 2.dp, 6.dp))
                    .background(BedrockTheme.colors.surfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                if (component == null) {
                    Icon(
                        painterResource(R.drawable.ic_widgets),
                        contentDescription = null,
                        tint = BedrockTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
