package net.aspw.client.injection.forge.mixins.entity;

import net.aspw.client.Client;
import net.aspw.client.event.*;
import net.aspw.client.features.api.PacketManager;
import net.aspw.client.features.module.impl.exploit.AntiDesync;
import net.aspw.client.features.module.impl.exploit.AntiHunger;
import net.aspw.client.features.module.impl.exploit.PortalMenu;
import net.aspw.client.features.module.impl.movement.Flight;
import net.aspw.client.features.module.impl.movement.NoSlow;
import net.aspw.client.features.module.impl.movement.SilentSneak;
import net.aspw.client.features.module.impl.movement.Sprint;
import net.aspw.client.features.module.impl.player.Scaffold;
import net.aspw.client.features.module.impl.visual.Interface;
import net.aspw.client.protocol.ProtocolBase;
import net.aspw.client.util.*;
import net.aspw.client.visual.client.GuiTeleportation;
import net.aspw.client.visual.client.clickgui.dropdown.ClickGui;
import net.aspw.client.visual.client.clickgui.tab.NewUi;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.raphimc.vialoader.util.VersionEnum;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;

/**
 * The type Mixin entity player sp.
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends MixinAbstractClientPlayer {

    @Unique
    private boolean viaForge$prevOnGround;

    /**
     * The Server sprint state.
     */
    @Shadow
    public boolean serverSprintState;
    /**
     * The Sprinting ticks left.
     */
    @Shadow
    public int sprintingTicksLeft;
    /**
     * The Time in portal.
     */
    @Shadow
    public float timeInPortal;
    /**
     * The Prev time in portal.
     */
    @Shadow
    public float prevTimeInPortal;
    /**
     * The Movement input.
     */
    @Shadow
    public MovementInput movementInput;
    /**
     * The Horse jump power.
     */
    @Shadow
    public float horseJumpPower;
    /**
     * The Horse jump power counter.
     */
    @Shadow
    public int horseJumpPowerCounter;
    /**
     * The Send queue.
     */
    @Shadow
    @Final
    public NetHandlerPlayClient sendQueue;
    /**
     * The Sprint toggle timer.
     */
    @Shadow
    protected int sprintToggleTimer;
    /**
     * The Mc.
     */
    @Shadow
    protected Minecraft mc;
    @Shadow
    private boolean serverSneakState;
    @Shadow
    private double lastReportedPosX;
    @Shadow
    public int positionUpdateTicks;
    @Shadow
    private double lastReportedPosY;
    @Shadow
    private double lastReportedPosZ;
    @Shadow
    private float lastReportedYaw;
    @Shadow
    private float lastReportedPitch;
    @Unique
    private boolean lastOnGround;

    /**
     * Play sound.
     *
     * @param name   the name
     * @param volume the volume
     * @param pitch  the pitch
     */
    @Shadow
    public abstract void playSound(String name, float volume, float pitch);

    /**
     * Sets sprinting.
     *
     * @param sprinting the sprinting
     */
    @Shadow
    public abstract void setSprinting(boolean sprinting);

    /**
     * Push out of blocks boolean.
     *
     * @param x the x
     * @param y the y
     * @param z the z
     * @return the boolean
     */
    @Shadow
    protected abstract boolean pushOutOfBlocks(double x, double y, double z);

    /**
     * Send player abilities.
     */
    @Shadow
    public abstract void sendPlayerAbilities();

    /**
     * Send horse jump.
     */
    @Shadow
    protected abstract void sendHorseJump();

    /**
     * Is riding horse boolean.
     *
     * @return the boolean
     */
    @Shadow
    public abstract boolean isRidingHorse();

    @Shadow
    public abstract boolean isSneaking();

    /**
     * Is current view entity boolean.
     *
     * @return the boolean
     * @author As_pw
     * @reason Fix Video
     */
    @Overwrite
    protected boolean isCurrentViewEntity() {
        final Flight flight = Objects.requireNonNull(Client.moduleManager.getModule(Flight.class));

        return (mc.getRenderViewEntity() != null && mc.getRenderViewEntity().equals(this)) || (Client.moduleManager != null && flight.getState());
    }

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/NetHandlerPlayClient;addToSendQueue(Lnet/minecraft/network/Packet;)V", ordinal = 7))
    public void emulateIdlePacket(NetHandlerPlayClient instance, Packet p_addToSendQueue_1_) {
        if (ProtocolBase.getManager().getTargetVersion().isNewerThan(VersionEnum.r1_8) && !MinecraftInstance.mc.isIntegratedServerRunning()) {
            if (this.viaForge$prevOnGround == this.onGround) {
                return;
            }
        }
        instance.addToSendQueue(p_addToSendQueue_1_);
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
    public void saveGroundState(CallbackInfo ci) {
        this.viaForge$prevOnGround = this.onGround;
    }

    /**
     * On update walking player.
     *
     * @author As_pw
     * @reason Update Event
     */
    @Overwrite
    public void onUpdateWalkingPlayer() {
        try {
            MotionEvent event = new MotionEvent(this.posX, this.getEntityBoundingBox().minY, this.posZ, this.rotationYaw, this.rotationPitch, this.onGround);
            Client.eventManager.callEvent(event);

            PacketManager.update();

            final AntiHunger antiHunger = Objects.requireNonNull(Client.moduleManager.getModule(AntiHunger.class));

            final SilentSneak sneak = Objects.requireNonNull(Client.moduleManager.getModule(SilentSneak.class));
            final boolean fakeSprint = antiHunger.getState() || (sneak.getState() && (!MovementUtils.isMoving()));

            ActionEvent actionEvent = new ActionEvent(this.isSprinting() && !fakeSprint, this.isSneaking());

            boolean sprinting = actionEvent.getSprinting();
            boolean sneaking = actionEvent.getSneaking();

            if (sprinting != this.serverSprintState) {
                if (sprinting)
                    this.sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, C0BPacketEntityAction.Action.START_SPRINTING));
                else
                    this.sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, C0BPacketEntityAction.Action.STOP_SPRINTING));

                this.serverSprintState = sprinting;
            }

            if (sneaking != this.serverSneakState && (!sneak.getState() || sneak.modeValue.get().equalsIgnoreCase("Legit"))) {
                if (sneaking)
                    this.sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, C0BPacketEntityAction.Action.START_SNEAKING));
                else
                    this.sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, C0BPacketEntityAction.Action.STOP_SNEAKING));

                this.serverSneakState = sneaking;
            }

            if (this.isCurrentViewEntity()) {
                float yaw = event.getYaw();
                float pitch = event.getPitch();
                float lastReportedYaw = RotationUtils.serverRotation.getYaw();
                float lastReportedPitch = RotationUtils.serverRotation.getPitch();

                if (RotationUtils.targetRotation != null) {
                    yaw = RotationUtils.targetRotation.getYaw();
                    pitch = RotationUtils.targetRotation.getPitch();
                }

                final AntiDesync antiDesync = Objects.requireNonNull(Client.moduleManager.getModule(AntiDesync.class));
                double xDiff = event.getX() - this.lastReportedPosX;
                double yDiff = event.getY() - this.lastReportedPosY;
                double zDiff = event.getZ() - this.lastReportedPosZ;
                double yawDiff = yaw - lastReportedYaw;
                double pitchDiff = pitch - lastReportedPitch;
                boolean moved = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff > (antiDesync.getState() ? 0D : 9.0E-4D) || this.positionUpdateTicks >= 20;
                boolean rotated = yawDiff != 0.0D || pitchDiff != 0.0D;

                if (this.ridingEntity == null) {
                    if (moved && rotated) {
                        this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(event.getX(), event.getY(), event.getZ(), yaw, pitch, event.getOnGround()));
                    } else if (moved) {
                        this.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(event.getX(), event.getY(), event.getZ(), event.getOnGround()));
                    } else if (rotated) {
                        this.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, event.getOnGround()));
                    } else {
                        this.sendQueue.addToSendQueue(new C03PacketPlayer(event.getOnGround()));
                    }
                } else {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(this.motionX, -999.0D, this.motionZ, yaw, pitch, event.getOnGround()));
                    moved = false;
                }

                ++this.positionUpdateTicks;

                if (moved) {
                    this.lastReportedPosX = event.getX();
                    this.lastReportedPosY = event.getY();
                    this.lastReportedPosZ = event.getZ();
                    this.positionUpdateTicks = 0;
                }

                if (rotated) {
                    this.lastReportedYaw = yaw;
                    this.lastReportedPitch = pitch;
                }
            }

            if (this.isCurrentViewEntity())
                lastOnGround = event.getOnGround();

            event.setEventState(EventState.POST);

            Client.eventManager.callEvent(event);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "swingItem", at = @At("HEAD"))
    private void swingItem(CallbackInfo callbackInfo) {
        CooldownHelper.INSTANCE.resetLastAttackedTicks();
        if (Objects.requireNonNull(Client.moduleManager.getModule(Interface.class)).getSwingSoundValue().get()) {
            Client.tipSoundManager.getSwingSound().asyncPlay(Client.moduleManager.getSwingSoundPower());
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocks(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        PushOutEvent event = new PushOutEvent();
        if (this.noClip) event.cancelEvent();
        Client.eventManager.callEvent(event);

        if (event.isCancelled())
            callbackInfoReturnable.setReturnValue(false);
    }

    /**
     * @author As_pw
     * @reason Fix Gui
     */
    @Overwrite
    public void onLivingUpdate() {
        Client.eventManager.callEvent(new UpdateEvent());
        if (mc.currentScreen instanceof NewUi || mc.currentScreen instanceof ClickGui || mc.currentScreen instanceof GuiTeleportation) {
            mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward);
            mc.gameSettings.keyBindBack.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindBack);
            mc.gameSettings.keyBindRight.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindRight);
            mc.gameSettings.keyBindLeft.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindLeft);
            mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump);
            mc.gameSettings.keyBindSprint.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindSprint);
        }

        if (this.sprintingTicksLeft > 0) {
            --this.sprintingTicksLeft;

            if (this.sprintingTicksLeft == 0) {
                this.setSprinting(false);
            }
        }

        if (this.sprintToggleTimer > 0) {
            --this.sprintToggleTimer;
        }

        this.prevTimeInPortal = this.timeInPortal;

        final PortalMenu portalMenu = Objects.requireNonNull(Client.moduleManager.getModule(PortalMenu.class));

        if (this.inPortal) {
            if (this.mc.currentScreen != null && !this.mc.currentScreen.doesGuiPauseGame()
                    && !portalMenu.getState()) {
                this.mc.displayGuiScreen(null);
            }

            if (this.timeInPortal == 0.0F) {
                this.mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("portal.trigger"), this.rand.nextFloat() * 0.4F + 0.8F));
            }

            this.timeInPortal += 0.0125F;

            if (this.timeInPortal >= 1.0F) {
                this.timeInPortal = 1.0F;
            }

            this.inPortal = false;
        } else if (this.isPotionActive(Potion.confusion) && this.getActivePotionEffect(Potion.confusion).getDuration() > 60) {
            this.timeInPortal += 0.006666667F;

            if (this.timeInPortal > 1.0F) {
                this.timeInPortal = 1.0F;
            }
        } else {
            if (this.timeInPortal > 0.0F) {
                this.timeInPortal -= 0.05F;
            }

            if (this.timeInPortal < 0.0F) {
                this.timeInPortal = 0.0F;
            }
        }

        if (this.timeUntilPortal > 0) {
            --this.timeUntilPortal;
        }

        boolean flag = this.movementInput.jump;
        boolean flag1 = this.movementInput.sneak;
        float f = 0.8F;
        boolean flag2 = this.movementInput.moveForward >= f;
        this.movementInput.updatePlayerMoveState();

        final NoSlow noSlow = Objects.requireNonNull(Client.moduleManager.getModule(NoSlow.class));

        if (getHeldItem() != null && (this.isUsingItem() || (getHeldItem().getItem() instanceof ItemSword && mc.thePlayer.isBlocking() && !this.isRiding()))) {
            final SlowDownEvent slowDownEvent = new SlowDownEvent(0.2F, 0.2F);
            Client.eventManager.callEvent(slowDownEvent);
            this.movementInput.moveStrafe *= slowDownEvent.getStrafe();
            this.movementInput.moveForward *= slowDownEvent.getForward();
            this.sprintToggleTimer = 0;
        }

        this.pushOutOfBlocks(this.posX - (double) this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ + (double) this.width * 0.35D);
        this.pushOutOfBlocks(this.posX - (double) this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ - (double) this.width * 0.35D);
        this.pushOutOfBlocks(this.posX + (double) this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ - (double) this.width * 0.35D);
        this.pushOutOfBlocks(this.posX + (double) this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ + (double) this.width * 0.35D);

        final Sprint sprint = Objects.requireNonNull(Client.moduleManager.getModule(Sprint.class));

        boolean flag3 = (float) this.getFoodStats().getFoodLevel() > 6.0F || this.capabilities.allowFlying;

        if (this.onGround && !flag1 && !flag2 && this.movementInput.moveForward >= f && !this.isSprinting() && flag3 && !this.isUsingItem() && !this.isPotionActive(Potion.blindness)) {
            if (this.sprintToggleTimer <= 0 && !this.mc.gameSettings.keyBindSprint.isKeyDown()) {
                this.sprintToggleTimer = 7;
            } else {
                this.setSprinting(true);
            }
        }

        if (!this.isSprinting() && this.movementInput.moveForward >= f && flag3 && (noSlow.getState() || !this.isUsingItem()) && !this.isPotionActive(Potion.blindness) && this.mc.gameSettings.keyBindSprint.isKeyDown())
            this.setSprinting(true);

        final Scaffold scaffold = Objects.requireNonNull(Client.moduleManager.getModule(Scaffold.class));

        if ((scaffold.getState() && scaffold.getCanTower() && scaffold.sprintModeValue.get().equalsIgnoreCase("Off")) || (scaffold.getState() && scaffold.sprintModeValue.get().equalsIgnoreCase("Off")) || !sprint.getAllDirectionsValue().get() && RotationUtils.targetRotation != null && RotationUtils.getRotationDifference(new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)) > 30)
            this.setSprinting(false);

        if (this.isSprinting() && ((!(sprint.getState() && sprint.getAllDirectionsValue().get()) && this.movementInput.moveForward < f) || mc.thePlayer.isCollidedHorizontally || !flag3))
            this.setSprinting(false);

        if (this.capabilities.allowFlying) {
            if (this.mc.playerController.isSpectatorMode()) {
                if (!this.capabilities.isFlying) {
                    this.capabilities.isFlying = true;
                    this.sendPlayerAbilities();
                }
            } else if (!flag && this.movementInput.jump) {
                if (this.flyToggleTimer == 0) {
                    this.flyToggleTimer = 7;
                } else {
                    this.capabilities.isFlying = !this.capabilities.isFlying;
                    this.sendPlayerAbilities();
                    this.flyToggleTimer = 0;
                }
            }
        }

        if (this.capabilities.isFlying && this.isCurrentViewEntity()) {
            if (this.movementInput.sneak) {
                this.motionY -= this.capabilities.getFlySpeed() * 3.0F;
            }

            if (this.movementInput.jump) {
                this.motionY += this.capabilities.getFlySpeed() * 3.0F;
            }
        }

        if (this.isRidingHorse()) {
            if (this.horseJumpPowerCounter < 0) {
                ++this.horseJumpPowerCounter;

                if (this.horseJumpPowerCounter == 0) {
                    this.horseJumpPower = 0.0F;
                }
            }

            if (flag && !this.movementInput.jump) {
                this.horseJumpPowerCounter = -10;
                this.sendHorseJump();
            } else if (!flag && this.movementInput.jump) {
                this.horseJumpPowerCounter = 0;
                this.horseJumpPower = 0.0F;
            } else if (flag) {
                ++this.horseJumpPowerCounter;

                if (this.horseJumpPowerCounter < 10) {
                    this.horseJumpPower = (float) this.horseJumpPowerCounter * 0.1F;
                } else {
                    this.horseJumpPower = 0.8F + 2.0F / (float) (this.horseJumpPowerCounter - 9) * 0.1F;
                }
            }
        } else {
            this.horseJumpPower = 0.0F;
        }

        super.onLivingUpdate();

        if (this.onGround && this.capabilities.isFlying && !this.mc.playerController.isSpectatorMode()) {
            this.capabilities.isFlying = false;
            this.sendPlayerAbilities();
        }
    }

    @Override
    public void moveEntity(double x, double y, double z) {
        MoveEvent moveEvent = new MoveEvent(x, y, z);
        Client.eventManager.callEvent(moveEvent);

        if (moveEvent.isCancelled())
            return;

        x = moveEvent.getX();
        y = moveEvent.getY();
        z = moveEvent.getZ();

        if (this.noClip) {
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, y, z));
            this.posX = (this.getEntityBoundingBox().minX + this.getEntityBoundingBox().maxX) / 2.0D;
            this.posY = this.getEntityBoundingBox().minY;
            this.posZ = (this.getEntityBoundingBox().minZ + this.getEntityBoundingBox().maxZ) / 2.0D;
        } else {
            this.worldObj.theProfiler.startSection("move");
            double d0 = this.posX;
            double d1 = this.posY;
            double d2 = this.posZ;

            if (this.isInWeb) {
                this.isInWeb = false;
                x *= 0.25D;
                y *= 0.05000000074505806D;
                z *= 0.25D;
                this.motionX = 0.0D;
                this.motionY = 0.0D;
                this.motionZ = 0.0D;
            }

            double d3 = x;
            double d4 = y;
            double d5 = z;
            boolean flag = this.onGround && this.isSneaking();

            if (flag || moveEvent.isSafeWalk()) {
                double d6;

                for (d6 = 0.05D; x != 0.0D && this.worldObj.getCollidingBoundingBoxes((Entity) (Object) this, this.getEntityBoundingBox().offset(x, -1.0D, 0.0D)).isEmpty(); d3 = x) {
                    if (x < d6 && x >= -d6) {
                        x = 0.0D;
                    } else if (x > 0.0D) {
                        x -= d6;
                    } else {
                        x += d6;
                    }
                }

                for (; z != 0.0D && this.worldObj.getCollidingBoundingBoxes((Entity) (Object) this, this.getEntityBoundingBox().offset(0.0D, -1.0D, z)).isEmpty(); d5 = z) {
                    if (z < d6 && z >= -d6) {
                        z = 0.0D;
                    } else if (z > 0.0D) {
                        z -= d6;
                    } else {
                        z += d6;
                    }
                }

                for (; x != 0.0D && z != 0.0D && this.worldObj.getCollidingBoundingBoxes((Entity) (Object) this, this.getEntityBoundingBox().offset(x, -1.0D, z)).isEmpty(); d5 = z) {
                    if (x < d6 && x >= -d6) {
                        x = 0.0D;
                    } else if (x > 0.0D) {
                        x -= d6;
                    } else {
                        x += d6;
                    }

                    d3 = x;

                    if (z < d6 && z >= -d6) {
                        z = 0.0D;
                    } else if (z > 0.0D) {
                        z -= d6;
                    } else {
                        z += d6;
                    }
                }
            }

            List<AxisAlignedBB> list1 = this.worldObj.getCollidingBoundingBoxes((Entity) (Object) this, this.getEntityBoundingBox().addCoord(x, y, z));
            AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();

            for (AxisAlignedBB axisalignedbb1 : list1) {
                y = axisalignedbb1.calculateYOffset(this.getEntityBoundingBox(), y);
            }

            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, y, 0.0D));
            boolean flag1 = this.onGround || d4 != y && d4 < 0.0D;

            for (AxisAlignedBB axisalignedbb2 : list1) {
                x = axisalignedbb2.calculateXOffset(this.getEntityBoundingBox(), x);
            }

            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0D, 0.0D));

            for (AxisAlignedBB axisalignedbb13 : list1) {
                z = axisalignedbb13.calculateZOffset(this.getEntityBoundingBox(), z);
            }

            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, 0.0D, z));

            if (this.stepHeight > 0.0F && flag1 && (d3 != x || d5 != z)) {
                StepEvent stepEvent = new StepEvent(this.stepHeight);
                Client.eventManager.callEvent(stepEvent);
                double d11 = x;
                double d7 = y;
                double d8 = z;
                AxisAlignedBB axisalignedbb3 = this.getEntityBoundingBox();
                this.setEntityBoundingBox(axisalignedbb);
                y = stepEvent.getStepHeight();
                List<AxisAlignedBB> list = this.worldObj.getCollidingBoundingBoxes((Entity) (Object) this, this.getEntityBoundingBox().addCoord(d3, y, d5));
                AxisAlignedBB axisalignedbb4 = this.getEntityBoundingBox();
                AxisAlignedBB axisalignedbb5 = axisalignedbb4.addCoord(d3, 0.0D, d5);
                double d9 = y;

                for (AxisAlignedBB axisalignedbb6 : list) {
                    d9 = axisalignedbb6.calculateYOffset(axisalignedbb5, d9);
                }

                axisalignedbb4 = axisalignedbb4.offset(0.0D, d9, 0.0D);
                double d15 = d3;

                for (AxisAlignedBB axisalignedbb7 : list) {
                    d15 = axisalignedbb7.calculateXOffset(axisalignedbb4, d15);
                }

                axisalignedbb4 = axisalignedbb4.offset(d15, 0.0D, 0.0D);
                double d16 = d5;

                for (AxisAlignedBB axisalignedbb8 : list) {
                    d16 = axisalignedbb8.calculateZOffset(axisalignedbb4, d16);
                }

                axisalignedbb4 = axisalignedbb4.offset(0.0D, 0.0D, d16);
                AxisAlignedBB axisalignedbb14 = this.getEntityBoundingBox();
                double d17 = y;

                for (AxisAlignedBB axisalignedbb9 : list) {
                    d17 = axisalignedbb9.calculateYOffset(axisalignedbb14, d17);
                }

                axisalignedbb14 = axisalignedbb14.offset(0.0D, d17, 0.0D);
                double d18 = d3;

                for (AxisAlignedBB axisalignedbb10 : list) {
                    d18 = axisalignedbb10.calculateXOffset(axisalignedbb14, d18);
                }

                axisalignedbb14 = axisalignedbb14.offset(d18, 0.0D, 0.0D);
                double d19 = d5;

                for (AxisAlignedBB axisalignedbb11 : list) {
                    d19 = axisalignedbb11.calculateZOffset(axisalignedbb14, d19);
                }

                axisalignedbb14 = axisalignedbb14.offset(0.0D, 0.0D, d19);
                double d20 = d15 * d15 + d16 * d16;
                double d10 = d18 * d18 + d19 * d19;

                if (d20 > d10) {
                    x = d15;
                    z = d16;
                    y = -d9;
                    this.setEntityBoundingBox(axisalignedbb4);
                } else {
                    x = d18;
                    z = d19;
                    y = -d17;
                    this.setEntityBoundingBox(axisalignedbb14);
                }

                for (AxisAlignedBB axisalignedbb12 : list) {
                    y = axisalignedbb12.calculateYOffset(this.getEntityBoundingBox(), y);
                }

                this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, y, 0.0D));

                if (d11 * d11 + d8 * d8 >= x * x + z * z) {
                    x = d11;
                    y = d7;
                    z = d8;
                    this.setEntityBoundingBox(axisalignedbb3);
                } else {
                    Client.eventManager.callEvent(new StepConfirmEvent());
                }
            }

            this.worldObj.theProfiler.endSection();
            this.worldObj.theProfiler.startSection("rest");
            this.posX = (this.getEntityBoundingBox().minX + this.getEntityBoundingBox().maxX) / 2.0D;
            this.posY = this.getEntityBoundingBox().minY;
            this.posZ = (this.getEntityBoundingBox().minZ + this.getEntityBoundingBox().maxZ) / 2.0D;
            this.isCollidedHorizontally = d3 != x || d5 != z;
            this.isCollidedVertically = d4 != y;
            this.onGround = this.isCollidedVertically && d4 < 0.0D;
            this.isCollided = this.isCollidedHorizontally || this.isCollidedVertically;
            int i = MathHelper.floor_double(this.posX);
            int j = MathHelper.floor_double(this.posY - 0.20000000298023224D);
            int k = MathHelper.floor_double(this.posZ);
            BlockPos blockpos = new BlockPos(i, j, k);
            Block block1 = this.worldObj.getBlockState(blockpos).getBlock();

            if (block1.getMaterial() == Material.air) {
                Block block = this.worldObj.getBlockState(blockpos.down()).getBlock();

                if (block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockFenceGate) {
                    block1 = block;
                    blockpos = blockpos.down();
                }
            }

            this.updateFallState(y, this.onGround, block1, blockpos);

            if (d3 != x) {
                this.motionX = 0.0D;
            }

            if (d5 != z) {
                this.motionZ = 0.0D;
            }

            if (d4 != y) {
                block1.onLanded(this.worldObj, (Entity) (Object) this);
            }

            if (this.canTriggerWalking() && !flag && this.ridingEntity == null) {
                double d12 = this.posX - d0;
                double d13 = this.posY - d1;
                double d14 = this.posZ - d2;

                if (block1 != Blocks.ladder) {
                    d13 = 0.0D;
                }

                if (block1 != null && this.onGround) {
                    block1.onEntityCollidedWithBlock(this.worldObj, blockpos, (Entity) (Object) this);
                }

                this.distanceWalkedModified = (float) ((double) this.distanceWalkedModified + (double) MathHelper.sqrt_double(d12 * d12 + d14 * d14) * 0.6D);
                this.distanceWalkedOnStepModified = (float) ((double) this.distanceWalkedOnStepModified + (double) MathHelper.sqrt_double(d12 * d12 + d13 * d13 + d14 * d14) * 0.6D);

                if (this.distanceWalkedOnStepModified > (float) getNextStepDistance() && block1.getMaterial() != Material.air) {
                    setNextStepDistance((int) this.distanceWalkedOnStepModified + 1);

                    if (this.isInWater()) {
                        float f = MathHelper.sqrt_double(this.motionX * this.motionX * 0.20000000298023224D + this.motionY * this.motionY + this.motionZ * this.motionZ * 0.20000000298023224D) * 0.35F;

                        if (f > 1.0F) {
                            f = 1.0F;
                        }

                        this.playSound(this.getSwimSound(), f, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                    }

                    this.playStepSound(blockpos, block1);
                }
            }

            try {
                this.doBlockCollisions();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Checking entity block collision");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
                this.addEntityCrashInfo(crashreportcategory);
                throw new ReportedException(crashreport);
            }

            boolean flag2 = this.isWet();

            if (this.worldObj.isFlammableWithin(this.getEntityBoundingBox().contract(0.001D, 0.001D, 0.001D))) {
                this.dealFireDamage(1);

                if (!flag2) {
                    setFire(getFire() + 1);

                    if (getFire() == 0) {
                        this.setFire(8);
                    }
                }
            } else if (getFire() <= 0) {
                setFire(-this.fireResistance);
            }

            if (flag2 && getFire() > 0) {
                this.playSound("random.fizz", 0.7F, 1.6F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                setFire(-this.fireResistance);
            }

            this.worldObj.theProfiler.endSection();
        }
    }
}
