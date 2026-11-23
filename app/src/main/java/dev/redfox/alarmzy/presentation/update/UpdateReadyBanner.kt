package dev.redfox.alarmzy.presentation.update

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.R
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.ButtonSize
import dev.redfox.bedrock.components.ButtonVariant
import dev.redfox.bedrock.effects.floatShadow
import dev.redfox.bedrock.effects.glass
import dev.redfox.bedrock.theme.BedrockTheme

/** Floating frosted pill shown once a flexible update has downloaded. */
@Composable
fun UpdateReadyBanner(onRestart: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BedrockTheme.colors
    Row(
        modifier = modifier
            .floatShadow(CircleShape)
            .glass(CircleShape, tint = colors.glassStrong)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(R.drawable.ic_notif_reset),
            contentDescription = null,
            tint = colors.accentSoft,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("Update ready", style = BedrockTheme.typography.label, color = colors.textPrimary)
        Spacer(Modifier.width(12.dp))
        BrButton(text = "Restart", onClick = onRestart, variant = ButtonVariant.Primary, size = ButtonSize.Small)
    }
}
