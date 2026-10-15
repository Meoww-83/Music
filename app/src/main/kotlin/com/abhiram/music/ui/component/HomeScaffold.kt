package com.abhiram.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.nestedscroll.nestedScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.nestedscroll.NestedScrollConnection
import androidx.compose.ui.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Home scaffold: a scroll-away big title with an optional [header] block over a caller-provided
 * [backgroundBrush], and a full-bleed [content] area below.
 *
 * The brush paints behind everything (the Home screen passes its top-40 % dynamic-colour fade to
 * black); the header collapses 1:1 with the content's scroll through a shared nested-scroll
 * connection ([rememberScaffoldHeaderCollapseState]), so the title and header slide away while
 * the content grows into their place — and expand again when the list overscrolls at the top.
 * [content] is a `BoxScope` slot so screens can add pull-refresh, FABs or other overlays on top
 * of their `LazyColumn`.
 *
 * @param title Big header title; `null` renders no title (only [header]).
 * @param modifier Modifier applied to the scaffold root.
 * @param header Optional block rendered under the title inside the scroll-away header.
 * @param backgroundBrush Optional brush painted behind the whole scaffold.
 * @param content Screen body; usually a `LazyColumn` filling the remaining space.
 */
@Composable
fun HomeScaffold(
    title: String?,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    backgroundBrush: Brush? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val collapse = rememberScaffoldHeaderCollapseState()

    Box(modifier = modifier.fillMaxSize()) {
        if (backgroundBrush != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(backgroundBrush),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .collapsibleHeader(collapse),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
                header?.invoke(this)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(collapse.nestedScrollConnection),
            ) {
                content()
            }
        }
    }
}

/**
 * Scroll-driven collapse state shared by [HomeScaffold] and [CoverScaffold].
 *
 * The content's nested-scroll deltas are consumed here while the header can still collapse or
 * expand; once the header is fully hidden (or fully shown) the remaining delta flows to the
 * content as usual. [naturalHeightPx] is re-measured on every layout pass so a header whose
 * content changes size (loading → loaded) keeps collapsing correctly.
 */
internal class ScaffoldHeaderCollapseState {
    /** Natural, fully expanded height of the header in pixels (written during layout). */
    internal var naturalHeightPx: Float = 0f

    /** How many pixels of the header are currently hidden above its top edge. */
    internal val collapsedPx = mutableFloatStateOf(0f)

    /** Consumes scroll deltas while the header can still collapse (negative) or expand. */
    internal val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                takeDelta(available.y)

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = takeDelta(available.y)

            private fun takeDelta(delta: Float): Offset {
                val current = collapsedPx.floatValue
                val taken = if (delta < 0f) {
                    // Scrolling up: hide at most what is left of the header.
                    maxOf(delta, -(naturalHeightPx - current))
                } else {
                    // Scrolling down: reveal at most what is already hidden.
                    minOf(delta, current)
                }
                if (taken != 0f) {
                    collapsedPx.floatValue = current - taken
                }
                return Offset(0f, taken)
            }
        }
}

/** Remembers a [ScaffoldHeaderCollapseState] for one collapsible header. */
@Composable
internal fun rememberScaffoldHeaderCollapseState(): ScaffoldHeaderCollapseState =
    remember { ScaffoldHeaderCollapseState() }

/**
 * Makes the modified header collapse into a shrinking window: the child is measured at its
 * natural height, then only [ScaffoldHeaderCollapseState.collapsedPx] pixels of it are shown and
 * the child is placed shifted up so its content scrolls away like the rest of the screen.
 */
internal fun Modifier.collapsibleHeader(state: ScaffoldHeaderCollapseState): Modifier =
    this
        .clipToBounds()
        .layout { measurables, constraints ->
            val child = measurables.first().measure(
                constraints.copy(maxHeight = Constraints.Infinity),
            )
            state.naturalHeightPx = child.height.toFloat()
            val hidden = state.collapsedPx.floatValue
                .roundToInt()
                .coerceIn(0, child.height)
            val visible = child.height - hidden
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth else child.width
            layout(width, visible) {
                child.place(x = 0, y = -hidden)
            }
        }
