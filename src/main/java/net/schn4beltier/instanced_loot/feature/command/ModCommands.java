package net.schn4beltier.instanced_loot.feature.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.schn4beltier.instanced_loot.Instanced_loot;
import net.schn4beltier.instanced_loot.feature.data.PlayerChestData;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Instanced_loot.MODID) // Dein MODID
public final class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();

        d.register(
            Commands.literal("instancedloot")
                .requires(src -> src.hasPermission(2))
                    .then(Commands.literal("reset")
                            // /instancedloot reset   -> nimmt Ausführenden
                            .executes(ctx -> resetFor(ctx, List.of(ctx.getSource().getPlayerOrException())))
                            // /instancedloot reset <player|selector>
                            .then(Commands.argument("player", EntityArgument.players())
                                    .executes(ctx -> resetFor(ctx, EntityArgument.getPlayers(ctx, "player")))
                            )
                    )
        );
    }


    private static int resetFor(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        var server = ctx.getSource().getServer();
        int dimsTouched = 0;

        for (ServerLevel level : server.getAllLevels()) {
            var store = PlayerChestData.get(level);
            for (ServerPlayer p : targets) {
                store.removeAllForPlayer(p.getUUID().toString());
            }
            dimsTouched++;
        }

        int finalDimsTouched = dimsTouched;
        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        "Reset instanced loot for " + targets.size() + " player(s) across " + finalDimsTouched + " dimension(s)."
                ),
                true
        );
        return targets.size();
    }
}

