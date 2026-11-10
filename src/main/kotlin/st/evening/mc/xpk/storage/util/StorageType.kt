package st.evening.mc.xpk.storage.util

import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import st.evening.mc.xpk.storage.backpack.ItemBackpack
import st.evening.mc.xpk.storage.barrel.BlockBarrel
import st.evening.mc.xpk.storage.barrel.TileEntityBarrel
import st.evening.mc.xpk.storage.chest.BlockChezt
import st.evening.mc.xpk.storage.chest.TileEntityChezt
import st.evening.mc.xpk.storage.tank.BlockTank
import st.evening.mc.xpk.storage.tank.TileEntityTank

enum class StorageType {
    BACKPACK {
        override fun copyStackContents(srcStack: ItemStack, dstStack: ItemStack) {
            ItemBackpack.BackpackInventory.copyStackContents(srcStack, dstStack)
        }
    },
    BARREL {
        override fun copyStackContents(srcStack: ItemStack, dstStack: ItemStack) {
            TileEntityBarrel.copyStackContents(srcStack, dstStack)
        }
    },
    CHEST {
        override fun copyStackContents(srcStack: ItemStack, dstStack: ItemStack) {
            TileEntityChezt.copyStackContents(srcStack, dstStack)
        }
    },
    TANK {
        override fun copyStackContents(srcStack: ItemStack, dstStack: ItemStack) {
            TileEntityTank.copyStackContents(srcStack, dstStack)
        }
    };

    abstract fun copyStackContents(srcStack: ItemStack, dstStack: ItemStack)

    fun copyStackContentsFromFirst(srcInv: IInventory, dstStack: ItemStack): Boolean {
        for (i in 0..<srcInv.sizeInventory) {
            val srcStack = srcInv.getStackInSlot(i)
            if (fromStack(srcStack) == this) {
                copyStackContents(srcStack, dstStack)
                return true
            }
        }
        return false
    }

    companion object {
        fun fromStack(stack: ItemStack): StorageType? = when (val item = stack.item) {
            is ItemBackpack -> BACKPACK
            is ItemBlock -> when (item.block) {
                is BlockBarrel -> BARREL
                is BlockChezt -> CHEST
                is BlockTank -> TANK
                else -> null
            }
            else -> null
        }
    }
}
