package com.abhiram.music.ui.screens.detail

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhiram.music.ui.component.TextPlaceholder
import com.abhiram.music.ui.component.TooltipIconButton
import com.abhiram.music.ui.glass.LiquidGlassSurface
import com.abhiram.music.ui.theme.expressiveShapes

/*
 * Shared shell of the five detail screens (owner SCR-D): back affordance, optional
 * pull-to-refresh, the expressive entrance and the overflow dropdown used by the screen
 * headers' ⋮ actions.
 *
 * The song lists, cover headers, action rows and loading/empty/error states all come from the
 * frozen CMP-D `ui.component.screen` components — this file only adds what those components
 * deliberately do not own (navigation callbacks, refresh gestures and screen-level menus).
 */

/**
 * Shell of a detail screen: status-bar aware content column, a floating liquid-glass back
 * button and — when [onRefresh] is supplied — a Material pull-to-refresh gesture.
 *
 * The content pops in with a bouncy spring (or a quick fade under reduced motion) so opening a
 * detail page feels alive without each screen re-implementing the entrance.
 *
 * @param onBack invoked by the floating back button (callbacks only — never a `NavController`).
 * @param modifier sizing modifier, `Modifier.fillMaxSize()` semantics.
 * @param refreshing whether the pull-to-refresh indicator is spinning.
 * @param onRefresh `null` disables pull-to-refresh (local, always-fresh Room-backed screens).
 * @param content body of the screen, scoped to a [Column]; usually one CMP-D screen component.
 */
@Composable
internal fun DetailScaffold(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = true,
            enter = rememberDetailHeaderEnter(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                content = content,
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (onRefresh != null) {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                body()
            }
        } else {
            body()
        }

        DetailBackButton(
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 8.dp, top = 8.dp),
        )
    }
}

/** Floating liquid-glass back button shared by every detail screen. */
@Composable
internal fun DetailBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier.size(48.dp),
        shape = MaterialTheme.expressiveShapes.pill,
    ) {
        TooltipIconButton(
            contentDescription = "Back",
            onClick = onBack,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Overflow dropdown anchored at the top end of the screen, fed by the CMP-D header's ⋮ action
 * (`onMenu = { expanded = true }`). Destructive entries render in the error colour.
 *
 * @param expanded whether the menu is currently open.
 * @param onDismissRequest closes the menu (outside tap / back press).
 * @param items entries in display order.
 * @param modifier positioning modifier; anchor it to the top end of the screen box.
 */
@Composable
internal fun DetailOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<DetailOverflowItem>,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier) {
        items.forEach { item ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = item.label,
                        color = if (item.destructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                enabled = item.enabled,
                onClick = {
                    onDismissRequest()
                    item.onClick()
                },
            )
        }
    }
}

/** One entry of a [DetailOverflowMenu]. */
internal data class DetailOverflowItem(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
)

/** Honest empty state built on the shared [TextPlaceholder]. */
@Composable
internal fun DetailEmptyState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextPlaceholder(text = text)
    }
}

/** Shares [link] through the system `ACTION_SEND` chooser. */
@Composable
internal fun rememberShareAction(link: String, title: String): () -> Unit {
    val context = LocalContext.current
    return remember(context, link, title) {
        {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, link)
                putExtra(Intent.EXTRA_TITLE, title)
            }
            runCatching {
                context.startActivity(Intent.createChooser(send, title))
            }.onFailure {
                Toast.makeText(context, "Unable to share", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/* -------------------------------------------------------------------------------------------
 *  Expressive motion
 * ---------------------------------------------------------------------------------------- */

/** Spring-based entrance: the header (cover art + title) pops in with a medium-bouncy spring. */
private fun detailSpringEnter(): EnterTransition =
    fadeIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
    ) + scaleIn(
        initialScale = 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )

/**
 * True when the system animator duration scale is 0 (reduced motion / accessibility); used to
 * swap the spring entrance for a short, motionless fade.
 */
@Composable
internal fun rememberDetailReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/** Screen entrance honouring [rememberDetailReduceMotion]: a quick fade instead of a spring. */
@Composable
internal fun rememberDetailHeaderEnter(): EnterTransition {
    val reduceMotion = rememberDetailReduceMotion()
    return remember(reduceMotion) {
        if (reduceMotion) fadeIn(tween(durationMillis = 120)) else detailSpringEnter()
    }
}
