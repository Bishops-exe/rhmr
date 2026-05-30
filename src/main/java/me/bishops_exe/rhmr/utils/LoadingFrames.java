package me.bishops_exe.rhmr.utils;

import java.util.HashSet;
import java.util.Set;
import org.joml.Vector2i;

public class LoadingFrames {

  public final int SCALE = 4;
  public final int MAX_X;
  public final int MAX_Y;

  private boolean fading = false;
  private Set<Integer> faded = new HashSet<>();

  private static final long FRAME_MS = 100;

  public LoadingFrames() {
    Vector2i bottomRight = getPositionForFrame(4).add(SCALE, SCALE);
    MAX_X = bottomRight.x();
    MAX_Y = bottomRight.y();
  }

  public boolean isFading() {
    return fading;
  }

  public void setFading(boolean isFading) {
    this.fading = isFading;
    if (!isFading) {
      faded = new HashSet<>();
    }
  }

  public boolean isCurrentFrame(int i) {
    boolean result = (System.currentTimeMillis() / FRAME_MS) % 8 == i;
    if (fading) {
      if (result) {
        faded.add(i);
        return true;
      } else {
        return faded.contains(i);
      }
    } else {
      return result;
    }
  }

  public Vector2i getPositionForFrame(int pos) {
    return (switch (pos) {
      case 0 -> new Vector2i(0, 0);
      case 1 -> new Vector2i(1, 0);
      case 2 -> new Vector2i(2, 0);
      case 3 -> new Vector2i(2, 1);
      case 4 -> new Vector2i(2, 2);
      case 5 -> new Vector2i(1, 2);
      case 6 -> new Vector2i(0, 2);
      case 7 -> new Vector2i(0, 1);
      default -> throw new IllegalArgumentException("Must be between 0 and 7");
    }).mul(SCALE * 2);
  }

  public Vector2i getPositionForFrame(int pos, Vector2i offset) {
    return getPositionForFrame(pos).add(offset);
  }

  public Vector2i getPositionForFrame(int pos, int offsetX, int offsetY) {
    return getPositionForFrame(pos, new Vector2i(offsetX, offsetY));
  }
}
