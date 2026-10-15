package com.abhiram.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Big-title scaffold with a pinned, horizontally scrolling `FilterChip` row — the shell of the
 * library tabs (Songs / Artists / Albums / Playlists).
 *
 * The title and the optional chip row stay pinned under the status bar while [content] (usually
 * a `LazyColumn`) scrolls below; the optional floating action button and snackbar host float
 * above the content. Everything insets itself for the system bars, so the screen works
 * edge-to-edge out of the box.
 *
 * @param title Big header title.
 * @param modifier Modifier applied to the scaffold root.
 * @param onBack Optional back button shown before the title; `null` hides it.
 * @param filterChips Optional pinned chip row rendered under the title.
 * @param floatingActionButton Optional FAB slot, pinned at the bottom end.
 * @param snackbarHost Snackbar host slot, pinned at the bottom centre.
 * @param content Screen body; give it its own scroll container (e.g. a `LazyColumn`).
 */
@Composable
fun ChipScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    filterChips: (@Composable RowScope.() -> Unit)? = null,
    floatingActionButton: (@Composable BoxScope.() -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceMotion = rememberScaffoldReduceMotion()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            val columnScope: ColumnScope = this

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .animateContentSize(
                        animationSpec = if (reduceMotion) {
                            spring(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
                        } else {
                            spring()
                        },
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (onBack != null) 4.dp else 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 4.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (filterChips != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(top = 2.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        filterChips()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (floatingActionButton != null) 88.dp else 16.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            snackbarHost()
        }
    }
}
