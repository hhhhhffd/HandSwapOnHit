package com.handswaponhit;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public final class HandSwapOnHit implements ClientModInitializer {
    private static final long HIT_TIMEOUT = Duration.ofSeconds(3).toNanos();
    private static final Map<Integer, Long> ATTEMPTS = new HashMap<>();
    static final Settings SETTINGS = new Settings();
    private static ClientLevel world;
    private static int saveDelay = -1;

    @Override
    public void onInitializeClient() {
        SETTINGS.load();
        var group = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("handswaponhit", "controls"));
        var toggle = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.handswaponhit.toggle", GLFW.GLFW_KEY_H, group));
        AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (level.isClientSide() && SETTINGS.enabled && hand == InteractionHand.MAIN_HAND
                    && player == Minecraft.getInstance().player && !player.isSpectator()
                    && target instanceof Player && target != player && target.isAlive()) {
                rememberWorld(Minecraft.getInstance().level);
                ATTEMPTS.put(target.getId(), System.nanoTime());
            }
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            rememberWorld(client.level);
            if (!ATTEMPTS.isEmpty()) {
                long now = System.nanoTime();
                ATTEMPTS.values().removeIf(at -> now - at > HIT_TIMEOUT);
            }
            while (toggle.consumeClick()) {
                setEnabled(!SETTINGS.enabled);
                if (SETTINGS.notice && client.player != null) {
                    client.gui.setOverlayMessage(Component.translatable(SETTINGS.enabled
                            ? "handswaponhit.notice.enabled" : "handswaponhit.notice.disabled"), false);
                }
            }
            if (saveDelay > 0 && --saveDelay == 0) flush(client);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> clearWorld());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            clearWorld();
            flush(client);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(HandSwapOnHit::flush);
    }

    private static void rememberWorld(ClientLevel level) {
        if (world != level) {
            ATTEMPTS.clear();
            world = level;
        }
    }

    private static void clearWorld() {
        ATTEMPTS.clear();
        world = null;
    }

    public static void confirmDamage(ClientboundDamageEventPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (!SETTINGS.enabled || client.player == null || client.level == null || client.player.isSpectator()) return;
        rememberWorld(client.level);
        int me = client.player.getId();
        if (packet.entityId() == me || packet.sourceCauseId() != me || packet.sourceDirectId() != me) return;
        if (!(client.level.getEntity(packet.entityId()) instanceof Player)) return;
        var type = packet.sourceType();
        if (!type.is(DamageTypes.PLAYER_ATTACK) && !type.is(DamageTypes.MACE_SMASH) && !type.is(DamageTypes.SPEAR)) return;
        Long attack = ATTEMPTS.remove(packet.entityId());
        if (attack == null || System.nanoTime() - attack > HIT_TIMEOUT) return;
        setHand(client, client.options.mainHand().get().getOpposite());
    }

    static void setEnabled(boolean enabled) {
        SETTINGS.enabled = enabled;
        ATTEMPTS.clear();
        SETTINGS.save();
    }

    static void setHand(Minecraft client, HumanoidArm side) {
        client.options.mainHand().set(side);
        // The renderer reads the local avatar's synchronized arm, not Options directly.
        if (client.player != null) client.player.setMainArm(side);
        saveDelay = 12;
    }

    private static void flush(Minecraft client) {
        if (saveDelay < 0) return;
        saveDelay = -1;
        client.options.save();
    }
}
