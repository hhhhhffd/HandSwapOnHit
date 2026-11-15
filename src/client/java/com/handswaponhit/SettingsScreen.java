package com.handswaponhit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;

final class SettingsScreen extends Screen {
    private final Screen parent;

    SettingsScreen(Screen parent) {
        super(Component.literal("HandSwapOnHit"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(width - 20, 220);
        int x = (width - w) / 2;
        int y = height / 2 - 46;
        var options = HandSwapOnHit.SETTINGS;
        button(x, y, w, state("handswaponhit.menu.enabled", options.enabled), b -> {
            HandSwapOnHit.setEnabled(!options.enabled);
            b.setMessage(state("handswaponhit.menu.enabled", options.enabled));
        });
        button(x, y + 24, w, state("handswaponhit.menu.notice", options.notice), b -> {
            options.notice = !options.notice;
            options.save();
            b.setMessage(state("handswaponhit.menu.notice", options.notice));
        });
        button(x, y + 48, w, currentSide(), b -> {
            HandSwapOnHit.setHand(Minecraft.getInstance(), minecraft.options.mainHand().get().getOpposite());
            b.setMessage(currentSide());
        });
        button(x, y + 72, w, Component.translatable("handswaponhit.menu.bind"), b ->
                minecraft.setScreen(new KeyBindsScreen(this, minecraft.options)));
        button(x, y + 101, w, Component.translatable("gui.done"), b -> onClose());
    }

    private void button(int x, int y, int w, Component label, Button.OnPress action) {
        addRenderableWidget(Button.builder(label, action).bounds(x, y, w, 20).build());
    }

    private Component currentSide() {
        HumanoidArm side = minecraft.options.mainHand().get();
        return Component.translatable("handswaponhit.menu.side", side.caption());
    }

    private static Component state(String key, boolean enabled) {
        return Component.translatable(key, Component.translatable(enabled ? "options.on" : "options.off"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, getTitle(), width / 2, Math.max(10, height / 2 - 75), 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("handswaponhit.menu.hint"),
                width / 2, Math.max(24, height / 2 - 61), 0xFFBBC9DA);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
