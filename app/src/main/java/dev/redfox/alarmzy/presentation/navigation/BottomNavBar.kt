package dev.redfox.alarmzy.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import dev.redfox.bedrock.navigation.BrNavBar
import dev.redfox.bedrock.navigation.BrNavItem

private val NavItems = BottomNavScreen.entries.map { BrNavItem(it.icon, it.label) }

/**
 * The app's tab bar: Bedrock's [BrNavBar] over the [BottomNavScreen] tabs. Icons only (the labels
 * are for screen readers); hold and slide across it to scrub between tabs. While [compact] is
 * true it eases into its slimmer form.
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (BottomNavScreen) -> Unit,
    compact: State<Boolean>,
    modifier: Modifier = Modifier
) {
    BrNavBar(
        items = NavItems,
        selectedIndex = BottomNavScreen.entries.indexOfFirst { it.route == currentRoute },
        onSelect = { onNavigate(BottomNavScreen.entries[it]) },
        modifier = modifier,
        compact = compact,
    )
}
