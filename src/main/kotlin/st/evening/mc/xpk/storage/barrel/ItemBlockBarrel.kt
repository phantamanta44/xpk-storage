package st.evening.mc.xpk.storage.barrel

import net.minecraft.item.ItemStack
import st.evening.mc.xpk.storage.StorageConfig
import st.evening.mc.xpk.storage.base.ItemBlockTieredStorage

class ItemBlockBarrel(block: BlockBarrel) : ItemBlockTieredStorage<StorageConfig.Barrel, BlockBarrel>(block) {
    override fun getItemStackLimit(stack: ItemStack): Int =
        if (TileEntityBarrel.stackHasContents(stack)) 1 else maxStackSize
}
