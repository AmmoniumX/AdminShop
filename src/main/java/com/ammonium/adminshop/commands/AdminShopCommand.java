package com.ammonium.adminshop.commands;

import com.ammonium.adminshop.item.ModItems;
import com.ammonium.adminshop.money.MoneyHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class AdminShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> adminShopCommand = Commands.literal("adminshop");

        // adminshop getPermit {permit}
        LiteralArgumentBuilder<CommandSourceStack> getPermitCommand = Commands.literal("getPermit").requires(source -> source.hasPermission(3));
        RequiredArgumentBuilder<CommandSourceStack, String> getPermitCommandTier = Commands.argument("tier", StringArgumentType.string())
                .executes(command -> {
                    String tier = StringArgumentType.getString(command, "tier");
                    return getPermit(command.getSource(), tier);
                });
        getPermitCommand.then(getPermitCommandTier);

        // adminshop give {player} {amount}
        LiteralArgumentBuilder<CommandSourceStack> giveMoneyCommand = Commands.literal("give")
                .requires(source -> source.hasPermission(3))
                .then(Commands.argument("player", EntityArgument.players()))
                .then(Commands.argument("amount", LongArgumentType.longArg())
                .executes(context -> {
                    EntitySelector playerSelector = context.getArgument("player", EntitySelector.class);
                    long amount = LongArgumentType.getLong(context, "amount");
                    return giveMoney(context.getSource(), playerSelector, amount);
                }));

        // adminshop remove {player} {amount}
        LiteralArgumentBuilder<CommandSourceStack> removeMoneyCommand = Commands.literal("remove")
                .requires(source -> source.hasPermission(3))
                .then(Commands.argument("player", EntityArgument.players())
                .then(Commands.argument("amount", LongArgumentType.longArg())
                .executes(context -> {
                    EntitySelector playerSelector = context.getArgument("player", EntitySelector.class);
                    long amount = LongArgumentType.getLong(context, "amount");
                    return removeMoney(context.getSource(), playerSelector, amount);
                })));

        // adminshop removePermit {permit}
        LiteralArgumentBuilder<CommandSourceStack> removePermitCommand = Commands.literal("removePermit")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("tier", StringArgumentType.string())
                .executes(command -> {
                    String tier = StringArgumentType.getString(command, "tier");
                    return removePermit(command.getSource(), tier);
                }));


        adminShopCommand.then(getPermitCommand)
                        .then(giveMoneyCommand)
                        .then(removeMoneyCommand);
        dispatcher.register(adminShopCommand);
    }

    static int getPermit(CommandSourceStack source, String tier) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (tier == null || tier.isEmpty()) {
            source.sendFailure(Component.literal("Permit tier 0 is the default, all accounts have it!"));
            return 0;
        }

        // Give item
        ItemStack permit = new ItemStack(ModItems.PERMIT.get());
        CompoundTag key = permit.getOrCreateTag();
        key.putString("key", tier);
        permit.setTag(key);

        boolean success = player.getInventory().add(permit);
        if (!success) {
            source.sendFailure(Component.literal("Unable to give permit to player"));
            return 0;
        }
        source.sendSuccess(Component.literal("Obtained trade permit"), true);
        return 1;
    }

    static int removePermit(CommandSourceStack source, String tier) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        if (tier == null || tier.isEmpty()) {
            source.sendFailure(Component.literal("Permit tier 0 is the default, all accounts have it!"));
            return 0;
        }

        // Remove permit
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getPlayerAccount(player);
        MoneyHelper.get(level).removePermit(account.teamId(), tier);
        source.sendSuccess(Component.literal("Removed trade permit"), true);
        return 1;
    }

    static int giveMoney(CommandSourceStack source, EntitySelector selector, long amount) throws CommandSyntaxException {
        // Skip non-positive values
        if (!(amount>0)) {
            source.sendFailure(Component.literal("Must be a positive value!"));
            return 0;
        }

        // Get player and MoneyManager
        ServerLevel level = source.getLevel();
        ServerPlayer player = selector.findSinglePlayer(source);
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getPlayerAccount(player);

        // Give money
        MoneyHelper.get(level).addMoney(account.teamId(), amount);
        source.sendSuccess(Component.literal("Successfully exchanged money"), true);
        return 1;
    }

    static int removeMoney(CommandSourceStack source, EntitySelector selector, long amount) throws CommandSyntaxException {
        // Skip non-positive values
        if (!(amount>0)) {
            source.sendFailure(Component.literal("Must be a positive value!"));
            return 0;
        }

        // Get player and account
        ServerLevel level = source.getLevel();
        ServerPlayer player = selector.findSinglePlayer(source);
        MoneyHelper.MoneyAccount account = MoneyHelper.get(level).getPlayerAccount(player);

        // Remove money
        boolean success = MoneyHelper.get(level).removeMoney(account.teamId(), amount);
        if (!success) {
            source.sendFailure(Component.literal("Error removing money from account"));
            return 0;
        }

        source.sendSuccess(Component.literal("Successfully removed money from account"), true);
        return 1;
    }
}
