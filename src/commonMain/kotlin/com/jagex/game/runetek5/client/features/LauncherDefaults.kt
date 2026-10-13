package com.jagex.game.runetek5.client.features

/**
 * Not part of the original client: the launcher's defaults for the extras, shared by the desktop
 * launcher (RuneTek5) and the browser page (main.js.kt), so that both start the client the same way.
 * Each is kept only when nothing set it before (a command-line option, a URL switch, the page's
 * configuration). ./gradlew runClient does not use them: there every extra is off unless passed.
 */
object LauncherDefaults {

	/**
	 * Sets every default not already set. screenHeight is the height of the screen the client opens
	 * on, in the desktop's (or the browser's) own units, or 0 when there is no screen.
	 */
	fun apply(screenHeight: Int) {
		setDefault("client.fps", "180")
		setDefault("client.renderDistance", "2")
		setDefault("client.zoom", "true")
		setDefault("client.console", "true")
		setDefault("client.shiftDrop", "true")
		setDefault("client.scale", "1")
		setDefault("client.smoothzoom", "1")
	}

	/**
	 * The scale for a screen this high: 2 at 2160 lines (4K) or more, 1.5 from 1080 lines (1080p,
	 * 1440p), otherwise 1. The height is in the system's own units, so on a display it already
	 * enlarges (Windows at 150%, a browser zoomed in) it reads smaller and the two scales do not stack.
	 */
	fun scaleFor(screenHeight: Int): String {
		if (screenHeight >= 2160) {
			return "2.0"
		}
		if (screenHeight >= 1080) {
			return "1.5"
		}
		return "1.0"
	}

	private fun setDefault(name: String, value: String) {
		if (lang.getProperty(name) == null) {
			lang.setProperty(name, value)
		}
	}
}
