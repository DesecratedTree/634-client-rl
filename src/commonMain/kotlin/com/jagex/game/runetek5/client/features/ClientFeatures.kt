package com.jagex.game.runetek5.client.features

import maxTicksPerFrame
import com.jagex.game.runetek5.core.constants.MiniMenuAction
import com.jagex.game.runetek5.client.menu.MiniMenuEntry
import com.jagex.game.runetek5.entity.NPCEntityNode
import com.jagex.game.runetek5.entity.PathingEntity
import com.jagex.game.runetek5.core.datastruct.key.Deque
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.graphics.Toolkit
import com.jagex.game.runetek5.graphics.gl.GlToolkit
import com.jagex.game.runetek5.statics.OutPlayerIndexCount
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.client.event.keyboard.SimpleKeyboardMonitorConstants
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.NPCList
import com.jagex.game.runetek5.client.ClientState
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import awt.Graphics
import awt.event.MouseEvent
import awt.event.MouseWheelEvent
import io.Reader
import lang.jClass
import lang.javaReplaceAll
import lang.javaSplit
import lang.javaToLowerCase
import lang.javaTrim

/**
 * Switches for behaviour that is not part of the original client, read once from system properties.
 * Every switch is off by default, so a client started without them behaves as the 2010 jar did.
 *
 * This class is not part of the original client.
 */
object ClientFeatures {

	/** Ids and tile coordinates on right-click menus, and a coordinates overlay beside the fps text. */
	@JvmField
	val DEBUG: Boolean = lang.getBoolean("client.debug")

	/** A mouse-wheel zoom wider than the original field of view allows. */
	@JvmField
	val ZOOM: Boolean = lang.getBoolean("client.zoom")

	/**
	 * The key names `client.zoomKey` accepts, and the keyboard code each one holds down while the
	 * wheel zooms. `ctrl` is the default; add an entry here for another key.
	 */
	private val ZOOM_KEYS: Map<String, Int> = mapOf(
		"ctrl" to SimpleKeyboardMonitorConstants.KEY_CODE_CONTROL,
		"control" to SimpleKeyboardMonitorConstants.KEY_CODE_CONTROL,
		"alt" to SimpleKeyboardMonitorConstants.KEY_CODE_ALT,
		"shift" to SimpleKeyboardMonitorConstants.KEY_CODE_SHIFT
	)

	/**
	 * The key that must be held for the mouse wheel to zoom the camera: `ctrl` by default, or
	 * `alt`, `shift` or `none` (the wheel zooms without one); see ZOOM_KEYS for the names.
	 */
	@JvmField
	val ZOOM_KEY: Int = zoomKey(lang.getProperty("client.zoomKey"))

	/** How much further than the original the scene is drawn and fogged; 1 is the original distance. */
	@JvmField
	val RENDER_DISTANCE: Double = parseDouble(lang.getProperty("client.renderDistance"), 1.0)

	/** Command history, word editing and server autocomplete in the developer console. */
	@JvmField
	val CONSOLE: Boolean = lang.getBoolean("client.console")

	/**
	 * Holding Shift makes a left click on an inventory item drop it, where the item can be dropped.
	 * The console command `shiftdrop` turns it on and off while playing.
	 */
	@JvmField
	var shiftDrop: Boolean = lang.getBoolean("client.shiftDrop")

	/** The interface of the inventory tab. */
	private const val INVENTORY: Int = 149


	/**
	 * The server the client talks to: `void` (the default) for a Void server, or `jagex`
	 * for the protocol the 2010 client spoke. Void has no lobby, sends the area sound packets 5 and
	 * 66 in a longer form, and its scripts use three instructions and more client variables than
	 * the 2010 cache; with `jagex` none of that applies.
	 */
	@JvmField
	val SERVER: String = server(lang.getProperty("client.server"))

	/** Whether the client talks to a Void server. */
	@JvmField
	val VOID: Boolean = "void" == SERVER

	/** The size of each server packet, by opcode, where it differs from the client's own; -3 means none. */
	private val PACKET_SIZES: IntArray = packetSizes(lang.getProperty("client.packets"))

	/**
	 * Frames drawn per second, or 0 for the client's own pacing of one frame per game tick (50 per
	 * second). The game logic keeps its 50 ticks per second either way, so the frames between two ticks
	 * show the same game state: a higher rate lowers input latency and smooths the camera, but does not
	 * make movement smoother.
	 */
	@JvmField
	val FPS: Int = kotlin.math.max(0, lang.getInteger("client.fps", 0))

	/** When the next game tick is due, by System.nanoTime, once client.fps paces the frames. */
	private var nextTick: Long = 0L

	/** The game tick interval the frames are paced against, in nanoseconds. */
	private var tickInterval: Long = 0L

	/** The camera of the previous and the current game cycle: x, y, z, pitch, yaw; see blendCamera. */
	private val cameraBefore: IntArray = IntArray(5)
	private val cameraNow: IntArray = IntArray(5)
	private var cameraCycle: Int = Int.MIN_VALUE
	private var cameraMode: Int = Int.MIN_VALUE

	/** How many cycles the camera's recorded move spans: more than one when the frames are slower than the cycles. */
	private var cameraGap: Int = 1

	/** A move recorded over more cycles than this is a cut; the frames are far behind the game. */
	private const val MAX_GAP: Int = 10

	/** A camera move of more than this many units in one cycle is a cut, not a motion, and is not blended. */
	private const val CAMERA_CUT: Int = 1024
	private const val CAMERA_TURN_CUT: Int = 4096

	/**
	 * How many screen pixels each game pixel takes, for high resolution displays; 1 draws the game at
	 * its own size. The Java renderers scale: safe mode, which draws through Java, and OpenGL and
	 * DirectX, which draw the frame off screen at the game's size and copy it to the window scaled.
	 * The native software renderer reads each frame back and draws it scaled through Java. The native
	 * OpenGL renderer draws through native code only, so with it the game keeps its own size.
	 */
	@JvmField
	val SCALE: Double = kotlin.math.max(1.0, parseDouble(lang.getProperty("client.scale"), 1.0))

	/** A host to connect to instead of the one the client works out itself, or null. */
	@JvmField
	val ADDRESS: String? = lang.getProperty("client.address")

	/** A port to use for both connection attempts instead of the client's own, or 0. */
	@JvmField
	val PORT: Int = lang.getInteger("client.port", 0)

	/** How far one wheel notch zooms, in field of view units. */
	const val ZOOM_STEP: Int = 15

	/** The zoom added to the field of view; kept within bounds by the projection setup. */
	@JvmField
	var zoomOffset: Int = 0

	/** Whether the pointer was over the game view while the interfaces were processed this tick. */
	@JvmField
	var overGameScreen: Boolean = false

	/** The last wheel movement of this tick, kept apart because the console clears the shared one. */
	@JvmField
	var wheelDelta: Int = 0

	/** Whether the zoom key is held now; with `none` there is none and the wheel zooms without one. */
	@JvmStatic
	fun zoomKeyHeld(): Boolean {
		return ZOOM_KEY == 0 || KeyRecorder.keyRecorder!!.isPressed(ZOOM_KEY)
	}

	/** The code of the key `client.zoomKey` names, `ctrl` when it is absent or unknown. */
	private fun zoomKey(value: String?): Int {
		if (value == null) {
			return SimpleKeyboardMonitorConstants.KEY_CODE_CONTROL
		}
		val name: String = value.javaToLowerCase().javaTrim()
		if (name == "none") {
			return 0
		}
		val key: Int? = ZOOM_KEYS[name]
		if (key == null) {
			lang.printlnErr("client.zoomKey: unknown key " + value + ", using ctrl")
			return SimpleKeyboardMonitorConstants.KEY_CODE_CONTROL
		}
		return key
	}

	private fun server(value: String?): String {
		if (value == null || value.equals("void", ignoreCase = true)) {
			return "void"
		}
		if (value.equals("jagex", ignoreCase = true)) {
			return "jagex"
		}
		lang.printlnErr("client.server: unknown server " + value + ", using void")
		return "void"
	}

	/**
	 * Server packet sizes that override the client's: Void's two longer packets, then the entries of
	 * the properties file `client.packets` names, one per line as `<opcode>=<size>`, where
	 * -1 means a size byte and -2 a size short precede the packet.
	 */
	private fun packetSizes(file: String?): IntArray {
		val sizes: IntArray = IntArray(256)
		for (i in 0 until sizes.size) {
			sizes[i] = -3
		}
		if ("void" == SERVER) {
			sizes[5] = 8
			sizes[66] = 8
		}
		if (file != null) {
			readPacketSizes(file, sizes)
		}
		return sizes
	}

	/**
	 * How many game ticks of `interval` nanoseconds are due now, at most maxTicksPerFrame, as the client's own
	 * timer counts them; used when client.fps paces the frames instead of the ticks.
	 */
	@JvmStatic
	fun ticksDue(interval: Long): Int {
		val now: Long = lang.nanoTime()
		tickInterval = interval
		if (nextTick == 0L) {
			nextTick = now
		}
		var count: Int = 0
		// at most maxTicksPerFrame (PlatformLoop.kt): 10 on the JVM
		while (count < maxTicksPerFrame && now >= nextTick) {
			count++
			nextTick += interval
		}
		if (nextTick < now) {
			nextTick = now
		}
		return count
	}

	/** How far the current game cycle has run, 0 at its start and 1 at the next tick; 1 without client.fps. */
	@JvmStatic
	fun cycleFraction(): Float {
		if (FPS <= 0 || nextTick == 0L || tickInterval <= 0L) {
			return 1.0f
		}
		val fraction: Float = 1.0f - (nextTick - lang.nanoTime()).toFloat() / tickInterval.toFloat()
		return if (fraction < 0.0f) 0.0f else if (fraction > 1.0f) 1.0f else fraction
	}

	/**
	 * With client.fps, moves the camera to where it is between the previous game cycle and this one,
	 * by how far the cycle has run, so that a frame drawn between two ticks shows the camera on its
	 * way rather than where the last tick left it. Called once the frame's camera is worked out; the
	 * first frame of each cycle records that camera. A cut (a mode change, a jump of more than two
	 * tiles, a turn of more than a quarter) is shown at once, not slid into.
	 */
	@JvmStatic
	fun blendCamera(cycle: Int, mode: Int) {
		if (FPS <= 0) {
			return
		}
		val now: IntArray = intArrayOf(Camera.cameraPositionAbsoluteX, Camera.cameraPositionAbsoluteZ, Camera.cameraPositionAbsoluteY, Camera.cameraPitch, Camera.cameraYaw)
		if (cycle != cameraCycle) {
			val gap: Int = cycle - cameraCycle
			val cut: Boolean = cameraCycle == Int.MIN_VALUE || gap <= 0 || gap > MAX_GAP || mode != cameraMode ||
				kotlin.math.abs(now[0] - cameraNow[0]) > CAMERA_CUT * gap || kotlin.math.abs(now[1] - cameraNow[1]) > CAMERA_CUT * gap || kotlin.math.abs(now[2] - cameraNow[2]) > CAMERA_CUT * gap ||
				kotlin.math.abs(angleDelta(cameraNow[3], now[3])) > CAMERA_TURN_CUT * gap || kotlin.math.abs(angleDelta(cameraNow[4], now[4])) > CAMERA_TURN_CUT * gap
			lang.arraycopy(if (cut) now else cameraNow, 0, cameraBefore, 0, 5)
			lang.arraycopy(now, 0, cameraNow, 0, 5)
			cameraGap = if (cut) 1 else gap
			cameraCycle = cycle
			cameraMode = mode
		}
		val fraction: Float = gapFraction(cameraGap)
		Camera.cameraPositionAbsoluteX = lerp(cameraBefore[0], cameraNow[0], fraction)
		Camera.cameraPositionAbsoluteZ = lerp(cameraBefore[1], cameraNow[1], fraction)
		Camera.cameraPositionAbsoluteY = lerp(cameraBefore[2], cameraNow[2], fraction)
		Camera.cameraPitch = lerpAngle(cameraBefore[3], cameraNow[3], fraction)
		Camera.cameraYaw = lerpAngle(cameraBefore[4], cameraNow[4], fraction)
	}

	/** An entity move of more than this many units in one cycle is a teleport, not a walk, and is not blended. */
	private const val ENTITY_CUT: Int = 1024

	/**
	 * How far along a move recorded over the given number of cycles the frame is: the cycles before
	 * this one have passed in full, and this one by how far it has run. When the frames are slower
	 * than the cycles, a recorded move spans several cycles, and the frame shows it evenly.
	 */
	private fun gapFraction(gap: Int): Float {
		return if (gap <= 1) cycleFraction() else ((gap - 1).toFloat() + cycleFraction()) / gap.toFloat()
	}

	private var entityCycle: Int = Int.MIN_VALUE

	/**
	 * With client.fps, moves every player and NPC to where it is between the previous game cycle and
	 * this one, by how far the cycle has run, for the frame about to be drawn; restoreEntities puts them
	 * back. The first frame of a cycle records where the cycle left each entity. An entity that is
	 * new, changed level, or moved more than two tiles in a cycle is shown where it is.
	 */
	@JvmStatic
	fun blendEntities(cycle: Int) {
		if (FPS <= 0) {
			return
		}
		val newCycle: Boolean = cycle != entityCycle
		entityCycle = cycle
		val players: Int = OutPlayerIndexCount.NUM_OUT_PLAYER_INDICES
		val playerIndices: IntArray = PlayerUpdate.OUT_PLAYER_INDICES
		val npcs: Int = NPCList.NPC_UPDATE_INDEX
		val npcIds: IntArray = NPCList.NPC_UPDATE_INDICES
		for (i in 0 until players + npcs) {
			val entity: PathingEntity?
			if (i < players) {
				entity = PlayerUpdate.players[playerIndices[i]]
			} else {
				val node: NPCEntityNode? = NPCList.NPC_MAP.get(npcIds[i - players].toLong()) as NPCEntityNode?
				entity = if (node == null) null else node.npc
			}
			if (entity == null || entity.blended) {
				continue
			}
			val x: Int = entity.x
			val z: Int = entity.z
			val yaw: Int = entity.yaw.rotation
			if (newCycle) {
				val gap: Int = cycle - entity.blendCycle
				val cut: Boolean = gap <= 0 || gap > MAX_GAP || entity.cycleLevel != entity.level.toInt() ||
					kotlin.math.abs(x - entity.cycleX) > ENTITY_CUT * gap || kotlin.math.abs(z - entity.cycleZ) > ENTITY_CUT * gap
				entity.previousX = if (cut) x else entity.cycleX
				entity.previousZ = if (cut) z else entity.cycleZ
				entity.previousYaw = if (cut) yaw else entity.cycleYaw
				entity.cycleX = x
				entity.cycleZ = z
				entity.cycleYaw = yaw
				entity.blendGap = if (cut) 1 else gap
				entity.blendCycle = cycle
				entity.cycleLevel = entity.level.toInt()
			} else if (entity.blendCycle != cycle) {
				continue // appeared during the cycle: shown where it is until the next cycle records it
			}
			val fraction: Float = gapFraction(entity.blendGap)
			entity.realX = x
			entity.realZ = z
			entity.realYaw = yaw
			entity.x = lerp(entity.previousX, entity.cycleX, fraction)
			entity.z = lerp(entity.previousZ, entity.cycleZ, fraction)
			entity.yaw.rotation = lerpAngle(entity.previousYaw, entity.cycleYaw, fraction)
			entity.blended = true
		}
	}

	/** Puts every entity back where this cycle left it, once the frame is drawn. */
	@JvmStatic
	fun restoreEntities() {
		if (FPS <= 0) {
			return
		}
		val players: Int = OutPlayerIndexCount.NUM_OUT_PLAYER_INDICES
		val playerIndices: IntArray = PlayerUpdate.OUT_PLAYER_INDICES
		val npcs: Int = NPCList.NPC_UPDATE_INDEX
		val npcIds: IntArray = NPCList.NPC_UPDATE_INDICES
		for (i in 0 until players + npcs) {
			val entity: PathingEntity?
			if (i < players) {
				entity = PlayerUpdate.players[playerIndices[i]]
			} else {
				val node: NPCEntityNode? = NPCList.NPC_MAP.get(npcIds[i - players].toLong()) as NPCEntityNode?
				entity = if (node == null) null else node.npc
			}
			if (entity != null && entity.blended) {
				entity.x = entity.realX
				entity.z = entity.realZ
				entity.yaw.rotation = entity.realYaw
				entity.blended = false
			}
		}
	}

	/** How many steps a game cycle is split into for the animations, with client.fps. */
	const val ANIMATION_STEPS: Int = 16

	/**
	 * With client.fps, how far the current game cycle has run, in steps out of ANIMATION_STEPS; 0
	 * otherwise. An animation blends between its keyframes by cycles elapsed over the keyframe's
	 * length, so a frame drawn between two cycles adds this step to the cycles elapsed, in finer
	 * units, and the animation moves on between cycles too.
	 */
	@JvmStatic
	fun animationStep(): Int {
		if (FPS <= 0) {
			return 0
		}
		val step: Int = (cycleFraction() * ANIMATION_STEPS.toFloat()).toInt()
		return if (step < 0) 0 else if (step >= ANIMATION_STEPS) ANIMATION_STEPS - 1 else step
	}

	/** Puts the camera back where this cycle left it, once the frame is drawn. */
	@JvmStatic
	fun restoreCamera() {
		if (FPS <= 0 || cameraCycle == Int.MIN_VALUE) {
			return
		}
		Camera.cameraPositionAbsoluteX = cameraNow[0]
		Camera.cameraPositionAbsoluteZ = cameraNow[1]
		Camera.cameraPositionAbsoluteY = cameraNow[2]
		Camera.cameraPitch = cameraNow[3]
		Camera.cameraYaw = cameraNow[4]
	}

	private fun lerp(from: Int, to: Int, fraction: Float): Int {
		return from + lang.round((to - from).toFloat() * fraction)
	}

	/** The signed shortest turn from one angle to another, in the client's 16384 units per turn. */
	private fun angleDelta(from: Int, to: Int): Int {
		val delta: Int = (to - from) and 0x3FFF
		return if (delta > 8192) delta - 16384 else delta
	}

	private fun lerpAngle(from: Int, to: Int, fraction: Float): Int {
		return (from + lang.round(angleDelta(from, to).toFloat() * fraction)) and 0x3FFF
	}

	/**
	 * The OpenGL anti-aliasing pipeline: with any of client.msaa, client.smaa or client.renderScale
	 * set, the OpenGL renderer draws the 3D view into its own framebuffer, multisampled by MSAA
	 * samples (0, 2, 4 or 8; -1 follows the game's own anti-aliasing setting), and copies the result
	 * into the frame, so the interface is never multisampled. The window's own surface then has no
	 * samples. The settings may change while the client runs (the console commands msaa, smaa and
	 * renderscale); the renderer rebuilds its framebuffer on the next frame.
	 */
	@JvmField
	val AA_PIPELINE: Boolean = lang.getProperty("client.msaa") != null || lang.getBoolean("client.smaa") || lang.getProperty("client.renderScale") != null
	@JvmField
	var msaa: Int = parseSamples(lang.getProperty("client.msaa"))
	@JvmField
	var smaa: Boolean = lang.getBoolean("client.smaa")
	@JvmField
	var renderScale: Double = parseRenderScale(lang.getProperty("client.renderScale"))
	/** Alpha-to-coverage for cut-out textures while the 3D view is multisampled; client.a2c=false turns it off. */
	@JvmField
	var a2c: Boolean = "false" != lang.getProperty("client.a2c")
	/** Set by the OpenGL renderer when the driver has no framebuffer objects; the pipeline then stays off. */
	@JvmField
	var aaPipelineUnavailable: Boolean = false

	/** The samples the 3D view is drawn with: client.msaa, or the game's own setting when it is -1. */
	@JvmStatic
	fun sceneSamples(settingSamples: Int): Int {
		return if (msaa < 0) settingSamples else msaa
	}

	private fun parseSamples(value: String?): Int {
		if (value == null) {
			return -1
		}
		try {
			val samples: Int = lang.parseInt(value.javaTrim())
			if (samples == 0 || samples == 2 || samples == 4 || samples == 8) {
				return samples
			}
		} catch (ex: NumberFormatException) {
		}
		lang.printlnErr("client.msaa: " + value + " is not 0, 2, 4 or 8; following the game's setting")
		return -1
	}

	private fun parseRenderScale(value: String?): Double {
		val scale: Double = parseDouble(value, 1.0)
		if (scale == 1.0 || scale == 1.5 || scale == 2.0) {
			return scale
		}
		lang.printlnErr("client.renderScale: " + value + " is not 1.0, 1.5 or 2.0; drawing at 1.0")
		return 1.0
	}

	/**
	 * The console commands msaa <0|2|4|8>, smaa and renderscale <1|1.5|2>: they change the pipeline's
	 * settings and drop the renderer's framebuffers, which are made again on the next frame. Returns
	 * the line to print, or null when the command is not one of them. The pipeline itself has to be
	 * on from the start, since the window's samples are chosen when the renderer is built.
	 */
	@JvmStatic
	fun antialiasingCommand(command: String?, toolkit: Toolkit?): String? {
		val words: Array<String> = command!!.javaTrim().javaSplit(" +")
		val name: String = words[0].javaToLowerCase()
		if (name != "msaa" && name != "smaa" && name != "renderscale") {
			return null
		}
		if (!AA_PIPELINE) {
			return "Start the client with --msaa, --smaa or --render-scale to use " + name + "."
		}
		if (name == "smaa") {
			smaa = !smaa
		} else if (words.size < 2) {
			return if (name == "msaa") "MSAA: " + msaa + " samples (msaa 0, 2, 4 or 8; -1 follows the game's setting)." else "Render scale: " + renderScale + " (renderscale 1, 1.5 or 2)."
		} else if (name == "msaa") {
			val samples: Int = parseSamples(words[1])
			if (samples < 0 && words[1] != "-1") {
				return "msaa takes 0, 2, 4 or 8."
			}
			msaa = samples
		} else {
			val scale: Double = parseDouble(words[1], 0.0)
			if (scale != 1.0 && scale != 1.5 && scale != 2.0) {
				return "renderscale takes 1, 1.5 or 2."
			}
			renderScale = scale
		}
		if (toolkit is GlToolkit) {
			toolkit.dropSceneTarget()
		}
		return "MSAA " + msaa + ", SMAA " + (if (smaa) "on" else "off") + ", render scale " + renderScale + "."
	}

	/** Starts the 3D view of a frame in the renderer's own framebuffer, when the pipeline is on and the renderer is OpenGL. */
	@JvmStatic
	fun beginScene(toolkit: Toolkit?, x: Int, y: Int, width: Int, height: Int) {
		if (AA_PIPELINE && toolkit is GlToolkit) {
			toolkit.beginScene(x, y, width, height)
		}
	}

	/** Ends the 3D view: the renderer resolves its framebuffer and draws it into the frame. */
	@JvmStatic
	fun endScene(toolkit: Toolkit?) {
		if (AA_PIPELINE && toolkit is GlToolkit) {
			toolkit.endScene()
		}
	}

	/** Set by a renderer that cannot scale after all (no framebuffer support), with a message printed once. */
	@JvmField
	var scaleUnavailable: Boolean = false

	/** Whether the window is scaled now: client.scale is above 1 and a renderer that can scale (or none yet) draws. */
	@JvmStatic
	fun scaled(): Boolean {
		if (SCALE <= 1.0 || scaleUnavailable) {
			return false
		}
		val toolkit: Any? = ClientState.activeToolkit
		if (toolkit == null) {
			return true
		}
		val name: String = toolkit.jClass.getName()
		return name == "JavaToolkit" || name == "oa" || name == "com.jagex.game.runetek5.graphics.gl.GlToolkit" || name == "com.jagex.game.runetek5.toolkit.D3DToolkit"
	}

	/** Whether the scale is a whole number, so that a game pixel maps to a block of screen pixels. */
	@JvmStatic
	fun wholeScale(): Boolean {
		return SCALE == kotlin.math.floor(SCALE)
	}

	/** A game size in screen pixels. */
	@JvmStatic
	fun scale(value: Int): Int {
		return if (scaled()) lang.round(value.toDouble() * SCALE).toInt() else value
	}

	/** A screen size in game pixels. */
	@JvmStatic
	fun unscale(value: Int): Int {
		return if (scaled()) (value.toDouble() / SCALE).toInt() else value
	}

	/** The graphics of the scaled window, drawing game pixels at the scale; unchanged when not scaled. */
	@JvmStatic
	fun scaleGraphics(graphics: Graphics?): Graphics? {
		if (!scaled()) {
			return graphics
		}
		return scaleGraphics2D(graphics, SCALE, SCALE == kotlin.math.floor(SCALE))
	}

	/** A mouse event of the scaled window, at the game pixel it points at. */
	@JvmStatic
	fun unscaleEvent(event: MouseEvent?): MouseEvent? {
		if (!scaled()) {
			return event
		}
		val x: Int = unscale(event!!.getX())
		val y: Int = unscale(event.getY())
		if (event is MouseWheelEvent) {
			val wheel: MouseWheelEvent = event
			return MouseWheelEvent(wheel.getComponent(), wheel.getID(), wheel.getWhen(), wheel.getModifiersEx(), x, y,
				wheel.getXOnScreen(), wheel.getYOnScreen(), wheel.getClickCount(), wheel.isPopupTrigger(), wheel.getScrollType(),
				wheel.getScrollAmount(), wheel.getWheelRotation(), wheel.getPreciseWheelRotation())
		}
		return MouseEvent(event.getComponent(), event.getID(), event.getWhen(), event.getModifiersEx(), x, y,
			event.getXOnScreen(), event.getYOnScreen(), event.getClickCount(), event.isPopupTrigger(), event.getButton())
	}

	/** The size of server packet `opcode`: an override if one is given, else the client's own. */
	@JvmStatic
	fun serverPacketSize(opcode: Int, size: Int): Int {
		return if (opcode >= 0 && opcode < PACKET_SIZES.size && PACKET_SIZES[opcode] != -3) PACKET_SIZES[opcode] else size
	}

	private fun parseDouble(value: String?, fallback: Double): Double {
		if (value == null) {
			return fallback
		}
		try {
			return lang.parseDouble(value)
		} catch (ex: NumberFormatException) {
			return fallback
		}
	}

	/**
	 * The inventory item's Drop entry among the menu entries, or null when the pointer is not over an
	 * item that can be dropped.
	 */
	@JvmStatic
	fun shiftDropEntry(entries: Deque?): MiniMenuEntry? {
		val drop: String? = LocalizedText.aClass351_10.get(ClientState.language)
		var node: Node? = entries!!.sentinel.previous
		while (node != null && node !== entries.sentinel) {
			if (node !is MiniMenuEntry) {
				node = node.previous
				continue
			}
			val entry: MiniMenuEntry = node
			if ((entry.clickType == MiniMenuAction.IF_BUTTONX1 || entry.clickType == MiniMenuAction.IF_BUTTONX2) && entry.param1 ushr 16 == INVENTORY && entry.opBase != null && drop!!.equals((entry.opBase.javaReplaceAll("<[^>]*>", "")).javaTrim(), ignoreCase = true)) {
				return entry
			}
			node = node.previous
		}
		return null
	}

	/** The id suffix for a menu entry, or an empty string when debug info is off. */
	@JvmStatic
	fun debugSuffix(id: Int): String {
		return if (DEBUG) " <col=00ff00>(</col>" + id + "<col=00ff00>)</col>" else ""
	}

	/** The id and component suffix for an interface menu entry, or an empty string. */
	@JvmStatic
	fun debugSuffix(id: Int, component: Int): String {
		return if (DEBUG) " <col=00ff00>(</col>" + id + " - " + component + "<col=00ff00>)</col>" else ""
	}

	/** The id and tile suffix for a world menu entry, or an empty string. */
	@JvmStatic
	fun debugSuffix(id: Int, x: Int, y: Int, level: Int): String {
		return if (DEBUG) " <col=00ff00>(</col>" + id + " - " + x + ", " + y + ", " + level + "<col=00ff00>)</col>" else ""
	}
}
