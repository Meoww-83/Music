package com.abhiram.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The app's set of expressive silhouette shapes, hand-picked from Material 3's shape library
 * ([MaterialShapes]) and rounded-corner fallbacks.
 *
 * Every field is a ready-to-use [Shape] for `Modifier.clip`, `Surface(shape = …)`,
 * `Card(shape = …)` and friends; the call sites read
 * `MaterialTheme.expressiveShapes.pill`.
 *
 * The polygons behind [oval], [cookie], [bun], [gem] and the optional extras come from
 * [MaterialShapes] (the M3 Expressive shape library) via `RoundedPolygon.toShape()`; [pill] and
 * [squircle] use corner-based shapes because MaterialShapes has no fully-rounded or continuous
 * "squircle" entry.
 *
 * @property pill Fully rounded stadium: every corner is 50% of the shorter side.
 * @property oval Elliptical/squashed circle from `MaterialShapes.Oval`.
 * @property cookie Organic asymmetric blob from `MaterialShapes.Cookie9Sided`.
 * @property squircle Chunky superellipse-ish rounded square (30% corners; MaterialShapes has no
 * continuous squircle, so a large `RoundedCornerShape` stands in).
 * @property bun Squashed dome/bun silhouette from `MaterialShapes.Bun`.
 * @property gem Cut-corner gem from `MaterialShapes.Gem`.
 * @property circle Perfect circle from `MaterialShapes.Circle`.
 * @property heart Heart silhouette from `MaterialShapes.Heart`.
 * @property clover Four-leaf clover from `MaterialShapes.Clover4Leaf`.
 * @property arch Arched slab from `MaterialShapes.Arch`.
 * @property slanted Slanted square from `MaterialShapes.Slanted`.
 * @property arrow Arrow from `MaterialShapes.Arrow`.
 * @property semiCircle Half-circle from `MaterialShapes.SemiCircle`.
 * @property pentagon Pentagon from `MaterialShapes.Pentagon`.
 * @property triangle Rounded triangle from `MaterialShapes.Triangle`.
 * @property diamond Diamond from `MaterialShapes.Diamond`.
 * @property ghostish Ghost-ish shape from `MaterialShapes.Ghostish`.
 * @property flower Flower from `MaterialShapes.Flower`.
 * @property sunny Sunny star from `MaterialShapes.Sunny`.
 */
@Immutable
data class ExpressiveShapes(
    val pill: Shape,
    val oval: Shape,
    val cookie: Shape,
    val squircle: Shape,
    val bun: Shape,
    val gem: Shape,
    val circle: Shape,
    val heart: Shape,
    val clover: Shape,
    val arch: Shape,
    val slanted: Shape,
    val arrow: Shape,
    val semiCircle: Shape,
    val pentagon: Shape,
    val triangle: Shape,
    val diamond: Shape,
    val ghostish: Shape,
    val flower: Shape,
    val sunny: Shape,
) {

    companion object {

        /**
         * Static, composable access to the current theme's silhouettes, for call sites that need
         * a shape without an explicit receiver — for example a default parameter of a
         * `@Composable` function: `shape: Shape = ExpressiveShapes.cookie`.
         *
         * Each accessor resolves through [MaterialTheme.expressiveShapes], i.e. the shapes
         * remembered by the closest [ExpressiveTheme].
         *
         * Fully rounded pill.
         */
        val pill: Shape
            @Composable get() = MaterialTheme.expressiveShapes.pill

        /** Elliptical oval cut from `MaterialShapes.Oval`. */
        val oval: Shape
            @Composable get() = MaterialTheme.expressiveShapes.oval

        /** Organic asymmetric cookie blob cut from `MaterialShapes.Cookie9Sided`. */
        val cookie: Shape
            @Composable get() = MaterialTheme.expressiveShapes.cookie

        /** Chunky superellipse-ish rounded square. */
        val squircle: Shape
            @Composable get() = MaterialTheme.expressiveShapes.squircle

        /** Squashed bun silhouette cut from `MaterialShapes.Bun`. */
        val bun: Shape
            @Composable get() = MaterialTheme.expressiveShapes.bun

        /** Cut-corner gem silhouette cut from `MaterialShapes.Gem`. */
        val gem: Shape
            @Composable get() = MaterialTheme.expressiveShapes.gem

        /** Perfect circle cut from `MaterialShapes.Circle`. */
        val circle: Shape
            @Composable get() = MaterialTheme.expressiveShapes.circle

        /** Heart silhouette cut from `MaterialShapes.Heart`. */
        val heart: Shape
            @Composable get() = MaterialTheme.expressiveShapes.heart

        /** Four-leaf clover cut from `MaterialShapes.Clover4Leaf`. */
        val clover: Shape
            @Composable get() = MaterialTheme.expressiveShapes.clover

        /** Arched slab cut from `MaterialShapes.Arch`. */
        val arch: Shape
            @Composable get() = MaterialTheme.expressiveShapes.arch

        /** Slanted square cut from `MaterialShapes.Slanted`. */
        val slanted: Shape
            @Composable get() = MaterialTheme.expressiveShapes.slanted

        /** Arrow silhouette cut from `MaterialShapes.Arrow`. */
        val arrow: Shape
            @Composable get() = MaterialTheme.expressiveShapes.arrow

        /** Half circle cut from `MaterialShapes.SemiCircle`. */
        val semiCircle: Shape
            @Composable get() = MaterialTheme.expressiveShapes.semiCircle

        /** Pentagon cut from `MaterialShapes.Pentagon`. */
        val pentagon: Shape
            @Composable get() = MaterialTheme.expressiveShapes.pentagon

        /** Rounded triangle cut from `MaterialShapes.Triangle`. */
        val triangle: Shape
            @Composable get() = MaterialTheme.expressiveShapes.triangle

        /** Diamond cut from `MaterialShapes.Diamond`. */
        val diamond: Shape
            @Composable get() = MaterialTheme.expressiveShapes.diamond

        /** Ghost-ish silhouette cut from `MaterialShapes.Ghostish`. */
        val ghostish: Shape
            @Composable get() = MaterialTheme.expressiveShapes.ghostish

        /** Flower cut from `MaterialShapes.Flower`. */
        val flower: Shape
            @Composable get() = MaterialTheme.expressiveShapes.flower

        /** Sunny star cut from `MaterialShapes.Sunny`. */
        val sunny: Shape
            @Composable get() = MaterialTheme.expressiveShapes.sunny
    }
}

/**
 * The Material 3 shape scale (`extraSmall` … `extraLarge`) that [ExpressiveTheme] installs in
 * [MaterialTheme], with the expressive radii of the M3 Expressive spec (4 / 8 / 12 / 20 / 28 dp).
 *
 * Component-shaped slots (buttons, cards, chips) read this through `MaterialTheme.shapes`;
 * bespoke silhouettes read [MaterialTheme.expressiveShapes] or the accessors below.
 */
object AppShapes {

    /**
     * The Material 3 [Shapes] instance installed into [MaterialTheme] by [ExpressiveTheme].
     */
    val shapes: Shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

    /** Fully rounded pill; alias of [MaterialTheme.expressiveShapes].pill. */
    val pill: Shape
        @Composable get() = MaterialTheme.expressiveShapes.pill

    /** Elliptical oval cut from `MaterialShapes.Oval`. */
    val oval: Shape
        @Composable get() = MaterialTheme.expressiveShapes.oval

    /** Organic asymmetric cookie blob cut from `MaterialShapes.Cookie9Sided`. */
    val cookie: Shape
        @Composable get() = MaterialTheme.expressiveShapes.cookie

    /** Chunky superellipse-ish rounded square. */
    val squircle: Shape
        @Composable get() = MaterialTheme.expressiveShapes.squircle

    /** Squashed bun silhouette cut from `MaterialShapes.Bun`. */
    val bun: Shape
        @Composable get() = MaterialTheme.expressiveShapes.bun

    /** Cut-corner gem silhouette cut from `MaterialShapes.Gem`. */
    val gem: Shape
        @Composable get() = MaterialTheme.expressiveShapes.gem

    /** Perfect circle cut from `MaterialShapes.Circle`. */
    val circle: Shape
        @Composable get() = MaterialTheme.expressiveShapes.circle

    /** Heart silhouette cut from `MaterialShapes.Heart`. */
    val heart: Shape
        @Composable get() = MaterialTheme.expressiveShapes.heart

    /** Four-leaf clover cut from `MaterialShapes.Clover4Leaf`. */
    val clover: Shape
        @Composable get() = MaterialTheme.expressiveShapes.clover

    /** Arched slab cut from `MaterialShapes.Arch`. */
    val arch: Shape
        @Composable get() = MaterialTheme.expressiveShapes.arch

    /** Slanted square cut from `MaterialShapes.Slanted`. */
    val slanted: Shape
        @Composable get() = MaterialTheme.expressiveShapes.slanted

    /** Arrow silhouette cut from `MaterialShapes.Arrow`. */
    val arrow: Shape
        @Composable get() = MaterialTheme.expressiveShapes.arrow

    /** Half circle cut from `MaterialShapes.SemiCircle`. */
    val semiCircle: Shape
        @Composable get() = MaterialTheme.expressiveShapes.semiCircle

    /** Pentagon cut from `MaterialShapes.Pentagon`. */
    val pentagon: Shape
        @Composable get() = MaterialTheme.expressiveShapes.pentagon

    /** Rounded triangle cut from `MaterialShapes.Triangle`. */
    val triangle: Shape
        @Composable get() = MaterialTheme.expressiveShapes.triangle

    /** Diamond cut from `MaterialShapes.Diamond`. */
    val diamond: Shape
        @Composable get() = MaterialTheme.expressiveShapes.diamond

    /** Ghost-ish silhouette cut from `MaterialShapes.Ghostish`. */
    val ghostish: Shape
        @Composable get() = MaterialTheme.expressiveShapes.ghostish

    /** Flower cut from `MaterialShapes.Flower`. */
    val flower: Shape
        @Composable get() = MaterialTheme.expressiveShapes.flower

    /** Sunny star cut from `MaterialShapes.Sunny`. */
    val sunny: Shape
        @Composable get() = MaterialTheme.expressiveShapes.sunny
}

/**
 * Builds (and remembers) the [ExpressiveShapes] set for the current composition.
 *
 * `RoundedPolygon.toShape()` is a composable, so the MaterialShapes-backed silhouettes are
 * converted once per composition and cached in [remember]; the corner-based [ExpressiveShapes.pill]
 * and [ExpressiveShapes.squircle] are computed cheaply alongside them.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberExpressiveShapes(): ExpressiveShapes {
    val pill = RoundedCornerShape(percent = 50)
    val squircle = RoundedCornerShape(percent = 30)
    val oval = MaterialShapes.Oval.toShape()
    val cookie = MaterialShapes.Cookie9Sided.toShape()
    val bun = MaterialShapes.Bun.toShape()
    val gem = MaterialShapes.Gem.toShape()
    val circle = MaterialShapes.Circle.toShape()
    val heart = MaterialShapes.Heart.toShape()
    val clover = MaterialShapes.Clover4Leaf.toShape()
    val arch = MaterialShapes.Arch.toShape()
    val slanted = MaterialShapes.Slanted.toShape()
    val arrow = MaterialShapes.Arrow.toShape()
    val semiCircle = MaterialShapes.SemiCircle.toShape()
    val pentagon = MaterialShapes.Pentagon.toShape()
    val triangle = MaterialShapes.Triangle.toShape()
    val diamond = MaterialShapes.Diamond.toShape()
    val ghostish = MaterialShapes.Ghostish.toShape()
    val flower = MaterialShapes.Flower.toShape()
    val sunny = MaterialShapes.Sunny.toShape()
    // No keys: every input is a constant silhouette, so the set is built once and stays stable
    // for as long as this composition lives (downstream remember(shape) call sites rely on it).
    return remember {
        ExpressiveShapes(
            pill = pill,
            oval = oval,
            cookie = cookie,
            squircle = squircle,
            bun = bun,
            gem = gem,
            circle = circle,
            heart = heart,
            clover = clover,
            arch = arch,
            slanted = slanted,
            arrow = arrow,
            semiCircle = semiCircle,
            pentagon = pentagon,
            triangle = triangle,
            diamond = diamond,
            ghostish = ghostish,
            flower = flower,
            sunny = sunny,
        )
    }
}

/**
 * The [ExpressiveShapes] of the closest [ExpressiveTheme] ancestor: MaterialShapes-backed
 * silhouettes plus the pill/squircle corner shapes, ready for `Modifier.clip` and any `shape:`
 * parameter.
 *
 * Example: `shape = MaterialTheme.expressiveShapes.squircle`.
 */
val MaterialTheme.expressiveShapes: ExpressiveShapes
    @Composable get() = rememberExpressiveShapes()
