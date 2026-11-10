package st.evening.mc.xpk.storage.tank

import net.minecraft.client.resources.I18n
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import st.evening.mc.prelude.api.component.fluid.ObservableFluidTank
import st.evening.mc.prelude.api.component.fluid.RatedFluidHandler
import st.evening.mc.prelude.api.data.ser.FluidStackSerializer
import st.evening.mc.prelude.api.data.ser.ServerSideSerializable
import st.evening.mc.prelude.api.data.state.ListStateComposite
import st.evening.mc.prelude.api.data.state.Observable
import st.evening.mc.prelude.api.data.sync.SyncContainer
import st.evening.mc.prelude.api.tileentity.prefab.PrefabTileEntitySynced
import st.evening.mc.prelude.api.util.data.getCompoundOrNull
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.Sided
import st.evening.mc.prelude.api.util.game.computeComparatorValue
import st.evening.mc.prelude.api.util.game.dataTagOrNull
import st.evening.mc.prelude.api.util.game.getOrCreateDataTag
import st.evening.mc.prelude.api.util.game.isEmpty
import st.evening.mc.prelude.api.util.system.Hoist
import st.evening.mc.xpk.storage.StorageStrings
import st.evening.mc.xpk.storage.base.BlockTieredStorage
import st.evening.mc.xpk.storage.base.TieredStorageContainer
import st.evening.mc.xpk.storage.util.DropState

class TileEntityTank @Suppress("unused") private constructor(val tankBlock: BlockTank, unused: Unit) :
    PrefabTileEntitySynced<TileEntityTank.State, TileEntityTank.Server, TileEntityTank.State>() {
    companion object {
        const val SER_TANK: String = "tank"

        fun writeStackFluid(stack: ItemStack, fluid: FluidStack?) {
            val handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)
            if (handler is ItemBlockTank.TankFluidHandler) {
                handler.fluid = fluid
            } else { // fallback
                if (fluid != null) {
                    stack.getOrCreateDataTag().setTag(SER_TANK, FluidStackSerializer.serializeToNbt(fluid))
                } else {
                    stack.dataTagOrNull?.removeTag(SER_TANK)
                }
            }
        }

        fun readStackFluid(stack: ItemStack): FluidStack? {
            val handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)
            return if (handler is ItemBlockTank.TankFluidHandler) {
                handler.fluid
            } else { // fallback
                stack.dataTagOrNull?.getCompoundOrNull(SER_TANK)?.let {
                    FluidStackSerializer.deserializeFromNbt(it)
                }
            }
        }

        fun copyStackContents(srcStack: ItemStack, dstStack: ItemStack) {
            writeStackFluid(dstStack, readStackFluid(srcStack))
        }

        @ClientSide.Physical
        fun addStackTooltip(tooltip: MutableList<String>, stack: ItemStack) {
            readStackFluid(stack)?.let {
                tooltip += TextFormatting.GRAY.toString() +
                    I18n.format(StorageStrings.TOOLTIP_TANK_CONTENTS, it.amount, it.localizedName)
            }
        }
    }

    constructor(tankBlock: BlockTank) : this(tankBlock, Unit.also { Hoist.push(tankBlock) })

    // field will not be populated by the time this is called in the super-constructor
    override fun initData(): Sided<State, Server, State> = Sided.of({ Server(Hoist.pop()) }, { State(Hoist.pop()) })

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing)

    override fun <T> getCapability(capability: Capability<T?>, facing: EnumFacing?): T? = when (capability) {
        CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ->
            CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(data.get().fluidHandler)
        else -> super.getCapability(capability, facing)
    }

    open class State(tankBlock: BlockTank) : SyncContainer {
        override val syncData: ObservableFluidTank = ObservableFluidTank(tankBlock.config.capacity)

        val fluidHandler: RatedFluidHandler =
            RatedFluidHandler(syncData, tankBlock.config.inputRate, tankBlock.config.outputRate)
    }

    @ServerSide
    class Server(
        tankBlock: BlockTank,
        private val state: ListStateComposite.Builder = ListStateComposite.Builder()
    ) : State(tankBlock), TieredStorageContainer, ServerSideSerializable, Observable.Composite by state.build() {
        override var dropState: DropState = DropState.NORMAL

        init {
            state part syncData
        }

        override fun computeComparatorValue(): Int = syncData.computeComparatorValue()

        override fun dropContents(world: World, pos: BlockPos, block: BlockTieredStorage<*>, keepContents: Boolean) {
            if (dropState == DropState.UPGRADING) return
            if (keepContents) {
                if (syncData.isEmpty()) {
                    if (dropState == DropState.NORMAL) {
                        InvHelper.dropItem(ItemStack(block), world, pos)
                    }
                } else {
                    val stack = ItemStack(block)
                    writeStackFluid(stack, syncData.fluid)
                    InvHelper.dropItem(stack, world, pos)
                }
            } // we *could* try placing the spilled fluid as a block, but that would be really annoying for players
        }

        override fun restoreFromStack(stack: ItemStack) {
            syncData.fluid = readStackFluid(stack)
        }

        fun copyState(tank: IFluidTank) {
            syncData.fluid = tank.fluid
        }

        override fun writeToNbtServerSide(dto: NBTTagCompound) {
            dto.setTag(SER_TANK, syncData.writeToNbt())
        }

        override fun readFromNbtServerSide(dto: NBTTagCompound) {
            syncData.readFromNbt(dto.getCompoundTag(SER_TANK))
        }
    }
}
