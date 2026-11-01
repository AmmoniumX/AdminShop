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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.*;
import java.util.stream.Collectors;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private final Identifier GUI = Identifier.fromNamespaceAndPath(AdminShop.MODID, "textures/gui/shop_gui.png");
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
    private List<RecipeHolder<ShopRecipe>> searchResults = new ArrayList<>();
    private boolean isBuy; //Whether the Buy option is currently selected
    private BuySellButton buySellButton;
    private EditBox searchBar;
    private int tickCounter = 0;
    private String search = "";
    private int relX, relY;
    private UUID teamId = null;

    public ShopScreen(ShopMenu container, Inventory inv, Component name) {
        super(container, inv, name, 195, 222);

        assert Minecraft.getInstance().player != null;
        assert Minecraft.getInstance().level != null;
        assert Minecraft.getInstance().level.isClientSide();

        this.teamId = ClientCache.getAccount().teamId();

        this.shopMenu = container;
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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        this.searchBar.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        this.tickCounter++;
        if (this.tickCounter > 20) {
            this.tickCounter = 0;
            filterSearch();
        }
        // Render scroll indicators
        int max_rows_passed = (int) Math.max(Math.ceil(searchResults.size() / (double) NUM_COLS) - 4, 0);
//        AdminShop.LOGGER.debug("rows_passed:"+rows_passed+", max_rows_passed:"+max_rows_passed+", searchResults.size:"+searchResults.size());
//        AdminShop.LOGGER.debug("relX:"+relX+", relY:"+relY);
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        guiGraphics.nextStratum();
        // Top scroll indicator
        if (rows_passed > 0) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, relX+15, relY+32, 15, 223, 162, 8, 256, 256);
        }
        // Bottom scroll indicator
        if (rows_passed < max_rows_passed) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, relX+15, relY+96, 15, 232, 162, 8, 256, 256);
        }
        poseStack.popMatrix();
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
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Matrix3x2fStack poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        //Block Title
        String blockName = I18n.get(ShopBlock.SCREEN_ADMINSHOP_SHOP);
        guiGraphics.centeredText(font, blockName, getXSize()/2, 6, 0xffffff);

        //Player Inventory Title
        guiGraphics.text(font, playerInventoryTitle, 16, getYSize()-94, 0xffffff);

        //Player Balance
        long balance = -1;
        Component name = Component.empty();
        MoneyHelper.MoneyAccount account = ClientCache.getAccount();
        if (account != null) {
            balance = account.balance();
            name = account.name();
        }
        String formatted = Minecraft.getInstance().hasAltDown() ? MoneyFormat.forcedFormat(balance, MoneyFormat.FormatType.RAW) :
                MoneyFormat.forcedFormat(balance, MoneyFormat.FormatType.SHORT);
        guiGraphics.text(Minecraft.getInstance().font,
                formatted,
                getXSize() - font.width(formatted) - 6,
                6, 0xffffff);

        // Bank account
        guiGraphics.text(font, name.getString(),16,112,0xffffff);

        //Tooltip for item the player is hovering over
        List<ShopButton> shopButtons = isBuy ? buyButtons : sellButtons;
        Optional<ShopButton> button = shopButtons.stream().filter(b -> b.isMouseOn).findFirst();
        button.ifPresent(shopButton -> guiGraphics.setTooltipForNextFrame(font, shopButton.getTooltipContent(),
                Optional.empty(), mouseX,
                mouseY));
        poseStack.popMatrix();

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        // De-focus search bar if clicked outside
        if (this.searchBar.isFocused() && !this.searchBar.isMouseOver(event.x(), event.y())) {
            this.searchBar.setFocused(false);
        }

        assert minecraft != null;
        Slot slot = this.getSlotUnderMouse();
        if (slot != null && Minecraft.getInstance().hasShiftDown()) {
            ItemStack itemStack = slot.getItem();
            AdminShop.LOGGER.debug("Clicked on item: {}", itemStack);
            if (!itemStack.isEmpty()) {
                // Get item clicked on
                // Check if item is trade permit
                  if (itemStack.is(ModItems.PERMIT.get())) {
                    // Check if it has a "key" value
                    CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        CompoundTag compoundTag = customData.copyTag();
                        if (!compoundTag.contains("key")) {
                            AdminShop.LOGGER.error("Trade permit has no key!");
                            return false;
                        }
                        String key = compoundTag.getStringOr("key", "");
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
                        Messages.sendToServer(new PacketAccountAddPermit(account, key, slot.getSlotIndex()));
                        return false;
                    }
                }
                // Check if item is fluid container
                IFluidHandlerItem fluidHandler = FluidUtil.getFluidHandler(itemStack).orElse(null);
                AdminShop.LOGGER.debug("Item is fluid container: {}", fluidHandler != null);
                if (fluidHandler != null) {
                    // Check if fluid is in recipes
                    for (int i = 0; i < fluidHandler.getTanks(); i++) {
                        FluidStack fluidStack = fluidHandler.getFluidInTank(i);
                        // Return if container is empty
                        if (fluidStack.isEmpty()) {
                            continue;
                        }
                        // Check if fluid is in recipes
                        RecipeHolder<SellFluidRecipe> fluidRecipeHolder = RecipeManager.isSellFluidRecipe(Minecraft.getInstance().level, fluidStack).orElse(null);
                        if (fluidRecipeHolder != null) {
                            SellFluidRecipe fluidRecipe = fluidRecipeHolder.value();
                            // Attempt to sell
                            AdminShop.LOGGER.debug("Found recipe: {}", fluidRecipeHolder.id().identifier());
                            if (fluidStack.getAmount() < fluidRecipe.getFluid().getAmount()) {
                                AdminShop.LOGGER.debug("Not enough fluid to sell");
                                return false;
                            }
                            Messages.sendToServer(new PacketSellRequest(this.teamId, fluidRecipeHolder.id().identifier(), slot.getSlotIndex(), 1));
                            return false;
                        }
                    }
                }

                // Check if item is in sell item map
                @Nullable RecipeHolder<SellItemRecipe> holder = RecipeManager.isSellItemRecipe(Minecraft.getInstance().level, itemStack).orElse(null);
                if (holder != null) {
                    AdminShop.LOGGER.debug("Found recipe: {}", holder.id().identifier());
                    // Attempt to sell it
                    SellItemRecipe itemRecipe = holder.value();
                    int maxFit = itemStack.getCount() / itemRecipe.getCount();
                    if (maxFit < 1) {
                        AdminShop.LOGGER.debug("Not enough items to sell");
                        return false;
                    }
                    Messages.sendToServer(new PacketSellRequest(this.teamId, holder.id().identifier(), slot.getSlotIndex(),
                            maxFit)); // broken too
                    return false;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTicks);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI, relX, relY, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }
    @Override
    public boolean keyPressed(KeyEvent event) {
        // Check if the Escape key is pressed
        if (event.key() == 256) { // 256 is the key code for Escape
            // If the search bar has focus, clear focus but don't close the screen
            if (this.searchBar.isFocused()) {
                this.searchBar.setFocused(false);
                return true; // Indicate that we've handled the key press
            }
            // Otherwise, let the screen handle it (closes the screen)
        }

        // If the search bar has focus, let it handle the key press first
        if (this.searchBar.isFocused()) {
            return this.searchBar.keyPressed(event);
        }

        // Let the parent class handle other key presses
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchBar.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }


    private void createShopButtons(boolean isBuy, int x, int y){
        searchResults.clear();
        if (isBuy) {
            searchResults.addAll(RecipeManager.getAllBuyRecipes(Minecraft.getInstance().level)
                .map(hol -> new RecipeHolder<ShopRecipe>(hol.id(), hol.value()))
                .sorted(ShopScreen::compareRecipes)
                .toList());
        } else {
            searchResults.addAll(RecipeManager.getAllSellRecipes(Minecraft.getInstance().level)
                .map(hol -> new RecipeHolder<ShopRecipe>(hol.id(), hol.value()))
                .sorted(ShopScreen::compareRecipes)
                .toList());
        }
//        AdminShop.LOGGER.debug("ShopScreen: createShopButtons: searchResults.size: "+searchResults.size());
        // Filter by search if it is set
        if (!this.search.isEmpty()) {
            searchResults = searchResults.stream().filter(recipe ->
                recipe.value().getSearchTerm().contains( this.search.toLowerCase().strip()) )
                .sorted(ShopScreen::compareRecipes)
                .collect(Collectors.toList());
        }
//        AdminShop.LOGGER.debug("ShopScreen: createShopButtons: searchResults.size after filter: "+searchResults.size());
        List<ShopButton> shopButtons = isBuy ? buyButtons : sellButtons;
        //Clear shop buttons if they already exist
        shopButtons.forEach(this::removeWidget);
        shopButtons.clear();

        // Create new shop buttons
        // Skip rows scrolled past
        List<RecipeHolder<ShopRecipe>> shopItems = new ArrayList<>(searchResults);
        int numPassed = rows_passed*NUM_COLS;
        if (numPassed < searchResults.size()) {
            shopItems = shopItems.subList(numPassed, Math.min(numPassed+NUM_ROWS*NUM_COLS, shopItems.size()));
        } else {
            // If we scrolled past the end, clear the list
            shopItems = new ArrayList<>();
        }
        // Add buttons
        for(int j = 0; j < shopItems.size(); j++){
            final int j2 = j;
            List<RecipeHolder<ShopRecipe>> finalShopItems = shopItems;
            ShopButton button = new ShopButton(shopItems.get(j),
                    x+SHOP_BUTTON_X+SHOP_BUTTON_SIZE*(j%NUM_COLS),
                    y+SHOP_BUTTON_Y+SHOP_BUTTON_SIZE*((j/NUM_COLS)%NUM_ROWS), (b) -> {
                int quantity = ((ShopButton)b).getQuantity();
                attemptTransaction(finalShopItems.get(j2), quantity);
            });
            shopButtons.add(button);
            button.visible = isBuy;
            addRenderableWidget(button);
        }

    }

    /**
     * Compares two ShopRecipes based on their IDs.
     * IDs may optionally contain numeric suffixes (e.g., "recipe1", "recipe2", "recipe10").
     * If numeric suffixes are present, they are sorted numerically.
     *
     * @param leftHolder  The first recipe to compare.
     * @param rightHolder The second recipe to compare.
     * @return A negative integer, zero, or a positive integer as the first argument is less than,
     * equal to, or greater than the second.
     */
    private static int compareRecipes(RecipeHolder<ShopRecipe> leftHolder, RecipeHolder<ShopRecipe> rightHolder) {
        String leftPath = leftHolder.id().identifier().toString();
        String rightPath = rightHolder.id().identifier().toString();

        // Extract base and optional numeric suffix
        String leftBase = leftPath.replaceAll("\\d+$", "");
        String rightBase = rightPath.replaceAll("\\d+$", "");
        int baseCompare = leftBase.compareTo(rightBase);
        if (baseCompare != 0) return baseCompare;

        // Extract numeric suffix, if present
        String leftNumStr = leftPath.substring(leftBase.length());
        String rightNumStr = rightPath.substring(rightBase.length());
        try {
            int leftNum = leftNumStr.isEmpty() ? -1 : Integer.parseInt(leftNumStr);
            int rightNum = rightNumStr.isEmpty() ? -1 : Integer.parseInt(rightNumStr);
            return Integer.compare(leftNum, rightNum);
        } catch (NumberFormatException e) {
            // Fallback to string compare if not numeric
            return leftPath.compareTo(rightPath);
        }
    }

    private void createSearchBar(int x, int y) {
        int searchBarWidth = 70;
        int searchBarHeight = 12;
        searchBar = new EditBox(font, x+16, y+18, searchBarWidth, searchBarHeight, Component.empty());
        addWidget(searchBar);
    }
    private void createBuySellButton(int x, int y){
        if(buySellButton != null){
            removeWidget(buySellButton);
        }
        buySellButton = new BuySellButton(x+15, y+4,
                Component.translatable(GUI_BUY), Component.translatable(GUI_SELL), isBuy, (b) -> {
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
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        if (pScrollY > 0) {
            // Scroll up
            rows_passed = Math.max(0, rows_passed - 1);
        } else if (pScrollY < 0) {
            // Scroll down
            int shopSize = searchResults.size();
            int max_rows_passed = (int) Math.max(Math.ceil(shopSize / (double) NUM_COLS) - 4, 0);
            rows_passed = Math.min(max_rows_passed, rows_passed + 1);
        }
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        createShopButtons(this.isBuy, relX, relY);
        refreshShopButtons();
        return super.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
    }

    private void attemptTransaction(RecipeHolder<ShopRecipe> recipe, int quantity){
        if (recipe.value() instanceof BuyRecipe buyRecipe) {
            Messages.sendToServer(new PacketBuyRequest(this.teamId, recipe.id().identifier(), quantity));
        } else if (recipe.value() instanceof SellRecipe sellRecipe) {
            Messages.sendToServer(new PacketSellRequest(this.teamId, recipe.id().identifier(), -1, quantity));
        }
    }
}
