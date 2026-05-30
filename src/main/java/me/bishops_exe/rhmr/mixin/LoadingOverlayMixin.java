package me.bishops_exe.rhmr.mixin;

import me.bishops_exe.rhmr.Rhmr;
import me.bishops_exe.rhmr.config.Config;
import me.bishops_exe.rhmr.utils.LoadingFrames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LoadingOverlay.class, priority = 1500)
public class LoadingOverlayMixin {

  @Shadow
  private long fadeOutStart;

  @Shadow
  @Final
  private Minecraft minecraft;

  @Inject(
      method = "rrls$miniRender(Lnet/minecraft/client/gui/GuiGraphics;F)V",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/util/ARGB;color(IIII)I",
          shift = Shift.AFTER
      ),
      cancellable = true
  )
  private void rhmr$beforeSwitch(GuiGraphics ctx, float partialTick, CallbackInfo ci) {
    Config config = Rhmr.CONFIG;
    LoadingFrames frames = Rhmr.LOADING_FRAMES;

    int x = config.location.isLeft ? config.padding : ctx.guiWidth() - config.padding - frames.MAX_Y;
    int y = config.location.isTop ? config.padding : ctx.guiHeight() - config.padding - frames.MAX_Y;



    frames.setFading(this.fadeOutStart != -1);

    for (int i = 0; i < 8; i++) {
      if (frames.isCurrentFrame(i)) {
        rhmr$drawPixel(ctx, frames.getPositionForFrame(i, x, y), frames.SCALE, 0xFF_99_99_99);
      } else {
        rhmr$drawPixel(ctx, frames.getPositionForFrame(i, x, y), frames.SCALE, 0xFF_FF_FF_FF);
      }
    }

    if (frames.isFading()) {
      int textX;
      int textY = y + (frames.MAX_Y - minecraft.font.lineHeight) / 2;
      String translateKey = "rhmr.reloaded";

      if (config.location.isRight) {
        int textWidth = minecraft.font.width(I18n.get(translateKey));
        textX = x - textWidth - 5;
      } else {
        textX =  x + frames.MAX_X + 5;
      }


      ctx.drawString(
          minecraft.font,
          Component.translatable(translateKey),
          textX,
          textY,
          0xFF_99_99_99
      );
    }

    ci.cancel();
  }

  @Unique
  private void rhmr$drawPixel(GuiGraphics ctx, Vector2i pos, int scale, int color) {
    ctx.fill(pos.x, pos.y, pos.x + scale, pos.y + scale, color);
  }
}
