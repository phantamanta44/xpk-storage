package st.evening.mc.xpk.storage.base

import net.minecraft.client.renderer.color.IItemColor
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.common.IRarity
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.xpk.storage.StorageConfig
import st.evening.mc.xpk.storage.XpkStorage

abstract class ItemTieredStorage<C : StorageConfig.Storage>(val config: C) : Item() {
    var colour: Int = config.colour.toInt(16)
        private set

    init {
        maxStackSize = 1
        setCreativeTab(XpkStorage.defns.creativeTab)
    }

    open fun refreshConfigCache() {
        colour = config.colour.toInt(16)
    }

    override fun getForgeRarity(stack: ItemStack): IRarity = config.rarity

    @ClientSide.Physical
    class ColourHandler(private val item: ItemTieredStorage<*>) : IItemColor {
        override fun colorMultiplier(stack: ItemStack, tintIndex: Int): Int = if (tintIndex == 1) item.colour else -1
    }
}
