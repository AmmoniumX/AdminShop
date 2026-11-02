package com.ammonium.adminshop.client;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class KeyInit {
    private KeyInit(){}

    public static final String CATEGORY_ADMIN_SHOP = "key.categories."+AdminShop.MODID;
    public static KeyMapping shopIncrease1;
    public static KeyMapping shopIncrease2;

    public static void init(){
        //shopIncrease1 = registerKey("shop_increase_1", CATEGORY_ADMIN_SHOP, InputConstants.KEY_LSHIFT);
        //shopIncrease2 = registerKey("shop_increase_2", CATEGORY_ADMIN_SHOP, InputConstants.KEY_LCONTROL);
    }

    private static KeyMapping registerKey(String name, String category, int keycode){
        final KeyMapping key = new KeyMapping("key."+ AdminShop.MODID+"."+name, keycode, new KeyMapping.Category(Identifier.parse(category)));
//        ClientRegistry.registerKeyBinding(key);
        return key;
    }
}
