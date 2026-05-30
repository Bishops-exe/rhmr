package me.bishops_exe.rhmr.utils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.bishops_exe.rhmr.Rhmr;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcePackWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger("rhmr");
  private static final long DEBOUNCE_MS = 500;

  private WatchService watchService;
  private ScheduledExecutorService executor;
  private boolean reloadPending = false;

  private final Map<WatchKey, Path> keyToDir = new ConcurrentHashMap<>();
  private final Map<Path, WatchKey> dirToKey = new ConcurrentHashMap<>();
  private final Set<Path> folderPackDirs = ConcurrentHashMap.newKeySet();
  private final Set<Path> watchedZips = ConcurrentHashMap.newKeySet();

  public void start() {
    try {
      watchService = FileSystems.getDefault().newWatchService();
    } catch (IOException e) {
      LOGGER.error("[rhmr] WatchService init failed", e);
      return;
    }
    executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "rhmr-pack-watcher");
      t.setDaemon(true);
      return t;
    });
    executor.scheduleWithFixedDelay(this::poll, 500, 500, TimeUnit.MILLISECONDS);
  }

  public void refresh() {
    if (executor == null || executor.isShutdown()) {
      return;
    }
    executor.execute(() -> {
      reloadPending = false;
      keyToDir.keySet().forEach(WatchKey::cancel);
      keyToDir.clear();
      dirToKey.clear();
      folderPackDirs.clear();
      watchedZips.clear();

      if (watchService == null) {
        return;
      }

      Minecraft mc = Minecraft.getInstance();
      Path packsDir = mc.gameDirectory.toPath().resolve("resourcepacks");

      mc.getResourcePackRepository().getSelectedPacks().forEach(pack -> {
        String id = pack.getId();
        if (!id.startsWith("file/")) {
          return;
        }
        Path packPath = packsDir.resolve(id.substring(5));
        if (!Files.exists(packPath)) {
          return;
        }

        if (Files.isDirectory(packPath)) {
          registerFolderPackDir(packPath);
        } else {
          watchedZips.add(packPath);
          registerDir(packsDir);
        }
      });
    });
  }


  private void registerFolderPackDir(Path dir) {
    try {
      Files.walkFileTree(dir, new SimpleFileVisitor<>() {
        @Override
        public @NonNull FileVisitResult preVisitDirectory(@NonNull Path d,
            @NonNull BasicFileAttributes attrs) throws IOException {
          folderPackDirs.add(d);
          registerDir(d);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      LOGGER.warn("[rhmr] Failed to walk {}", dir, e);
    }
  }

  private void registerDir(Path dir) {
    if (dirToKey.containsKey(dir)) {
      return;
    }
    try {
      WatchKey key = dir.register(
          watchService,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_MODIFY,
          StandardWatchEventKinds.ENTRY_DELETE
      );
      dirToKey.put(dir, key);
      keyToDir.put(key, dir);
    } catch (IOException e) {
      LOGGER.warn("[rhmr] Failed to watch {}", dir, e);
    }
  }

  private void poll() {
    try {
      WatchKey key;
      while ((key = watchService.poll()) != null) {
        Path dir = keyToDir.get(key);
        if (dir != null) {
          for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();
            if (kind == StandardWatchEventKinds.OVERFLOW) {
              continue;
            }

            @SuppressWarnings("unchecked")
            Path changed = dir.resolve(((WatchEvent<Path>) event).context());

            if (folderPackDirs.contains(dir)) {
              if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changed)) {
                registerFolderPackDir(changed);
              }
              scheduleReload();
            } else if (watchedZips.contains(changed)) {
              scheduleReload();
            }
          }
        }
        if (!key.reset()) {
          Path removed = keyToDir.remove(key);
          if (removed != null) {
            dirToKey.remove(removed);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("[rhmr] Error in pack watcher poll", e);
    }
  }

  private void scheduleReload() {
    if (reloadPending || !Rhmr.CONFIG.enabled) {
      return;
    }
    reloadPending = true;
    executor.schedule(
        () -> {
          Minecraft mc = Minecraft.getInstance();
          mc.execute(mc::reloadResourcePacks);
        },
        DEBOUNCE_MS,
        TimeUnit.MILLISECONDS
    );
  }

  public void stop() {
    if (executor != null) {
      executor.shutdownNow();
    }
    if (watchService != null) {
      try {
        watchService.close();
      } catch (IOException ignored) {
      }
    }
  }
}
