package net.aspw.client.injection.forge.mixins.network;

import io.netty.channel.Channel;
import net.aspw.client.protocol.ProtocolBase;
import net.aspw.client.protocol.api.VFNetworkManager;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.NetworkManager$5")
public class MixinNetworkManager_5 {

    @Final
    @Mutable
    NetworkManager val$networkmanager;

    @Inject(method = "initChannel", at = @At(value = "TAIL"), remap = false)
    private void onInitChannel(Channel channel, CallbackInfo ci) {
        ProtocolBase.getManager().inject(channel, (VFNetworkManager) val$networkmanager);
    }
}