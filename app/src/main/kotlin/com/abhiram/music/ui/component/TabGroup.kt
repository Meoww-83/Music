package com.abhiram.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.abhiram.music.ui.theme.expressiveShapes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material3.Tab as Material3Tab

/**
 * One tab of [TabGroup] / [TabScaffold]: a required [title] and an optional leading [icon].
 *
 * @param title Text shown on the tab (kept to a single ellipsised line).
 * @param icon Optional icon rendered above the title.
 */
data class Tab(
    val title: String,
    val icon: ImageVector? = null,
)

/**
 * Secondary indicator tab row with an expressive morphing pill selection, synchronised with
 * [pagerState].
 *
 * The pill glides between the (equal width) tab slots following the pager's fractional scroll
 * position, so it trails the finger during a swipe and springs to the tapped tab through
 * `PagerState.animateScrollToPage`. Under reduced motion the pill snaps directly to the current
 * page. Tapping a tab scrolls [pagerState] and additionally reports the index through
 * [onTabSelected] so callers can react (e.g. refresh the page's data).
 *
 * @param pagerState State of the pager (or tab index holder) this row reflects.
 * @param tabs Tabs to render; each occupies an equal share of the row width.
 * @param modifier Modifier applied to the row root.
 * @param onTabSelected Optional callback invoked with the tapped tab index, before the pager
 * animates to it.
 */
@Composable
fun TabGroup(
    pagerState: PagerState,
    tabs: List<Tab>,
    modifier: Modifier = Modifier,
    onTabSelected: ((Int) -> Unit)? = null,
) {
    if (tabs.isEmpty()) return
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberScaffoldReduceMotion()
    val pillColor = MaterialTheme.colorScheme.primaryContainer

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    ) {
        val cellWidth = maxWidth / tabs.size.toFloat()
        val cellWidthPx = with(LocalDensity.current) { cellWidth.toPx() }
        val insetPx = with(LocalDensity.current) { 8.dp.toPx() }
        val page = pagerState.currentPage.coerceIn(0, tabs.lastIndex)
        val fraction = if (reduceMotion) {
            page.toFloat()
        } else {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, tabs.lastIndex.toFloat())
        }
        val pillX = fraction * cellWidthPx + insetPx

        // Morphing selection pill, drawn behind the tabs; offset translation keeps it off the
        // layout path so following the swipe costs nothing.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(pillX.roundToInt(), 0) }
                .width(cellWidth - 16.dp)
                .height(40.dp)
                .clip(MaterialTheme.expressiveShapes.pill)
                .background(pillColor),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                key(index) {
                    Material3Tab(
                        selected = index == page,
                        onClick = {
                            onTabSelected?.invoke(index)
                            if (index != pagerState.currentPage) {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = true,
                        selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        interactionSource = remember(index) { MutableInteractionSource() },
                        content = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.wrapContentHeight(),
                            ) {
                                if (tab.icon != null) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (index == page) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
    }
}
