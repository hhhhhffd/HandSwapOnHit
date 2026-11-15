package com.handswaponhit.mixin;

import com.handswaponhit.HandSwapOnHit;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class DamageEventMixin {
    @Inject(method = "handleDamageEvent", at = @At("RETURN"))
    private void handswaponhit$onDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        HandSwapOnHit.confirmDamage(packet);
    }
}
