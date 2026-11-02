package com.ammonium.adminshop.client.events;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.recipes.RecipeManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

@EventBusSubscriber(modid = AdminShop.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {

    private ClientForgeEvents(){}

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event){

    }

    @SubscribeEvent
    public static void recipesReceived(RecipesReceivedEvent event){
        RecipeManager.setClientRecipes(event.getRecipeMap());
    }
}
