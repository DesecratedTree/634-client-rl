import com.jagex.game.runetek5.config.rsinterface.MediaType
import com.jagex.game.runetek5.AudioRenderer
import com.jagex.game.runetek5.core.constants.MainLogicStep
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.cache.node.lru.PulseEvent
import com.jagex.game.runetek5.EntityTick
import com.jagex.game.runetek5.MainLogicManager
import com.jagex.game.runetek5.Preferences
import com.jagex.game.runetek5.config.sequence.SeqType
import com.jagex.game.runetek5.core.crypto.GpuModelRetainMasks
import com.jagex.game.runetek5.core.datastruct.LoginStateUpdate
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.core.io.connection.TickHookRequests
import com.jagex.game.runetek5.core.util.TimeUtils
import com.jagex.game.runetek5.DelayedStateChange
import com.jagex.game.runetek5.camera.CameraMode
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.graphics.MiniMenuReset
import com.jagex.game.runetek5.statics.SceneProjectionCenterY
import io.IOException
import org.openrs2.deob.annotation.OriginalMember
import org.openrs2.deob.annotation.Pc
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord
import com.jagex.game.runetek5.client.event.mouse.MouseLog
import com.jagex.game.runetek5.client.features.ClientFeatures
import com.jagex.game.runetek5.client.features.SmoothZoom
import com.jagex.game.runetek5.client.input.console.CommandExecutor
import com.jagex.game.runetek5.client.menu.MenuInteraction
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.network.PingRequester
import com.jagex.game.runetek5.config.varc.VarcTypeList
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.network.ConnectionInfo
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.ClientPacket
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.ChatLineWriter
import com.jagex.game.runetek5.botdetection.ReflectionCheckManager
import com.jagex.game.runetek5.client.periodic.PulseEventHandler
import com.jagex.game.runetek5.client.PingWorker
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.Cursor
import com.jagex.game.runetek5.InterfaceDrag
import com.jagex.game.runetek5.Minimap
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.InterfaceManager
import com.jagex.game.runetek5.MapRegion
import com.jagex.game.runetek5.SoundManager
import com.jagex.game.runetek5.camera.Shake
import com.jagex.game.runetek5.client.input.mouse.MouseManager
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.core.datastruct.key.MouseOverTextY
import com.jagex.game.runetek5.graphics.hint.HintDetails
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.scene.LocAction
import com.jagex.game.runetek5.scene.environment.EnvironmentManager
import com.jagex.game.runetek5.NPCList
import com.jagex.game.runetek5.vardomain.PlayerVarDomain
import com.jagex.game.runetek5.config.rsinterface.ComponentType
import com.jagex.game.runetek5.client.ClientState
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.utils.Utils
import kotlin.jvm.JvmStatic

object GameTick {

	@OriginalMember(owner = "client!ls", name = "a", descriptor = "(I)V")
	@JvmStatic
	fun processGameTick() {
		if (client.rebootTimer > 1) {
			InterfaceManager.lastMiscUpdateTick = ClientState.serverTick
			client.rebootTimer--
		}
		if (BufferedConnectionContext.dropped) {
			BufferedConnectionContext.dropped = false
			ConnectionLostHandler.handleConnectionLost()
			return
		}
		if (!MiniMenu.isOpen) {
			MiniMenuReset.resetEntries()
		}
		@Pc(29) var i: Int = 0
		while (i < 100 && GuardedPacketDecode.decodePacketFromStream()) {
			i++
		}
		if (ClientState.mainLogicStep != MainLogicStep.STEP_GAME_SCREEN) {
			return
		}
		@Pc(51) var tcpPacket: TcpPacket
		@Pc(60) var local60: Int
		while (ReflectionCheckManager.isCheckReady()) {
			tcpPacket = TcpPacket.createPacket(ClientProt.REFLECTION_CHECK_REPLY, TcpPacket.isaac)
			tcpPacket.buffer!!.writeByte(0)
			val startPosition: Int = tcpPacket.buffer!!.currentPosition
			ReflectionCheckManager.collectResult(tcpPacket.buffer)
			tcpPacket.buffer!!.writeIndex(tcpPacket.buffer!!.currentPosition - startPosition)
			BufferedConnectionContext.queuePacket(tcpPacket)
		}
		if (PingWorker.aClass2_Sub36_1 == null) {
			if (Utils.currentTimeMillis() >= BufferedConnectionContext.NEXT_PING_UPDATE_TO_SERVER) {
				PingWorker.aClass2_Sub36_1 = PingWorker.aClass262_1!!.requestPing(TcpPacket.CURRENT_REQUEST!!.host)
			}
		} else if (PingWorker.aClass2_Sub36_1!!.ping != -1) {
			tcpPacket = TcpPacket.createPacket(ClientProt.WRITE_PING, TcpPacket.isaac)
			tcpPacket.buffer!!.writeShort(PingWorker.aClass2_Sub36_1!!.ping)
			BufferedConnectionContext.queuePacket(tcpPacket)
			PingWorker.aClass2_Sub36_1 = null
			BufferedConnectionContext.NEXT_PING_UPDATE_TO_SERVER = Utils.currentTimeMillis() + 30000L
		}
		@Pc(131) val mouseEvent: MouseRecord? = MouseManager.MOUSE_MOVEMENTS.first() as MouseRecord?
		@Pc(148) var currentPosition: Int
		@Pc(170) var local170: Int
		@Pc(187) var local187: Int
		@Pc(233) var local233: Int
		@Pc(239) var local239: Int
		@Pc(251) var local251: Int
		@Pc(146) var outgoingPacket: TcpPacket?
		if (mouseEvent != null || Utils.currentTimeMillis() - 2000L > PingRequester.NEXT_PING_UPDATE_TO_SERVER) {
			outgoingPacket = null
			currentPosition = 0
			@Pc(153) var mouseEvent2: MouseRecord? = PingRequester.CURRENT_REQUEST.first() as MouseRecord?
			while (mouseEvent2 != null && (outgoingPacket == null || outgoingPacket.buffer!!.currentPosition - currentPosition < 240)) {
				mouseEvent2.unlink()
				var mouseY: Int = mouseEvent2.getMouseY()
				if (mouseY < -1) {
					mouseY = -1
				} else if (mouseY > 65534) {
					mouseY = 65534
				}
				var mouseX: Int = mouseEvent2.getMouseX()
				if (mouseX < -1) {
					mouseX = -1
				} else if (mouseX > 65534) {
					mouseX = 65534
				}
				if (MouseManager.lastSentMouseX != mouseX || mouseY != MouseManager.lastSentMouseY) {
					if (outgoingPacket == null) {
						outgoingPacket = TcpPacket.createPacket(ClientProt.EVENT_MOUSE_MOVE, TcpPacket.isaac)
						outgoingPacket.buffer!!.writeByte(0)
						currentPosition = outgoingPacket.buffer!!.currentPosition
					}
					var deltaX: Int = mouseX - MouseManager.lastSentMouseX
					MouseManager.lastSentMouseX = mouseX
					var deltaY: Int = mouseY - MouseManager.lastSentMouseY
					MouseManager.lastSentMouseY = mouseY
					val timeDelta: Int = ((mouseEvent2.getTime() - PingRequester.NEXT_PING_UPDATE_TO_SERVER) / 20L).toInt()
					if (timeDelta < 8 && deltaX >= -32 && deltaX <= 31 && deltaY >= -32 && deltaY <= 31) {
						deltaX += 32
						deltaY += 32
						outgoingPacket.buffer!!.writeShort(deltaY + (deltaX shl 6) + (timeDelta shl 12))
					} else if (timeDelta < 32 && deltaX >= -128 && deltaX <= 127 && deltaY >= -128 && deltaY <= 127) {
						deltaX += 128
						deltaY += 128
						outgoingPacket.buffer!!.writeByte(timeDelta + 128)
						outgoingPacket.buffer!!.writeShort(deltaY + (deltaX shl 8))
					} else if (timeDelta >= 32) {
						outgoingPacket.buffer!!.writeShort(timeDelta + 57344)
						if (mouseX == 1 || mouseY == -1) {
							outgoingPacket.buffer!!.writeInt(Int.MIN_VALUE)
						} else {
							outgoingPacket.buffer!!.writeInt((mouseY shl 16) or mouseX)
						}
					} else {
						outgoingPacket.buffer!!.writeByte(timeDelta + 192)
						if (mouseX == 1 || mouseY == -1) {
							outgoingPacket.buffer!!.writeInt(Int.MIN_VALUE)
						} else {
							outgoingPacket.buffer!!.writeInt(mouseX or (mouseY shl 16))
						}
					}
					PingRequester.NEXT_PING_UPDATE_TO_SERVER = mouseEvent2.getTime()
				}
				mouseEvent2 = PingRequester.CURRENT_REQUEST.next() as MouseRecord?
			}
			if (outgoingPacket != null) {
				outgoingPacket.buffer!!.writeIndex(outgoingPacket.buffer!!.currentPosition - currentPosition)
				BufferedConnectionContext.queuePacket(outgoingPacket)
			}
		}
		@Pc(452) var local452: Int
		if (mouseEvent != null) {
			@Pc(436) var clickTimeDelta: Long = (mouseEvent.getTime() - MouseManager.lastClickTime) / 50L
			MouseManager.lastClickTime = mouseEvent.getTime()
			if (clickTimeDelta > 32767L) {
				clickTimeDelta = 32767L
			}
			var mouseY: Int = mouseEvent.getMouseY()
			if (mouseY < 0) {
				mouseY = 0
			} else if (mouseY > 65535) {
				mouseY = 65535
			}
			var mouseX: Int = mouseEvent.getMouseX()
			if (mouseX < 0) {
				mouseX = 0
			} else if (mouseX > 65535) {
				mouseX = 65535
			}
			@Pc(485) var rightClick: Byte = 0
			if (mouseEvent.getClickType() == MouseLog.TYPE_PRESS_RIGHT) {
				rightClick = 1
			}
			val clickDelta: Int = clickTimeDelta.toInt()
			@Pc(510) val clickPacket: TcpPacket = TcpPacket.createPacket(ClientProt.EVENT_MOUSE_CLICK, TcpPacket.isaac)
			clickPacket.buffer!!.writeShort(clickDelta or (rightClick.toInt() shl 15))
			clickPacket.buffer!!.writeInt((mouseY shl 16) or mouseX)
			BufferedConnectionContext.queuePacket(clickPacket)
		}
		if (KeyRecorder.keyCachePointerPoss > 0) {
			outgoingPacket = TcpPacket.createPacket(ConnectionInfo.EVENT_KEYBOARD, TcpPacket.isaac)
			outgoingPacket.buffer!!.writeByte(KeyRecorder.keyCachePointerPoss * 3)
			currentPosition = 0
			while (currentPosition < KeyRecorder.keyCachePointerPoss) {
				@Pc(557) val keyLog: KeyRecord? = KeyRecorder.KEYS_PRESSED[currentPosition]
				@Pc(566) var keyTimeDelta: Long = (keyLog!!.getTime() - KeyRecorder.lastKeyTime) / 50L
				KeyRecorder.lastKeyTime = keyLog.getTime()
				if (keyTimeDelta > 65535L) {
					keyTimeDelta = 65535L
				}
				outgoingPacket.buffer!!.writeByte(keyLog.getKeyCode())
				outgoingPacket.buffer!!.writeShort(keyTimeDelta.toInt())
				currentPosition++
			}
			BufferedConnectionContext.queuePacket(outgoingPacket)
		}
		if (Camera.cameraAngleSendCooldown > 0) {
			Camera.cameraAngleSendCooldown--
		}
		if (Camera.sendCameraAngle && Camera.cameraAngleSendCooldown <= 0) {
			Camera.sendCameraAngle = false
			Camera.cameraAngleSendCooldown = 20
			outgoingPacket = TcpPacket.createPacket(ClientProt.EVENT_CAMERA_POSITION, TcpPacket.isaac)
			outgoingPacket.buffer!!.writeShortAddLittle(Camera.camAngleY.toInt() shr 3)
			outgoingPacket.buffer!!.writeShort(Camera.camAngleX.toInt() shr 3)
			BufferedConnectionContext.queuePacket(outgoingPacket)
		}
		if (GameShell.lastSentFocus != GameShell.appletHasFocus) {
			GameShell.lastSentFocus = GameShell.appletHasFocus
			outgoingPacket = TcpPacket.createPacket(ClientProt.EVENT_APPLET_FOCUS, TcpPacket.isaac)
			outgoingPacket.buffer!!.writeByte(if (GameShell.appletHasFocus) 1 else 0)
			BufferedConnectionContext.queuePacket(outgoingPacket)
		}
		if (!GamePreferences.preferencesSavedOnServer) {
			outgoingPacket = TcpPacket.createPacket(ClientProt.aClass94_37, TcpPacket.isaac)
			outgoingPacket.buffer!!.writeByte(0)
			currentPosition = outgoingPacket.buffer!!.currentPosition
			@Pc(702) val packet: Packet = GamePreferences.uid!!.encode()
			outgoingPacket.buffer!!.writeBytes(packet.currentPosition, 0, packet.buffer)
			outgoingPacket.buffer!!.writeIndex(outgoingPacket.buffer!!.currentPosition - currentPosition)
			BufferedConnectionContext.queuePacket(outgoingPacket)
			GamePreferences.preferencesSavedOnServer = true
		}
		if (Scene.aClass100ArrayArrayArray3 != null) {
			if (Camera.cameraCalculationType == CameraMode.MODE_FOLLOWPLAYER) {
				Camera.calculateCameraPosition()
			} else if (Camera.cameraCalculationType == CameraMode.MODE_SPLINE) {
				Camera.calculateCutsceneCameraPosition()
			}
		}
		if (Camera.yawKeyHeld) {
			Camera.yawKeyHeld = false
		} else {
			Camera.cameraYawVelocity /= 2.0f
		}
		if (Camera.pitchKeyHeld) {
			Camera.pitchKeyHeld = false
		} else {
			Camera.cameraPitchVelocity /= 2.0f
		}
		LevelChangeRebuild.checkLevelChange()
		if (ClientState.mainLogicStep != MainLogicStep.STEP_GAME_SCREEN) {
			return
		}
		EnvironmentManager.updateAtmosphere()
		LocAction.processPendingChanges()
		AudioRenderer.updateAreaSounds()
		BufferedConnectionContext.idleReadPulses++
		if (BufferedConnectionContext.idleReadPulses > 750) {
			ConnectionLostHandler.handleConnectionLost()
			return
		}
		EntityTick.movePlayers()
		NPCList.moveNpcs()
		HintDetails.tickHintTimers()
		var varpId: Int = PlayerVarDomain.playerVarProvider!!.removeNext(true)
		while (varpId != -1) {
			MainLogicManager.onVarChange(varpId)
			PlayerVarDomain.LOGIN_XTEAS[PlayerVarDomain.pulseEventConfigsPtr++ and 0x1F] = varpId
			varpId = PlayerVarDomain.playerVarProvider!!.removeNext(false)
		}
		@Pc(894) var component: Component?
		@Pc(828) var pulseEvent: PulseEvent? = PulseEventHandler.getNextExecutedEvent()
		while (pulseEvent != null) {
			val eventType: Int = pulseEvent.getType()
			val componentId: Int = pulseEvent.getId()
			if (eventType == DelayedStateChange.TYPE_SETVARC) {
				VarcTypeList.VARC_INT!![componentId] = pulseEvent.primary
				VarcTypeList.clientIsActive = VarcTypeList.clientIsActive or VarcTypeList.VARC_PERSISTENT!![componentId]
				client.recentVarcUpdates!![VarcTypeList.varcUpdateCycle++ and 0x1F] = componentId
			} else if (eventType == DelayedStateChange.TYPE_SETVARCSTR) {
				VarcTypeList.VARC_STRING!![componentId] = pulseEvent.string
				VarcTypeList.recentVarcStrUpdates[VarcTypeList.varcStrUpdateCycle++ and 0x1F] = componentId
			} else if (eventType == DelayedStateChange.TYPE_IF_SETTEXT) {
				component = Interface.getComponent(componentId)
				if (!pulseEvent.string!!.equals(component!!.text)) {
					component.text = pulseEvent.string
					Component.redrawComponent(component)
				}
			} else if (eventType == DelayedStateChange.TYPE_IF_SETMODEL) {
				component = Interface.getComponent(componentId)
				val mediaTypeId: Int = pulseEvent.primary
				val mediaId: Int = pulseEvent.secondary
				val secondaryMediaId: Int = pulseEvent.tertiary
				if (component!!.mediaType !== MediaType.fromId(mediaTypeId) || mediaId != component.primaryMediaId || component.secondaryMediaId != secondaryMediaId) {
					component.primaryMediaId = mediaId
					component.mediaType = MediaType.fromId(mediaTypeId)
					component.secondaryMediaId = secondaryMediaId
					Component.redrawComponent(component)
				}
			} else if (eventType == DelayedStateChange.TYPE_IF_SETMODELANIM) {
				component = Interface.getComponent(componentId)
				if (pulseEvent.primary != component!!.animation || pulseEvent.primary == -1) {
					component.animation = pulseEvent.primary
					component.currentFrameId = 0
					component.currentFrameDuration = 0
					component.nextFrameId = 1
					@Pc(988) val seqType: SeqType? = if (component.animation == -1) null else TypeLists.SEQ!!.list(component.animation)
					if (seqType != null) {
						SoundManager.playInterfaceSequenceSound(component.currentFrameId, seqType)
					}
					Component.redrawComponent(component)
				}
			} else if (eventType == DelayedStateChange.TYPE_IF_SETCOLOUR) {
				val packedColour: Int = pulseEvent.primary
				val red: Int = (packedColour shr 10) and 0x1F
				val green: Int = (packedColour shr 5) and 0x1F
				val blue: Int = packedColour and 0x1F
				@Pc(1037) val rgb: Int = (blue shl 3) + ((red shl 19) + (green shl 11))
				@Pc(1041) val component2: Component? = Interface.getComponent(componentId)
				if (rgb != component2!!.color) {
					component2.color = rgb
					Component.redrawComponent(component2)
				}
			} else if (eventType == 7) {
				component = Interface.getComponent(componentId)
				@Pc(1072) val hidden: Boolean = pulseEvent.primary == 1
				if (component!!.hidden != hidden) {
					component.hidden = hidden
					Component.redrawComponent(component)
				}
			} else if (eventType == 8) {
				component = Interface.getComponent(componentId)
				if (component!!.spritePitch != pulseEvent.primary || pulseEvent.secondary != component.spriteRoll || pulseEvent.tertiary != component.spriteScale) {
					component.spriteScale = pulseEvent.tertiary
					component.spriteRoll = pulseEvent.secondary
					component.spritePitch = pulseEvent.primary
					if (component.containerItemId != -1) {
						if (component.aspectWidth > 0) {
							component.spriteScale = component.spriteScale * 32 / component.aspectWidth
						} else if (component.baseWidth > 0) {
							component.spriteScale = component.spriteScale * 32 / component.baseWidth
						}
					}
					Component.redrawComponent(component)
				}
			} else if (eventType == DelayedStateChange.TYPE_IF_SETOBJECT) {
				component = Interface.getComponent(componentId)
				if (pulseEvent.primary != component!!.containerItemId || pulseEvent.secondary != component.itemStackSize) {
					component.itemStackSize = pulseEvent.secondary
					component.containerItemId = pulseEvent.primary
					Component.redrawComponent(component)
				}
			} else if (eventType == DelayedStateChange.TYPE_IF_SETMODELOFFSET) {
				component = Interface.getComponent(componentId)
				if (component!!.offsetX != pulseEvent.primary || pulseEvent.secondary != component.offsetY || component.spriteYaw != pulseEvent.tertiary) {
					component.offsetY = pulseEvent.secondary
					component.offsetX = pulseEvent.primary
					component.spriteYaw = pulseEvent.tertiary
					Component.redrawComponent(component)
				}
			} else if (eventType == DelayedStateChange.TYPE_IF_SETPOSITION) {
				component = Interface.getComponent(componentId)
				component!!.aspectXType = 0
				component.basePositionY = pulseEvent.secondary
				component.y = component.basePositionY
				component.aspectYType = 0
				component.basePositionX = pulseEvent.primary
				component.x = component.basePositionX
				Component.redrawComponent(component)
			} else if (eventType == DelayedStateChange.TYPE_IF_SETSCROLLPOS) {
				component = Interface.getComponent(componentId)
				var scrollPosition: Int = pulseEvent.primary
				if (component != null && component.type == ComponentType.CONTAINER) {
					if (component.scrollHeight - component.height < scrollPosition) {
						scrollPosition = component.scrollHeight - component.height
					}
					if (scrollPosition < 0) {
						scrollPosition = 0
					}
					if (scrollPosition != component.scrollY) {
						component.scrollY = scrollPosition
						Component.redrawComponent(component)
					}
				}
			} else if (eventType == DelayedStateChange.TYPE_IF_SETGRAPHIC) {
				component = Interface.getComponent(componentId)
				component!!.spriteId = pulseEvent.primary
			} else if (eventType == DelayedStateChange.TYPE_SETMAPFLAG) {
				Minimap.minimapFlagY = pulseEvent.secondary
				Minimap.minimapFlagX = pulseEvent.primary
				Minimap.minimapContainsFlag = true
			} else if (eventType == DelayedStateChange.TYPE_IF_SETTEXTFONT) {
				component = Interface.getComponent(componentId)
				component!!.fontId = pulseEvent.primary
			} else if (eventType == 17) {
				component = Interface.getComponent(componentId)
				component!!.videoFileId = pulseEvent.primary
			}
			pulseEvent = PulseEventHandler.getNextExecutedEvent()
		}
		ClientState.animatingStep++
		if (StaticMedia.crossType != 0) {
			StaticMedia.crossSpritePtr += 20
			if (StaticMedia.crossSpritePtr >= 400) {
				StaticMedia.crossType = 0
			}
		}
		if (MenuInteraction.lastHoveredComponent != null) {
			MenuInteraction.menuItemHoverCycle++
			if (MenuInteraction.menuItemHoverCycle >= 15) {
				Component.redrawComponent(MenuInteraction.lastHoveredComponent)
				MenuInteraction.lastHoveredComponent = null
			}
		}
		InterfaceDrag.aClass24_13 = null
		InterfaceDrag.dragComponentFound = false
		WorldMap.worldmapComponent = null
		InterfaceDrag.dragParentFound = false
		InterfaceComponentRenderer.setChatDisplay(null, -1, -1)
		MouseOverTextY.setMouseOverText(-1, null, -1)
		if (!MiniMenu.interfaceSelectionActive) {
			Cursor.interfaceCursorId = -1
		}
		if (ClientFeatures.ZOOM) {
			ClientFeatures.overGameScreen = false
		}
		InterfaceInputTick.processRootInterfaceInput()
		ClientState.serverTick++
		if (ClientFeatures.ZOOM && ClientFeatures.overGameScreen && ClientFeatures.wheelDelta != 0 && !Console.isOpen() && ClientFeatures.zoomKeyHeld()) {
			// Not part of the original client: while the zoom key (Ctrl by default; client.zoomKey) is
			// held, the wheel zooms the camera, unless staff are changing level with Ctrl+Shift.
			if (LoginManager.staffModLevel <= 0 || !KeyRecorder.keyRecorder!!.isPressed(82) || !KeyRecorder.keyRecorder!!.isPressed(81)) {
				SmoothZoom.wheel(ClientFeatures.wheelDelta)
			}
		}
		// Not part of the original client: eases the field of view toward the wheel with smooth zoom.
		SmoothZoom.tick()
		if (WorldMap.worldMapTeleportPending) {
			@Pc(1478) val teleportPacket: TcpPacket = TcpPacket.createPacket(BufferedConnectionContext.WORLDMAP_CLICK, TcpPacket.isaac)
			teleportPacket.buffer!!.writeInt((CommandExecutor.teleportCommandPositionZ shl 14) or (CommandExecutor.teleportCommandPositionX shl 28) or CommandExecutor.teleportCommandPositionY)
			BufferedConnectionContext.queuePacket(teleportPacket)
			WorldMap.worldMapTeleportPending = false
		}
		while (true) {
			@Pc(1501) var hookRequest: HookRequest?
			@Pc(1506) var component3: Component?
			do {
				hookRequest = TickHookRequests.TICK_HOOK_REQUESTS.removeFirst() as HookRequest?
				if (hookRequest == null) {
					while (true) {
						do {
							hookRequest = TickHookRequests.MOUSE_HOOK_REQUESTS.removeFirst() as HookRequest?
							if (hookRequest == null) {
								while (true) {
									do {
										hookRequest = InterfaceManager.PENDING_HOOK_REQUESTS.removeFirst() as HookRequest?
										if (hookRequest == null) {
											if (WorldMap.worldmapComponent == null) {
												Preferences.favoriteWorlds = 0
											}
											if (InterfaceDrag.component != null) {
												ComponentDragTick.processDrag()
											}
											if (LoginManager.staffModLevel > 0 && KeyRecorder.keyRecorder!!.isPressed(82) && KeyRecorder.keyRecorder!!.isPressed(81) && client.mouseWheelDelta != 0) {
												var targetLevel: Int = PlayerUpdate.localPlayer!!.level - client.mouseWheelDelta
												if (targetLevel < 0) {
													targetLevel = 0
												} else if (targetLevel > 3) {
													targetLevel = 3
												}
												StaffTeleportCommand.executeTeleportCommand(PlayerUpdate.localPlayer!!.movementBaseY!![0] + MapRegion.sceneBaseY, targetLevel, PlayerUpdate.localPlayer!!.movementBaseX!![0] + MapRegion.sceneBaseX)
											}
											MiniMenuInputTick.processMenuInput()
											for (shakeIndex in 0 until 5) {
												@Pc(1712) val local1712: Int = Shake.CAM_SHAKE_PHASE[shakeIndex]++
											}
											if (VarcTypeList.clientIsActive && Utils.currentTimeMillis() - TimeUtils.MILLISECONDS_PER_MINUTE > VarcTypeList.exitTime) {
												VarcTypeList.savePersistentVarcs()
											}
											@Pc(1741) var friendNotification: LoginStateUpdate? = client.PLAYER_LOGINSTATE_UPDATES!!.getFirst() as LoginStateUpdate?
											while (friendNotification != null) {
												if (Utils.currentTimeMillis() / 1000L - 5L > friendNotification.time.toLong()) {
													if (friendNotification.actionType > 0) {
														ChatLineWriter.writeMessage("", 5, 0, friendNotification.name + LocalizedText.HAS_LOGGED_IN.get(ClientState.language), "", "")
													}
													if (friendNotification.actionType.toInt() == 0) {
														ChatLineWriter.writeMessage("", 5, 0, friendNotification.name + LocalizedText.HAS_LOGGED_OUT.get(ClientState.language), "", "")
													}
													friendNotification.unlinkNode()
												}
												friendNotification = client.PLAYER_LOGINSTATE_UPDATES!!.getNext() as LoginStateUpdate?
											}
											Camera.cameraRandomisationCycle++
											if (Camera.cameraRandomisationCycle > 500) {
												Camera.cameraRandomisationCycle = 0
												val randomBits: Int = (util.random() * 8.0).toInt()
												if ((randomBits and 0x4) == 4) {
													Camera.cameraRandomisationA += Camera.cameraStepA
												}
												if ((randomBits and 0x1) == 1) {
													Camera.cameraRandomisationX += Camera.cameraStepH
												}
												if ((randomBits and 0x2) == 2) {
													Camera.cameraRandomisationV += Camera.cameraStepV
												}
											}
											if (Camera.cameraRandomisationX < -50) {
												Camera.cameraStepH = 2
											}
											if (Camera.cameraRandomisationX > 50) {
												Camera.cameraStepH = -2
											}
											if (Camera.cameraRandomisationV < -55) {
												Camera.cameraStepV = 2
											}
											if (Camera.cameraRandomisationA < -40) {
												Camera.cameraStepA = 1
											}
											if (Camera.cameraRandomisationV > 55) {
												Camera.cameraStepV = -2
											}
											Minimap.minimapRandomizationCycle++
											if (Camera.cameraRandomisationA > 40) {
												Camera.cameraStepA = -1
											}
											if (Minimap.minimapRandomizationCycle > 500) {
												Minimap.minimapRandomizationCycle = 0
												val randomBits: Int = (util.random() * 8.0).toInt()
												if ((randomBits and 0x2) == 2) {
													Minimap.minimapZoom += Minimap.minimapZoomStep
												}
												if ((randomBits and 0x1) == 1) {
													Minimap.minimapRotation += Minimap.minimapRotationStep
												}
											}
											if (Minimap.minimapRotation < -60) {
												Minimap.minimapRotationStep = 2
											}
											if (Minimap.minimapZoom < -20) {
												Minimap.minimapZoomStep = 1
											}
											if (Minimap.minimapRotation > 60) {
												Minimap.minimapRotationStep = -2
											}
											if (Minimap.minimapZoom > 10) {
												Minimap.minimapZoomStep = -1
											}
											BufferedConnectionContext.lastFlush++
											if (BufferedConnectionContext.lastFlush > 50) {
												@Pc(1970) val pingPacket: TcpPacket = TcpPacket.createPacket(ClientProt.PING, TcpPacket.isaac)
												BufferedConnectionContext.queuePacket(pingPacket)
											}
											if (PlayerVarDomain.varVerifyIdPending) {
												PlayerVarDomain.sendTransmitVarVerifyId()
												PlayerVarDomain.varVerifyIdPending = false
											}
											try {
												BufferedConnectionContext.flush()
												return
											} catch (@Pc(1985) ex: IOException) {
												ConnectionLostHandler.handleConnectionLost()
												return
											}
										}
										component3 = hookRequest!!.source
										if (component3!!.slotId < 0) {
											break
										}
										component = Interface.getComponent(component3.parentId)
									} while (component == null || component.slotChildren == null || component3!!.slotId >= component.slotChildren!!.size || component.slotChildren!![component3.slotId] !== component3)
									CS2Interpreter.executeHookInner(hookRequest!!)
								}
							}
							component3 = hookRequest!!.source
							if (component3!!.slotId < 0) {
								break
							}
							component = Interface.getComponent(component3.parentId)
						} while (component == null || component.slotChildren == null || component.slotChildren!!.size <= component3!!.slotId || component.slotChildren!![component3.slotId] !== component3)
						CS2Interpreter.executeHookInner(hookRequest!!)
					}
				}
				component3 = hookRequest!!.source
				if (component3!!.slotId < 0) {
					break
				}
				component = Interface.getComponent(component3.parentId)
			} while (component == null || component.slotChildren == null || component.slotChildren!!.size <= component3!!.slotId || component3 !== component.slotChildren!![component3.slotId])
			CS2Interpreter.executeHookInner(hookRequest!!)
		}
	}
}
