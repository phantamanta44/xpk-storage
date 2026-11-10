package st.evening.mc.xpk.storage.recipe

import com.google.gson.JsonObject
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.Ingredient
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.crafting.CraftingHelper
import net.minecraftforge.common.crafting.IRecipeFactory
import net.minecraftforge.common.crafting.JsonContext
import net.minecraftforge.oredict.ShapelessOreRecipe
import st.evening.mc.xpk.storage.util.StorageType

class ShapelessUpgradeRecipe(group: ResourceLocation?, input: NonNullList<Ingredient>, result: ItemStack) :
    ShapelessOreRecipe(group, input, result) {

    constructor(group: ResourceLocation?, result: ItemStack, vararg recipe: Any?) :
        this(group, recipe.mapTo(NonNullList.create()) { CraftingHelper.getIngredient(it) }, result)

    private val resultType: StorageType? = StorageType.fromStack(result)

    override fun getCraftingResult(inv: InventoryCrafting): ItemStack {
        val stack = super.getCraftingResult(inv)
        resultType?.copyStackContentsFromFirst(inv, stack)
        return stack
    }

    class Factory : IRecipeFactory {
        override fun parse(context: JsonContext, json: JsonObject): IRecipe {
            val parent = /*ShapelessUpgradeRecipe.*/factory(context, json)
            return ShapelessUpgradeRecipe(
                parent.group.let { if (it.isEmpty()) null else ResourceLocation(it) },
                parent.ingredients,
                parent.recipeOutput
            )
        }
    }
}
