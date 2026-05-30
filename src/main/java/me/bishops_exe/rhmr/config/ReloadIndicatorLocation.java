package me.bishops_exe.rhmr.config;

public enum ReloadIndicatorLocation {
  TOP_LEFT(true, false, false, true),
  TOP_RIGHT(true, true, false, false),
  BOTTOM_LEFT(false, false, true, true),
  BOTTOM_RIGHT(false, true, true, false);

  public final boolean isTop;
  public final boolean isRight;
  public final boolean isBottom;
  public final boolean isLeft;

  ReloadIndicatorLocation(boolean isTop, boolean isRight, boolean isBottom, boolean isLeft) {
    this.isTop = isTop;
    this.isRight = isRight;
    this.isBottom = isBottom;
    this.isLeft = isLeft;
  }
}
