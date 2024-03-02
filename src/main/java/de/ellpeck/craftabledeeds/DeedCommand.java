package de.ellpeck.craftabledeeds;

import com.mojang.brigadier.CommandDispatcher;
import de.ellpeck.craftabledeeds.items.EmptyDeedItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DeedCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(CraftableDeeds.ID)
                .then(Commands.literal("info").executes(c -> {
                                    CommandSourceStack source = c.getSource();
                                    DeedStorage.Claim claim = DeedStorage.get(source.getLevel()).getClaim(source.getPosition().x, 64, source.getPosition().z);
                                    if (claim == null) {
                                        source.sendFailure(new TranslatableComponent("info." + CraftableDeeds.ID + ".not_claimed"));
                                    } else {
                                        source.sendSuccess(new TranslatableComponent("info." + CraftableDeeds.ID + ".claimed", claim.getOwnerName()), true);
                                    }
                                    return 0;
                                })
                                .then(Commands.literal("claim").requires(s -> s.hasPermission(2)).executes(c -> {
                                    CommandSourceStack sourceStack = c.getSource();
                                    DeedStorage storage = DeedStorage.get(sourceStack.getLevel());
                                    DeedStorage.Claim claim = storage.getClaim(sourceStack.getPosition().x, 64, sourceStack.getPosition().z);
                                    if (claim != null) {
                                        sourceStack.sendFailure(new TranslatableComponent("info." + CraftableDeeds.ID + ".already_claimed", claim.getOwnerName()));
                                    } else {
                                        Player player = sourceStack.getPlayerOrException();
                                        EmptyDeedItem.createMapData(ItemStack.EMPTY, player, Mth.floor(player.getX()), Mth.floor(player.getZ()), (byte) 0, true, false);
                                        sourceStack.sendSuccess(new TranslatableComponent("info." + CraftableDeeds.ID + ".claim_added"), true);
                                    }
                                    return 0;
                                })).then(Commands.literal("unclaim").requires(s -> s.hasPermission(2)).executes(c -> {
                                    CommandSourceStack sourceStack = c.getSource();
                                    DeedStorage storage = DeedStorage.get(sourceStack.getLevel());
                                    DeedStorage.Claim claim = storage.getClaim(sourceStack.getPosition().x, 64, sourceStack.getPosition().z);
                                    if (claim == null) {
                                        sourceStack.sendFailure(new TranslatableComponent("info." + CraftableDeeds.ID + ".not_claimed"));
                                    } else {
                                        storage.removeClaim(claim.mapId);
                                        sourceStack.sendSuccess(new TranslatableComponent("info." + CraftableDeeds.ID + ".claim_removed"), true);
                                    }
                                    return 0;
                                }))
                )
        );
    }


}
