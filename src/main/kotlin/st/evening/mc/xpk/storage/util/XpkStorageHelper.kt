package st.evening.mc.xpk.storage.util

import net.minecraft.block.Block
import net.minecraft.block.BlockShulkerBox
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.oredict.OreIngredient

object XpkStorageHelper {
    fun parseIngredient(ingStr: String): Ingredient? {
        val k = ingStr.indexOf(':')
        if (k == -1) {
            return parseItemIngredient("minecraft", ingStr)
        }
        val domain = ingStr.substring(0, k)
        return if (domain == "ore") {
            OreIngredient(ingStr.substring(k + 1))
        } else {
            parseItemIngredient(domain, ingStr.substring(k + 1))
        }
    }

    fun parseItemIngredient(modId: String, itemStr: String): Ingredient? {
        val k = itemStr.indexOf(':')
        return if (k == -1) {
            Ingredient.fromItem(ForgeRegistries.ITEMS.getValue(ResourceLocation(modId, itemStr)) ?: return null)
        } else {
            Ingredient.fromStacks(
                ItemStack(
                    ForgeRegistries.ITEMS.getValue(ResourceLocation(modId, itemStr.substring(0, k))) ?: return null,
                    1,
                    itemStr.substring(k + 1).toInt(10)
                )
            )
        }
    }

    fun hasInventory(stack: ItemStack): Boolean =
        stack.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) ||
            Block.getBlockFromItem(stack.item) is BlockShulkerBox
}
