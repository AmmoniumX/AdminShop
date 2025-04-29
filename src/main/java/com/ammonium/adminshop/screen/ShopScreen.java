package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ShopBlock;
import com.ammonium.adminshop.client.gui.BuySellButton;
import com.ammonium.adminshop.client.gui.ShopButton;
import com.ammonium.adminshop.item.ModItems;
import com.ammonium.adminshop.money.ClientCache;
import com.ammonium.adminshop.money.MoneyFormat;
import com.ammonium.adminshop.money.MoneyHelper;
import com.ammonium.adminshop.network.PacketAccountAddPermit;
import com.ammonium.adminshop.network.PacketBuyRequest;
import com.ammonium.adminshop.network.PacketSellRequest;
import com.ammonium.adminshop.recipes.*;
import com.ammonium.adminshop.recipes.interfaces.BuyRecipe;
import com.ammonium.adminshop.recipes.interfaces.SellRecipe;
import com.ammonium.adminshop.recipes.interfaces.ShopRecipe;
import com.ammonium.adminshop.setup.Messages;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private final String GUI_BUY = "gui.buy";
    private final String GUI_SELL = "gui.sell";
    private static final int NUM_ROWS = 4, NUM_COLS = 9;
    private static final int SHOP_BUTTON_X = 16;
    private static final int SHOP_BUTTON_Y = 33;
    private static final int SHOP_BUTTON_SIZE = 18;
    private int rows_passed = 0;
    private final ShopMenu shopMenu;
    private final List<ShopButton> buyButtons;
    private final List<ShopButton> sellButtons;
    private List<ShopRecipe> searchResults = new ArrayList<>();
    private boolean isBuy; //Whether the Buy option is currently selected
    private BuySellButton buySellButton;
    private EditBox searchBar;
    private int tickCounter = 0;
    private String search = "";
    private int relX, relY;
    private UUID teamId = null;

    public ShopScreen(ShopMenu container, Inventory inv, Component name) {
        super(container, inv, name);

        assert Minecraft.getInstance().player != null;
        assert Minecraft.getInstance().level != null;
        assert Minecraft.getInstance().level.isClientSide;

        this.teamId = ClientCache.getAccount().teamId();

        this.shopMenu = container;
        this.imageWidth = 195;
        this.imageHeight = 222;
        this.tickCounter = 0;
        this.search = "";

        buyButtons = new ArrayList<>();
        sellButtons = new ArrayList<>();

        isBuy = true;
    }

//    @SuppressWarnings("resource")
    @Override
    protected void init() {
        super.init();
        relX = (this.width - this.imageWidth) / 2;
        relY = (this.height - this.imageHeight) / 2;
        rows_passed = 0;
        createShopButtons(false, relX, relY);
        createShopButtons(true, relX, relY);
        createBuySellButton(relX, relY);
        createSearchBar(relX, relY);
        refreshShopButtons();
    }

    @Override
    public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks){
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.searchBar.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        this.tickCounter++;
        if (this.tickCounter > 20) {
            this.tickCounter = 0;
            filterSearch();
        }
        // Render scroll indicators
        int max_rows_passed = (int) Math.max(Math.ceil(searchResults.size() / (double) NUM_COLS) - 4, 0);
//        AdminShop.LOGGER.debug("rows_passed:"+rows_passed+", max_rows_passed:"+max_rows_passed+", searchResults.size:"+searchResults.size());
//        AdminShop.LOGGER.debug("relX:"+relX+", relY:"+relY);
        matrixStack.pushPose();
        RenderSystem.setShaderTexture(0, GUI);
        matrixStack.translate(0, 0, 300);
        // Top scroll indicator
        if (rows_passed > 0) {
            blit(matrixStack, relX+15, relY+32, 15, 223, 162, 8);
        }
        // Bottom scroll indicator
        if (rows_passed < max_rows_passed) {
            blit(matrixStack, relX+15, relY+96, 15, 232, 162, 8);
        }
        matrixStack.popPose();
    }

    private void filterSearch() {
        String currSearch = searchBar.getValue();
        if (this.search.equals(currSearch)) {
            return;
        }
        this.search = currSearch;
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        // reset rows passed
        rows_passed = 0;
        createShopButtons(this.isBuy, relX, relY);
        refreshShopButtons();
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        matrixStack.pushPose();
        //Block Title
        String blockName = I18n.get(ShopBlock.SCREEN_ADMINSHOP_SHOP);
        drawCenteredString(matrixStack, font, blockName, getXSize()/2, 6, 0xffffff);

        //Player Inventory Title
        drawString(matrixStack, font, playerInventoryTitle, 16, getYSize()-94, 0xffffff);

        //Player Balance
        long balance = -1;
        Component name = Component.empty();
        MoneyHelper.MoneyAccount account = ClientCache.getAccount();
        if (account != null) {
            balance = account.balance();
            name = account.name();
        }
        String formatted = Screen.hasAltDown() ? MoneyFormat.forcedFormat(balance, MoneyFormat.FormatType.RAW) :
                MoneyFormat.forcedFormat(balance, MoneyFormat.FormatType.SHORT);
        drawString(matrixStack, Minecraft.getInstance().font,
                formatted,
                getXSize() - font.width(formatted) - 6,
                6, 0xffffff);

        // Bank account
        drawString(matrixStack, font, name.getString(),16,112,0xffffff);

        //Tooltip for item the player is hovering over
        List<ShopButton> shopButtons = isBuy ? buyButtons : sellButtons;
        Optional<ShopButton> button = shopButtons.stream().filter(b -> b.isMouseOn).findFirst();
        button.ifPresent(shopButton -> renderTooltip(matrixStack, shopButton.getTooltipContent(),
                Optional.empty(), mouseX-(this.width - this.imageWidth)/2,
                mouseY-(this.height - this.imageHeight)/2));
        matrixStack.popPose();

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        assert minecraft != null;
        Slot slot = this.getSlotUnderMouse();
        if (slot != null && Screen.hasShiftDown()) {
            ItemStack itemStack = slot.getItem();
            AdminShop.LOGGER.debug("Clicked on item: {}", itemStack);
            if (!itemStack.isEmpty()) {
                // Get item clicked on
                // Check if item is trade permit
                  if (itemStack.is(ModItems.PERMIT.get())) {
                    // Check if it has a “key” value
                    if (itemStack.hasTag()) {
                        CompoundTag compoundTag = itemStack.getTag();
                        if (compoundTag == null || !compoundTag.contains("key")) {
                            AdminShop.LOGGER.error("Trade permit has no key!");
                            return false;
                        }
                        String key = compoundTag.getString("key");
                        System.out.println("Key: "+key);
                        // check if key is valid
                        if (key.isEmpty()) {
                            AdminShop.LOGGER.error("Trade permit has invalid key!");
                            return false;
                        }

                        // Check if we have an account
                        MoneyHelper.MoneyAccount account = ClientCache.getAccount();
                        if (account == null) {
                            AdminShop.LOGGER.error("No account found!");
                            return false;
                        }

                        // Add permit tier to bank account
                        AdminShop.LOGGER.info("Adding permit to account: {}", key);
//                        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Adding permit "+key+" to account"),
//                                Minecraft.getInstance().player.getUUID());
//                        Minecraft.getInstance().player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        Messages.sendToServer(new PacketAccountAddPermit(account, key, slot.getSlotIndex()));
                        return false;
                    }
                }
                // Check if item is fluid container
                boolean isFluidContainer = itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
                AdminShop.LOGGER.debug("Item is fluid container: {}", isFluidContainer);
                if (isFluidContainer) {
                    IFluidHandlerItem fluidHandler = itemStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
                    // Check if fluid is in recipes
                    for (int i = 0; i < fluidHandler.getTanks(); i++) {
                        FluidStack fluidStack = fluidHandler.getFluidInTank(i);
                        // Return if container is empty
                        if (fluidStack.isEmpty()) {
                            continue;
                        }
                        // Check if fluid is in recipes
                        SellFluidRecipe fluidRecipe = RecipeManager.isSellFluidRecipe(Minecraft.getInstance().level, fluidStack).orElse(null);
                        if (fluidRecipe != null) {
                            // Attempt to sell
                            AdminShop.LOGGER.debug("Found recipe: {}", fluidRecipe.getId());
                            if (fluidStack.getAmount() < fluidRecipe.getFluid().getAmount()) {
                                AdminShop.LOGGER.debug("Not enough fluid to sell");
                                return false;
                            }
                            Messages.sendToServer(new PacketSellRequest(this.teamId, fluidRecipe.getId(), slot.getSlotIndex(), 1));
                            return false;
                        }
                    }
                }

                // Check if item is in sell item map
                SellItemRecipe itemRecipe = RecipeManager.isSellItemRecipe(Minecraft.getInstance().level, itemStack).orElse(null);
                if (itemRecipe != null) {
                    // Attempt to sell it
                    AdminShop.LOGGER.debug("Found recipe: {}", itemRecipe.getId());
                    int maxFit = itemStack.getCount() / itemRecipe.getCount();
                    if (maxFit < 1) {
                        AdminShop.LOGGER.debug("Not enough items to sell");
                        return false;
                    }
                    Messages.sendToServer(new PacketSellRequest(this.teamId, itemRecipe.getId(), slot.getSlotIndex(),
                            maxFit));
                    return false;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(@NotNull PoseStack matrixStack, float partialTicks, int mouseX, int mouseY){
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBar.keyPressed(keyCode, scanCode, modifiers) || this.searchBar.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBar.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }


    private void createShopButtons(boolean isBuy, int x, int y){
        searchResults.clear();
        if (isBuy) {
            searchResults.addAll(RecipeManager.getAllBuyRecipes(Minecraft.getInstance().level)
                .stream()
                .map(recipe -> (ShopRecipe) recipe)
                .toList());
        } else {
            searchResults.addAll(RecipeManager.getAllSellRecipes(Minecraft.getInstance().level)
                .stream()
                .map(recipe -> (ShopRecipe) recipe)
                .toList());
        }
//        AdminShop.LOGGER.debug("ShopScreen: createShopButtons: searchResults.size: "+searchResults.size());
        // Filter by search if it is set
        if (!this.search.isEmpty()) {
            searchResults = searchResults.stream().filter(recipe -> {
                if (recipe instanceof BuyItemRecipe) {
                    return ((BuyItemRecipe) recipe).getItem().get().getDisplayName().getString()
                            .toLowerCase().strip().contains(this.search.toLowerCase().strip());
                } else if (recipe instanceof SellItemRecipe) {
                    return ((SellItemRecipe) recipe).getItem().get().getDisplayName().getString()
                            .toLowerCase().strip().contains(this.search.toLowerCase().strip());
                } else if (recipe instanceof BuyFluidRecipe) {
                    return ((BuyFluidRecipe) recipe).getFluid().getDisplayName().getString()
                            .toLowerCase().strip().contains(this.search.toLowerCase().strip());
                } else if (recipe instanceof SellFluidRecipe) {
                    return ((SellFluidRecipe) recipe).getFluid().getDisplayName().getString()
                            .toLowerCase().strip().contains(this.search.toLowerCase().strip());
                }
                return false;
            }).toList();
        }
//        AdminShop.LOGGER.debug("ShopScreen: createShopButtons: searchResults.size after filter: "+searchResults.size());
        List<ShopButton> shopButtons = isBuy ? buyButtons : sellButtons;
        //Clear shop buttons if they already exist
        shopButtons.forEach(this::removeWidget);
        shopButtons.clear();

        // Create new shop buttons
        // Skip rows scrolled past
        List<ShopRecipe> shopItems = new ArrayList<>(searchResults);
        int numPassed = rows_passed*NUM_COLS;
        if (numPassed < searchResults.size()) {
            shopItems = shopItems.subList(numPassed, Math.min(numPassed+NUM_ROWS*NUM_COLS, shopItems.size()));
        } else {
            AdminShop.LOGGER.debug("Scrolled farther down that should've!");
            shopItems = new ArrayList<>(); // or however you want to handle this case
        }
        // Add buttons
        for(int j = 0; j < shopItems.size(); j++){
            final int j2 = j;
            List<ShopRecipe> finalShopItems = shopItems;
            ShopButton button = new ShopButton(shopItems.get(j),
                    x+SHOP_BUTTON_X+SHOP_BUTTON_SIZE*(j%NUM_COLS),
                    y+SHOP_BUTTON_Y+SHOP_BUTTON_SIZE*((j/NUM_COLS)%NUM_ROWS), itemRenderer, (b) -> {
                int quantity = ((ShopButton)b).getQuantity();
                attemptTransaction(finalShopItems.get(j2), quantity);
            });
            shopButtons.add(button);
            button.visible = isBuy;
            addRenderableWidget(button);
        }

    }

    private void createSearchBar(int x, int y) {
        int searchBarWidth = 70;
        int searchBarHeight = 12;
        searchBar = new EditBox(font, x+16, y+18, searchBarWidth, searchBarHeight, Component.literal(""));
        addWidget(searchBar);
    }
    private void createBuySellButton(int x, int y){
        if(buySellButton != null){
            removeWidget(buySellButton);
        }
        buySellButton = new BuySellButton(x+15, y+4,
                I18n.get(GUI_BUY), I18n.get(GUI_SELL), isBuy, (b) -> {
            isBuy = ((BuySellButton)b).switchBuySell();
            int relX = (this.width - this.imageWidth) / 2;
            int relY = (this.height - this.imageHeight) / 2;
            // reset rows passed
            rows_passed = 0;
            createShopButtons(this.isBuy, relX, relY);
            refreshShopButtons();
        });
        addRenderableWidget(buySellButton);
    }

    private void refreshShopButtons(){
        buyButtons.forEach(b -> b.visible = false);
        sellButtons.forEach(b -> b.visible = false);
        List<ShopButton> categoryButtons = isBuy ? buyButtons : sellButtons;
        categoryButtons.forEach(b -> b.visible = true);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (pDelta > 0) {
            // Scroll up
            rows_passed = Math.max(0, rows_passed - 1);
        } else if (pDelta < 0) {
            // Scroll down
            int shopSize = searchResults.size();
            int max_rows_passed = (int) Math.max(Math.ceil(shopSize / (double) NUM_COLS) - 4, 0);
            rows_passed = Math.min(max_rows_passed, rows_passed + 1);
        }
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        createShopButtons(this.isBuy, relX, relY);
        refreshShopButtons();
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    private void attemptTransaction(ShopRecipe recipe, int quantity){
        if (recipe instanceof BuyRecipe buyRecipe) {
            Messages.sendToServer(new PacketBuyRequest(this.teamId, buyRecipe.getId(), quantity));
        } else if (recipe instanceof SellRecipe sellRecipe) {
            Messages.sendToServer(new PacketSellRequest(this.teamId, sellRecipe.getId(), -1, quantity));
        }
    }
}
