package me.bishops_exe.rhmr;

import me.bishops_exe.rhmr.config.Config;
import me.bishops_exe.rhmr.utils.LoadingFrames;
import me.bishops_exe.rhmr.utils.ResourcePackWatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class Rhmr implements ClientModInitializer {

  public static final LoadingFrames LOADING_FRAMES = new LoadingFrames();
  public static final ResourcePackWatcher RESOURCE_PACK_WATCHER = new ResourcePackWatcher();
  public static final Config CONFIG;
  static {
    Config.HANDLER.load();
    CONFIG = Config.HANDLER.instance();
  }

  public static Identifier RELOADER_ID = Identifier.fromNamespaceAndPath("rhmr", "reloader");

  @Override
  public void onInitializeClient() {

    RESOURCE_PACK_WATCHER.start();

    ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
        RELOADER_ID,
        (ResourceManagerReloadListener) _ -> RESOURCE_PACK_WATCHER.refresh()
    );

    ClientLifecycleEvents.CLIENT_STOPPING.register(client -> RESOURCE_PACK_WATCHER.stop());
  }
}
