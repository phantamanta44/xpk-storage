package st.evening.mc.xpk.storage.compat.crt

import crafttweaker.CraftTweakerAPI
import crafttweaker.IAction
import crafttweaker.annotations.ZenRegister
import crafttweaker.api.item.IIngredient
import crafttweaker.api.item.IItemStack
import crafttweaker.api.minecraft.CraftTweakerMC
import net.minecraft.item.crafting.Ingredient
import net.minecraft.util.NonNullList
import net.minecraftforge.common.crafting.CraftingHelper
import net.minecraftforge.fml.common.registry.ForgeRegistries
import st.evening.mc.xpk.storage.XpkStorageConsts
import st.evening.mc.xpk.storage.recipe.ShapedUpgradeRecipe
import st.evening.mc.xpk.storage.recipe.ShapelessUpgradeRecipe
import stanhebben.zenscript.annotations.ZenClass
import stanhebben.zenscript.annotations.ZenMethod

@ZenRegister
@ZenClass("mods.${XpkStorageConsts.MOD_ID}.UpgradeRecipes")
object CrTUpgradeRecipes {
    @ZenMethod
    @JvmStatic
    fun addShaped(name: String, output: IItemStack, input: Array<Array<IIngredient>>) {
        CraftTweakerAPI.apply(AddShapedAction(name, output, input, false))
    }

    @ZenMethod
    @JvmStatic
    fun addShapedMirrored(name: String, output: IItemStack, input: Array<Array<IIngredient>>) {
        CraftTweakerAPI.apply(AddShapedAction(name, output, input, true))
    }

    @ZenMethod
    @JvmStatic
    fun addShapeless(name: String, output: IItemStack, input: Array<IIngredient>) {
        CraftTweakerAPI.apply(object : IAction {
            override fun apply() {
                val recipe = ShapelessUpgradeRecipe(
                    null,
                    input.mapTo(NonNullList.create()) { CraftTweakerMC.getIngredient(it) },
                    CraftTweakerMC.getItemStack(output)
                )
                recipe.setRegistryName(CrTCompat.MOD_ID, name)
                ForgeRegistries.RECIPES.register(recipe)
            }

            override fun describe(): String =
                "Adding shapeless storage upgrade recipe for ${output.displayName} with name $name"
        })
    }

    private class AddShapedAction(
        private val name: String,
        private val output: IItemStack,
        private val input: Array<Array<IIngredient>>,
        private val mirrored: Boolean
    ) : IAction {
        override fun apply() {
            val primer = CraftingHelper.ShapedPrimer()
            primer.width = input.maxOf { it.size }
            primer.height = input.size
            primer.input = NonNullList.withSize(primer.width * primer.height, Ingredient.EMPTY)
            input.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { colIndex, ingredient ->
                    primer.input[rowIndex * primer.width + colIndex] = CraftTweakerMC.getIngredient(ingredient)
                }
            }
            primer.mirrored = mirrored
            val recipe = ShapedUpgradeRecipe(null, CraftTweakerMC.getItemStack(output), primer)
            recipe.setRegistryName(CrTCompat.MOD_ID, name)
            ForgeRegistries.RECIPES.register(recipe)
        }

        override fun describe(): String =
            "Adding shaped storage upgrade recipe for ${output.displayName} with name $name"
    }
}
