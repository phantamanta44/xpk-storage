package st.evening.mc.xpk.storage.recipe

import com.google.gson.JsonObject
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.JsonUtils
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.crafting.CraftingHelper
import net.minecraftforge.common.crafting.IRecipeFactory
import net.minecraftforge.common.crafting.JsonContext
import net.minecraftforge.oredict.ShapedOreRecipe
import st.evening.mc.xpk.storage.util.StorageType

class ShapedUpgradeRecipe(group: ResourceLocation?, result: ItemStack, primer: CraftingHelper.ShapedPrimer) :
    ShapedOreRecipe(group, result, primer) {

    private val resultType: StorageType? = StorageType.fromStack(result)

    constructor(group: ResourceLocation?, result: ItemStack, vararg recipe: Any?) :
        this(group, result, CraftingHelper.parseShaped(recipe))

    override fun getCraftingResult(inv: InventoryCrafting): ItemStack {
        val stack = super.getCraftingResult(inv)
        resultType?.copyStackContentsFromFirst(inv, stack)
        return stack
    }

    class Factory : IRecipeFactory {
        override fun parse(context: JsonContext, json: JsonObject): IRecipe {
            val parent = /*ShapedOreRecipe.*/factory(context, json)
            return ShapedUpgradeRecipe(
                parent.group.let { if (it.isEmpty()) null else ResourceLocation(it) },
                parent.recipeOutput,
                CraftingHelper.ShapedPrimer().apply {
                    width = parent.recipeWidth
                    height = parent.recipeHeight
                    input = parent.ingredients
                    mirrored = JsonUtils.getBoolean(json, "mirrored", true) // field in parent isn't visible from here
                }
            )
        }
    }
}
