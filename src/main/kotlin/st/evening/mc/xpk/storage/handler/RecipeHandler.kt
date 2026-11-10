package st.evening.mc.xpk.storage.handler

import net.minecraft.block.Block
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.NonNullList
import net.minecraftforge.common.crafting.CraftingHelper
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.registries.IForgeRegistry
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.resource
import st.evening.mc.xpk.storage.XpkStorage
import st.evening.mc.xpk.storage.recipe.ShapedUpgradeRecipe

object RecipeHandler {
    fun init(reg: ModRegistrar) {
        reg.on<RegistryEvent.Register<IRecipe>> { event ->
            if (XpkStorage.config.exampleRecipes) {
                event.registry.registerExampleRecipes()
            }
        }
    }

    private fun IForgeRegistry<IRecipe>.registerExampleRecipes() {
        val defns = XpkStorage.defns
        val backpackLeather = defns.getBackpack("leather")
        val backpackIron = defns.getBackpack("iron")
        val backpackGold = defns.getBackpack("gold")
        addExampleRecipe(backpackLeather, "chestWood", "leather")
        addExampleRecipe(backpackIron, backpackLeather, "ingotIron")
        addExampleRecipe(backpackGold, backpackIron, "ingotGold")
        addExampleRecipe(defns.getBackpack("diamond"), backpackGold, "gemDiamond")

        val barrelStone = defns.getBarrel("stone")
        val barrelIron = defns.getBarrel("iron")
        val barrelGold = defns.getBarrel("gold")
        addExampleRecipe(barrelStone, "logWood", "cobblestone")
        addExampleRecipe(barrelIron, barrelStone, "ingotIron")
        addExampleRecipe(barrelGold, barrelIron, "ingotGold")
        addExampleRecipe(defns.getBarrel("diamond"), barrelGold, "gemDiamond")

        val chestStone = defns.getChest("stone")
        val chestIron = defns.getChest("iron")
        val chestGold = defns.getChest("gold")
        addExampleRecipe(chestStone, "chestWood", "cobblestone")
        addExampleRecipe(chestIron, chestStone, "ingotIron")
        addExampleRecipe(chestGold, chestIron, "ingotGold")
        addExampleRecipe(defns.getChest("diamond"), chestGold, "gemDiamond")

        val tankStone = defns.getTank("stone")
        val tankIron = defns.getTank("iron")
        val tankGold = defns.getTank("gold")
        addExampleRecipe(tankStone, Items.BUCKET, "cobblestone")
        addExampleRecipe(tankIron, tankStone, "ingotIron")
        addExampleRecipe(tankGold, tankIron, "ingotGold")
        addExampleRecipe(defns.getTank("diamond"), tankGold, "gemDiamond")
    }

    private fun IForgeRegistry<IRecipe>.addExampleRecipe(result: Item?, center: Any?, around: Any?) {
        if (result == null || center == null || around == null) return
        addExampleRecipe(ItemStack(result), center, around)
    }

    private fun IForgeRegistry<IRecipe>.addExampleRecipe(result: Block?, center: Any?, around: Any?) {
        if (result == null || center == null || around == null) return
        addExampleRecipe(ItemStack(result), center, around)
    }

    private fun IForgeRegistry<IRecipe>.addExampleRecipe(result: ItemStack, center: Any, around: Any) {
        val primer = CraftingHelper.ShapedPrimer()
        primer.width = 3
        primer.height = 3
        primer.mirrored = false
        primer.input = NonNullList.withSize(9, CraftingHelper.getIngredient(around))
        primer.input[4] = CraftingHelper.getIngredient(center)
        val recipe = ShapedUpgradeRecipe(null, result, primer)
        recipe.registryName = XpkStorage.resource("example_${result.item.registryName!!.path}")
        register(recipe)
    }
}
