package com.jagex.game.runetek5.client.features

import com.jagex.game.runetek5.MapRegion
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * Not part of the original client: while the fog key (`alt` by default; client.fogKey) is held,
 * the mouse wheel moves the distance fog in and out, HDOS style. The movement is added to the
 * environment's own fog depth (EnvironmentManager.applyFogSettings), so the fog keeps the colour
 * and shape of the area it is in and stays where it is put until the wheel moves it again.
 * `client.fogControl` turns the whole thing on and off (on in the launcher); the console command
 * `fogcontrol` toggles it while playing.
 *
 * The movement is held to where the game's own fog ends: the fade `(depth + 256) << 2` reaches at
 * most to the far plane (MapRegion.viewRangeEnd), so the fog is never pushed past it or behind the
 * camera, and never pulls the area's own fog back.
 *
 * The wheel only moves the fog while the game's own Fog setting is on: with it off no fog is drawn,
 * so there is nothing to move.
 */
object FogControl {

	@JvmField
	var enabled: Boolean = lang.getBoolean("client.fogControl")

	/** How far one wheel notch moves the fog, in the environment's fog-depth units. */
	const val STEP: Int = 256

	/** The movement added to the environment's fog depth, moved by the wheel. */
	@JvmField
	var offset: Int = 0

	/**
	 * One wheel movement of `delta` notches. The sign follows the zoom's, so the direction that
	 * widens the view (pushing the fog out, clearer) is the one that zooms out, and the other pulls
	 * the fog in (foggier). The movement is held to the fog end so it cannot run on past it.
	 */
	@JvmStatic
	fun wheel(delta: Int) {
		offset += delta * STEP
		val end: Int = fogEndDepth()
		if (end > 0) {
			if (offset < -end) {
				offset = -end
			} else if (offset > end) {
				offset = end
			}
		}
	}

	/**
	 * The environment's fog depth with the control's movement, held to between none and the depth
	 * whose fade reaches where the game's own fog ends; an area fog deeper than that is left as it
	 * is, so the clamp only ever moves the extra fog it was added to.
	 */
	@JvmStatic
	fun depth(depth: Int): Int {
		if (!enabled) {
			return depth
		}
		var moved: Int = depth + offset
		if (moved < 0) {
			moved = 0
		}
		val end: Int = fogEndDepth()
		val cap: Int = if (depth > end) depth else end
		return if (moved > cap) cap else moved
	}

	/** The fog depth whose fade `(depth + 256) << 2` reaches the far plane, where the fog ends. */
	private fun fogEndDepth(): Int {
		return (MapRegion.viewRangeEnd shr 2) - 256
	}

	/** Turns the fog control on or off, leaving the fog where it is. */
	@JvmStatic
	fun toggle() {
		enabled = !enabled
	}
}
