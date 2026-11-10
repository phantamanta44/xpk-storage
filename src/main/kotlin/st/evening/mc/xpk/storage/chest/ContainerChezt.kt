package st.evening.mc.xpk.storage.chest

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.items.SlotItemHandler
import st.evening.mc.prelude.api.registration.ContainerFactory
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.forEachSlot
import st.evening.mc.prelude.api.util.game.mergeStacksSafe
import st.evening.mc.prelude.api.util.world.findTileEntity
import st.evening.mc.prelude.api.util.world.onServer

class ContainerChezt(val chest: TileEntityChezt, player: EntityPlayer) : Container() {
    val playerInv: InventoryPlayer = player.inventory

    init {
        for (i in 9..<36) {
            addSlotToContainer(Slot(playerInv, i, 0, 0))
        }
        for (i in 0..<9) {
            addSlotToContainer(Slot(playerInv, i, 0, 0))
        }
        val chestInv = chest.data.get().inventory
        chestInv.forEachSlot { i ->
            addSlotToContainer(SlotItemHandler(chestInv, i, 0, 0))
        }
        chest.world.onServer {
            chest.data.getServer().onOpened(player)
        }
    }

    override fun canInteractWith(player: EntityPlayer): Boolean = player.getDistanceSqToCenter(chest.pos) <= 64.0

    override fun transferStackInSlot(player: EntityPlayer, index: Int): ItemStack =
        InvHelper.transferStacks(inventorySlots[index], player) { stack ->
            if (index < 36) {
                mergeStacksSafe(stack, 36, inventorySlots.size, false)
            } else {
                mergeStacksSafe(stack, 0, 36, false)
            }
        }

    override fun onContainerClosed(player: EntityPlayer) {
        super.onContainerClosed(player)
        chest.world.onServer {
            chest.data.getServer().onClosed(player)
        }
    }

    object Factory : ContainerFactory {
        @ServerSide
        override fun createServerContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Container? =
            world.findTileEntity<TileEntityChezt>(x, y, z)?.let { ContainerChezt(it, player) }

        @ClientSide.Strong
        override fun createClientContainer(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): GuiContainer? =
            world.findTileEntity<TileEntityChezt>(x, y, z)?.let { GuiChezt(ContainerChezt(it, player)) }
    }
}
