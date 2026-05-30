package me.bishops_exe.rhmr.config;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.EnumCycler;
import dev.isxander.yacl3.config.v2.api.autogen.IntSlider;
import dev.isxander.yacl3.config.v2.api.autogen.TickBox;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public class Config {

  public static ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
      .id(Identifier.fromNamespaceAndPath("rhmr", "cfg"))
      .serializer(config -> GsonConfigSerializerBuilder.create(config)
          .setPath(FabricLoader.getInstance().getConfigDir().resolve("rhmr.json"))
          .build())
      .build();

  @SerialEntry
  @AutoGen(category = "main")
  @TickBox
  public boolean enabled = true;

  @SerialEntry
  @AutoGen(category = "main", group = "reload_indicator")
  @EnumCycler
  public ReloadIndicatorLocation location = ReloadIndicatorLocation.TOP_LEFT;

  @SerialEntry
  @AutoGen(category = "main", group = "reload_indicator")
  @IntSlider(min = 0, max = 50, step = 1)
  public int padding = 3;
}
