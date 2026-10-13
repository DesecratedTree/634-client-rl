package com.jagex.game.runetek5.client.features

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * Not part of the original client: with `client.smoothZoom` (the HDOS-style smooth zoom) one wheel
 * movement sets a target zoom and the field of view eases toward it over the next few game ticks,
 * rather than the step being applied at once. Off by default, where the wheel is applied
 * immediately, exactly as before. The console command `smoothzoom` turns it on and off while playing.
 *
 * The eased value is ClientFeatures.zoomOffset, which the projection setup widens and holds to its
 * range each frame; when it refuses a value (the zoom is at an end of the range) the projection
 * reports that through [clampTarget], and the target follows it, so scrolling back reacts at once
 * instead of first undoing zoom that was never shown.
 */
object SmoothZoom {

	@JvmField
	var enabled: Boolean = lang.getBoolean("client.smoothZoom")

	/** The zoom offset the field of view is easing toward, in ClientFeatures.zoomOffset's units. */
	private var target: Int = 0

	/** How much of the distance to the target one tick closes; it nearly settles in about six. */
	private const val EASE_NUMERATOR: Int = 3
	private const val EASE_DENOMINATOR: Int = 8

	/** The offset is put on the target within this many units of it, instead of easing forever. */
	private const val SNAP: Int = 2

	/** One wheel movement of `delta` notches: straight to the zoom at once, or kept to ease toward. */
	@JvmStatic
	fun wheel(delta: Int) {
		if (enabled) {
			target -= delta * ClientFeatures.ZOOM_STEP
		} else {
			ClientFeatures.zoomOffset -= delta * ClientFeatures.ZOOM_STEP
		}
	}

	/** Eases the zoom offset toward the wheel's target; nothing without smooth zoom. */
	@JvmStatic
	fun tick() {
		if (!enabled) {
			return
		}
		val difference: Int = target - ClientFeatures.zoomOffset
		if (difference == 0) {
			return
		}
		if (difference > -SNAP && difference < SNAP) {
			ClientFeatures.zoomOffset = target
			return
		}
		var step: Int = difference * EASE_NUMERATOR / EASE_DENOMINATOR
		if (step == 0) {
			step = if (difference > 0) 1 else -1
		}
		ClientFeatures.zoomOffset += step
	}

	/**
	 * The zoom offset the projection accepted once it held the wanted one to its range; when it
	 * differs from what was wanted, the wheel's target follows it there so the ease does not run on
	 * past what can be drawn.
	 */
	@JvmStatic
	fun clampTarget(offset: Int) {
		if (enabled) {
			target = offset
		}
	}

	/** Turns smooth zoom on or off, dropping any pending ease so the next wheel starts from here. */
	@JvmStatic
	fun toggle() {
		enabled = !enabled
		reset()
	}

	/** Keeps the target where the zoom is now, so a later change does not resume an old movement. */
	@JvmStatic
	fun reset() {
		target = ClientFeatures.zoomOffset
	}
}
