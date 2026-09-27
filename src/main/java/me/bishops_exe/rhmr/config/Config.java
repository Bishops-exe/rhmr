package me.bishops_exe.rhmr.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.*;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import me.bishops_exe.rhmr.utils.TimeAmount;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.time.Duration;
import java.time.temporal.TemporalAmount;

public class Config {

  public static ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
      .id(Identifier.fromNamespaceAndPath("rhmr", "cfg"))
      .serializer(config -> GsonConfigSerializerBuilder.create(config)
          .setPath(FabricLoader.getInstance().getConfigDir().resolve("rhmr.json"))
          .build())
      .build();

  public TimeAmount getDebounce() {
    return TimeAmount.fromSeconds(this.debounce);
  }

  @SerialEntry
  @AutoGen(category = "main")
  @TickBox
  public boolean enabled = true;

  @SerialEntry()
  @AutoGen(category = "main")
  @DoubleField
  @CustomDescription("yacl3.config.rhmr:cfg.debounce.description")
  public double debounce = 0.500;
}
