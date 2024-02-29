package de.ellpeck.craftabledeeds;

import com.mojang.brigadier.CommandDispatcher;
import de.ellpeck.craftabledeeds.items.EmptyDeedItem;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;

public class DeedCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal(CraftableDeeds.ID)
                .then(Commands.literal("info").executes(c -> {
                    CommandSource source = c.getSource();
                    DeedStorage.Claim claim = DeedStorage.get(source.getWorld()).getClaim(source.getPos().x, 64, source.getPos().z);
                    if (claim == null) {
                        source.sendErrorMessage(new TranslationTextComponent("info." + CraftableDeeds.ID + ".not_claimed"));
                    } else {
                        source.sendFeedback(new TranslationTextComponent("info." + CraftableDeeds.ID + ".claimed", claim.getOwnerName()), true);
                    }
                    return 0;
                }))
                .then(Commands.literal("claim").requires(s -> s.hasPermissionLevel(2)).executes(c -> {
                    CommandSource source = c.getSource();
                    DeedStorage storage = DeedStorage.get(source.getWorld());
                    DeedStorage.Claim claim = storage.getClaim(source.getPos().x, 64, source.getPos().z);
                    if (claim != null) {
                        source.sendErrorMessage(new TranslationTextComponent("info." + CraftableDeeds.ID + ".already_claimed", claim.getOwnerName()));
                    } else {
                        PlayerEntity player = source.asPlayer();
                        EmptyDeedItem.createMapData(ItemStack.EMPTY, player, MathHelper.floor(player.getPosX()), MathHelper.floor(player.getPosZ()), 0, true, false);
                        source.sendFeedback(new TranslationTextComponent("info." + CraftableDeeds.ID + ".claim_added"), true);
                    }
                    return 0;
                }))
                .then(Commands.literal("unclaim").requires(s -> s.hasPermissionLevel(2)).executes(c -> {
                    CommandSource source = c.getSource();
                    DeedStorage storage = DeedStorage.get(source.getWorld());
                    DeedStorage.Claim claim = storage.getClaim(source.getPos().x, 64, source.getPos().z);
                    if (claim == null) {
                        source.sendErrorMessage(new TranslationTextComponent("info." + CraftableDeeds.ID + ".not_claimed"));
                    } else {
                        storage.removeClaim(claim.mapId);
                        source.sendFeedback(new TranslationTextComponent("info." + CraftableDeeds.ID + ".claim_removed"), true);
                    }
                    return 0;
                })));
    }

}
