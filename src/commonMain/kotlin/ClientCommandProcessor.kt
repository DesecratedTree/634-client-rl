import com.jagex.game.runetek5.client.features.GroundItemLabels
import com.jagex.game.runetek5.client.features.NpcLabels
import com.jagex.game.runetek5.client.features.XpDrops
import com.jagex.game.runetek5.client.features.TileMarkers
import com.jagex.game.runetek5.client.features.SmoothZoom
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.client.window.WindowModeType
import com.jagex.game.runetek5.core.constants.MainLogicStep
import com.jagex.game.runetek5.ScriptRunner
import com.jagex.game.runetek5.ChatLineCount
import com.jagex.game.runetek5.ClipComponentsDebug
import com.jagex.game.runetek5.W2DebugMode
import com.jagex.game.runetek5.core.crypto.GpuModelRetainMasks
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.graphics.Renderer
import com.jagex.game.runetek5.sign.SignLinkEnvironment
import com.jagex.game.runetek5.statics.CS2State
import jagex3.jagmisc.jagmisc
import io.File
import io.FileNotFoundException
import io.FileOutputStream
import org.openrs2.deob.annotation.OriginalArg
import org.openrs2.deob.annotation.OriginalMember
import org.openrs2.deob.annotation.Pc
import com.jagex.game.runetek5.client.features.ClientFeatures
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.js5.Js5Compression
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.scene.culling.Culling
import com.jagex.game.runetek5.sound.player.area.AmbientSound
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.config.varc.VarcTypeList
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.ClientPacket
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.MapRegion
import com.jagex.game.runetek5.ModeWhere
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.Minimap
import com.jagex.game.runetek5.OrthoMode
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.encoding.HuffmanList
import com.jagex.game.runetek5.network.Connection
import com.jagex.game.runetek5.scene.tile.TileMessage
import com.jagex.game.runetek5.NPCList
import com.jagex.game.runetek5.config.sequence.SeqType
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.network.world.WorldMeta
import com.jagex.game.runetek5.vardomain.PlayerVarDomain
import com.jagex.game.runetek5.client.ClientState
import com.jagex.game.runetek5.InterfaceManager
import com.jagex.game.runetek5.NativeLibraryOwner
import com.jagex.game.runetek5.preference.impl.MapSizePreference
import com.jagex.game.runetek5.sign.SignLink
import kotlin.jvm.JvmStatic
import lang.SecurityException
import lang.javaTrim
import lang.OutOfMemoryError

object ClientCommandProcessor {

	@OriginalMember(owner = "client!lba", name = "a", descriptor = "(Ljava/lang/String;ZZB)V")
	@JvmStatic
	fun executeCommand(@OriginalArg(0) string: String?, @OriginalArg(1) clientCommand: Boolean, @OriginalArg(2) tabComplete: Boolean) {
		try {
			// Not part of the original client: msaa, smaa and renderscale change the OpenGL anti-aliasing.
			@Pc(0) val antialiasing: String? = ClientFeatures.antialiasingCommand(string, ClientState.activeToolkit)
			if (antialiasing != null) {
				Console.printToConsole(antialiasing)
				return
			}
			// Not part of the original client: turns the ground item labels on and off (GroundItemLabels).
			if (string!!.equals("grounditems", ignoreCase = true)) {
				GroundItemLabels.enabled = !GroundItemLabels.enabled
				Console.printToConsole(if (GroundItemLabels.enabled) "Ground item labels enabled." else "Ground item labels disabled.")
				return
			}
			// Not part of the original client: turns the npc labels on and off (NpcLabels).
			if (string.equals("npclabels", ignoreCase = true)) {
				NpcLabels.enabled = !NpcLabels.enabled
				Console.printToConsole(if (NpcLabels.enabled) "Npc labels enabled." else "Npc labels disabled.")
				return
			}
			// Not part of the original client: turns the experience drops on and off (XpDrops).
			if (string.equals("xpdrops", ignoreCase = true)) {
				XpDrops.enabled = !XpDrops.enabled
				Console.printToConsole(if (XpDrops.enabled) "Experience drops enabled." else "Experience drops disabled.")
				return
			}
			// Not part of the original client: turns the tile markers on and off (TileMarkers).
			if (string.equals("tilemarkers", ignoreCase = true)) {
				TileMarkers.enabled = !TileMarkers.enabled
				Console.printToConsole(if (TileMarkers.enabled) "Tile markers enabled." else "Tile markers disabled.")
				return
			}
			// Not part of the original client: turns HDOS-style smooth zoom on and off (SmoothZoom).
			if (string.equals("smoothzoom", ignoreCase = true)) {
				SmoothZoom.toggle()
				Console.printToConsole(if (SmoothZoom.enabled) "Smooth zoom enabled." else "Smooth zoom disabled.")
				return
			}
			if (string.equals("shiftdrop", ignoreCase = true)) {
				// Not part of the original client: turns Shift-click drop on and off.
				ClientFeatures.shiftDrop = !ClientFeatures.shiftDrop
				Console.printToConsole(if (ClientFeatures.shiftDrop) "Shift-click drop enabled." else "Shift-click drop disabled.")
				return
			}
			// Not part of the original client: with client.console, help in game is the server's.
			if ((string.equals("commands", ignoreCase = true) || string.equals("help", ignoreCase = true)) && !(ClientFeatures.CONSOLE && ClientState.mainLogicStep === MainLogicStep.STEP_GAME_SCREEN)) {
				Console.printToConsole("commands - This command")
				Console.printToConsole("cls - Clear console")
				Console.printToConsole("displayfps - Toggle FPS and other information")
				Console.printToConsole("renderer - Print graphics renderer information")
				Console.printToConsole("heap - Print java memory information")
				return
			}
			if (string.equals("cls", ignoreCase = true)) {
				Console.scrollOffset = 0
				InterfaceComponentRenderer.consoleMessageCount = 0
				return
			}
			if (string.equals("displayfps", ignoreCase = true)) {
				Culling.wallCullingEnabled = !Culling.wallCullingEnabled
				if (Culling.wallCullingEnabled) {
					Console.printToConsole("FPS on")
					return
				}
				Console.printToConsole("FPS off")
				return
			}
			if (string == "renderer") {
				@Pc(73) val renderer: Renderer? = ClientState.activeToolkit!!.renderer()
				Console.printToConsole("Vendor: " + renderer!!.vendor)
				Console.printToConsole("Name: " + renderer.name)
				Console.printToConsole("Version: " + renderer.version)
				Console.printToConsole("Device: " + renderer.device)
				Console.printToConsole("Driver Version: " + renderer.driverVersion)
				return
			}
			if (string == "heap") {
				Console.printToConsole("Heap: " + GameShell.maxMemory + "MB")
				return
			}
		} catch (@Pc(145) ex: Exception) {
			Console.printToConsole(LocalizedText.ERROR_EXECUTING_COMMAND.get(ClientState.language))
			return
		}
		if (ServerEnvironment.LIVE !== ModeWhere.WTRC || LoginManager.staffModLevel >= 2) {
			if (string!!.equals("errortest", ignoreCase = true)) {
				throw RuntimeException()
			}
			if (string == "nativememerror") {
				throw OutOfMemoryError("native(MPR")
			}
			try {
				if (string.equals("printfps", ignoreCase = true)) {
					Console.printToConsole("FPS: " + Camera.fps)
					return
				}
				if (string.equals("occlude", ignoreCase = true)) {
					Culling.cullingEnabled = !Culling.cullingEnabled
					if (!Culling.cullingEnabled) {
						Console.printToConsole("Occlsion now off!")
						return
					}
					Console.printToConsole("Occlsion now on!")
					return
				}
				if (string.equals("fpson", ignoreCase = true)) {
					Culling.wallCullingEnabled = true
					Console.printToConsole("fps debug enabled")
					return
				}
				if (string.equals("fpsoff", ignoreCase = true)) {
					Culling.wallCullingEnabled = false
					Console.printToConsole("fps debug disabled")
					return
				}
				if (string == "systemmem") {
					try {
						Console.printToConsole("System memory: " + jagmisc.getAvailablePhysicalMemory() / 1048576L + "/" + SystemInfo.SYSTEM_INFO!!.ram + "Mb")
						return
					} catch (@Pc(257) ex2: Throwable) {
						return
					}
				}
				if (string.equals("cleartext", ignoreCase = true)) {
					TileMessage.aClass325_8.clear()
					Console.printToConsole("Text coords cleared")
					return
				}
				@Pc(300) val local300: Int
				@Pc(281) val local281: Int
				@Pc(290) var runtime: lang.PlatformRuntime
				if (string.equals("gc", ignoreCase = true)) {
					TypeLists.removeSoftReferences()
					var gcPass: Int = 0
					while (gcPass < 10) {
						lang.gc()
						gcPass++
					}
					runtime = lang.PlatformRuntime
					val usedMemoryKb: Int = ((runtime.totalMemory() - runtime.freeMemory()) / 1024L).toInt()
					Console.printToConsole("mem=" + usedMemoryKb + "k")
					return
				}
				if (string.equals("compact", ignoreCase = true)) {
					TypeLists.removeSoftReferences()
					var gcPass: Int = 0
					while (gcPass < 10) {
						lang.gc()
						gcPass++
					}
					runtime = lang.PlatformRuntime
					var usedMemoryKb: Int = ((runtime.totalMemory() - runtime.freeMemory()) / 1024L).toInt()
					Console.printToConsole("Memory before cleanup=" + usedMemoryKb + "k")
					Scene.uploadAllGameObjects()
					TypeLists.removeSoftReferences()
					@Pc(356) var i: Int = 0
					while (i < 10) {
						lang.gc()
						i++
					}
					usedMemoryKb = ((runtime.totalMemory() - runtime.freeMemory()) / 1024L).toInt()
					Console.printToConsole("Memory after cleanup=" + usedMemoryKb + "k")
					return
				}
				if (string.equals("unloadnatives", ignoreCase = true)) {
					Console.printToConsole(if (NativeLibraryOwner.unloadNativeLibraries()) "Libraries unloaded" else "Library unloading failed!")
					return
				}
				if (string.equals("clientdrop", ignoreCase = true)) {
					Console.printToConsole("Dropped client connection")
					if (ClientState.mainLogicStep === MainLogicStep.STEP_GAME_SCREEN) {
						ConnectionLostHandler.handleConnectionLost()
						return
					}
					if (ClientState.mainLogicStep === MainLogicStep.STEP_GAME_SCREEN_MAP_BUILD) {
						BufferedConnectionContext.dropped = true
					}
					return
				}
				if (string.equals("rotateconnectmethods", ignoreCase = true)) {
					TcpPacket.CURRENT_REQUEST!!.cycleConnectionMode()
					Console.printToConsole("Rotated connection methods")
					return
				}
				if (string.equals("clientjs5drop", ignoreCase = true)) {
					Resource.remoteFileRequesterJs5!!.quit()
					Console.printToConsole("Dropped client js5 net queue")
					return
				}
				if (string.equals("serverjs5drop", ignoreCase = true)) {
					Resource.remoteFileRequesterJs5!!.writeTerminate()
					Console.printToConsole("Dropped server js5 net queue")
					return
				}
				if (string.equals("breakcon", ignoreCase = true)) {
					GameShell.signLink!!.breakConnections()
					Connection.connection!!.breakConnection()
					Resource.remoteFileRequesterJs5!!.breakConnection()
					Console.printToConsole("Breaking new connections for 5 seconds")
					return
				}
				if (string.equals("rebuild", ignoreCase = true)) {
					MapRebuild.rebuildMap()
					ComponentMinimap.resetMinimap()
					Console.printToConsole("Rebuilding map")
					return
				}
				if (string.equals("rebuildprofile", ignoreCase = true)) {
					MapRegion.debugRegionLoadTimeStart = Utils.currentTimeMillis()
					MapBuildStep.rebuildProfile = true
					MapRebuild.rebuildMap()
					ComponentMinimap.resetMinimap()
					Console.printToConsole("Rebuilding map (with profiling)")
					return
				}
				if (string.equals("wm1", ignoreCase = true)) {
					WindowModeRequest.requestWindowMode(WindowModeType.FIXED, -1, false, -1)
					if (WindowMode.getWindowMode() !== WindowModeType.FIXED) {
						Console.printToConsole("wm1 failed")
						return
					}
					Console.printToConsole("wm1 succeeded")
					return
				}
				if (string.equals("wm2", ignoreCase = true)) {
					WindowModeRequest.requestWindowMode(WindowModeType.RESIZABLE, -1, false, -1)
					if (WindowMode.getWindowMode() === WindowModeType.RESIZABLE) {
						Console.printToConsole("wm2 succeeded")
						return
					}
					Console.printToConsole("wm2 failed")
					return
				}
				if (string.equals("wm3", ignoreCase = true)) {
					WindowModeRequest.requestWindowMode(WindowModeType.FULLSCREEN, 1024, false, 768)
					if (WindowMode.getWindowMode() === WindowModeType.FULLSCREEN) {
						Console.printToConsole("wm3 succeeded")
						return
					}
					Console.printToConsole("wm3 failed")
					return
				}
				if (string.equals("tk0", ignoreCase = true)) {
					ToolkitSwitch.loadAssets(false, 0)
					if (GamePreferences.uid!!.toolkit!!.getValue() != 0) {
						Console.printToConsole("Failed to enter tk0")
						return
					}
					Console.printToConsole("Entered tk0")
					GamePreferences.uid!!.setValue(GamePreferences.uid!!.toolkitDefault, 0)
					GamePreferences.savePreferences()
					GamePreferences.preferencesSavedOnServer = false
					return
				}
				if (string.equals("tk1", ignoreCase = true)) {
					ToolkitSwitch.loadAssets(false, 1)
					if (GamePreferences.uid!!.toolkit!!.getValue() != 1) {
						Console.printToConsole("Failed to enter tk1")
						return
					}
					Console.printToConsole("Entered tk1")
					GamePreferences.uid!!.setValue(GamePreferences.uid!!.toolkitDefault, 1)
					GamePreferences.savePreferences()
					GamePreferences.preferencesSavedOnServer = false
					return
				}
				if (string.equals("tk2", ignoreCase = true)) {
					ToolkitSwitch.loadAssets(false, 2)
					if (GamePreferences.uid!!.toolkit!!.getValue() == 2) {
						Console.printToConsole("Entered tk2")
						GamePreferences.uid!!.setValue(GamePreferences.uid!!.toolkitDefault, 2)
						GamePreferences.savePreferences()
						GamePreferences.preferencesSavedOnServer = false
						return
					}
					Console.printToConsole("Failed to enter tk2")
					return
				}
				if (string.equals("tk3", ignoreCase = true)) {
					ToolkitSwitch.loadAssets(false, 3)
					if (GamePreferences.uid!!.toolkit!!.getValue() != 3) {
						Console.printToConsole("Failed to enter tk3")
						return
					}
					Console.printToConsole("Entered tk3")
					GamePreferences.uid!!.setValue(GamePreferences.uid!!.toolkitDefault, 3)
					GamePreferences.savePreferences()
					GamePreferences.preferencesSavedOnServer = false
					return
				}
				if (string.equals("tk5", ignoreCase = true)) {
					ToolkitSwitch.loadAssets(false, 5)
					if (GamePreferences.uid!!.toolkit!!.getValue() == 5) {
						Console.printToConsole("Entered tk5")
						GamePreferences.uid!!.setValue(GamePreferences.uid!!.toolkitDefault, 5)
						GamePreferences.savePreferences()
						GamePreferences.preferencesSavedOnServer = false
						return
					}
					Console.printToConsole("Failed to enter tk5")
					return
				}
				if (string.startsWith("setba")) {
					if (string.length < 6) {
						Console.printToConsole("Invalid buildarea value")
						return
					}
					val buildArea: Int = Utils.parseInt(string.substring(6))
					if (buildArea >= 0 && MapSizePreference.getMaxBuildArea(GameShell.maxMemory) >= buildArea) {
						GamePreferences.uid!!.setValue(GamePreferences.uid!!.buildArea, buildArea)
						GamePreferences.savePreferences()
						GamePreferences.preferencesSavedOnServer = false
						Console.printToConsole("maxbuildarea=" + GamePreferences.uid!!.buildArea!!.getValue())
						return
					}
					Console.printToConsole("Invalid buildarea value")
					return
				}
				if (string.startsWith("rect_debug")) {
					if (string.length < 10) {
						Console.printToConsole("Invalid rect_debug value")
						return
					}
					InterfaceManager.interfaceDebugMode = Utils.parseInt((string.substring(10)).javaTrim())
					Console.printToConsole("rect_debug=" + InterfaceManager.interfaceDebugMode)
					return
				}
				if (string.equals("qa_op_test", ignoreCase = true)) {
					InterfaceManager.qaOpTest = true
					Console.printToConsole("qa_op_test=" + InterfaceManager.qaOpTest)
					return
				}
				if (string.equals("clipcomponents", ignoreCase = true)) {
					ClipComponentsDebug.clipComponents = !ClipComponentsDebug.clipComponents
					Console.printToConsole("clipcomponents=" + ClipComponentsDebug.clipComponents)
					return
				}
				if (string.startsWith("bloom")) {
					@Pc(885) val bloomEnabled: Boolean = ClientState.activeToolkit!!.bloom()
					if (!GamePreferences.setBloom(!bloomEnabled)) {
						Console.printToConsole("Failed to enable bloom")
						return
					}
					if (!bloomEnabled) {
						Console.printToConsole("Bloom enabled")
						return
					}
					Console.printToConsole("Bloom disabled")
					return
				}
				if (string.equals("tween", ignoreCase = true)) {
					if (!SeqType.forceTweening) {
						SeqType.forceTweening = true
						Console.printToConsole("Forced tweening ENABLED!")
						return
					}
					SeqType.forceTweening = false
					Console.printToConsole("Forced tweening disabled.")
					return
				}
				if (string.equals("shiftclick", ignoreCase = true)) {
					if (MiniMenu.shiftClickEnabled) {
						Console.printToConsole("Shift-click disabled.")
						MiniMenu.shiftClickEnabled = false
						return
					}
					Console.printToConsole("Shift-click ENABLED!")
					MiniMenu.shiftClickEnabled = true
					return
				}
				if (string.equals("getcgcoord", ignoreCase = true)) {
					Console.printToConsole("x:" + (PlayerUpdate.localPlayer!!.x shr 9) + " z:" + (PlayerUpdate.localPlayer!!.z shr 9))
					return
				}
				if (string.equals("getheight", ignoreCase = true)) {
					Console.printToConsole("Height: " + Scene.aClass65Array1!![PlayerUpdate.localPlayer!!.level.toInt()]!!.getHeight(PlayerUpdate.localPlayer!!.z shr 9, PlayerUpdate.localPlayer!!.x shr 9))
					return
				}
				if (string.equals("resetminimap", ignoreCase = true)) {
					Resource.SPRITES!!.discardPacked()
					Resource.SPRITES!!.discardUnpacked()
					TypeLists.renderer!!.cacheReset()
					TypeLists.MEC!!.cacheReset()
					ComponentMinimap.resetMinimap()
					Console.printToConsole("Minimap reset")
					return
				}
				if (string.startsWith("mc")) {
					if (ClientState.activeToolkit!!.method7311()) {
						var coreCount: Int = lang.parseInt(string.substring(3))
						if (coreCount < 1) {
							coreCount = 1
						} else if (coreCount > 4) {
							coreCount = 4
						}
						Scene.renderCores = coreCount
						MapRebuild.rebuildMap()
						Console.printToConsole("Render cores now: " + Scene.renderCores)
						return
					}
					Console.printToConsole("Current toolkit doesn't support multiple cores")
					return
				}
				if (string.startsWith("cachespace")) {
					Console.printToConsole("I(s): " + Component.NATIVE_SPRITE_CACHE.remaining() + "/" + Component.NATIVE_SPRITE_CACHE.capacity())
					Console.printToConsole("I(m): " + Component.MODEL_CACHE.remaining() + "/" + Component.MODEL_CACHE.capacity())
					Console.printToConsole("O(s): " + TypeLists.OBJ!!.spriteCache!!.remaining() + "/" + TypeLists.OBJ!!.spriteCache!!.capacity())
					return
				}
				if (string.equals("getcamerapos", ignoreCase = true)) {
					Console.printToConsole("Pos: " + PlayerUpdate.localPlayer!!.level + "," + (((Camera.cameraPositionAbsoluteX shr 9) + MapRegion.sceneBaseX) shr 6) + "," + (((Camera.cameraPositionAbsoluteY shr 9) + MapRegion.sceneBaseY) shr 6) + "," + (((Camera.cameraPositionAbsoluteX shr 9) + MapRegion.sceneBaseX) and 0x3F) + "," + (((Camera.cameraPositionAbsoluteY shr 9) + MapRegion.sceneBaseY) and 0x3F) + " Height: " + (Scene.tileHeightForAbsolutePosition(Camera.cameraPositionAbsoluteX, PlayerUpdate.localPlayer!!.level.toInt(), Camera.cameraPositionAbsoluteY) - Camera.cameraPositionAbsoluteZ))
					Console.printToConsole("Look: " + PlayerUpdate.localPlayer!!.level + "," + ((Camera.lookAtLocalX + MapRegion.sceneBaseX) shr 6) + "," + ((MapRegion.sceneBaseY + Camera.lookAtLocalY) shr 6) + "," + ((Camera.lookAtLocalX + MapRegion.sceneBaseX) and 0x3F) + "," + ((Camera.lookAtLocalY + MapRegion.sceneBaseY) and 0x3F) + " Height: " + (Scene.tileHeightForAbsolutePosition(Camera.lookAtLocalX, PlayerUpdate.localPlayer!!.level.toInt(), Camera.lookAtLocalY) - Camera.lookAtHeightOffset))
					return
				}
				if (string == "renderprofile" || string == "rp") {
					ToolkitProfiler.showProfiling = !ToolkitProfiler.showProfiling
					ClientState.activeToolkit!!.method7250(ToolkitProfiler.showProfiling)
					ToolkitProfiler.resetEntityDebugCounters()
					Console.printToConsole("showprofiling=" + ToolkitProfiler.showProfiling)
					return
				}
				@Pc(1305) var strings: Array<String?>
				@Pc(1296) val local1296: Int
				if (string.startsWith("performancetest")) {
					var toolkitId: Int = -1
					var profilingTime: Int = 1000
					if (string.length > 15) {
						strings = Utils.split(' ', string)
						try {
							if (strings.size > 1) {
								profilingTime = lang.parseInt(strings[1])
							}
						} catch (@Pc(1316) ex3: Throwable) {
						}
						try {
							if (strings.size > 2) {
								toolkitId = lang.parseInt(strings[2])
							}
						} catch (@Pc(1327) ex4: Throwable) {
						}
					}
					if (toolkitId != -1) {
						Console.printToConsole("Performance: " + ToolkitProfiler.getProfilingPerformance(toolkitId, profilingTime))
						return
					}
					Console.printToConsole("Java toolkit: " + ToolkitProfiler.getProfilingPerformance(0, profilingTime))
					Console.printToConsole("SSE toolkit:  " + ToolkitProfiler.getProfilingPerformance(2, profilingTime))
					Console.printToConsole("D3D toolkit:  " + ToolkitProfiler.getProfilingPerformance(3, profilingTime))
					Console.printToConsole("GL toolkit:   " + ToolkitProfiler.getProfilingPerformance(1, profilingTime))
					Console.printToConsole("GLX toolkit:  " + ToolkitProfiler.getProfilingPerformance(5, profilingTime))
					return
				}
				if (string == "nonpcs") {
					NPCList.hideNpcs = !NPCList.hideNpcs
					Console.printToConsole("nonpcs=" + NPCList.hideNpcs)
					return
				}
				if (string == "autoworld") {
					HostPreparation.prepareHost()
					Console.printToConsole("auto world selected")
					return
				}
				if (string.startsWith("switchworld")) {
					val worldId: Int = lang.parseInt(string.substring(12))
					GameHostSelection.setGameHost(WorldMeta.getWorldDescriptor(worldId)!!.activity, worldId)
					Console.printToConsole("switched")
					return
				}
				if (string == "getworld") {
					Console.printToConsole("w: " + TcpPacket.CURRENT_REQUEST!!.worldId)
					return
				}
				@Pc(1486) var packet: TcpPacket?
				if (string.startsWith("pc")) {
					packet = TcpPacket.createPacket(ClientProt.SEND_PRIVATE_MESSAGE, TcpPacket.isaac)
					packet!!.buffer!!.writeByte(0)
					val payloadStart: Int = packet.buffer!!.currentPosition
					val nameEnd: Int = string.indexOf(" ", 4)
					packet.buffer!!.writeString(string.substring(3, nameEnd))
					HuffmanList.writeChatMessage(packet.buffer, string.substring(nameEnd))
					packet.buffer!!.writeIndex(packet.buffer!!.currentPosition - payloadStart)
					BufferedConnectionContext.queuePacket(packet)
					return
				}
				if (string == "savevarcs") {
					VarcTypeList.savePersistentVarcs()
					Console.printToConsole("perm varcs saved")
					return
				}
				if (string == "scramblevarcs") {
					var varcId: Int = 0
					while (varcId < VarcTypeList.VARC_INT!!.size) {
						if (VarcTypeList.VARC_PERSISTENT!![varcId]) {
							VarcTypeList.VARC_INT!![varcId] = (util.random() * 99999.0).toInt()
							if (util.random() > 0.5) {
								VarcTypeList.VARC_INT!![varcId] *= -1
							}
						}
						varcId++
					}
					VarcTypeList.savePersistentVarcs()
					Console.printToConsole("perm varcs scrambled")
					return
				}
				if (string == "showcolmap") {
					Minimap.showCollisionMap = true
					ComponentMinimap.resetMinimap()
					Console.printToConsole("colmap is shown")
					return
				}
				if (string == "hidecolmap") {
					Minimap.showCollisionMap = false
					ComponentMinimap.resetMinimap()
					Console.printToConsole("colmap is hidden")
					return
				}
				if (string == "resetcache") {
					TypeLists.resetAllCaches()
					Console.printToConsole("Caches reset")
					return
				}
				if (string == "profilecpu") {
					Console.printToConsole(SoftwareRendererBenchmark.benchmarkSoftwareRenderer().toString() + "ms")
					return
				}
				if (string.startsWith("getclientvarpbit")) {
					val varbitId: Int = lang.parseInt(string.substring(17))
					Console.printToConsole("varpbit=" + PlayerVarDomain.playerVarProvider!!.getVarBitValue(varbitId))
					return
				}
				if (string.startsWith("getclientvarp")) {
					val varpId: Int = lang.parseInt(string.substring(14))
					Console.printToConsole("varp=" + PlayerVarDomain.playerVarProvider!!.getVarValueInt(varpId))
					return
				}
				if (string.startsWith("directlogin")) {
					@Pc(1700) val strings2: Array<String?> = Utils.split(' ', string.substring(12))
					if (strings2.size >= 2) {
						val worldId: Int = if (strings2.size > 2) lang.parseInt(strings2[2]) else 0
						LoginManager.worldLogin(strings2[1], worldId, strings2[0])
						return
					}
				}
				if (string.startsWith("csprofileclear")) {
					ScriptRunner.profileClear()
					return
				}
				if (string.startsWith("csprofileoutputc")) {
					CS2State.profileOutput()
					return
				}
				if (string.startsWith("csprofileoutputt")) {
					CS2State.profileOutput()
					return
				}
				if (string.startsWith("texsize")) {
					val textureSize: Int = lang.parseInt(string.substring(8))
					ClientState.activeToolkit!!.setTextureSize(textureSize)
					return
				}
				if (string == "soundstreamcount") {
					Console.printToConsole("Active streams: " + AmbientSound.gameSounds!!.count())
					return
				}
				if (string == "autosetup") {
					GraphicsAutoDetect.calculateGraphics()
					Console.printToConsole("Complete. Toolkit now: " + GamePreferences.uid!!.toolkit!!.getValue())
					return
				}
				if (string == "errormessage") {
					Console.printToConsole(ActiveClient.activeClient!!.getDebugString())
					return
				}
				if (string == "heapdump") {
					if (SystemInfo.operatingSystemLowercase!!.startsWith("win")) {
						SystemInfo.dumpHeap(File("C:\\Temp\\heap.dump"))
					} else {
						SystemInfo.dumpHeap(File("/tmp/heap.dump"))
					}
					Console.printToConsole("Done")
					return
				}
				if (string == "os") {
					Console.printToConsole("Name: " + SystemInfo.operatingSystemLowercase)
					Console.printToConsole("Arch: " + SystemInfo.OS_ARCH)
					Console.printToConsole("Ver: " + SystemInfo.osVersion)
					return
				}
				if (string.startsWith("w2debug")) {
					val debugMode: Int = lang.parseInt(string.substring(8, 9))
					W2DebugMode.w2DebugMode = debugMode
					MapRebuild.rebuildMap()
					Console.printToConsole("Toggled!")
					return
				}
				if (string.startsWith("ortho ")) {
					val spaceIndex: Int = string.indexOf(32.toChar())
					if (spaceIndex < 0) {
						Console.printToConsole("Syntax: ortho <n>")
						return
					}
					val orthoMode: Int = Utils.parseInt(string.substring(spaceIndex + 1))
					GamePreferences.uid!!.setValue(GamePreferences.uid!!.orthographic, orthoMode)
					GamePreferences.savePreferences()
					GamePreferences.preferencesSavedOnServer = false
					OrthoMode.applyOrthographicMode()
					if (GamePreferences.uid!!.orthographic!!.getValue() != orthoMode) {
						Console.printToConsole("Failed to change ortho mode")
						return
					}
					Console.printToConsole("Successfully changed ortho mode")
					return
				}
				if (string.startsWith("orthozoom ")) {
					if (GamePreferences.uid!!.orthographic!!.getValue() == 0) {
						Console.printToConsole("enable ortho mode first (use 'ortho <n>')")
						return
					}
					val zoom: Int = Utils.parseInt(string.substring(string.indexOf(32.toChar()) + 1))
					OrthoMode.orthoZoom = zoom
					Console.printToConsole("orthozoom=" + OrthoMode.orthoZoom)
					return
				}
				if (string.startsWith("orthotilesize ")) {
					val tileSize: Int = Utils.parseInt(string.substring(string.indexOf(32.toChar()) + 1))
					Js5Compression.maxSize = tileSize
					OrthoMode.orthoTileSizeX = tileSize
					Console.printToConsole("ortho tile size=" + tileSize)
					OrthoMode.applyOrthographicMode()
					return
				}
				if (string == "orthocamlock") {
					Camera.orthoCameraLock = !Camera.orthoCameraLock
					Console.printToConsole("ortho camera lock is " + (if (Camera.orthoCameraLock) "on" else "off"))
					return
				}
				@Pc(2049) var file: File
				if (string.startsWith("setoutput ")) {
					file = File(string.substring(10))
					if (file.exists()) {
						file = File(string.substring(10) + "." + Utils.currentTimeMillis() + ".log")
						if (file.exists()) {
							Console.printToConsole("file already exists!")
							return
						}
					}
					if (Console.outputStream != null) {
						Console.outputStream!!.close()
						Console.outputStream = null
					}
					try {
						Console.outputStream = FileOutputStream(file)
						return
					} catch (@Pc(2091) ex5: FileNotFoundException) {
						Console.printToConsole("Could not create " + file.getName())
						return
					} catch (@Pc(2103) ex6: SecurityException) {
						Console.printToConsole("Cannot write to " + file.getName())
						return
					}
				}
				if (string == "closeoutput") {
					if (Console.outputStream != null) {
						Console.outputStream!!.close()
					}
					Console.outputStream = null
					return
				}
				if (string.startsWith("runscript ")) {
					file = File(string.substring(10))
					if (!file.exists()) {
						Console.printToConsole("No such file")
						return
					}
					@Pc(2151) val fileData: ByteArray? = SignLink.readFile(file)
					if (fileData == null) {
						Console.printToConsole("Failed to read file")
						return
					}
					strings = Utils.split('\n', Utils.replaceCarriageReturns(Utils.bytesToString(fileData), ""))
					ConsoleScriptRunner.runScript(strings)
				}
				if (string.startsWith("zoom ")) {
					@Pc(2182) val zoom: Short = Utils.parseInt(string.substring(5)).toShort()
					if (zoom > 0) {
						Camera.cameraZoomMax = zoom
					}
					return
				}
				if (ClientState.mainLogicStep === MainLogicStep.STEP_GAME_SCREEN) {
					packet = TcpPacket.createPacket(ClientProt.CLIENT_CHEAT, TcpPacket.isaac)
					packet!!.buffer!!.writeByte(string.length + 3)
					packet.buffer!!.writeByte(if (clientCommand) 1 else 0)
					packet.buffer!!.writeByte(if (tabComplete) 1 else 0)
					packet.buffer!!.writeString(string)
					BufferedConnectionContext.queuePacket(packet)
				}
				if (string.startsWith("fps ") && ServerEnvironment.LIVE !== ModeWhere.WTRC) {
					GameShell.setPulseRate(Utils.parseInt(string.substring(4)))
					return
				}
			} catch (@Pc(2251) ex7: Exception) {
				Console.printToConsole(LocalizedText.ERROR_EXECUTING_COMMAND.get(ClientState.language))
				return
			}
		}
		if (ClientState.mainLogicStep !== MainLogicStep.STEP_GAME_SCREEN) {
			Console.printToConsole(LocalizedText.UNKNOWN_DEV_COMMAND.get(ClientState.language) + string)
		}
	}
}
