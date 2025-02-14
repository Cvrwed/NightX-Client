package net.aspw.client.features.module.impl.combat

import net.aspw.client.event.EventTarget
import net.aspw.client.event.UpdateEvent
import net.aspw.client.features.module.Module
import net.aspw.client.features.module.ModuleCategory
import net.aspw.client.features.module.ModuleInfo
import net.aspw.client.util.EntityUtils
import net.aspw.client.util.RotationUtils
import net.aspw.client.value.BoolValue
import net.aspw.client.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemBow
import java.util.*

@ModuleInfo(name = "BowAura", spacedName = "Bow Aura", description = "", category = ModuleCategory.COMBAT)
class BowAura : Module() {

    private val silentValue = BoolValue("Silent", true)
    private val throughWallsValue = BoolValue("ThroughWalls", false)
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "Direction"), "Direction")

    private var target: Entity? = null

    override fun onDisable() {
        target = null
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        target = null

        if (mc.thePlayer.itemInUse?.item is ItemBow) {
            val entity = getTarget(throughWallsValue.get(), priorityValue.get()) ?: return

            target = entity
            RotationUtils.faceBow(target!!, silentValue.get(), false, 5f)
        }
    }

    private fun getTarget(throughWalls: Boolean, priorityMode: String): Entity? {
        val targets = mc.theWorld.loadedEntityList.filter {
            it is EntityLivingBase && EntityUtils.isSelected(it, true) &&
                    (throughWalls || mc.thePlayer.canEntityBeSeen(it))
        }

        return when (priorityMode.uppercase(Locale.getDefault())) {
            "DISTANCE" -> targets.minByOrNull { mc.thePlayer.getDistanceToEntity(it) }
            "DIRECTION" -> targets.minByOrNull { RotationUtils.getRotationDifference(it) }
            "HEALTH" -> targets.minByOrNull { (it as EntityLivingBase).health }
            else -> null
        }
    }

    fun hasTarget() = target != null && mc.thePlayer.canEntityBeSeen(target)
}