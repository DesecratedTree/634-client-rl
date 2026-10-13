package com.jagex.game.runetek5.toolkit

import org.openrs2.deob.annotation.OriginalMember
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.features.ClientFeatures
import com.jagex.game.runetek5.client.features.SmoothZoom
import com.jagex.game.runetek5.client.input.camera.Camera
import org.openrs2.deob.annotation.OriginalArg
import org.openrs2.deob.annotation.Pc
import com.jagex.game.runetek5.client.ClientState
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

object AbstractToolkit {
	@OriginalMember(owner = "client!uf", name = "P", descriptor = "[Z")
	@JvmField
	val ACTIVE_RENDER_UNITS: BooleanArray = BooleanArray(8)

	@OriginalMember(owner = "client!lk", name = "a", descriptor = "(IIIZBI)V")
	@JvmStatic
	fun calculateGameArea(@OriginalArg(0) x: Int, @OriginalArg(1) y: Int, @OriginalArg(2) width: Int, @OriginalArg(3) drawBorders: Boolean, @OriginalArg(5) height: Int) {
		var x: Int = x
		var y: Int = y
		var width: Int = width
		var height: Int = height
		if (width < 1) {
			width = 1
		}
		if (height < 1) {
			height = 1
		}
		@Pc(21) var heightFactor: Int = height - 334
		if (heightFactor < 0) {
			heightFactor = 0
		} else if (heightFactor > 100) {
			heightFactor = 100
		}
		@Pc(45) var fov: Int = (Camera.viewportFovMax.toInt() - Camera.viewportFovMin.toInt()) * heightFactor / 100 + Camera.viewportFovMin.toInt()
		if (ClientFeatures.ZOOM) {
			// Not part of the original client: the zoom offset widens the field of view to between an eighth of the
			// narrowest (and no less than 50) and eight times the widest, and is held to that range.
			var min: Int = kotlin.math.max(Camera.viewportFovMin.toInt() / 8, 50)
			var max: Int = Camera.viewportFovMax.toInt() * 8
			if (min > max) {
				val swap: Int = min
				min = max
				max = swap
			}
			val wanted: Int = fov + ClientFeatures.zoomOffset
			val zoomed: Int = kotlin.math.min(kotlin.math.max(wanted, min), max)
			if (zoomed != wanted) {
				// Not part of the original client: the zoom is at an end of its range, so smooth zoom's
				// target follows it there (SmoothZoom); nothing when smooth zoom is off.
				SmoothZoom.clampTarget(zoomed - fov)
			}
			ClientFeatures.zoomOffset = zoomed - fov
			fov = zoomed
		}
		if (Camera.fovScaleMin.toInt() > fov) {
			fov = Camera.fovScaleMin.toInt()
		} else if (Camera.fovScaleMax.toInt() < fov) {
			fov = Camera.fovScaleMax.toInt()
		}
		@Pc(71) val aspectFov: Int = fov * 512 * height / (width * 334)
		@Pc(112) var fittedSize: Int
		@Pc(119) var borderSize: Int
		@Pc(83) var aspectLimit: Short
		if (aspectFov < Camera.aspectFovMin.toInt()) {
			aspectLimit = Camera.aspectFovMin
			fov = aspectLimit.toInt() * width * 334 / (height * 512)
			if (fov > Camera.fovScaleMax.toInt()) {
				fov = Camera.fovScaleMax.toInt()
				fittedSize = fov * height * 512 / (aspectLimit.toInt() * 334)
				borderSize = (width - fittedSize) / 2
				if (drawBorders) {
					ClientState.activeToolkit!!.la()
					ClientState.activeToolkit!!.fillRect(borderSize, x, y, height, -16777216)
					ClientState.activeToolkit!!.fillRect(borderSize, x + width - borderSize, y, height, -16777216)
				}
				x += borderSize
				width -= borderSize * 2
			}
		} else if (Camera.aspectFovMax.toInt() < aspectFov) {
			aspectLimit = Camera.aspectFovMax
			fov = width * aspectLimit.toInt() * 334 / (height * 512)
			if (Camera.fovScaleMin.toInt() > fov) {
				fov = Camera.fovScaleMin.toInt()
				fittedSize = aspectLimit.toInt() * width * 334 / (fov * 512)
				borderSize = (height - fittedSize) / 2
				if (drawBorders) {
					ClientState.activeToolkit!!.la()
					ClientState.activeToolkit!!.fillRect(width, x, y, borderSize, -16777216)
					ClientState.activeToolkit!!.fillRect(width, x, height + y - borderSize, borderSize, -16777216)
				}
				y += borderSize
				height -= borderSize * 2
			}
		}
		Camera.viewportScaledFov = height * fov / 334
		Camera.gameAreaY = y
		Camera.gameAreaHeight = height.toShort().toInt()
		GameShell.gameAreaWidth = width.toShort().toInt()
		Camera.gameAreaX = x
	}
}
