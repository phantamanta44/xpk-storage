# XPK Storage

Configurable tiered storage containers for modpack developers, including:

* Backpacks: inventory items that can be accessed on-the-go -- can also be equipped as a bauble
* Barrels: mass storage for a single item type
* Chests: standard storage blocks with a number of slots
* Tanks: fluid storage blocks -- can also be configured to act as fluid-handling items

## Usage

The XPK Storage config contains a list of storage tier config objects for each container type. New tiers can be added or removed by modifying these lists. The comments in the config already offer a great deal of information on container configuration, but the sections below describe some extra things to know about each respective container type.

Additionally, you will need to provide models and translation entries for your custom items. This can be done using a mod like [Resource Loader](https://github.com/lumien231/Resource-Loader). The specific assets required are also listed in the below sections.

### Backpacks

Each backpack config entry generates an item with ID `xpkstorage:backpack_{name}`. The mod also adds an open-backpack key which can be used to automatically open a backpack from anywhere in the inventory. Backpacks can also be worn as a body-type bauble, which has priority for the open-backpack key.

Backpacks are implemented using Forge's serializable capabilities; see the section on that later for more information.

#### Assets

You will need to provide an item model `xpkstorage:models/item/backpack_{name}.json`. Model elements with tint index 1 will be tinted with the configured theme colour. You can use the built-in backpack textures with a model [like this](src/main/resources/assets/xpkstorage/models/item/backpack_diamond.json).

You will need to provide the item name under the translation key `item.xpkstorage.backpack_{name}.name`.

### Barrels

Each barrel config entry generates a block with ID `xpkstorage:barrel_{name}`. Barrels function much like other in-world mass-storage blocks you might already be familiar with:

* Right-click the front face to insert the held item
* Double-right-click the front face to insert all the items in your inventory matching the barrel's stored item type.
* Sneak+right-click the front face to toggle the barrel's item type lock
* Left-click the front face to remove one stored item
* Sneak+left-click the front face to remove a stack of the stored item

#### Assets

You will need to provide a state mapping `xpkstorage:blockstates/barrel_{name}.json`. Model elements with tint index 1 will be tinted with the configured theme colour. You can use the built-in barrel block model with a state mapping [like this](src/main/resources/assets/xpkstorage/blockstates/barrel_diamond.json).

You will also need to provide an item model `xpkstorage:models/item/barrel_{name}.json`. Model elements with tint index 1 will be tinted with the configured theme colour. You can use the built-in barrel item model with a model [like this](src/main/resources/assets/xpkstorage/models/item/barrel_diamond.json).

You will need to provide the block name under the translation key `tile.xpkstorage.barrel_{name}.name`.

### Chests

Each chest config entry generates a block with ID `xpkstorage:chest_{name}`. Chests function much like vanilla chests, with the exception that they cannot form double-chests. As a consequence, they can be placed next to each other with no restrictions.

#### Assets

Chests use a built-in renderer (similar to vanilla chests), but you will still need to provide a block state mapping to assign a texture for block-breaking particles. You can use the model of any block you like, since only the textures are accessed; for example, the [default diamond chest "model"](src/main/resources/assets/xpkstorage/blockstates/chest_diamond.json) is simply the vanilla diamond block.

If you configure a custom chest texture, you will need to provide that texture at `xpkstorage:textures/entity/chest/{texture_name}.png`. The texture has the same UV mapping as the vanilla chest model.

You will need to provide the block name under the translation key `tile.xpkstorage.chest_{name}.name`.

### Tanks

Each tank config entry generates a block with ID `xpkstorage:tank_{name}`. Tanks are similar in function to Thermal Expansion's portable tanks. They can be interacted with while holding fluid-handling items to fill or drain them.

Tanks can be configured to act as fluid-handling items, which allows them to interact with other mods that support them (e.g. they can be filled or drained in the Thermal Expansion fluid transposer). If enabled, tanks have their stack size limited to one; this prevents a critical dupe bug where a stack of multiple tanks may receive the same quantity of fluid multiple times. However, if this option was previously disabled and is then later enabled, it is possible that there might still be stacked tanks left in the world!

Tank items are implemented using Forge's serializable capabilities; see the section on that later for more information.

#### Assets

You will need to provide a state mapping `xpkstorage:blockstates/tank_{name}.json`. Model elements with tint index 1 will be tinted with the configured theme colour. You can use the built-in tank block model with a state mapping [like this](src/main/resources/assets/xpkstorage/blockstates/tank_diamond.json).

Tanks use a built-in renderer for the item (in order to render the tank contents), so you will not need to provide an item model. Instead, the block model is rendered in the inventory.

You will need to provide the block name under the translation key `tile.xpkstorage.tank_{name}.name`.

## Upgrade Items

XPK Storage supports configurable "upgrade items" for container blocks, which can be shift+right-clicked on the corresponding block to upgrade it in-place to the configured tier. The intention is for modpack developers to add their own upgrade items (e.g. using ContentTweaker or GroovyScript), then to configure those items as upgrades. Note that if a "downgrade" to a tier with a smaller storage capacity is configured, it may cause overflowing contents to be deleted on use!

There is an additional configuration section for upgrades to the vanilla wooden chest, which can target any XPK Storage custom chest tier. Note that custom chests cannot be upgraded to the vanilla chest.

## Upgrade Recipes

XPK Storage provides a custom crafting recipe type for upgrade recipes, which retain the contents of XPK Storage containers used as ingredients. The recipe searches the crafting grid for the first matching container item, then copies its contents to the output stack. Note that if the output container has a smaller storage capacity than the input container, the overflowing contents may be lost!

There are several different ways to register upgrade recipes; three are listed in the sections below.

### Vanilla Loader

Upgrade recipes can be registered using the vanilla recipe loader using one of the recipe factories `xpkstorage:upgrade` or `xpkstorage:upgrade_shapeless`. These accept recipes in the same format as `forge:ore_shaped` or `forge:ore_shapeless` respectively. For example:

```json
{
  "type": "xpkstorage:upgrade",
  "pattern": [
    "GGG",
    "GCG",
    "GGG"
  ],
  "key": {
    "C": {
      "item": "xpkstorage:chest_iron"
    },
    "G": {
      "type": "forge:ore_dict",
      "ore": "ingotGold"
    }
  },
  "result": {
    "item": "xpkstorage:chest_gold"
  }
}
```

### CraftTweaker

Upgrade recipes can be registered using CraftTweaker with an API similar to the default crafting recipe API:

```ts
class mods.xpkstorage.UpgradeRecipes {
    static addShaped(name: String, output: IItemStack, input: IIngredient[][])
    static addShapedMirrored(name: String, output: IItemStack, input: IIngredient[][])
    static addShapeless(name: String, output: IItemStack, input: IIngredient[])
}
```

For example:

```zs
mods.xpkstorage.UpgradeRecipes.addShaped("upgrade_iron_to_gold", <xpkstorage:chest_gold>, [
    [<ore:ingotGold>, <ore:ingotGold>        , <ore:ingotGold>],
    [<ore:ingotGold>, <xpkstorage:chest_iron>, <ore:ingotGold>],
    [<ore:ingotGold>, <ore:ingotGold>        , <ore:ingotGold>]
])
```

### GroovyScript

Upgrade recipes can be registered with the vanilla crafting recipe registry. They reside in the [`st.evening.mc.xpk.storage.recipe`](src/main/kotlin/st/evening/mc/xpk/storage/recipe) package and offer constructors similar to Forge's ore recipe constructors:

```kt
/**
 * @param group  The ID of the recipe's group, if any.
 * @param result The result of the upgrade recipe.
 * @param recipe A recipe template, given as a grid followed by a key.
 */
class ShapedUpgradeRecipe(group: ResourceLocation?, result: ItemStack, vararg recipe: Any?)

/**
 * @param group  The ID of the recipe's group, if any.
 * @param result The result of the upgrade recipe.
 * @param recipe A list of ingredients.
 */
class ShapelessUpgradeRecipe(group: ResourceLocation?, result: ItemStack, vararg recipe: Any?)
```

For example:

```groovy
crafting.add(new ShapedUpgradeRecipe(null, item("xpkstorage:chest_gold"),
    "GGG", "GCG", "GGG",
    'C', item("xpkstorage:chest_iron"),
    'G', ore("ingotGold")))
```

## A Note on Serializable Capabilities

Backpacks and tank items are implemented using Forge's serializable capability system, and so they suffer from some of the same issues that affect items from other mods implemented in the same way (e.g. the Botania flower pouch or the Immersive Engineering toolbox). In particular, inventory-handling code that doesn't properly account for serializable capabilities may cause these items to lose their contents! Prelude itself fixes the issue of this type in the vanilla creative menu (if the `networking.sendItemStackCapsToServer` option is enabled), but there is still the possibility that other inventory-handling mods may unexpectedly delete your item contents.
