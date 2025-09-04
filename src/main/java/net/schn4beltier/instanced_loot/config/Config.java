package net.schn4beltier.instanced_loot.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.schn4beltier.instanced_loot.Instanced_loot;


@EventBusSubscriber(modid = Instanced_loot.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.EnumValue<BreakBehaviour> CHEST_BREAK_BEHAVIOUR = BUILDER
            .comment("Should loot chests be breakable")
            .comment("Possible values: DISABLED | WHILE_SNEAKING | OP_ONLY | CREATIVE | BREAKABLE")
            .comment("DISABLED = Chests are not breakable")
            .comment("WHILE_SNEAKING = Chests are breakable while sneaking")
            .comment("OP_ONLY = Only OPs can break chests")
            .comment("CREATIVE = Chests can only be broken in creative mode")
            .comment("BREAKABLE = Everyone can break chests like normal")
            .defineEnum("chest_break_behaviour", BreakBehaviour.WHILE_SNEAKING);

    private static final ModConfigSpec.BooleanValue BREAK_MESSAGE = BUILDER
            .comment("Should a warning be desplayed when a player tries to break a chest?")
            .define("break_message", true);
    private static final ModConfigSpec.ConfigValue<String> MESSAGE = BUILDER
            .comment("The message that should be displayed when trying to break a chest")
            .define("message", "This is a loot chest. Please be mindful about other player before breaking the chest");

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static BreakBehaviour chestBreakBehaviour;
    public static boolean breakMessage;
    public static String message;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        chestBreakBehaviour = CHEST_BREAK_BEHAVIOUR.get();
        breakMessage = BREAK_MESSAGE.get();
        message = MESSAGE.get();
    }
}
