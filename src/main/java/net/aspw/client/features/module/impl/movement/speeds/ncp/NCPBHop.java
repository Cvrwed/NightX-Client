package net.aspw.client.features.module.impl.movement.speeds.ncp;

import net.aspw.client.Client;
import net.aspw.client.event.MoveEvent;
import net.aspw.client.features.module.impl.movement.speeds.SpeedMode;
import net.aspw.client.features.module.impl.player.Scaffold;
import net.aspw.client.util.MovementUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovementInput;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The type Ncpb hop.
 */
public class NCPBHop extends SpeedMode {

    private int level = 1;
    private double moveSpeed = 0.2873;
    private double lastDist;

    /**
     * Instantiates a new Ncpb hop.
     */
    public NCPBHop() {
        super("NCPBHop");
    }

    @Override
    public void onEnable() {
        level = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, mc.thePlayer.motionY, 0.0)).size() > 0 || mc.thePlayer.isCollidedVertically ? 1 : 4;
    }

    @Override
    public void onDisable() {
        moveSpeed = getBaseMoveSpeed();
        level = 0;

        final Scaffold scaffold = Client.moduleManager.getModule(Scaffold.class);

        if (!mc.thePlayer.isSneaking() && !scaffold.getState()) {
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    @Override
    public void onMotion() {
        double xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX;
        double zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
        lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onMove(MoveEvent event) {
        if (MovementUtils.isMoving()) {
            mc.thePlayer.motionX *= 1.0199999809265137;
            mc.thePlayer.motionZ *= 1.0199999809265137;
        }

        if (mc.thePlayer.onGround && MovementUtils.isMoving())
            level = 2;

        if (round(mc.thePlayer.posY - (double) ((int) mc.thePlayer.posY)) == round(0.138)) {
            EntityPlayerSP thePlayer = mc.thePlayer;
            thePlayer.motionY -= 0.08;
            event.setY(event.getY() - 0.09316090325960147);
            thePlayer.posY -= 0.09316090325960147;
        }

        if (level == 1 && (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f)) {
            level = 2;
            moveSpeed = 1.35 * getBaseMoveSpeed() - 0.01;
        } else if (level == 2) {
            level = 3;
            mc.thePlayer.motionY = 0.399399995803833;
            event.setY(0.399399995803833);
            moveSpeed *= 2.149;
        } else if (level == 3) {
            level = 4;
            double difference = 0.66 * (lastDist - getBaseMoveSpeed());
            moveSpeed = lastDist - difference;
        } else {
            if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, mc.thePlayer.motionY, 0.0)).size() > 0 || mc.thePlayer.isCollidedVertically)
                level = 1;

            moveSpeed = lastDist - lastDist / 159.0;
        }

        moveSpeed = Math.max(moveSpeed, getBaseMoveSpeed());
        final MovementInput movementInput = mc.thePlayer.movementInput;
        float forward = movementInput.moveForward;
        float strafe = movementInput.moveStrafe;
        float yaw = mc.thePlayer.rotationYaw;
        if (forward == 0.0f && strafe == 0.0f) {
            event.setX(0.0);
            event.setZ(0.0);
        } else if (forward != 0.0f) {
            if (strafe >= 1.0f) {
                yaw += (float) (forward > 0.0f ? -45 : 45);
                strafe = 0.0f;
            } else if (strafe <= -1.0f) {
                yaw += (float) (forward > 0.0f ? 45 : -45);
                strafe = 0.0f;
            }
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }

        final double mx2 = Math.cos(Math.toRadians(yaw + 90.0f));
        final double mz2 = Math.sin(Math.toRadians(yaw + 90.0f));
        event.setX((double) forward * moveSpeed * mx2 + (double) strafe * moveSpeed * mz2);
        event.setZ((double) forward * moveSpeed * mz2 - (double) strafe * moveSpeed * mx2);

        mc.thePlayer.stepHeight = 0.6F;
        if (forward == 0.0F && strafe == 0.0F) {
            event.setX(0.0);
            event.setZ(0.0);
        }
    }

    private double getBaseMoveSpeed() {
        double baseSpeed = 0.2873;
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed))
            baseSpeed *= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        return baseSpeed;
    }

    private double round(double value) {
        BigDecimal bigDecimal = new BigDecimal(value);
        bigDecimal = bigDecimal.setScale(3, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }
}
