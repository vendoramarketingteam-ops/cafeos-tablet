package com.cafeos.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cafeos.tablet.ui.GameFeedback
import com.cafeos.tablet.ui.theme.HubCream
import com.cafeos.tablet.ui.theme.Dimens
import com.cafeos.tablet.ui.theme.HubEspresso
import com.cafeos.tablet.ui.theme.PebotTheme
import com.cafeos.tablet.ui.theme.ThemeMode
import com.cafeos.tablet.ui.theme.isGamified
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

/**
 * Persistent top bar for hub-and-spoke app screens (spec 003).
 *
 * - Home button is always top-left, same position on every app screen
 *   (muscle memory — per spec: "persistent Home control in a fixed position").
 * - Tap feedback via [GameFeedback.tap] in gamified mode.
 * - Height = 56dp (standard Material toolbar height, ≥ 48dp touch target).
 */
@Composable
fun AppTopBar(
    title: String,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    homeIcon: ImageVector = Icons.Default.Home,
) {
    val context = LocalContext.current
    val gamified = isGamified()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(HubCream)
            .padding(horizontal = Dimens.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        IconButton(
            onClick = {
                if (gamified) GameFeedback.tap(context)
                navController.popBackStack("home", inclusive = false)
            },
            modifier = Modifier
                .background(HubEspresso, shape = RoundedCornerShape(Dimens.radiusXLarge))
                .size(width = 72.dp, height = 36.dp)
        ) {
            Icon(
                imageVector = homeIcon,
                contentDescription = "Home",
                tint = HubCream,
                modifier = Modifier.size(Dimens.space12)
            )
            Spacer(modifier = Modifier.width(Dimens.space4))
            Text(
                text = "Home",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = HubCream
            )
        }
        Spacer(modifier = Modifier.width(Dimens.space12))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = HubEspresso  // matches HTML mockup --espresso
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────

@Preview(name = "AppTopBar - Gamified")
@Composable
private fun AppTopBarGamifiedPreview() {
    val navController = rememberNavController()
    PebotTheme(themeMode = ThemeMode.GAMIFIED) {
        AppTopBar(title = "New Order", navController = navController)
    }
}

@Preview(name = "AppTopBar - Classic")
@Composable
private fun AppTopBarClassicPreview() {
    val navController = rememberNavController()
    PebotTheme(themeMode = ThemeMode.CLASSIC) {
        AppTopBar(title = "New Order", navController = navController)
    }
}
