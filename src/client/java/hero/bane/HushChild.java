package hero.bane;

import hero.bane.command.HushChildCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.sound.SoundCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class HushChild implements ClientModInitializer {
	private static final Path CONFIG_PATH = Paths.get(MinecraftClient.getInstance().runDirectory.getPath(), "config", "hushchild.txt");
	private static final Logger LOGGER = LoggerFactory.getLogger(HushChild.class);
	private static final Map<String, String> serverWorldMap = new HashMap<>();
	private String lastServer = "";
	private String currentWorld = "";
	private static float previousVolume = 1.0f;
	public static boolean isMuted = false;
	private static MinecraftClient client;

	@Override
	public void onInitializeClient() {
		if (Files.exists(CONFIG_PATH)) {
			try {
				List<String> lines = Files.readAllLines(CONFIG_PATH);
				for (String line : lines) {
					String[] parts = line.split(" ", 2);
					if (parts.length == 2) {
						serverWorldMap.put(parts[0], parts[1]);
					}
				}
			} catch (IOException e) {
				LOGGER.error("Failed to load config", e);
			}
		}

		client = MinecraftClient.getInstance();

		// Join server event
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (client.getCurrentServerEntry() != null) {
				lastServer = client.getCurrentServerEntry().address;
			} else {
				lastServer = "";
			}

			if (client.world != null) {
				currentWorld = client.world.getRegistryKey().getValue().toString();
			} else {
				currentWorld = "";
			}
			applyHushIfNeeded();
		});

		// Leave server event
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			if (isMuted) {
				restoreVolume();
			}
			lastServer = "";
			currentWorld = "";
		});

		// World change event
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world != null) {
				String newWorld = client.world.getRegistryKey().getValue().toString();
				if (!newWorld.equals(currentWorld)) {
					/*
					String message = String.format("Went from %s to %s on %s", currentWorld, newWorld, lastServer);
					if (client.player != null) {
						client.player.sendMessage(Text.literal(message), false);
					}
					*/ //Meant for debugging
					currentWorld = newWorld;
					applyHushIfNeeded();
				}
			}
		});

		// Register the command
		HushChildCommand.registerCommands();
	}

	public static Map<String, String> getServerWorldMap() {
		return serverWorldMap;
	}

	public static void saveConfig() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			StringBuilder builder = new StringBuilder();
			for (Map.Entry<String, String> entry : serverWorldMap.entrySet()) {
				builder.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
			}
			Files.write(CONFIG_PATH, builder.toString().getBytes());
		} catch (IOException e) {
			LOGGER.error("Failed to save config", e);
		}
	}

	private void applyHushIfNeeded() {
		if (currentWorld.isEmpty()) {
			return;
		}

		String hushWorld = serverWorldMap.get(lastServer);
		if (hushWorld != null && hushWorld.equals(currentWorld)) {
			muteVolume();
		} else if (isMuted) {
			restoreVolume();
		}
	}

	public static void muteVolume() {
		try {
			GameOptions options = client.options;
			previousVolume = options.getSoundVolumeOption(SoundCategory.MASTER).getValue().floatValue();
			options.getSoundVolumeOption(SoundCategory.MASTER).setValue(0.0);
			options.write();
			isMuted = true;
		} catch (Exception e) {
			LOGGER.error("Failed to mute volume", e);
		}
	}

	public static void restoreVolume() {
		try {
			GameOptions options = client.options;
			options.getSoundVolumeOption(SoundCategory.MASTER).setValue((double) previousVolume);
			options.write();
			isMuted = false;
		} catch (Exception e) {
			LOGGER.error("Failed to restore volume", e);
		}
	}
}
