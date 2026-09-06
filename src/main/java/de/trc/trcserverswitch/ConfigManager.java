package de.trc.trcserverswitch;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ConfigManager {

    private final ConfigurationNode config;

    public ConfigManager(Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);

        Path file = dataDirectory.resolve("config.yml");

        if (Files.notExists(file)) {
            Files.copy(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.yml")), file);
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(file).build();
        config = loader.load();
    }

    public String message(String key) {
        return config.node("messages", key).getString("");
    }

    public String message(String key, String... replacements) {
        String rawMessage = message(key);

        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be present as a Pair (Key, Value)!");
        }

        for (int i = 0; i < replacements.length; i += 2) {
            String target = replacements[i];
            String replacement = replacements[i + 1];
            if (target != null && replacement != null) {
                rawMessage = rawMessage.replace(target, replacement);
            }
        }

        return rawMessage;
    }
}
