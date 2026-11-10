package st.evening.mc.xpk.storage.tank

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.capabilities.ICapabilitySerializable
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandlerItem
import st.evening.mc.prelude.api.component.fluid.ObservableFluidTank
import st.evening.mc.prelude.api.item.CustomModelItem
import st.evening.mc.prelude.api.util.data.getCompoundOrNull
import st.evening.mc.prelude.api.util.data.runAction
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.onPhysicalClient
import st.evening.mc.xpk.storage.StorageConfig
import st.evening.mc.xpk.storage.base.ItemBlockTieredStorage
import st.evening.mc.xpk.storage.util.XpkStorageClientHelper

class ItemBlockTank(val tankBlock: BlockTank) :
    ItemBlockTieredStorage<StorageConfig.Tank, BlockTank>(tankBlock), CustomModelItem {
    companion object {
        private const val SER_TANK_SHARE: String = "tank_share"
    }

    init {
        maxStackSize = 1 // fail-safe default; see getItemStackLimit below
        onPhysicalClient {
            tileEntityItemStackRenderer = TileEntityTankRenderer.ItemRenderer
        }
    }

    @ClientSide.Physical
    override fun registerItemModels() {
        ModelLoader.setCustomModelResourceLocation(this, 0, XpkStorageClientHelper.CHEST_MODEL)
    }

    override fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?): ICapabilityProvider {
        val item = stack.item
        if (item !is ItemBlockTank) {
            return TankFluidHandler(stack, 0, false)
        }
        return TankFluidHandler(stack, item.tankBlock.config.capacity, item.tankBlock.config.fluidHandlerItem)
    }

    @Suppress("OVERRIDE_DEPRECATION") // stackable fluid handler items can potentially lead to fluid dupe bugs
    override fun getItemStackLimit(): Int =
        if (tankBlock.config.fluidHandlerItem) 1 else maxStackSize

    override fun getItemStackLimit(stack: ItemStack): Int =
        if (tankBlock.config.fluidHandlerItem || TileEntityTank.readStackFluid(stack) != null) {
            1
        } else {
            maxStackSize
        }

    override fun getNBTShareTag(stack: ItemStack): NBTTagCompound? {
        val handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)
        if (handler !is TankFluidHandler) return super.getNBTShareTag(stack)
        return (super.getNBTShareTag(stack) ?: NBTTagCompound()).runAction {
            SER_TANK_SHARE tag handler.writeToNbt()
        }
    }

    override fun readNBTShareTag(stack: ItemStack, nbt: NBTTagCompound?) {
        nbt?.getCompoundOrNull(SER_TANK_SHARE)?.let {
            (stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null) as? TankFluidHandler)
                ?.readFromNbt(it)
            nbt.removeTag(SER_TANK_SHARE)
        }
        super.readNBTShareTag(stack, nbt)
    }

    class TankFluidHandler(private val stack: ItemStack, capacity: Int, val usable: Boolean) :
        ObservableFluidTank(capacity), IFluidHandlerItem, ICapabilitySerializable<NBTTagCompound> {

        override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
            capability == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY

        override fun <T> getCapability(capability: Capability<T>, facing: EnumFacing?): T? = when (capability) {
            CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY ->
                CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY.cast(this)
            else -> null
        }

        override fun getContainer(): ItemStack = stack

        override fun canFill(): Boolean = usable

        override fun canDrain(): Boolean = usable

        override fun fill(resource: FluidStack?, doFill: Boolean): Int = if (usable) super.fill(resource, doFill) else 0

        override fun drain(maxDrain: Int, doDrain: Boolean): FluidStack? =
            if (usable) super.drain(maxDrain, doDrain) else null

        override fun drain(resource: FluidStack?, doDrain: Boolean): FluidStack? =
            if (usable) super.drain(resource, doDrain) else null

        override fun serializeNBT(): NBTTagCompound = writeToNbt()

        override fun deserializeNBT(nbt: NBTTagCompound) {
            readFromNbt(nbt)
        }
    }
}
