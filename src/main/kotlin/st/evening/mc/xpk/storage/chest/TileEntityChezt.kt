package st.evening.mc.xpk.storage.chest

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
import net.minecraft.client.resources.I18n
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.ITickable
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler
import st.evening.mc.prelude.api.component.item.ObservableItemHandler
import st.evening.mc.prelude.api.data.ser.BoolSerializer
import st.evening.mc.prelude.api.data.ser.ServerSideSerializable
import st.evening.mc.prelude.api.data.state.ListStateComposite
import st.evening.mc.prelude.api.data.state.Observable
import st.evening.mc.prelude.api.data.state.ValueStateAtom
import st.evening.mc.prelude.api.data.sync.SyncContainer
import st.evening.mc.prelude.api.tileentity.prefab.PrefabTileEntitySynced
import st.evening.mc.prelude.api.util.collection.IdentityHashStrategy
import st.evening.mc.prelude.api.util.data.NbtDsl
import st.evening.mc.prelude.api.util.data.getListOrNull
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.Sided
import st.evening.mc.prelude.api.util.game.computeComparatorValue
import st.evening.mc.prelude.api.util.game.dataTagOrNull
import st.evening.mc.prelude.api.util.game.forEachSlot
import st.evening.mc.prelude.api.util.game.getOrCreateDataTag
import st.evening.mc.prelude.api.util.game.isEmpty
import st.evening.mc.prelude.api.util.system.Hoist
import st.evening.mc.xpk.storage.StorageStrings
import st.evening.mc.xpk.storage.base.BlockTieredStorage
import st.evening.mc.xpk.storage.base.TieredStorageContainer
import st.evening.mc.xpk.storage.util.DropState

class TileEntityChezt @Suppress("unused") private constructor(val chestBlock: BlockChezt, unused: Unit) :
    PrefabTileEntitySynced<TileEntityChezt.State, TileEntityChezt.Server, TileEntityChezt.Client>(), ITickable {
    companion object {
        private const val USER_CHECK_TIMER_MIN: Int = 200
        private const val USER_CHECK_TIMER_VAR: Int = 100
        private const val SER_INV: String = "inv"

        fun stackHasContents(stack: ItemStack): Boolean = stack.dataTagOrNull?.getListOrNull(SER_INV)?.isEmpty == false

        fun copyStackContents(srcStack: ItemStack, dstStack: ItemStack) {
            srcStack.dataTagOrNull?.getListOrNull(SER_INV)?.let {
                dstStack.getOrCreateDataTag().setTag(SER_INV, it.copy())
            }
        }

        @ClientSide.Physical
        fun addStackTooltip(tooltip: MutableList<String>, stack: ItemStack) {
            stack.dataTagOrNull?.getListOrNull(SER_INV)?.let {
                tooltip += "${TextFormatting.GRAY}${I18n.format(StorageStrings.TOOLTIP_CHEST_CONTENTS, it.tagCount())}"
            }
        }
    }

    constructor(chestBlock: BlockChezt) : this(chestBlock, Unit.also { Hoist.push(chestBlock) })

    // field will not be populated by the time this is called in the super-constructor
    override fun initData(): Sided<State, Server, Client> = Sided.of({ Server(Hoist.pop()) }, { Client(Hoist.pop()) })

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
        capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing)

    override fun <T> getCapability(capability: Capability<T?>, facing: EnumFacing?): T? = when (capability) {
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ->
            CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(data.get().inventory)
        else -> super.getCapability(capability, facing)
    }

    override fun update() {
        data.get().tick()
    }

    abstract class State : SyncContainer {
        override val syncData: ValueStateAtom<Boolean> = ValueStateAtom(false, BoolSerializer)

        abstract val inventory: IItemHandler

        abstract fun tick()
    }

    @ServerSide
    inner class Server(
        chestBlock: BlockChezt,
        private val state: ListStateComposite.Builder = ListStateComposite.Builder()
    ) : State(), TieredStorageContainer, ServerSideSerializable, Observable.Composite by state.build() {
        override val inventory: IItemHandler
            field: ObservableItemHandler = state part ObservableItemHandler.nSlots(chestBlock.config.getSlotCount())

        override var dropState: DropState = DropState.NORMAL

        private val users: MutableSet<EntityPlayer> = ObjectOpenCustomHashSet(IdentityHashStrategy())
        private var userCheckTimer: Int = USER_CHECK_TIMER_MIN

        private fun updateOpenState() {
            if (syncData.value) {
                if (users.isEmpty()) {
                    syncData.value = false
                    world.playSound(
                        null, pos, SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS,
                        0.5F, 0.9F + 0.1F * world.rand.nextFloat()
                    )
                }
            } else if (users.isNotEmpty()) {
                syncData.value = true
                world.playSound(
                    null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS,
                    0.5F, 0.9F + 0.1F * world.rand.nextFloat()
                )
            }
        }

        fun onOpened(user: EntityPlayer) {
            if (users.add(user)) {
                updateOpenState()
            }
        }

        fun onClosed(user: EntityPlayer) {
            if (users.remove(user)) {
                updateOpenState()
            }
        }

        override fun tick() {
            if (userCheckTimer <= 0) {
                userCheckTimer = USER_CHECK_TIMER_MIN + world.rand.nextInt(USER_CHECK_TIMER_VAR)
                if (
                    users.removeIf {
                        val container = it.openContainer
                        return@removeIf container !is ContainerChezt || container.chest != this@TileEntityChezt
                    }
                ) {
                    updateOpenState()
                }
            } else {
                userCheckTimer--
            }
        }

        override fun computeComparatorValue(): Int = inventory.computeComparatorValue()

        override fun dropContents(world: World, pos: BlockPos, block: BlockTieredStorage<*>, keepContents: Boolean) {
            if (dropState == DropState.UPGRADING) return
            if (keepContents) {
                if (inventory.isEmpty()) {
                    if (dropState == DropState.NORMAL) {
                        InvHelper.dropItem(ItemStack(block), world, pos)
                    }
                } else {
                    val stack = ItemStack(block)
                    stack.tagCompound = NbtDsl.compound {
                        SER_INV tag inventory.writeToNbt()
                    }
                    InvHelper.dropItem(stack, world, pos)
                }
            } else {
                InvHelper.dropItems(inventory, world, pos)
            }
        }

        override fun restoreFromStack(stack: ItemStack) {
            stack.dataTagOrNull?.getListOrNull(SER_INV)?.let { inventory.readFromNbt(it) }
        }

        fun copyState(inv: IItemHandler) {
            inv.forEachSlot {
                inventory.setStackInSlot(it, inv.getStackInSlot(it))
            }
        }

        override fun writeToNbtServerSide(dto: NBTTagCompound) {
            dto.setTag(SER_INV, inventory.writeToNbt())
        }

        override fun readFromNbtServerSide(dto: NBTTagCompound) {
            inventory.readFromNbt(dto.getTagList(SER_INV, Constants.NBT.TAG_COMPOUND))
        }
    }

    @ClientSide
    class Client(chestBlock: BlockChezt) : State() {
        override val inventory: IItemHandler = ItemStackHandler(chestBlock.config.getSlotCount())

        var lidAngle: Float = 0F
        var prevLidAngle: Float = 0F

        override fun tick() {
            prevLidAngle = lidAngle
            if (syncData.value) {
                if (lidAngle < 1F) {
                    lidAngle = (lidAngle + 0.1F).coerceAtMost(1F)
                }
            } else if (lidAngle > 0) {
                lidAngle = (lidAngle - 0.1F).coerceAtLeast(0F)
            }
        }
    }
}
