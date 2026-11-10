package st.evening.mc.xpk.storage.backpack

import baubles.api.BaubleType
import baubles.api.IBauble
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.capabilities.ICapabilitySerializable
import net.minecraftforge.fml.common.Optional
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemStackHandler
import st.evening.mc.prelude.api.registration.openContainer
import st.evening.mc.prelude.api.util.data.getCompoundOrNull
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.forEachSlot
import st.evening.mc.prelude.api.util.game.isEmpty
import st.evening.mc.prelude.api.util.world.onServer
import st.evening.mc.xpk.storage.StorageConfig
import st.evening.mc.xpk.storage.StorageStrings
import st.evening.mc.xpk.storage.XpkStorage
import st.evening.mc.xpk.storage.base.ItemTieredStorage
import st.evening.mc.xpk.storage.compat.baubles.BaublesCompat
import st.evening.mc.xpk.storage.util.XpkStorageHelper
import kotlin.math.min

@Optional.Interface(iface = "baubles.api.IBauble", modid = BaublesCompat.MOD_ID)
class ItemBackpack(config: StorageConfig.Backpack) :
    ItemTieredStorage<StorageConfig.Backpack>(config), IBauble {
    companion object {
        private const val SER_INV_SHARE: String = "inv_share"

        @ServerSide
        fun tryOpenBackpack(player: EntityPlayer) {
            // check bauble inventory first
            val baubleInv = BaublesCompat.getBaubleInventory(player)
            baubleInv.forEachSlot {
                if (baubleInv.getStackInSlot(it).item is ItemBackpack) {
                    player.openContainer(XpkStorage.defns.containerBackpack, player.world, 36 + it, 0, 0)
                    return
                }
            }
            // check off-hand slot
            if (player.heldItemOffhand.item is ItemBackpack) {
                player.openContainer(XpkStorage.defns.containerBackpack, player.world, -1, 0, 0)
                return
            }
            // check main inventory (hotbar slots first)
            for (i in 0..<36) {
                if (player.inventory.getStackInSlot(i).item is ItemBackpack) {
                    player.openContainer(XpkStorage.defns.containerBackpack, player.world, i, 0, 0)
                    return
                }
            }
        }
    }

    override fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?): ICapabilityProvider =
        BackpackInventory((stack.item as? ItemBackpack)?.config?.getSlotCount() ?: 0)

    @Optional.Method(modid = BaublesCompat.MOD_ID)
    override fun getBaubleType(stack: ItemStack): BaubleType = BaubleType.BODY

    override fun onItemRightClick(world: World, player: EntityPlayer, hand: EnumHand): ActionResult<ItemStack?> {
        world.onServer {
            player.openContainer(XpkStorage.defns.containerBackpack, world, player.inventory.currentItem, 0, 0)
        }
        return ActionResult(EnumActionResult.SUCCESS, player.getHeldItem(hand))
    }

    @ClientSide.Physical
    override fun addInformation(stack: ItemStack, world: World?, tooltip: MutableList<String>, flag: ITooltipFlag) {
        BackpackInventory.addStackTooltip(tooltip, stack)
        tooltip += TextFormatting.DARK_GRAY.toString() +
            I18n.format(StorageStrings.TOOLTIP_BACKPACK_SIZE, config.getSlotCount())
    }

    override fun getNBTShareTag(stack: ItemStack): NBTTagCompound? {
        val inv = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
        return if (inv is BackpackInventory && !inv.isEmpty()) {
            (super.getNBTShareTag(stack) ?: NBTTagCompound()).runAction {
                SER_INV_SHARE tag inv.serializeNBT()
            }
        } else {
            super.getNBTShareTag(stack)
        }
    }

    override fun readNBTShareTag(stack: ItemStack, nbt: NBTTagCompound?) {
        nbt?.getCompoundOrNull(SER_INV_SHARE)?.let {
            (stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) as? BackpackInventory)
                ?.deserializeNBT(it)
            nbt.removeTag(SER_INV_SHARE)
        }
        super.readNBTShareTag(stack, nbt)
    }

    class BackpackInventory(slotCount: Int) : ItemStackHandler(slotCount), ICapabilitySerializable<NBTTagCompound> {
        companion object {
            fun copyStackContents(srcStack: ItemStack, dstStack: ItemStack) {
                val srcInv = srcStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) ?: return
                val dstInv = dstStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) ?: return
                for (i in 0..<min(srcInv.slots, dstInv.slots)) {
                    dstInv.insertItem(i, srcInv.getStackInSlot(i), false)
                }
            }

            @ClientSide.Physical
            fun addStackTooltip(tooltip: MutableList<String>, stack: ItemStack) {
                val inv = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) ?: return
                var filled = 0
                inv.forEachSlot {
                    if (!inv.getStackInSlot(it).isEmpty) {
                        filled++
                    }
                }
                if (filled > 0) {
                    tooltip += "${TextFormatting.GRAY}${I18n.format(StorageStrings.TOOLTIP_BACKPACK_CONTENTS, filled)}"
                }
            }
        }

        override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
            capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY

        override fun <T> getCapability(capability: Capability<T>, facing: EnumFacing?): T? = when (capability) {
            CapabilityItemHandler.ITEM_HANDLER_CAPABILITY -> CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this)
            else -> null
        }

        override fun setSize(size: Int) { // size cannot be changed!
            stacks.fill(ItemStack.EMPTY)
        }

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean = !XpkStorageHelper.hasInventory(stack)
    }
}
