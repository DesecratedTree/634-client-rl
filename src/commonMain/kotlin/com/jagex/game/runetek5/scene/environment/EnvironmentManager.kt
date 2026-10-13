package com.jagex.game.runetek5.scene.environment

import org.openrs2.deob.annotation.OriginalMember
import org.openrs2.deob.annotation.OriginalArg
import org.openrs2.deob.annotation.Pc
import com.jagex.game.runetek5.EnvironmentSampler
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.MainLogicManager
import com.jagex.game.runetek5.MapRegion
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.entity.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.Toolkit
import com.jagex.game.runetek5.EnvironmentSamplerCacheReset
import com.jagex.game.runetek5.client.ClientState
import com.jagex.game.runetek5.client.features.FogControl
import com.jagex.game.runetek5.SunPositionZ
import com.jagex.game.runetek5.entity.StaticLocation
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.utils.Utils
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

object EnvironmentManager {
	@OriginalMember(owner = "client!lb", name = "a", descriptor = "[I")
	@JvmField
	var cubeTextureIds: IntArray? = null

	@OriginalMember(owner = "client!oh", name = "a", descriptor = "(IIII)I")
	@JvmStatic
	fun rotatedX(@OriginalArg(0) z: Int, @OriginalArg(1) x: Int, @OriginalArg(3) rotation: Int): Int {
		@Pc(3) val rotationMasked: Int = rotation and 0x3
		if (rotationMasked == 0) {
			return x
		} else if (rotationMasked == 1) {
			return z
		} else if (rotationMasked == 2) {
			return 4095 - x
		} else {
			return 4095 - z
		}
	}

	@OriginalMember(owner = "client!uha", name = "a", descriptor = "([BB)V")
	@JvmStatic
	fun decodeMapDefaults(@OriginalArg(0) src: ByteArray?) {
		@Pc(26) val packet: Packet = Packet(src)
		while (true) {
			while (true) {
				while (true) {
					@Pc(30) val opcode: Int = packet.readUByte()
					if (opcode == 0) {
						return
					}
					if (opcode == 1) {
						EnvironmentManager.cubeTextureIds = IntArray(6)
						@Pc(124) val textureIds: IntArray = EnvironmentManager.cubeTextureIds!!
						textureIds[0] = packet.readUShort()
						textureIds[1] = packet.readUShort()
						textureIds[2] = packet.readUShort()
						textureIds[3] = packet.readUShort()
						textureIds[4] = packet.readUShort()
						textureIds[5] = packet.readUShort()
					} else {
						@Pc(53) var i: Int
						@Pc(48) var count: Int
						if (opcode == 4) {
							count = packet.readUByte()
							PlayerEntity.defaultTitleEnumIds = IntArray(count)
							i = 0
							while (i < count) {
								PlayerEntity.defaultTitleEnumIds!![i] = packet.readUShort()
								if (PlayerEntity.defaultTitleEnumIds!![i] == 65535) {
									PlayerEntity.defaultTitleEnumIds!![i] = -1
								}
								i++
							}
						} else if (opcode == 5) {
							count = packet.readUByte()
							PlayerEntity.femaleTitleEnumIds = IntArray(count)
							i = 0
							while (i < count) {
								PlayerEntity.femaleTitleEnumIds!![i] = packet.readUShort()
								if (PlayerEntity.femaleTitleEnumIds!![i] == 65535) {
									PlayerEntity.femaleTitleEnumIds!![i] = -1
								}
								i++
							}
						}
					}
				}
			}
		}
	}

	@OriginalMember(owner = "client!sj", name = "a", descriptor = "(IIIIIII)Lclient!wh;")
	@JvmStatic
	fun getCubeTexture(@OriginalArg(0) materialPositiveZ: Int, @OriginalArg(1) materialNegativeY: Int, @OriginalArg(2) materialPositiveX: Int, @OriginalArg(3) materialNegativeZ: Int, @OriginalArg(4) materialNegativeX: Int, @OriginalArg(6) materialPositiveY: Int): EnvironmentSampler? {
		@Pc(33) val key: Long = (materialNegativeZ.toLong() * 76724863L) xor (materialPositiveZ.toLong() * 32147369L) xor (materialNegativeY.toLong() * 986053L) xor (materialPositiveX.toLong() * 67481L) xor (materialNegativeX.toLong() * 97549L) xor (materialPositiveY.toLong() * 475427L)
		@Pc(39) var environmentSampler: EnvironmentSampler? = EnvironmentSampler.aClass169_27.get(key) as EnvironmentSampler?
		if (environmentSampler == null) {
			environmentSampler = EnvironmentManager.toolkit!!.createCubeTexture(materialPositiveX, materialNegativeX, materialPositiveY, materialNegativeY, materialPositiveZ, materialNegativeZ)
			EnvironmentSampler.aClass169_27.put(environmentSampler, key)
			return environmentSampler
		} else {
			return environmentSampler
		}
	}

	@OriginalMember(owner = "client!wga", name = "L", descriptor = "(I)V")
	@JvmStatic
	fun resetChunkEnvironments() {
		if (EnvironmentManager.aClass41ArrayArray1 == null) {
			return
		}
		@Pc(9) var i: Int = 0
		while (i < EnvironmentManager.aClass41ArrayArray1!!.size) {
			@Pc(13) var j: Int = 0
			while (j < EnvironmentManager.aClass41ArrayArray1!![i]!!.size) {
				EnvironmentManager.aClass41ArrayArray1!![i]!![j] = EnvironmentManager.aClass41_2
				j++
			}
			i++
		}
	}

	@OriginalMember(owner = "client!td", name = "H", descriptor = "Lclient!wh;")
	@JvmField
	var aClass78_4: EnvironmentSampler? = null

	@OriginalMember(owner = "client!uh", name = "m", descriptor = "[[Lclient!bp;")
	@JvmField
	var aClass41ArrayArray1: Array<Array<Environment?>?>? = null

	@OriginalMember(owner = "client!wv", name = "a", descriptor = "(IIII)I")
	@JvmStatic
	fun rotatedZ(@OriginalArg(1) rotation: Int, @OriginalArg(2) z: Int, @OriginalArg(3) x: Int): Int {
		@Pc(12) val orientation: Int = rotation and 0x3
		if (orientation == 0) {
			return z
		} else if (orientation == 1) {
			return 4095 - x
		} else if (orientation == 2) {
			return 4095 - z
		} else {
			return x
		}
	}

	@OriginalMember(owner = "client!pp", name = "w", descriptor = "Lclient!bp;")
	@JvmField
	var environment: Environment? = null

	@OriginalMember(owner = "client!kq", name = "P", descriptor = "Z")
	@JvmField
	var directEnvironmentTransition: Boolean = false

	@OriginalMember(owner = "client!ks", name = "a", descriptor = "(Z)V")
	@JvmStatic
	fun requestDirectEnvironmentTransition() {
		EnvironmentManager.directEnvironmentTransition = true
	}

	@OriginalMember(owner = "client!vfa", name = "a", descriptor = "(IIIB)V")
	@JvmStatic
	fun chunkEnvironmentUpdate(@OriginalArg(0) fadeMillis: Int, @OriginalArg(1) zoneX: Int, @OriginalArg(2) zoneY: Int) {
		@Pc(9) val environment: Environment? = EnvironmentManager.aClass41ArrayArray1!![zoneX]!![zoneY]
		EnvironmentManager.startEnvironmentTransition(fadeMillis, if (environment == null) EnvironmentManager.aClass41_2 else environment)
	}

	@OriginalMember(owner = "client!wi", name = "f", descriptor = "(I)V")
	@JvmStatic
	fun transitionSceneEnvironment() {
		if (LoginManager.lastDisallowResult < 0) {
			return
		}
		@Pc(13) val now: Long = Utils.currentTimeMillis()
		LoginManager.lastDisallowResult = (LoginManager.lastDisallowResult.toLong() + EnvironmentManager.lastEnvironmentUpdate - now).toInt()
		if (LoginManager.lastDisallowResult > 0) {
			@Pc(33) val previousWeight: Int = (LoginManager.lastDisallowResult shl 8) / EnvironmentManager.totalTransitionDuration
			@Pc(38) val currentWeight: Int = 255 - previousWeight
			@Pc(43) val previousFactor: Float = previousWeight.toFloat() / 255.0f
			@Pc(47) val currentFactor: Float = 1.0f - previousFactor
			EnvironmentManager.currentSunColour = (((currentWeight * (EnvironmentManager.environment!!.sunColour and 0xFF00FF) + (EnvironmentManager.previousSunColour and 0xFF00FF) * previousWeight) and 0xFF00FF00.toInt()) + ((previousWeight * (EnvironmentManager.previousSunColour and 0xFF00) + (EnvironmentManager.environment!!.sunColour and 0xFF00) * currentWeight) and 0xFF0000)) ushr 8
			EnvironmentManager.currentWhitePoint = EnvironmentManager.previousWhitePoint + currentFactor * (EnvironmentManager.environment!!.whitePoint - EnvironmentManager.previousWhitePoint)
			EnvironmentManager.currentBloom = (EnvironmentManager.environment!!.bloom - EnvironmentManager.previousBloom) * currentFactor + EnvironmentManager.previousBloom
			EnvironmentManager.fogDepth = (previousWeight * EnvironmentManager.previousFogDepth + EnvironmentManager.environment!!.fogDepth * currentWeight) shr 8
			EnvironmentManager.currentSunAmbient = EnvironmentManager.previousSunAmbient + (EnvironmentManager.environment!!.sunAmbient - EnvironmentManager.previousSunAmbient) * currentFactor
			EnvironmentManager.currentSunLight = EnvironmentManager.previousSunLight + currentFactor * (EnvironmentManager.environment!!.sunLight - EnvironmentManager.previousSunLight)
			EnvironmentManager.fogColour = (((previousWeight * (EnvironmentManager.previousFogColour and 0xFF00) + (EnvironmentManager.environment!!.fogColour and 0xFF00) * currentWeight) and 0xFF0000) + ((previousWeight * (EnvironmentManager.previousFogColour and 0xFF00FF) + (currentWeight * (EnvironmentManager.environment!!.fogColour and 0xFF00FF))) and 0xFF00FF00.toInt())) ushr 8
			EnvironmentManager.currentSunBacklight = EnvironmentManager.previousSunBacklight + currentFactor * (EnvironmentManager.environment!!.sunBacklight - EnvironmentManager.previousSunBacklight)
			EnvironmentManager.currentBrightpass = EnvironmentManager.previousBrightpass + (EnvironmentManager.environment!!.brightpass - EnvironmentManager.previousBrightpass) * currentFactor
			if (EnvironmentManager.aClass78_2 !== EnvironmentManager.environment!!.sunCubeTexture) {
				EnvironmentManager.aClass78_3 = EnvironmentManager.toolkit!!.drawQuad(EnvironmentManager.aClass78_2, EnvironmentManager.environment!!.sunCubeTexture, currentFactor, EnvironmentManager.aClass78_3)
			}
		} else {
			EnvironmentManager.aClass78_3 = EnvironmentManager.environment!!.sunCubeTexture
			EnvironmentManager.currentSunLight = EnvironmentManager.environment!!.sunLight
			EnvironmentManager.currentBloom = EnvironmentManager.environment!!.bloom
			EnvironmentManager.currentWhitePoint = EnvironmentManager.environment!!.whitePoint
			EnvironmentManager.fogDepth = EnvironmentManager.environment!!.fogDepth
			EnvironmentManager.currentBrightpass = EnvironmentManager.environment!!.brightpass
			EnvironmentManager.currentSunBacklight = EnvironmentManager.environment!!.sunBacklight
			LoginManager.lastDisallowResult = -1
			EnvironmentManager.fogColour = EnvironmentManager.environment!!.fogColour
			EnvironmentManager.currentSunColour = EnvironmentManager.environment!!.sunColour
			EnvironmentManager.currentSunAmbient = EnvironmentManager.environment!!.sunAmbient
		}
		EnvironmentManager.lastEnvironmentUpdate = now
	}

	@OriginalMember(owner = "client!cb", name = "p", descriptor = "I")
	@JvmField
	var defaultFogColour: Int = 13156520

	@OriginalMember(owner = "client!cb", name = "k", descriptor = "I")
	@JvmField
	var defaultSunColour: Int = 16777215

	@OriginalMember(owner = "client!lj", name = "a", descriptor = "(Lclient!bp;III)V")
	@JvmStatic
	fun addChunkEnvironment(@OriginalArg(0) environment: Environment?, @OriginalArg(2) chunkX: Int, @OriginalArg(3) chunkZ: Int) {
		EnvironmentManager.aClass41ArrayArray1!![chunkX]!![chunkZ] = environment
	}

	@OriginalMember(owner = "client!lk", name = "e", descriptor = "(B)V")
	@JvmStatic
	fun updateAtmosphere() {
		if (MainLogicManager.isLoginScreenStep(ClientState.mainLogicStep) || MainLogicManager.inLobby(ClientState.mainLogicStep)) {
			EnvironmentManager.chunkEnvironmentUpdate(EnvironmentManager.ENVIRONMENT_FADE_MILLIS, Camera.cameraPositionAbsoluteX shr 12, Camera.cameraPositionAbsoluteY shr 12)
		} else {
			@Pc(16) val zoneX: Int = PlayerUpdate.localPlayer!!.movementBaseX[0] shr 3
			@Pc(23) val zoneY: Int = PlayerUpdate.localPlayer!!.movementBaseY[0] shr 3
			if (zoneX >= 0 && MapRegion.sceneSizeX shr 3 > zoneX && zoneY >= 0 && zoneY < MapRegion.sceneSizeY shr 3) {
				EnvironmentManager.chunkEnvironmentUpdate(EnvironmentManager.ENVIRONMENT_FADE_MILLIS, zoneX, zoneY)
			} else {
				EnvironmentManager.chunkEnvironmentUpdate(0, MapRegion.sceneSizeX shr 4, MapRegion.sceneSizeY shr 4)
			}
		}
		EnvironmentManager.transitionSceneEnvironment()
		EnvironmentManager.applySunSettings()
		EnvironmentManager.applyBloomParams()
		EnvironmentManager.applyFogSettings()
	}

	@OriginalMember(owner = "client!vg", name = "k", descriptor = "I")
	@JvmField
	var previousFogColour: Int = 0

	@OriginalMember(owner = "client!ot", name = "h", descriptor = "Lclient!wh;")
	@JvmField
	var aClass78_3: EnvironmentSampler? = null

	@OriginalMember(owner = "client!rg", name = "z", descriptor = "I")
	@JvmField
	var totalTransitionDuration: Int = 0

	@OriginalMember(owner = "client!ur", name = "m", descriptor = "F")
	@JvmField
	var currentBrightpass: Float = 0f

	@OriginalMember(owner = "client!ka", name = "m", descriptor = "F")
	@JvmField
	var previousSunLight: Float = 0f

	@OriginalMember(owner = "client!raa", name = "x", descriptor = "J")
	@JvmField
	var lastEnvironmentUpdate: Long = 0L

	@OriginalMember(owner = "client!tda", name = "r", descriptor = "F")
	@JvmField
	var currentSunLight: Float = 0f

	@OriginalMember(owner = "client!dba", name = "C", descriptor = "Lclient!wh;")
	@JvmField
	var aClass78_2: EnvironmentSampler? = null

	@OriginalMember(owner = "client!wha", name = "z", descriptor = "F")
	@JvmField
	var previousWhitePoint: Float = 0f

	@OriginalMember(owner = "client!fe", name = "y", descriptor = "F")
	@JvmField
	var currentSunBacklight: Float = 0f

	@OriginalMember(owner = "client!mc", name = "a", descriptor = "(IILclient!ha;I)V")
	@JvmStatic
	fun initialize(@OriginalArg(0) zoneCountX: Int, @OriginalArg(2) toolkit: Toolkit?, @OriginalArg(3) zoneCountZ: Int) {
		EnvironmentManager.toolkit = toolkit
		EnvironmentManager.aClass41ArrayArray1 = Array<Array<Environment?>?>(zoneCountX) { arrayOfNulls<Environment>(zoneCountZ) }
		if (EnvironmentManager.cubeTextureIds != null) {
			EnvironmentManager.aClass78_4 = EnvironmentManager.getCubeTexture(EnvironmentManager.cubeTextureIds!![4], EnvironmentManager.cubeTextureIds!![3], EnvironmentManager.cubeTextureIds!![0], EnvironmentManager.cubeTextureIds!![5], EnvironmentManager.cubeTextureIds!![1], EnvironmentManager.cubeTextureIds!![2])
		}
		EnvironmentManager.aClass41_2 = Environment()
		EnvironmentManager.resetChunkEnvironments()
	}

	@OriginalMember(owner = "client!ca", name = "e", descriptor = "F")
	@JvmField
	var previousSunBacklight: Float = 0f

	@OriginalMember(owner = "client!cw", name = "i", descriptor = "I")
	@JvmField
	var currentSunColour: Int = 0

	@OriginalMember(owner = "client!va", name = "sb", descriptor = "Lclient!bp;")
	@JvmField
	var aClass41_2: Environment? = null

	@OriginalMember(owner = "client!tt", name = "r", descriptor = "I")
	@JvmField
	var previousFogDepth: Int = 0

	@OriginalMember(owner = "client!eu", name = "J", descriptor = "F")
	@JvmField
	var currentBloom: Float = 0f

	@OriginalMember(owner = "client!od", name = "y", descriptor = "F")
	@JvmField
	var currentWhitePoint: Float = 0f

	@OriginalMember(owner = "client!hj", name = "e", descriptor = "I")
	@JvmField
	var sunPositionX: Int = -50

	@OriginalMember(owner = "client!pp", name = "a", descriptor = "(B)V")
	@JvmStatic
	fun reset() {
		EnvironmentSamplerCacheReset.reset()
		EnvironmentManager.aClass41ArrayArray1 = null
		EnvironmentManager.toolkit = null
		EnvironmentManager.aClass78_2 = null
		EnvironmentManager.aClass78_3 = null
		EnvironmentManager.aClass78_4 = null
	}

	@OriginalMember(owner = "client!pc", name = "a", descriptor = "(ZILclient!bp;)V")
	@JvmStatic
	fun startEnvironmentTransition(@OriginalArg(1) transitionTime: Int, @OriginalArg(2) environment: Environment?) {
		var transitionTime: Int = transitionTime
		if (EnvironmentManager.directEnvironmentTransition) {
			EnvironmentManager.directEnvironmentTransition = false
			transitionTime = 0
		}
		if (EnvironmentManager.environment != null && EnvironmentManager.environment!!.isEqual(environment)) {
			return
		}
		EnvironmentManager.environment = environment
		EnvironmentManager.lastEnvironmentUpdate = Utils.currentTimeMillis()
		LoginManager.lastDisallowResult = transitionTime
		EnvironmentManager.totalTransitionDuration = transitionTime
		if (EnvironmentManager.totalTransitionDuration == 0) {
			EnvironmentManager.transitionSceneEnvironment()
			return
		}
		EnvironmentManager.previousSunAmbient = EnvironmentManager.currentSunAmbient
		EnvironmentManager.previousFogColour = EnvironmentManager.fogColour
		EnvironmentManager.aClass78_2 = EnvironmentManager.aClass78_3
		EnvironmentManager.previousSunLight = EnvironmentManager.currentSunLight
		EnvironmentManager.previousBrightpass = EnvironmentManager.currentBrightpass
		EnvironmentManager.previousBloom = EnvironmentManager.currentBloom
		EnvironmentManager.previousWhitePoint = EnvironmentManager.currentWhitePoint
		EnvironmentManager.previousSunBacklight = EnvironmentManager.currentSunBacklight
		EnvironmentManager.previousSunColour = EnvironmentManager.currentSunColour
		EnvironmentManager.previousFogDepth = EnvironmentManager.fogDepth
	}

	@OriginalMember(owner = "client!jb", name = "z", descriptor = "I")
	@JvmField
	var previousSunColour: Int = 0

	@OriginalMember(owner = "client!aba", name = "D", descriptor = "F")
	@JvmField
	var previousSunAmbient: Float = 0f

	@OriginalMember(owner = "client!pa", name = "i", descriptor = "F")
	@JvmField
	var currentSunAmbient: Float = 0f

	const val ENVIRONMENT_FADE_MILLIS: Int = 5000

	@OriginalMember(owner = "client!jr", name = "o", descriptor = "Lclient!ha;")
	@JvmField
	var toolkit: Toolkit? = null

	@OriginalMember(owner = "client!aq", name = "b", descriptor = "(I)V")
	@JvmStatic
	fun applyFogSettings() {
		// Not part of the original client: FogControl moves the fog in and out while its key is held;
		// with it off the depth is the environment's own, as the original set it.
		val distance: Int = if (GamePreferences.uid!!.fog!!.getValue() == 1) (FogControl.depth(EnvironmentManager.fogDepth) + 256) shl 2 else -1
		EnvironmentManager.toolkit!!.L(EnvironmentManager.fogColour, distance, 0)
	}

	@OriginalMember(owner = "client!vj", name = "b", descriptor = "(B)V")
	@JvmStatic
	fun applyBloomParams() {
		EnvironmentManager.toolkit!!.setBloomParams(EnvironmentManager.currentWhitePoint, EnvironmentManager.currentBloom, EnvironmentManager.currentBrightpass)
	}

	@OriginalMember(owner = "client!in", name = "S", descriptor = "F")
	@JvmField
	var previousBloom: Float = 0f

	@OriginalMember(owner = "client!ts", name = "a", descriptor = "(Z)V")
	@JvmStatic
	fun applySunSettings() {
		EnvironmentManager.toolkit!!.xa((GamePreferences.uid!!.brightness!!.getValue().toFloat() * 0.1f + 0.7f) * EnvironmentManager.currentSunAmbient)
		EnvironmentManager.toolkit!!.ZA(EnvironmentManager.currentSunColour, EnvironmentManager.currentSunLight, EnvironmentManager.currentSunBacklight, (EnvironmentManager.sunPositionX shl 2).toFloat(), (StaticLocation.lb shl 2).toFloat(), (SunPositionZ.sunPositionZ shl 2).toFloat())
		EnvironmentManager.toolkit!!.setSunCubeTexture(EnvironmentManager.aClass78_3)
	}

	@OriginalMember(owner = "client!qga", name = "c", descriptor = "I")
	@JvmField
	var fogColour: Int = 0

	@OriginalMember(owner = "client!id", name = "a", descriptor = "I")
	@JvmField
	var fogDepth: Int = 0

	@OriginalMember(owner = "client!mo", name = "a", descriptor = "(IIZ)V")
	@JvmStatic
	fun applyEnvironmentSunPosition(@OriginalArg(0) chunkX: Int, @OriginalArg(1) chunkZ: Int) {
		@Pc(9) val environment: Environment? = EnvironmentManager.aClass41ArrayArray1!![chunkX]!![chunkZ]
		if (environment != null) {
			SunPositionZ.sunPositionZ = environment.sunPositionZ
			EnvironmentManager.sunPositionX = environment.sunPositionX
			StaticLocation.lb = environment.sunPositionY
		}
		EnvironmentManager.applySunSettings()
	}

	@OriginalMember(owner = "client!te", name = "d", descriptor = "F")
	@JvmField
	var previousBrightpass: Float = 0f
}
