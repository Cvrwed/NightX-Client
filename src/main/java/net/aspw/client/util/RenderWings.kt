package net.aspw.client.util

import net.minecraft.client.model.ModelBase
import net.minecraft.client.model.ModelRenderer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import kotlin.math.cos
import kotlin.math.sin

class RenderWings : ModelBase() {
    private val location: ResourceLocation = ResourceLocation("client/models/wing.png")
    private val wing: ModelRenderer
    private val wingTip: ModelRenderer
    private val playerUsesFullHeight: Boolean = true

    init {
        setTextureOffset("wing.bone", 0, 0)
        setTextureOffset("wing.skin", -10, 8)
        setTextureOffset("wingtip.bone", 0, 5)
        setTextureOffset("wingtip.skin", -10, 18)
        wing = ModelRenderer(this, "wing")
        wing.setTextureSize(30, 30)
        wing.setRotationPoint(-2.0f, 0.0f, 0.0f)
        wing.addBox("bone", -10.0f, -1.0f, -1.0f, 10, 2, 2)
        wing.addBox("skin", -10.0f, 0.0f, 0.5f, 10, 0, 10)
        wingTip = ModelRenderer(this, "wingtip")
        wingTip.setTextureSize(30, 30)
        wingTip.setRotationPoint(-10.0f, 0.0f, 0.0f)
        wingTip.addBox("bone", -10.0f, -0.5f, -0.5f, 10, 1, 1)
        wingTip.addBox("skin", -10.0f, 0.0f, 0.5f, 10, 0, 10)
        wing.addChild(wingTip)
    }

    fun renderWings(partialTicks: Float) {
        val scale = 100 / 100.0
        val rotate = interpolate(
            MinecraftInstance.mc.thePlayer.prevRenderYawOffset,
            MinecraftInstance.mc.thePlayer.renderYawOffset,
            partialTicks
        )
        GL11.glPushMatrix()
        GL11.glScaled(-scale, -scale, scale)
        GL11.glRotated(180.0 + rotate, 0.0, 1.0, 0.0)
        GL11.glTranslated(0.0, -if (playerUsesFullHeight) 1.45 else 1.25 / scale, 0.0)
        GL11.glTranslated(0.0, 0.0, 0.2 / scale)
        if (MinecraftInstance.mc.thePlayer.isSneaking) {
            GL11.glTranslated(0.0, 0.125 / scale, 0.0)
        }
        GL11.glColor3f(0.9f, 0.9f, 0.9f)
        MinecraftInstance.mc.textureManager.bindTexture(location)
        for (j in 0..1) {
            GL11.glEnable(2884)
            val f11 = (System.currentTimeMillis() % 1000L).toFloat() / 1000.0f * 3.1415927f * 2.0f
            wing.rotateAngleX = Math.toRadians(-10.0).toFloat() - cos(f11.toDouble()).toFloat() * -0.6f
            wing.rotateAngleY = Math.toRadians(30.0).toFloat() + sin(f11.toDouble()).toFloat() * 0.2f
            wing.rotateAngleZ = Math.toRadians(30.0).toFloat()
            wingTip.rotateAngleZ = -(sin((f11 + 1.2f).toDouble()) + 1.1).toFloat() * 0.75f
            wing.render(0.0525f)
            GL11.glScalef(-1.0f, 1.0f, 1.0f)
            if (j == 0) {
                GL11.glCullFace(1028)
            }
        }
        GL11.glCullFace(1029)
        GL11.glDisable(2884)
        GL11.glColor3f(255.0f, 255.0f, 255.0f)
        GL11.glPopMatrix()
    }

    private fun interpolate(yaw1: Float, yaw2: Float, percent: Float): Double {
        var f = (yaw1 + (yaw2 - yaw1) * percent) % 360.0
        if (f < 0.0f) {
            f += 360.0
        }
        return f
    }
}