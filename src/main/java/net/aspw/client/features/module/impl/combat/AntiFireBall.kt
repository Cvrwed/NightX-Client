package net.aspw.client.features.module.impl.combat

import net.aspw.client.event.EventTarget
import net.aspw.client.event.UpdateEvent
import net.aspw.client.features.module.Module
import net.aspw.client.features.module.ModuleCategory
import net.aspw.client.features.module.ModuleInfo
import net.aspw.client.protocol.ProtocolBase
import net.aspw.client.util.RotationUtils
import net.aspw.client.util.misc.RandomUtils
import net.aspw.client.util.timer.MSTimer
import net.aspw.client.value.BoolValue
import net.aspw.client.value.FloatValue
import net.aspw.client.value.ListValue
import net.minecraft.entity.projectile.EntityFireball
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation
import net.raphimc.vialoader.util.VersionEnum
import java.util.*

@ModuleInfo(name = "AntiFireBall", spacedName = "Anti Fire Ball", description = "", category = ModuleCategory.COMBAT)
class AntiFireBall : Module() {
    private val timer = MSTimer()

    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val rotationValue = BoolValue("Rotation", true)
    private val maxTurnSpeed: FloatValue =
        object : FloatValue("MaxTurnSpeed", 120f, 0f, 180f, "°", { rotationValue.get() }) {
            override fun onChanged(oldValue: Float, newValue: Float) {
                val i = minTurnSpeed.get()
                if (i > newValue) set(i)
            }
        }
    private val minTurnSpeed: FloatValue =
        object : FloatValue("MinTurnSpeed", 80f, 0f, 180f, "°", { rotationValue.get() }) {
            override fun onChanged(oldValue: Float, newValue: Float) {
                val i = maxTurnSpeed.get()
                if (i < newValue) set(i)
            }
        }

    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityFireball && mc.thePlayer.getDistanceToEntity(entity) < 5.5 && timer.hasTimePassed(300)) {
                if (rotationValue.get()) {
                    RotationUtils.setTargetRotation(
                        RotationUtils.limitAngleChange(
                            RotationUtils.serverRotation!!,
                            RotationUtils.getRotations(entity),
                            RandomUtils.nextFloat(minTurnSpeed.get(), maxTurnSpeed.get())
                        )
                    )
                }

                if (ProtocolBase.getManager().targetVersion.protocol != VersionEnum.r1_8.protocol)
                    mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

                when (swingValue.get().lowercase(Locale.getDefault())) {
                    "normal" -> mc.thePlayer.swingItem()
                    "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
                }

                if (ProtocolBase.getManager().targetVersion.protocol == VersionEnum.r1_8.protocol)
                    mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

                timer.reset()
                break
            }
        }
    }
}