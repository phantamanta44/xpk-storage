package st.evening.mc.xpk.storage.chest

import net.minecraft.item.ItemStack
import net.minecraftforge.client.model.ModelLoader
import st.evening.mc.prelude.api.item.CustomModelItem
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.onPhysicalClient
import st.evening.mc.xpk.storage.StorageConfig
import st.evening.mc.xpk.storage.base.ItemBlockTieredStorage
import st.evening.mc.xpk.storage.util.XpkStorageClientHelper

class ItemBlockChezt(block: BlockChezt) :
    ItemBlockTieredStorage<StorageConfig.Chest, BlockChezt>(block), CustomModelItem {

    init {
        onPhysicalClient {
            tileEntityItemStackRenderer = TileEntityCheztRenderer.ItemRenderer
        }
    }

    @ClientSide.Physical
    override fun registerItemModels() {
        ModelLoader.setCustomModelResourceLocation(this, 0, XpkStorageClientHelper.CHEST_MODEL)
    }

    override fun getItemStackLimit(stack: ItemStack): Int =
        if (TileEntityChezt.stackHasContents(stack)) 1 else maxStackSize
}
