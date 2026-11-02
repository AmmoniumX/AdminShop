package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.ItemSellerMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.interfaces.ItemRecipe;
import com.ammonium.adminshop.recipes.interfaces.SellRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class SellItemRecipe implements SellRecipe, ItemRecipe {
    public static final int TAG_DISPLAY_ROTATION_TICKS = 30;
    private final Identifier id;
    private final String permit;
    // Item stacks can't be created while recipes load, so the recipe keeps a template and creates the stack when first used
    private final @Nullable ItemStackTemplate template;
    private ItemStack item;

    private final @Nullable Identifier tagId;
    private final int tagCount;
    public @Nullable Identifier getTagId() {
        return tagId;
    }
    public int getCount() {
        if (type == SellTypes.TAG) {
            return tagCount;
        } else {
            return item().getCount();
        }
    }

    public enum SellTypes {
        ITEM,
        TAG
    }
    private final @NotNull SellTypes type;
    public SellTypes getSellType() {
        return type;
    }

    private final long price;

    public SellItemRecipe(Identifier id, long price, @Nullable ItemStackTemplate item, String permit, @Nullable Identifier tagId, int tagCount) {
        assert !(item != null && tagId != null) : "ItemStack and TagKey cannot both be set";
        this.id = id;
        this.price = price;
        this.template = item;
        this.tagId = tagId;
        this.tagCount = tagCount;
        this.type = tagId != null ? SellTypes.TAG : SellTypes.ITEM;
        this.permit = permit != null ? permit : "";
    }

    private ItemStack item() {
        if (item == null) {
            item = template == null ? ItemStack.EMPTY : template.create();
        }
        return item;
    }

    public boolean isMatchingItemStack(ItemStack toMatch) {
        if (toMatch.isEmpty()) { return false; }

        if (type == SellTypes.ITEM) {
            // Check if both items match
            if (item().isEmpty()) { return false; }
            if (item().getItem() != toMatch.getItem()) { return false; }
            // Check if item components match (replaces 1.20 NBT tag check)
            if (!item().getComponentsPatch().isEmpty()) {
                if (!item().getComponentsPatch().equals(toMatch.getComponentsPatch())) {
                    return false;
                }
            }
            // Check if item count matches
            return toMatch.getCount() >= item().getCount();
        } else if (type == SellTypes.TAG) {
            // Check if item is in the tag
            assert tagId != null;
            TagKey<Item> tag = ItemTags.create(tagId);
            if (!toMatch.is(tag)) { return false; }
            // Check if item count matches
            return toMatch.getCount() >= tagCount;

        } else {
            AdminShop.LOGGER.debug("ShopRecipeManager.matches: unknown recipe type {}", type);
            return false;
        }
    }

    public boolean isMatchingItemNoCount(ItemStack toMatch) {
        if (toMatch.isEmpty()) { return false; }

        if (type == SellTypes.ITEM) {
            // Check if both items match
            if (item().isEmpty()) { return false; }
            if (item().getItem() != toMatch.getItem()) { return false; }
            // Check if item components match (replaces 1.20 NBT tag check)
            if (!item().getComponentsPatch().isEmpty()) {
                if (!item().getComponentsPatch().equals(toMatch.getComponentsPatch())) {
                    return false;
                }
            }
            return true;
        } else if (type == SellTypes.TAG) {
            // Check if item is in the tag
            assert tagId != null;
            TagKey<Item> tag = ItemTags.create(tagId);
            return toMatch.is(tag);
        } else {
            AdminShop.LOGGER.debug("ShopRecipeManager.matches: unknown recipe type {}", type);
            return false;
        }
    }

    public Optional<ItemStack> getItem() {
        return (type == SellTypes.ITEM) ? Optional.of(item().copy()) : Optional.empty();
    }

    public List<ItemStack> getValidItems() {
        if (type == SellTypes.ITEM) {
            return List.of(item());
        } else if (type == SellTypes.TAG) {
            assert tagId != null;
            TagKey<Item> tag = ItemTags.create(tagId);
//            return ForgeRegistries.ITEMS.getValues().stream()
//                    .map(i -> new ItemStack(i, tagCount))
//                    .filter(i -> i.is(tag))
//                    .filter(i -> i.getMaxStackSize() >= tagCount)
//                    .toList();
            // Get the stream of items directly from the registry for this tag
            return BuiltInRegistries.ITEM.get(tag)
                    .map(holderSet -> holderSet.stream()
                            .map(holder -> new ItemStack(holder.value(), tagCount))
                            .filter(stack -> stack.getMaxStackSize() >= tagCount)
                            .toList()
                    ).orElse(List.of()); // Return empty list if tag doesn't exist
        } else {
            AdminShop.LOGGER.debug("ShopRecipeManager.getValidItemStacks: unknown recipe type {}", type);
            return List.of();
        }
    }

    public ItemStack getFirstItem() {
        return getValidItems().stream().findFirst().orElse(ItemStack.EMPTY);
    }

    // Only used for displaying inside GUIs
    public ItemStack getDisplayItem() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            AdminShop.LOGGER.warn("Client level is null");
            return ItemStack.EMPTY;
        }
        List<ItemStack> itemList = getValidItems();
        long rotateIndex = level.getGameTime() / TAG_DISPLAY_ROTATION_TICKS;
        if (itemList.isEmpty()) {
            AdminShop.LOGGER.warn("No item found for tag: {}", tagId);
            return new ItemStack(Items.BARRIER);
        }
        return itemList.get((int) (rotateIndex % itemList.size()));
    }

    public long getPrice() {
        return price;
    }

    public String getPermit() {
        return permit;
    }

    public String getPermitTranslationKey() {
        return "adminshop.permit." + permit;
    }

    public Component getName() {
        assert type != null;
        if (type == SellTypes.ITEM) {
            return item().getDisplayName();
        } else if (type == SellTypes.TAG) {
            assert tagId != null;
            return Component.translatable("sellitem.recipe.tag", tagId.toString());
        } else {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.getName: type is null");
            return Component.empty();
        }
    }

    @Override
    public String getSearchTerm() {
        assert type != null;
        if (type == SellTypes.ITEM) {
            return item().getDisplayName().getString().toLowerCase().strip();
        } else if (type == SellTypes.TAG) {
            assert tagId != null;
            return tagId.toString().toLowerCase().strip();
        } else {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.getName: type is null");
            return "";
        }
    }

    public boolean matches(MoneyHelper.MoneyAccount account, ItemSellerMachine machine) {
        if (account == null) {
            AdminShop.LOGGER.info("SellItemRecipe.matches: account is null");
            return false;
        }
        // Check permit status
        if ((!permit.isEmpty()) && !machine.bypassesPermits() && (!MoneyHelper.hasPermit(account, permit))) {
            AdminShop.LOGGER.info("SellItemRecipe.matches: account does not have permit {}", permit);
            return false;
        }

        // Check if machine contains at least said number of items
        IItemHandler handler = machine.getItemHandler();
        if (handler == null) {
            AdminShop.LOGGER.info("SellItemRecipe.matches: machine {} returned null handler", machine.getClass().getSimpleName());
            return false;
        }

        int requiredCount = item().getCount();
        int totalCount = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack slotItem = handler.getStackInSlot(slot);
            if (!isMatchingItemNoCount(slotItem)) { continue; }
            totalCount += slotItem.getCount();

            if (totalCount >= requiredCount) { return true; } else {
                AdminShop.LOGGER.info("SellItemRecipe.matches: not enough items — required {}, found {}", requiredCount, totalCount);
            }
        }

        return false;
    }

    public void sell(ServerLevel level, ItemSellerMachine machine) {
        // Get account information from server side
        // Important: we assume that this is only ever called after matches() succeeds
        MoneyHelper.get(level).addMoney(machine.getTeamId(), price);
        return;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(RecipeInput input) {
        return ItemStack.EMPTY;
    }

    public Identifier getId() {
        return id;
    }

    @Override
    public RecipeSerializer<SellItemRecipe> getSerializer() {
        return ModRecipeSerializers.SHOP_SELL_ITEM_SERIALIZER.get();
    }

    @Override
    public RecipeType<SellItemRecipe> getType() {
        return ModRecipeTypes.SHOP_SELL_ITEM.get();
    }

    public static class Serializer {
        static final MapCodec<SellItemRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.LONG.fieldOf("price").forGetter(r -> r.price),
                ItemStackTemplate.CODEC.optionalFieldOf("item").forGetter(r -> java.util.Optional.ofNullable(r.type == SellTypes.ITEM ? r.template : null)),
                Identifier.CODEC.optionalFieldOf("tag").forGetter(r -> java.util.Optional.ofNullable(r.tagId)),
                Codec.INT.optionalFieldOf("count", 1).forGetter(r -> r.tagCount > 0 ? r.tagCount : 1),
                Codec.STRING.optionalFieldOf("permit", "").forGetter(r -> r.permit)
            ).apply(instance, (price, item, tagOpt, count, permit) ->
                new SellItemRecipe(null, price, item.orElse(null), permit, tagOpt.orElse(null), count)
            )
        );

        static final StreamCodec<RegistryFriendlyByteBuf, SellItemRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeLong(r.price);
                ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).encode(buf, Optional.ofNullable(r.template));
                buf.writeUtf(r.permit);
                buf.writeBoolean(r.tagId != null);
                if (r.tagId != null) { buf.writeIdentifier(r.tagId); }
                buf.writeInt(r.tagCount);
            },
            buf -> {
                long price = buf.readLong();
                ItemStackTemplate item = ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC).decode(buf).orElse(null);
                String permit = buf.readUtf();
                boolean hasTag = buf.readBoolean();
                Identifier tag = hasTag ? buf.readIdentifier() : null;
                int tagCount = buf.readInt();
                return new SellItemRecipe(null, price, item, permit, tag, tagCount);
            }
        );

        public MapCodec<SellItemRecipe> codec() { return CODEC; }

        public StreamCodec<RegistryFriendlyByteBuf, SellItemRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
