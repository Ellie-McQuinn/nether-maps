package com.duck.elliemcquinn.nethermaps;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Map;

@EventBusSubscriber(modid = "nethermaps", bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ConfigHelper.ConfigObject<Map<ResourceLocation, ColorRGBA>> DIMENSION_COLORS = ConfigHelper.defineObject(
            BUILDER,
            "dimension_colors",
            Codec.unboundedMap(ResourceLocation.CODEC, ColorRGBA.CODEC),
            Map.of(
                    BuiltinDimensionTypes.OVERWORLD.location(), new ColorRGBA(0x344F17FF),
                    BuiltinDimensionTypes.NETHER.location(), new ColorRGBA(0x2E0100FF),
                    BuiltinDimensionTypes.END.location(), new ColorRGBA(0x656743FF)
            )
    );

    private static final ConfigHelper.ConfigObject<Map<ResourceLocation, Integer>> DIMENSION_SCALES = ConfigHelper.defineObject(
            BUILDER,
            "dimension_scales",
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT),
            Map.of(
                    BuiltinDimensionTypes.OVERWORLD.location(), 1,
                    BuiltinDimensionTypes.NETHER.location(), 8,
                    BuiltinDimensionTypes.END.location(), 1
            )
    );

    static final ModConfigSpec SPEC = BUILDER.build();

    private static final ColorRGBA defaultColor = new ColorRGBA(0x3F443FFF);
    private static Map<ResourceLocation, ColorRGBA> dimensionColors;
    private static final int defaultScale = 1;
    private static Map<ResourceLocation, Integer> dimensionScales;

    public static ColorRGBA getColor(ResourceLocation location) {
        ColorRGBA color = dimensionColors.get(location);

        if (color == null) {
            return defaultColor;
        }

        return color;
    }

    public static int getScale(ResourceLocation location) {
        Integer scale = dimensionScales.get(location);

        if (scale == null) {
            return defaultScale;
        }

        return scale;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        dimensionColors = DIMENSION_COLORS.get();
        dimensionScales = DIMENSION_SCALES.get();
    }
}
