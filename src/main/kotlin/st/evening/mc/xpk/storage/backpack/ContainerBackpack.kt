package st.evening.mc.xpk.storage.backpack

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.SlotItemHandler
import net.minecraftforge.items.wrapper.EmptyHandler
import st.evening.mc.prelude.api.registration.ContainerFactory
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.forEachSlot
import st.evening.mc.prelude.api.util.game.mergeStacksSafe
import st.evening.mc.xpk.storage.compat.baubles.BaublesCompat

class ContainerBackpack(
    val playerInv: InventoryPlayer,
    val backpackItem: ItemBackpack,
    val backpackStack: ItemStack,
    val backpackSlotIndex: Int,
    val backpackSlot: () -> ItemStack
) : Container() {
    init {
        for (i in 9..<36) {
            addSlotToContainer(if (i == backpackSlotIndex) SlotLocked(playerInv, i, 0, 0) else Slot(playerInv, i, 0, 0))
        }
        for (i in 0..<9) {
            addSlotToContainer(if (i == backpackSlotIndex) SlotLocked(playerInv, i, 0, 0) else Slot(playerInv, i, 0, 0))
        }
        val backpackInv = backpackStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
            ?: EmptyHandler.INSTANCE // really shouldn't happen
        backpackInv.forEachSlot { i ->
            addSlotToContainer(SlotItemHandler(backpackInv, i, 0, 0))
        }
    }

    override fun canInteractWith(player: EntityPlayer): Boolean = backpackSlot() === backpackStack

    override fun slotClick(slotId: Int, dragType: Int, clickType: ClickType, player: EntityPlayer): ItemStack {
        if (clickType == ClickType.SWAP && dragType == backpackSlotIndex) return ItemStack.EMPTY
        return super.slotClick(slotId, dragType, clickType, player)
    }

    override fun transferStackInSlot(player: EntityPlayer, index: Int): ItemStack =
        InvHelper.transferStacks(inventorySlots[index], player) { stack ->
            if (index < 36) {
                mergeStacksSafe(stack, 36, inventorySlots.size, false)
            } else {
                mergeStacksSafe(stack, 0, 36, false)
            }
        }

    class SlotLocked(inv: IInventory, slotIndex: Int, x: Int, y: Int) : Slot(inv, slotIndex, x, y) {
        @ClientSide.Physical
        override fun isEnabled(): Boolean = false
        override fun canTakeStack(playerIn: EntityPlayer): Boolean = false
        override fun decrStackSize(amount: Int): ItemStack = ItemStack.EMPTY
        override fun isItemValid(stack: ItemStack): Boolean = false
        override fun putStack(stack: ItemStack) {}
    }

    object Factory : ContainerFactory {
        @ServerSide
        override fun createServerContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Container? =
            createContainer(player, x)

        @ClientSide.Strong
        override fun createClientContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): GuiContainer? =
            createContainer(player, x)?.let { GuiBackpack(it) }

        private fun createContainer(player: EntityPlayer, slotIndex: Int): ContainerBackpack? {
            when {
                // off-hand slot
                slotIndex < 0 -> return createContainerForSlot(player, -1) { player.heldItemOffhand }
                // main inventory slot
                slotIndex < 36 ->
                    return createContainerForSlot(player, slotIndex) { player.inventory.getStackInSlot(slotIndex) }
                // bauble slot
                else -> {
                    val baubleSlotIndex = slotIndex - 36
                    return createContainerForSlot(player, -1) { BaublesCompat.getBaubleInSlot(player, baubleSlotIndex) }
                }
            }
        }

        private inline fun createContainerForSlot(
            player: EntityPlayer,
            slotIndex: Int,
            crossinline getStackInSlot: () -> ItemStack
        ): ContainerBackpack? {
            val stack = getStackInSlot()
            if (stack.isEmpty) return null
            val item = stack.item
            if (item !is ItemBackpack) return null
            return ContainerBackpack(player.inventory, item, stack, slotIndex) { getStackInSlot() }
        }
    }
}
