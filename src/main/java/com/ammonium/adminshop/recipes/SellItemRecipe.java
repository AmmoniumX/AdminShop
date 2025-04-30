package com.ammonium.adminshop.recipes;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.interfaces.ItemSellerMachine;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.recipes.interfaces.ItemRecipe;
import com.ammonium.adminshop.recipes.interfaces.SellRecipe;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SellItemRecipe implements SellRecipe, ItemRecipe {
    public static final int TAG_DISPLAY_ROTATION_TICKS = 30;
    private final ResourceLocation id;
    private final String permit;
    private final ItemStack item;

    private final @Nullable ResourceLocation tagId;
    private final int tagCount;
    public @Nullable ResourceLocation getTagId() {
        return tagId;
    }
    public int getCount() {
        if (type == SellTypes.TAG) {
            return tagCount;
        } else {
            return item.getCount();
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

    public SellItemRecipe(ResourceLocation id, long price, ItemStack item, String permit, @Nullable ResourceLocation tagId, int tagCount) {
        assert !(!item.isEmpty() && tagId != null) : "ItemStack and TagKey cannot both be set";
        this.id = id;
        this.price = price;
        this.item = item;
        this.tagId = tagId;
        this.tagCount = tagCount;
        this.type = tagId != null ? SellTypes.TAG : SellTypes.ITEM;
        this.permit = permit != null ? permit : "";
    }

    public boolean isMatchingItemStack(ItemStack toMatch) {
        if (toMatch.isEmpty()) { return false; }

        if (type == SellTypes.ITEM) {
            // Check if both items match
            if (item.isEmpty()) { return false; }
            if (item.getItem() != toMatch.getItem()) { return false; }
            // Check if item tags match
            if (item.hasTag()) {
                if (toMatch.getTag() == null || !toMatch.getTag().equals(item.getTag())) {
                    return false;
                }
            }
            // Check if item count matches
            return toMatch.getCount() >= item.getCount();
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
            if (item.isEmpty()) { return false; }
            if (item.getItem() != toMatch.getItem()) { return false; }
            // Check if item tags match
            if (item.hasTag()) {
                if (toMatch.getTag() == null || !toMatch.getTag().equals(item.getTag())) {
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
        return (type == SellTypes.ITEM) ? Optional.of(item.copy()) : Optional.empty();
    }

    public List<ItemStack> getValidItems() {
        if (type == SellTypes.ITEM) {
            return List.of(item);
        } else if (type == SellTypes.TAG) {
            assert tagId != null;
            TagKey<Item> tag = ItemTags.create(tagId);
            return ForgeRegistries.ITEMS.getValues().stream()
                    .map(i -> new ItemStack(i, tagCount))
                    .filter(i -> i.is(tag))
                    .filter(i -> i.getMaxStackSize() >= tagCount)
                    .toList();
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

    public String getName() {
        assert type != null;
        if (type == SellTypes.ITEM) {
            return item.getDisplayName().getString();
        } else if (type == SellTypes.TAG) {
            assert tagId != null;
            return "Any "+ tagId;
        } else {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.getName: type is null");
            return "";
        }
    }

    public boolean matches(MoneyHelper.MoneyAccount account, ItemSellerMachine machine) {
        if (account == null) {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: account is null");
            return false;
        }
        // Check permit status
        if ((!permit.isEmpty()) && (!MoneyHelper.hasPermit(account, permit))) {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: account does not have permit {}", permit);
            return false;
        }

        // Check if machine contains at least said number of items
        IItemHandler handler = machine.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler == null) {
            AdminShop.LOGGER.debug("ShopBuyItemRecipe.matches: handler is null");
            return false;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack simulatedResult = handler.extractItem(slot, item.getCount(), true);
            if (!simulatedResult.isEmpty() && simulatedResult.getCount() == item.getCount()) {
                return true;
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
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(Container container) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return false;
    }

    @Override
    public @NotNull ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SHOP_SELL_ITEM_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SHOP_SELL_ITEM.get();
    }

    public static class Serializer implements RecipeSerializer<SellItemRecipe> {

        public @NotNull SellItemRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            long price = GsonHelper.getAsLong(json, "price");
            ItemStack item = ItemStack.EMPTY;
            ResourceLocation tagId = null;
            int tagCount = -1;
            JsonObject inputJson = GsonHelper.getAsJsonObject(json, "input");
            if (inputJson.has("item")) {
                item = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(inputJson, "item"), true, true);
                if (item.getCount() > item.getMaxStackSize()) {
                    AdminShop.LOGGER.warn("ItemStack count {} exceeds max stack size {} for item {}", item.getCount(), item.getMaxStackSize(), item.getItem());
                    item.setCount(item.getMaxStackSize());
                }
            } else if (inputJson.has("tag")) {
                JsonObject innerTag = GsonHelper.getAsJsonObject(inputJson, "tag");
                tagId = new ResourceLocation(
                        GsonHelper.getAsString(innerTag, "tag")
                );
                tagCount = GsonHelper.getAsInt(innerTag, "count", 1);

            } else {
                AdminShop.LOGGER.warn("No item or tag found in recipe {}", id);
                return null;
            }
            String permit = GsonHelper.getAsString(json, "permit", "");
            return new SellItemRecipe(id, price, item, permit, tagId, tagCount);
        }

        public SellItemRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf buffer) {
            long price = buffer.readLong();
            ItemStack item = buffer.readItem();
            String permit = buffer.readUtf();
            boolean hasTag = buffer.readBoolean();
            ResourceLocation tag = null;
            if (hasTag) { tag = buffer.readResourceLocation(); }
            int tagCount = buffer.readInt();
            return new SellItemRecipe(id, price, item, permit, tag, tagCount);
        }

        public void toNetwork(FriendlyByteBuf buffer, SellItemRecipe pRecipe) {
            buffer.writeLong(pRecipe.price);
            buffer.writeItem(pRecipe.item);
            buffer.writeUtf(pRecipe.permit);
            buffer.writeBoolean(pRecipe.tagId != null);
            if (pRecipe.tagId != null) { buffer.writeResourceLocation(pRecipe.tagId); }
            buffer.writeInt(pRecipe.tagCount);
        }
    }
}
