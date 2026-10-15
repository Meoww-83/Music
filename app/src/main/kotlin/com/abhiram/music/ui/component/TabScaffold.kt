package com.abhiram.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Big-title host for a horizontally paged, tabbed screen (artist / album variants, library
 * sections…): a chunky title with an optional back button, a [TabGroup] pill strip below it and
 * a [HorizontalPager] filling the rest of the screen.
 *
 * The pager state is owned by the caller through [pagerState] (defaults to a fresh one sized
 * from [tabs]) so screens can drive page changes from anywhere; tapping a tab animates the
 * pager and swiping the pager updates the tab strip automatically. Adjacent pages fade with the
 * swipe (an `enter`/`exit` style fade driven by the pager's fractional offset), reduced to a
 * hard cut when reduce motion is active.
 *
 * @param title Big header title.
 * @param tabs Tabs shown by the [TabGroup] strip; one page per tab.
 * @param modifier Modifier applied to the scaffold root.
 * @param onBack Optional back handler; `null` hides the back button.
 * @param pagerState Pager state; defaults to `rememberPagerState { tabs.size }`.
 * @param content Page body for the given page index.
 */
@Composable
fun TabScaffold(
    title: String,
    tabs: List<Tab>,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    pagerState: PagerState = rememberPagerState { tabs.size },
    content: @Composable (Int) -> Unit,
) {
    val reduceMotion = rememberScaffoldReduceMotion()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = if (onBack != null) 4.dp else 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 2.dp,
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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        TabGroup(
            pagerState = pagerState,
            tabs = tabs,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            userScrollEnabled = tabs.size > 1,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Page fade tied to the swipe fraction; read in the layer block so the
                        // pages themselves never recompose while scrolling.
                        if (!reduceMotion) {
                            val offset =
                                page - (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            alpha = (1f - abs(offset) * 0.35f).coerceIn(0.65f, 1f)
                        }
                    },
            ) {
                content(page)
            }
        }
    }
}
