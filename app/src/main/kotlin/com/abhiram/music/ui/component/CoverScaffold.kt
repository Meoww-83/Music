package com.abhiram.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Big-art header scaffold for album / playlist / built-in detail screens: a springy cover
 * header, the title and optional subtitle over an expressive gradient, a sticky action row and
 * a scrolling [content] area that pulls the header away as it scrolls.
 *
 * Layout, top to bottom: an optional back button, the [cover] (which pops in with a bouncy
 * spring and a quick fade, both replaced by a hard cut under reduced motion), the centred
 * [title] and [subtitle] over a `primaryContainer` → transparent gradient, then the optional
 * sticky [actions] row (play / shuffle / menu cluster…) which stays pinned while [content]
 * scrolls underneath and collapses the header through the shared nested-scroll connection.
 * A floating action button may be overlaid at the bottom end.
 *
 * @param cover Big artwork block, typically sized by the scaffold's constraints.
 * @param modifier Modifier applied to the scaffold root.
 * @param title Header title.
 * @param subtitle Optional line under the title (artists, author, uploader…).
 * @param onBack Optional back button shown above the cover; `null` hides it.
 * @param actions Optional sticky action row under the header.
 * @param floatingActionButton Optional FAB slot pinned at the bottom end.
 * @param content Screen body; usually a `LazyColumn` filling the remaining space.
 */
@Composable
fun CoverScaffold(
    cover: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    floatingActionButton: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceMotion = rememberScaffoldReduceMotion()
    val collapse = rememberScaffoldHeaderCollapseState()
    val coverScale = remember { Animatable(0.86f) }
    val coverFade = remember { Animatable(0f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            coverScale.snapTo(1f)
            coverFade.snapTo(1f)
        } else {
            launch {
                coverScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            launch {
                coverFade.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 240))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            val columnScope: ColumnScope = this

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(coverHeaderBrush())
                    .statusBarsPadding()
                    .collapsibleHeader(collapse),
            ) {
                if (onBack != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 4.dp),
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp)
                            .widthIn(max = 300.dp)
                            .fillMaxWidth(0.72f)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                scaleX = coverScale.value
                                scaleY = coverScale.value
                                alpha = coverFade.value
                            },
                        content = cover,
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
            }

            if (actions != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    actions()
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(collapse.nestedScrollConnection),
            ) {
                columnScope.content()
            }
        }

        if (floatingActionButton != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                floatingActionButton()
            }
        }
    }
}

/**
 * The cover header's backdrop: the theme's dynamic `primaryContainer` fading into transparency
 * so the header melts into the screen background below it.
 */
@Composable
private fun coverHeaderBrush(): Brush =
    Brush.verticalGradient(
        0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        0.85f to Color.Transparent,
    )
