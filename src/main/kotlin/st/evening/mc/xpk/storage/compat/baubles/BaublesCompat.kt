package st.evening.mc.xpk.storage.compat.baubles

import baubles.api.BaublesApi
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper.EmptyHandler
import st.evening.mc.prelude.api.util.game.ifModLoaded

interface BaublesCompat {
    companion object : BaublesCompat by ifModLoaded(BaublesCompat.MOD_ID, ::Impl) ?: Noop {
        const val MOD_ID: String = "baubles"
    }

    fun getBaubleInventory(player: EntityPlayer): IItemHandler

    fun getBaubleInSlot(player: EntityPlayer, slotIndex: Int): ItemStack

    object Noop : BaublesCompat {
        override fun getBaubleInventory(player: EntityPlayer): IItemHandler = EmptyHandler.INSTANCE

        override fun getBaubleInSlot(player: EntityPlayer, slotIndex: Int): ItemStack = ItemStack.EMPTY
    }

    class Impl : BaublesCompat {
        override fun getBaubleInventory(player: EntityPlayer): IItemHandler = BaublesApi.getBaublesHandler(player)

        override fun getBaubleInSlot(player: EntityPlayer, slotIndex: Int): ItemStack {
            val inv = BaublesApi.getBaublesHandler(player)
            return if (slotIndex < inv.slots) inv.getStackInSlot(slotIndex) else ItemStack.EMPTY
        }
    }
}
