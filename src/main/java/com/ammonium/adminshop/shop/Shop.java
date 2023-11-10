package com.ammonium.adminshop.shop;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.client.jei.ShopBuyWrapper;
import com.ammonium.adminshop.client.jei.ShopSellWrapper;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and stores the shop contents from a csv file. Is a singleton.
 */
public class Shop {
    private static final Path SHOP_FILE_PATH = FMLPaths.CONFIGDIR.get().resolve("adminshop/shop.csv");
    private static final String DEFAULT_SHOP_FILE = "assets/adminshop/default_shop.csv";

    //Matches "(value) as (var_type)" outside a string to turn back into just the value
//    private static final String CT_CAST_REGEX = "([0-9]+(\\.[0-9]*)?|true|false) as " +
//            "(int|short|byte|bool|float|double|long)(\\[\\])?(?=[^\"]*(\"[^\"]*\"[^\"]*)*$)";
    private static Shop instance;
    public String shopTextRaw;
    private final List<ShopItem> shopStockBuy;
    private final List<ShopItem> shopStockSell;
    private final Map<Item, List<ShopItem>> shopStockBuyNBT;
    private final Map<Item, ShopItem> shopBuyItemMap;
    private final Map<Item, ShopItem> shopSellItemMap;
    private final Map<Fluid, ShopItem> shopBuyFluidMap;
    private final Map<Fluid, ShopItem> shopSellFluidMap;
    private final Map<TagKey<Item>, ShopItem> shopSellItemTagMap;
    private final Map<TagKey<Fluid>, ShopItem> shopSellFluidTagMap;
    private final Map<ItemStack, ShopItem> shopBuyItemNBTMap;
    public List<String> errors;

    public static Shop get(){
        if(instance == null)
            instance = new Shop();
        return instance;
    }

    public Shop(){
        shopStockBuy = new ArrayList<>();
        shopStockSell = new ArrayList<>();
        shopStockBuyNBT = new HashMap<>();
        shopBuyItemMap = new HashMap<>();
        shopSellItemMap = new HashMap<>();
        shopBuyFluidMap = new HashMap<>();
        shopSellFluidMap = new HashMap<>();
        shopSellItemTagMap = new HashMap<>();
        shopSellFluidTagMap = new HashMap<>();
        shopBuyItemNBTMap = new HashMap<>();
        errors = new ArrayList<>();

        loadFromFile((CommandSource) null);
    }

    public List<ShopItem> getShopStockBuy() {
        return shopStockBuy;
    }
    public List<ShopItem> getShopStockSell() {
        return shopStockSell;
    }
    public Map<Item, List<ShopItem>> getShopStockBuyNBT() { return shopStockBuyNBT;}

    public Map<Item, ShopItem> getShopBuyItemMap() {
        return shopBuyItemMap;
    }
    public Map<Item, ShopItem> getShopSellItemMap() {
        return shopSellItemMap;
    }
    public Map<Fluid, ShopItem> getShopBuyFluidMap() {
        return shopBuyFluidMap;
    }
    public Map<Fluid, ShopItem> getShopSellFluidMap() {
        return shopSellFluidMap;
    }
    public Map<TagKey<Item>, ShopItem> getShopSellItemTagMap() {
        return shopSellItemTagMap;
    }
    public Map<TagKey<Fluid>, ShopItem> getShopSellFluidTagMap() {
        return shopSellFluidTagMap;
    }
    public Map<ItemStack, ShopItem> getShopBuyItemNBTMap() {
        return shopBuyItemNBTMap;
    }

    public List<ShopBuyWrapper> getBuyRecipes() {
        List<ShopBuyWrapper> buyRecipes = new ArrayList<>();
        shopStockBuy.forEach(buyItem -> {
            if (buyItem.isItem()) {
                buyRecipes.add(new ShopBuyWrapper(buyItem.getItem(), buyItem.getPrice(), buyItem.getPermitTier()));
            } else {
                buyRecipes.add(new ShopBuyWrapper(buyItem.getFluid().getFluid(), buyItem.getPrice(), buyItem.getPermitTier()));
            }
        });
        AdminShop.LOGGER.debug("Read "+buyRecipes.size()+" buy recipes");
        return buyRecipes;
    }

    public List<ShopSellWrapper> getSellRecipes() {
        List<ShopSellWrapper> sellRecipes = new ArrayList<>();
        shopStockSell.forEach(sellItem -> {
            if (sellItem.isItem()) {
                sellRecipes.add(new ShopSellWrapper(sellItem.getItem(), sellItem.getPrice(), sellItem.getPermitTier()));
            } else {
                sellRecipes.add(new ShopSellWrapper(sellItem.getFluid().getFluid(), sellItem.getPrice(), sellItem.getPermitTier()));
            }
        });
        AdminShop.LOGGER.debug("Read "+sellRecipes.size()+" sell recipes");
        return sellRecipes;
    }

    public boolean hasBuyShopItem(Item item) {
        return shopBuyItemMap.containsKey(item);
    }
    public ShopItem getBuyShopItem(Item item) {
        return shopBuyItemMap.get(item);
    }
    public boolean hasSellShopItem(Item item) {
        return shopSellItemMap.containsKey(item);
    }
    public ShopItem getSellShopItem(Item item) {
        return shopSellItemMap.get(item);
    }
    public boolean hasBuyShopFluid(Fluid fluid) {
        return shopBuyFluidMap.containsKey(fluid);
    }
    public ShopItem getBuyShopFluid(Fluid fluid) {
        return shopBuyFluidMap.get(fluid);
    }
    public boolean hasSellShopFluid(Fluid fluid) {
        return shopSellFluidMap.containsKey(fluid);
    }
    public ShopItem getSellShopFluid(Fluid fluid) {
        return shopSellFluidMap.get(fluid);
    }
    public boolean hasSellShopItemTag(TagKey<Item> itemTag) {
        return shopSellItemTagMap.containsKey(itemTag);
    }
    public ShopItem getSellShopItemTag(TagKey<Item> itemTag) {
        return shopSellItemTagMap.get(itemTag);
    }
    public boolean hasSellShopFluidTag(TagKey<Fluid> fluidTag) {
        return shopSellFluidTagMap.containsKey(fluidTag);
    }
    public ShopItem getSellShopFluidTag(TagKey<Fluid> fluidTag) {
        return shopSellFluidTagMap.get(fluidTag);
    }
    public boolean hasBuyShopItemNBT(ItemStack item) {
        if (!shopStockBuyNBT.containsKey(item.getItem())) return false;
        return (shopStockBuyNBT.get(item.getItem()).stream().anyMatch(shopItem -> shopItem.getItem().getTag().equals(item.getTag())));
    }
    public ShopItem getShopBuyItemNBT(ItemStack item) {
        if (!shopStockBuyNBT.containsKey(item.getItem())) return null;
        return (shopStockBuyNBT.get(item.getItem()).stream().filter(shopItem -> shopItem.getItem().getTag().equals(item.getTag())).findAny().orElse(null));
    }

    public void loadFromFile(CommandSource initiator){
        generateDefaultShopFile();
        try {
            loadFromFile(Files.readString(SHOP_FILE_PATH), initiator);
        }catch (FileNotFoundException e) {
            AdminShop.LOGGER.error("Shop file not found. This should not happen!");
        }catch (IOException e){
            AdminShop.LOGGER.error("Problem reading header/skipping first record in shop file!");
            e.printStackTrace();
        }
    }

    public void loadFromFile(String csv, CommandSource initiator) {
        //Clear out existing shop data
        shopTextRaw = csv;
        errors.clear();
        shopStockBuy.clear();
        shopStockSell.clear();
        shopStockBuyNBT.clear();
        shopBuyItemMap.clear();
        shopSellItemMap.clear();
        shopBuyFluidMap.clear();
        shopSellFluidMap.clear();
        shopSellItemTagMap.clear();
        shopSellFluidTagMap.clear();
        shopBuyItemNBTMap.clear();

        // Generate defaults if csv data is empty
        if (csv == null || csv.equals("")) {
            generateDefaultShopFile();
            return;
        }

        //Parse file
        List<List<String>> parsedCSV = CSVParser.parseCSV(csv);
        int line = 0;
        for(List<String> record : parsedCSV){
            line++;
            parseLine(record.toArray(new String[]{}), line, errors);
        }

        printErrors(initiator);
    }

    public void loadFromFile(String csv) {
        //Clear out existing shop data
        shopTextRaw = csv;
        errors.clear();
        shopStockBuy.clear();
        shopStockSell.clear();
        shopStockBuyNBT.clear();
        shopBuyItemMap.clear();
        shopSellItemMap.clear();
        shopBuyFluidMap.clear();
        shopSellFluidMap.clear();
        shopSellItemTagMap.clear();
        shopSellFluidTagMap.clear();
        shopBuyItemNBTMap.clear();

        // Generate defaults if csv data is empty
        if (csv == null || csv.equals("")) {
            generateDefaultShopFile();
            return;
        }

        //Parse file
        List<List<String>> parsedCSV = CSVParser.parseCSV(csv);
        int line = 0;
        for(List<String> record : parsedCSV){
            line++;
            parseLine(record.toArray(new String[]{}), line, errors);
        }
        ;
    }

    public void printErrors(CommandSource initiator){
        if(initiator != null){
            if(errors.size() == 0)
                initiator.sendSystemMessage(Component.literal("Shop reloaded, syntax is correct!"));
            errors.forEach(e -> initiator.sendSystemMessage(Component.literal(e)));
            errors.clear();
        }
    }

    private void parseLine(String[] line, int lineNumber, List<String> errors){
        StringBuilder debugLine = new StringBuilder("Parsing line "+lineNumber+": ");
        for(String segment: line) {
            debugLine.append(segment).append(",");
        }
        AdminShop.LOGGER.debug(debugLine.toString());
        //Skip empty lines
        if(line.length == 0)
            return;
        //Skip comments
        if(line[0].indexOf("//") == 0)
            return;
        //Skip lines that begin with an empty cell
        if(line[0].equals(""))
            return;

        //Shop item (default)

        //Parse each column and confirm it contains "valid" input
        boolean isError = false;
        if(line.length < 5){
            errors.add("Line "+lineNumber+": Expected shop item on this row, which requires 5 columns." +
                    " Not enough columns");
            isError = true;
            return;
        }
            //Check if buy or sell is correctly specified
        if(!(line[0].equalsIgnoreCase("buy") || line[0].equalsIgnoreCase("sell")
                || line[0].equalsIgnoreCase("b") || line[0].equalsIgnoreCase("s"))){
            errors.add("Line "+lineNumber+": First column must be either \"buy\", \"sell\", \"b\", or \"s\""+
                    "Value: "+line[0]);
            isError = true;
        }
            //Check if item or fluid is correctly specified
        if(!(line[1].equalsIgnoreCase("item") || line[1].equalsIgnoreCase("i")
                || line[1].equals("fluid") || line[1].equalsIgnoreCase("f"))) {
            errors.add("Line "+lineNumber+": Second column must be either \"item\", \"fluid\", \"i\", or \"f\""+
                    "Value: "+line[1]);
            isError = true;
        }
            //Check if price is a number
        long price;
        try {
            price = Long.parseLong(line[3]);
        }catch (NumberFormatException e){
            price = 1;
            errors.add("Line "+lineNumber+": Fourth column must be a whole number. Value:"+line[3]);
            isError = true;
        }
            // Check if permit tier is a non-negative integer
        int permitTier;
        try {
            permitTier = Integer.parseInt(line[4]);
        } catch (NumberFormatException e){
            permitTier = 0;
            errors.add("Line "+lineNumber+": Fifth column must be a non-negative integer. Value:"+line[3]);
            isError = true;
        }
        if (permitTier < 0) {
            permitTier = 0;
            errors.add("Line "+lineNumber+": Fifth column must be a non-negative integer. Value:"+line[3]);
            isError = true;
        }

        //Discovered at least one shop item-breaking error. Return here
        if(isError) {
            return;
        }

            //Check if buy or sell
        boolean isBuy = line[0].equalsIgnoreCase("buy") || line[0].equalsIgnoreCase("b");

            //Check if both tag and buying
        boolean isTag = line[2].contains("<tag:") || line[2].contains("#");
        if(isTag && isBuy){
            errors.add("Line "+lineNumber+": Tags can only be sold, not bought." +
                    " Please specify a unique item or change the first column to sell");
            isError = true;
        }
        boolean isItem = line[1].equals("item") || line[1].equals("i");
        //Separate nbt from item/fluid name
        boolean hasNBT = false;
        String nbtText = null;
        CompoundTag nbt = null;
        // Old KubeJS format
//        if(line[2].contains(".withTag(")){
//            hasNBT = true;
//            String nbtBase = line[2].split("\\.withTag\\(")[1];
//            nbtText = nbtBase.substring(0, nbtBase.length());
//            line[2] = line[2].split("\\.withTag\\(")[0];
//        }else
        if(line[2].contains("{")){
            hasNBT = true;
            nbtText = line[2].substring(line[2].indexOf('{')-1).trim();
            line[2] = line[2].substring(0, line[2].indexOf('{')-1).trim();
        }
        //Check if has NBT and fluid
        if(hasNBT && !isItem){
            errors.add("Line "+lineNumber+": Only items can have NBT, not fluids."+
                    "Please remove NBT from fluid or change to item");
            isError = true;
        }
        //Check if has NBT and selling
        if(hasNBT && !isBuy){
            errors.add("Line "+lineNumber+": Items with NBT can only be bought, not sold." +
                    " Please remove NBT from item or change to buy");
            isError = true;
        }
        //Check if NBT can be parsed
        if(nbtText != null){
            nbtText = kjsIntoNBT(nbtText);
            AdminShop.LOGGER.debug("Parsing NBT: "+nbtText);
            try {
                nbt = TagParser.parseTag(nbtText);
            }catch (CommandSyntaxException e){
                errors.add("Line "+lineNumber+": Improperly formatted NBT: "+nbtText);
                isError = true;
            }
        }

        //Strip extraneous text from item/fluid name
        AdminShop.LOGGER.debug("Parsing resource location: "+line[2]);
        String itemResource = line[2];
        StringBuilder nameBuilder = new StringBuilder();
        String[] split = itemResource.split(":");
            //KubeJS style
        if (split.length == 1) {
            errors.add("Line "+lineNumber+": Item \""+itemResource+"\" is not a recognized item");
            isError = true;
        } if(split.length == 2){
            if(isTag){
                AdminShop.LOGGER.debug("KubeJS Tag");
                nameBuilder.append(split[0].substring(1));
                nameBuilder.append(':');
                nameBuilder.append(split[1]);
                itemResource = nameBuilder.toString();
            }else{
                AdminShop.LOGGER.debug("KubeJS Item");
                // Parse if in Item.of(''), form
                if(itemResource.startsWith("Item.of('") && itemResource.endsWith("',")) {
                    AdminShop.LOGGER.debug("Trimming Item.of(''),");
                    // Remove them
                    itemResource = itemResource.substring("Item.of('".length(), itemResource.length() - 2);
                }
            }
        }
            //Crafttweaker style
        else{
            //mod name : item name, remove the > at the end
            if(isTag){
                AdminShop.LOGGER.debug("Crafttweaker Tag");
                nameBuilder.append(split[2]);
                nameBuilder.append(split[3].substring(0, split[3].length()));
            }else{
                AdminShop.LOGGER.debug("Crafttweaker Item");
                nameBuilder.append(split[1]);
                nameBuilder.append(split[2].substring(0, split[2].length()));
            }
            itemResource = nameBuilder.toString();
        }
        // Check if itemResource matches ResourceLocation pattern
        String pattern = "^[a-z0-9_.-]+:[a-z0-9_.-]+$";
        if (!isTag && !itemResource.matches(pattern)) {
            errors.add("Line "+lineNumber+": Missing ':' or non [a-z0-9_.-] character in item/fluid \""+itemResource+"\"");
            isError = true;
        }

        //Discovered at least one shop item-breaking error. Return here
        if(isError) {
            return;
        }

        // Check if item or fluid are a valid ResourceLocation
        AdminShop.LOGGER.debug("Checking resource location: "+itemResource);
        if(!isTag && !hasNBT) {
            ResourceLocation resourceLocation = new ResourceLocation(itemResource);
            if (isItem && !ForgeRegistries.ITEMS.containsKey(resourceLocation)) {
                errors.add("Line "+lineNumber+": Item \""+itemResource+"\" is not a recognized item");
                isError = true;
            } else if (!isItem && !ForgeRegistries.FLUIDS.containsKey(resourceLocation)) {
                errors.add("Line "+lineNumber+": Item \""+itemResource+"\" is not a recognized fluid");
                isError = true;
            }
        } else if (isItem && !isTag) {
            ResourceLocation resourceLocation = new ResourceLocation(itemResource);
            if (!ForgeRegistries.ITEMS.containsKey(resourceLocation)) {
                errors.add("Line "+lineNumber+": Item \""+itemResource+"\" is not a recognized item");
                isError = true;
            }
        }

        //Discovered at least one shop item-breaking error. Return here
        if(isError) {
            return;
        }

        AdminShop.LOGGER.debug("Adding ShopItem: isBuy="+isBuy+", isItem="+isItem+", isTag="+isTag+", item:"+itemResource+", nbt:"+((nbtText != null) ? nbtText : "none"));
        ShopItem shopItem = new ShopItem.Builder()
                .setIsBuy(isBuy)
                .setIsItem(isItem)
                .setIsTag(isTag)
                .setData(itemResource, nbt)
                .setPrice(price)
                .setPermitTier(permitTier)
                .build();

            //Check if ShopItem was created correctly
        if(!isTag && shopItem.getItem() == null){
            errors.add("Line "+lineNumber+": Shop Item could not be created. The item or fluid name does not map to" +
                    " an existing item or fluid.");
            isError = true;
            return;
        }
            //Check if ShopItem found a matching item/fluid for the supplied tag
        if(isTag && shopItem.getItem() == null){
            errors.add("Line "+lineNumber+": [WARNING] Supplied tag does not match any existing item or fluid." +
                    " The shop item will still be created, but will be virtually useless until something is mapped to" +
                    " the supplied tag.");
            //Continue anyway if no other errors have occurred yet
        }

        // assertions
        assert !hasNBT || (isItem && isBuy); // only buying items can have NBT
        assert !isTag || !isBuy; // only selling items/fluids can have tags

        List<ShopItem> shopList = isBuy ? shopStockBuy : shopStockSell;
        if (!isTag && isItem && !hasNBT) {
            // Item without NBT
            Map<Item, ShopItem> itemShopItemMap = isBuy ? shopBuyItemMap : shopSellItemMap;
            itemShopItemMap.put(shopItem.getItem().getItem(), shopItem);
        } else if (!isTag && isItem && hasNBT && isBuy) {
            // Buying Item with NBT
            AdminShop.LOGGER.debug("Saving shopItem to item NBT map");
            shopBuyItemNBTMap.put(shopItem.getItem(), shopItem);
            // Add to list of NBT items
            if (!shopStockBuyNBT.containsKey(shopItem.getItem().getItem())) {
                shopStockBuyNBT.put(shopItem.getItem().getItem(), new ArrayList<>());
            }
            shopStockBuyNBT.get(shopItem.getItem().getItem()).add(shopItem);
        } else if (!isTag && !isItem) {
            // Fluid
            Map<Fluid, ShopItem> fluidShopItemMap = isBuy ? shopBuyFluidMap : shopSellFluidMap;
            fluidShopItemMap.put(shopItem.getFluid().getFluid(), shopItem);
        } else if (isTag && isItem && !isBuy) {
            // Selling Item tag
            shopSellItemTagMap.put(shopItem.getItemTag(), shopItem);
        } else if (isTag && !isItem && !isBuy) {
            // Selling Fluid tag
            shopSellFluidTagMap.put(shopItem.getFluidTag(), shopItem);
        }
        shopList.add(shopItem);
    }

//    /**
//     * Clean the nbt string for any Crafttweaker casting expressions and convert to CompoundTag
//     * @param nbt NBT data in String format
//     * @return CompoundTag equivalent of the parameter string
//     * @throws CommandSyntaxException when nbt cannot be parsed
//     */
//    private CompoundTag parseNbtString(String nbt) throws CommandSyntaxException {
//        Matcher matcher = Pattern.compile(CT_CAST_REGEX).matcher(nbt);
//        while (matcher.find()){
//            String num = matcher.group().substring(0, matcher.group().indexOf(" as"));
//            nbt = nbt.replace(matcher.group(), num);
//        }
//        AdminShop.LOGGER.debug("Attempting to parse tag: "+nbt);
//        return TagParser.parseTag(nbt);
//    }

    // Turn KJS-like NBT format into real Minecraft format
    public static String kjsIntoNBT(String input) {
        // Replace escaped quote pairs with a single quote
        String cleaned = input.replaceAll("\\\\\"", "\"");
        // Remove single quotes around JSON objects
        cleaned = cleaned.replaceAll("':\\{'", ":{");
        cleaned = cleaned.replaceAll("'}'", "}");
        // Remove single quotes around JSON arrays
        cleaned = cleaned.replaceAll("':\\['", ":['");
        cleaned = cleaned.replaceAll("']'", "']");
        // Remove tailing "s and ending ')' character
        cleaned = cleaned.substring(1, cleaned.length() -2);

        return cleaned;
    }

    /**
     * Generate a default shop file if one does not already exist
     */
    private void generateDefaultShopFile(){
        if(Files.notExists(SHOP_FILE_PATH)){
            try {
                InputStream defStream = AdminShop.class.getClassLoader().getResourceAsStream(DEFAULT_SHOP_FILE);
                byte [] buffer = new byte[defStream.available()];
                defStream.read(buffer);
                Files.createDirectories(Path.of("config/"+AdminShop.MODID));
                Files.createFile(SHOP_FILE_PATH);
                FileOutputStream outStream = new FileOutputStream(SHOP_FILE_PATH.toFile());
                outStream.write(buffer);
            }catch (IOException e){
                AdminShop.LOGGER.error("Could not copy default shop file to config");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
