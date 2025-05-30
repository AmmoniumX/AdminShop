# AdminShop
Admin Shop Minecraft Mod

Adminshop is a powerful mod that allows both server administrator and modpack developers to add their own "shop" into Minecraft, where players can buy and sell items at fixed prices. Balance and permit unlocks are automatically synced across FTB Teams parties. The mod was originally for 1.12 by Vnator, but it has been forked to newer versions and overhauled by Ammonium.


# Information for Players:

AdminShop is a mod where you can buy and sell stuff manually or automatically.

To buy and sell items manually, right-click the Shop block.

To buy and sell items automatically, use the Buyer or Seller blocks.

The buyer block needs to be set a "target" by opening its GUI and clicking the item that you want to buy. Not all items are buyable, and some can be locked behind permits.

Your account balance is automatically synced across your FTB Teams party, so if you want to play with friends, you can create a party and share your balance with them that way.
 
To check your owned permits, use the command `/adminshop listOwnedPermits`.

# Information for Server Admins and Modpack Developers:

Adminshop is a "player-to-server" shop, not a "player-to-player" shop. What does this mean?

Adminshop allows you to:

- Set up fixed prices at which to buy and sell items from a config file.
The items are created/destroyed the moment that a player buys or sells them, there is no "limit" to the amount that can be bought or sold.

Adminshop does *not* allow you to:

- Buy and sell items between players at a player's desired price.
- Have players create their own shops with custom items and prices.
- A player-to-server shop is ideal for server administrators that want to offer players the ability to buy and sell items at a specified price that will never change and will never run out of "stock", or for modpack developers that want to include a buying/selling mechanic in their modpack as a way to progress through it.

For modpack developers, they can add their own permits as a "gatekeep" in progression in the shop. I.E, you can lock the ability to buy and sell specific items behind a craftable trade permit, and only once you craft and redeem it you can unlock said items in the shop.

The shop's contents can be edited through JSON recipes, these can be added either through datapacks or KubeJS scripts. 

For more information on how to add recipes, check the wiki.
