package hero.bane.command;

import hero.bane.HushChild;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HushChildCommand {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(HushChildCommand.class);

    public static void registerCommands() {
        LOGGER.info("Registering HushChild command");

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("hushchild")
                    .executes(context -> {
                        if (client.world == null || client.getCurrentServerEntry() == null) {
                            context.getSource().sendError(Text.literal("You must be connected to a server to use this command."));
                            LOGGER.warn("Command executed while not connected to a server.");
                            return 1;
                        }

                        String serverAddress = client.getCurrentServerEntry().address;
                        String worldName = client.world.getRegistryKey().getValue().toString();

                        boolean wasRemoved = hero.bane.HushChild.getServerWorldMap().containsKey(serverAddress) &&
                                hero.bane.HushChild.getServerWorldMap().remove(serverAddress, worldName);

                        if (wasRemoved) {
                            hero.bane.HushChild.saveConfig();
                            context.getSource().sendFeedback(Text.literal("HushChild disabled for server: " + serverAddress + " in world: " + worldName));
                            LOGGER.info("Disabled HushChild for server: {}, world: {}", serverAddress, worldName);
                            if(HushChild.isMuted)
                            {
                                HushChild.restoreVolume();
                            }
                        } else {
                            hero.bane.HushChild.getServerWorldMap().put(serverAddress, worldName);
                            hero.bane.HushChild.saveConfig();
                            context.getSource().sendFeedback(Text.literal("HushChild enabled for server: " + serverAddress + " in world: " + worldName));
                            HushChild.muteVolume();
                            LOGGER.info("Enabled HushChild for server: {}, world: {}", serverAddress, worldName);
                        }

                        return 0;
                    }));
        });
    }
}
