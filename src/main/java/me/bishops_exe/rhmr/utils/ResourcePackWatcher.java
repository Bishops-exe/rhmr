package me.bishops_exe.rhmr.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.bishops_exe.rhmr.Rhmr;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcePackWatcher {
  private enum WatchFileType {
    ZIP,
    DIR
  }

  private record WatchedPath(Path path, WatchFileType type) {}

  private static final Logger LOGGER = LoggerFactory.getLogger("rhmr");

  /** How long {@link #stop()} waits for an in-flight poll before closing the watch service. */
  private static final long SHUTDOWN_TIMEOUT_MS = 2000;

  /** Written on the client thread in {@link #start()}, read on the watcher thread. */
  private volatile WatchService watchService;
  private volatile ScheduledExecutorService executor;

  /**
   * The state below is confined to the {@code rhmr-pack-watcher} thread: only {@link #poll()} and
   * {@link #rebuildWatches} touch it, and both run on that executor. Plain collections are correct
   * under that confinement -- if you ever read or mutate them from the client thread, this becomes
   * a data race.
   */
  private boolean reloadPending = false;

  private final BiMap<WatchKey, Path> keyToDir = HashBiMap.create();
  private final Set<WatchedPath> watched = new HashSet<>();

  public void start() {
    WatchService service;
    try {
      service = FileSystems.getDefault().newWatchService();
    } catch (IOException e) {
      LOGGER.error("[rhmr] WatchService init failed", e);
      return;
    }
    watchService = service;

    ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "rhmr-pack-watcher");
      t.setDaemon(true);
      return t;
    });
    executor = exec;

    exec.scheduleWithFixedDelay(() -> {
      try {
        poll();
      } catch (ClosedWatchServiceException e) {
        // stop() closed the service underneath us; nothing left to poll.
      } catch (Exception e) {
        LOGGER.error("[rhmr] Error in pack watcher poll", e);
      }
    }, 500, 500, TimeUnit.MILLISECONDS);
  }

  /**
   * Must be called on the client thread. {@code PackRepository.selected} is plain mutable state, so
   * the selected packs are snapshotted here and the filesystem work is handed to the watcher thread.
   */
  public void refresh() {
    ScheduledExecutorService exec = executor;
    if (exec == null || exec.isShutdown()) {
      return;
    }

    Minecraft mc = Minecraft.getInstance();
    Path packsDir = mc.gameDirectory.toPath().resolve("resourcepacks");
    List<Path> packPaths = mc.getResourcePackRepository().getSelectedPacks().stream()
        .map(Pack::getId)
        .filter(id -> id.startsWith("file/"))
        .map(id -> packsDir.resolve(id.substring(5)))
        .toList();

    try {
      exec.execute(() -> rebuildWatches(packsDir, packPaths));
    } catch (RejectedExecutionException e) {
      // stop() shut the executor down between the check above and here.
    }
  }

  private void rebuildWatches(Path packsDir, List<Path> packPaths) {
    reloadPending = false;
    keyToDir.keySet().forEach(WatchKey::cancel);
    keyToDir.clear();
    watched.clear();

    if (watchService == null) {
      return;
    }

    packPaths.forEach(packPath -> {
      if (!Files.exists(packPath)) {
        return;
      }
      if (Files.isDirectory(packPath)) {
        registerFolderPackDir(packPath);
      } else {
        watched.add(new WatchedPath(packPath, WatchFileType.ZIP));
        registerDir(packsDir);
      }
    });
  }

  private void registerFolderPackDir(Path dir) {
    try {
      Files.walkFileTree(dir, new SimpleFileVisitor<>() {
        @Override
        public @NonNull FileVisitResult preVisitDirectory(@NonNull Path d,
            @NonNull BasicFileAttributes attrs) throws IOException {
          watched.add(new WatchedPath(d, WatchFileType.DIR));
          registerDir(d);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      LOGGER.warn("[rhmr] Failed to walk {}", dir, e);
    }
  }

  private void registerDir(Path dir) {
    if (keyToDir.inverse().containsKey(dir)) {
      return;
    }
    try {
      WatchKey key = dir.register(
          watchService,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_MODIFY,
          StandardWatchEventKinds.ENTRY_DELETE
      );
      keyToDir.put(key, dir);
    } catch (IOException e) {
      LOGGER.warn("[rhmr] Failed to watch {}", dir, e);
    }
  }

  private Iterable<WatchKey> iterateWatchService() {
    return () -> new Iterator<>() {
        private WatchKey nextKey = null;

        {
          forwards();
        }

        private WatchKey forwards() {
          WatchKey current = nextKey;
          nextKey = watchService.poll();

          return current;
        }

        @Override
        public boolean hasNext() {
            return nextKey != null;
        }

        @Override
        public WatchKey next() {
            if (nextKey == null) {
                throw new NoSuchElementException();
            }

            return forwards();
        }
    };
  }

  private void poll() {
    for (WatchKey key : iterateWatchService()) {
      Path dir = keyToDir.get(key);
      if (dir != null) {
        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          if (kind == StandardWatchEventKinds.OVERFLOW) {
            continue;
          }

          Path changed = dir.resolve((Path) event.context());

          if (watched.contains(new WatchedPath(dir, WatchFileType.DIR))) {
            if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changed)) {
              registerFolderPackDir(changed);
            }
            scheduleReload();
          } else if (watched.contains(new WatchedPath(changed, WatchFileType.ZIP))) {
            scheduleReload();
          }
        }
      }
      if (!key.reset()) {
        keyToDir.remove(key);
      }
    }
  }

  private void scheduleReload() {
    ScheduledExecutorService exec = executor;
    if (exec == null || reloadPending || !Rhmr.CONFIG.enabled) {
      return;
    }

    TimeAmount amount = Rhmr.CONFIG.getDebounce();

    reloadPending = true;
    try {
      exec.schedule(
              () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(mc::reloadResourcePacks);
              },
              amount.amount(),
              amount.unit()
      );
    } catch (RejectedExecutionException e) {
      // Shutting down; the reload is moot.
      reloadPending = false;
    }
  }

  public void stop() {
    ScheduledExecutorService exec = executor;
    if (exec != null) {
      exec.shutdownNow();
      try {
        if (!exec.awaitTermination(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
          LOGGER.warn("[rhmr] Pack watcher did not stop within {}ms", SHUTDOWN_TIMEOUT_MS);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    WatchService service = watchService;
    if (service != null) {
      try {
        service.close();
      } catch (IOException ignored) {
      }
    }
  }
}