package com.handswaponhit;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

final class Settings {
    boolean enabled = true;
    boolean notice = true;

    private Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("handswaponhit.properties");
    }

    void load() {
        Path path = path();
        if (!Files.isRegularFile(path)) return;
        var properties = new Properties();
        try (var input = Files.newInputStream(path)) {
            properties.load(input);
            enabled = !"false".equalsIgnoreCase(properties.getProperty("enabled", "true"));
            notice = !"false".equalsIgnoreCase(properties.getProperty("notice", "true"));
        } catch (IOException | IllegalArgumentException e) {
            LoggerFactory.getLogger("HandSwapOnHit").warn("Could not load settings", e);
        }
    }

    void save() {
        Path file = path();
        var properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("notice", Boolean.toString(notice));
        try {
            Files.createDirectories(file.getParent());
            Path pending = Files.createTempFile(file.getParent(), "handswaponhit-", ".tmp");
            try {
                try (var out = Files.newOutputStream(pending)) {
                    properties.store(out, "HandSwapOnHit");
                }
                try {
                    Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(pending, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(pending);
            }
        } catch (IOException e) {
            LoggerFactory.getLogger("HandSwapOnHit").warn("Could not save settings", e);
        }
    }
}
