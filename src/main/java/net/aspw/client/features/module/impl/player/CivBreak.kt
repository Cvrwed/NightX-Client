package net.aspw.client.features.module.impl.player

import net.aspw.client.event.*
import net.aspw.client.features.module.Module
import net.aspw.client.features.module.ModuleCategory
import net.aspw.client.features.module.ModuleInfo
import net.aspw.client.util.PacketUtils
import net.aspw.client.util.RotationUtils
import net.aspw.client.util.block.BlockUtils
import net.aspw.client.util.render.RenderUtils
import net.aspw.client.value.BoolValue
import net.aspw.client.value.FloatValue
import net.aspw.client.value.IntegerValue
import net.aspw.client.value.ListValue
import net.minecraft.block.BlockAir
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import java.awt.Color
import java.util.*

@ModuleInfo(name = "CivBreak", spacedName = "Civ Break", description = "", category = ModuleCategory.PLAYER)
class CivBreak : Module() {

    private var blockPos: BlockPos? = null
    private var enumFacing: EnumFacing? = null
    private var isBreaking = false

    private val modeValue = ListValue("Mode", arrayOf("Instant", "Legit"), "Instant")
    private val delayValue = IntegerValue("Instant-Delay", 1, 1, 20) { modeValue.get().equals("instant", true) }
    private val range = FloatValue("Range", 5F, 1F, 6F)
    private val rotationsValue = BoolValue("Rotations", true)
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Packet")
    private val airNoSlow = BoolValue("Air-NoSlow", false)
    private val airResetValue = BoolValue("Air-Reset", false)
    private val rangeResetValue = BoolValue("Range-Reset", false)
    private val redValue = IntegerValue("R", 255, 0, 255)
    private val greenValue = IntegerValue("G", 255, 0, 255)
    private val blueValue = IntegerValue("B", 255, 0, 255)
    private val outLine = BoolValue("Outline", true)

    override val tag: String
        get() = modeValue.get()

    @EventTarget
    fun onBlockClick(event: ClickBlockEvent) {
        blockPos = event.clickedBlock
        enumFacing = event.enumFacing
    }

    override fun onEnable() {
        isBreaking = false
    }

    override fun onDisable() {
        blockPos ?: return
        blockPos = null
        isBreaking = false
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val pos = blockPos ?: return

        if (airResetValue.get() && BlockUtils.getBlock(pos) is BlockAir ||
            rangeResetValue.get() && BlockUtils.getCenterDistance(pos) > range.get()
        ) {
            blockPos = null
            isBreaking = false
            return
        }

        if (BlockUtils.getBlock(pos) is BlockAir || BlockUtils.getCenterDistance(pos) > range.get()) {
            isBreaking = false
            return
        }

        if (blockPos !== null) {
            isBreaking = true
            if (airNoSlow.get())
                event.onGround = true
        }

        if (blockPos == null) {
            isBreaking = false
        }

        when (event.eventState) {
            EventState.PRE -> if (rotationsValue.get()) {
                RotationUtils.setTargetRotation((RotationUtils.faceBlock(pos) ?: return).rotation)
            }

            EventState.POST -> {
                if (modeValue.get() == "Instant") {
                    if (mc.thePlayer.ticksExisted % delayValue.get() == 0) {
                        when (swingValue.get().lowercase(Locale.getDefault())) {
                            "normal" -> mc.thePlayer.swingItem()
                            "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
                        }
                        PacketUtils.sendPacketNoEvent(
                            C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                                blockPos,
                                enumFacing
                            )
                        )
                        PacketUtils.sendPacketNoEvent(
                            C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                                blockPos,
                                enumFacing
                            )
                        )
                    }
                }
                if (modeValue.get() == "Legit") {
                    when (swingValue.get().lowercase(Locale.getDefault())) {
                        "normal" -> mc.thePlayer.swingItem()
                        "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
                    }
                    mc.playerController.onPlayerDamageBlock(blockPos, enumFacing)
                }
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        RenderUtils.drawBlockBox(
            blockPos ?: return,
            Color(redValue.get(), greenValue.get(), blueValue.get()),
            outLine.get()
        )
    }
}
