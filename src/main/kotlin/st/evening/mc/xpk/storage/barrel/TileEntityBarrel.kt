package st.evening.mc.xpk.storage.barrel

import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap
import net.minecraft.client.resources.I18n
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import st.evening.mc.prelude.Prelude
import st.evening.mc.prelude.api.capability.ItemStore
import st.evening.mc.prelude.api.component.item.ItemStoreItemHandler
import st.evening.mc.prelude.api.component.item.ObservableItemStore
import st.evening.mc.prelude.api.data.ser.ServerSideSerializable
import st.evening.mc.prelude.api.data.state.ListStateComposite
import st.evening.mc.prelude.api.data.state.Observable
import st.evening.mc.prelude.api.data.state.Observer
import st.evening.mc.prelude.api.data.state.Piecewise
import st.evening.mc.prelude.api.data.state.ValueStateAtom
import st.evening.mc.prelude.api.data.state.observeAll
import st.evening.mc.prelude.api.data.sync.SyncContainer
import st.evening.mc.prelude.api.tileentity.prefab.PrefabTileEntitySynced
import st.evening.mc.prelude.api.util.collection.IdentityHashStrategy
import st.evening.mc.prelude.api.util.data.NbtDsl
import st.evening.mc.prelude.api.util.data.getCompoundOrNull
import st.evening.mc.prelude.api.util.data.getIntOrNull
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.InvHelper
import st.evening.mc.prelude.api.util.game.ItemKey
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.game.Sided
import st.evening.mc.prelude.api.util.game.computeComparatorValue
import st.evening.mc.prelude.api.util.game.dataTagOrNull
import st.evening.mc.prelude.api.util.game.forEachSlot
import st.evening.mc.prelude.api.util.game.getOrCreateDataTag
import st.evening.mc.prelude.api.util.system.Hoist
import st.evening.mc.xpk.storage.StorageStrings
import st.evening.mc.xpk.storage.XpkStorage
import st.evening.mc.xpk.storage.base.BlockTieredStorage
import st.evening.mc.xpk.storage.base.TieredStorageContainer
import st.evening.mc.xpk.storage.util.DropState

class TileEntityBarrel @Suppress("unused") private constructor(val barrelBlock: BlockBarrel, unused: Unit) :
    PrefabTileEntitySynced<TileEntityBarrel.State, TileEntityBarrel.Server, TileEntityBarrel.Client>() {
    companion object {
        private const val DOUBLE_INSERT_INTERVAL: Int = 10
        private const val SER_INV: String = "inv"
        private const val SER_FILTER: String = "filter"

        fun stackHasContents(stack: ItemStack): Boolean =
            stack.dataTagOrNull?.getCompoundTag(SER_INV)?.getIntOrNull("count")?.let { it > 0 } ?: false

        fun copyStackContents(srcStack: ItemStack, dstStack: ItemStack) {
            srcStack.dataTagOrNull?.getCompoundOrNull(SER_INV)?.let {
                dstStack.getOrCreateDataTag().setTag(SER_INV, it.copy())
            }
        }

        @ClientSide.Physical
        fun addStackTooltip(tooltip: MutableList<String>, stack: ItemStack) {
            val dto = stack.dataTagOrNull ?: return
            dto.getCompoundOrNull(SER_INV)?.let { invDto ->
                invDto.getCompoundOrNull("item")?.let { // bit questionable to be using ObservableItemStore internals
                    tooltip += TextFormatting.GRAY.toString() + I18n.format(
                        StorageStrings.TOOLTIP_BARREL_CONTENTS,
                        invDto.getInteger("count"),
                        ItemKey.Serializer.deserializeFromNbt(it).newStack(1).displayName
                    )
                    return
                }
            }
            dto.getCompoundOrNull(SER_FILTER)?.let {
                tooltip += TextFormatting.GRAY.toString() + I18n.format(
                    StorageStrings.TOOLTIP_BARREL_CONTENTS,
                    0, ItemKey.Serializer.deserializeFromNbt(it).newStack(1).displayName
                )
            }
        }
    }

    constructor(barrelBlock: BlockBarrel) : this(barrelBlock, Unit.also { Hoist.push(barrelBlock) })

    // field will not be populated by the time this is called in the super-constructor
    override fun initData(): Sided<State, Server, Client> = Sided.of({ Server(Hoist.pop()) }, { Client(Hoist.pop()) })

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean = when (capability) {
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Prelude.defns.capItemStore -> true
        else -> super.hasCapability(capability, facing)
    }

    override fun <T> getCapability(capability: Capability<T?>, facing: EnumFacing?): T? = when (capability) {
        CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ->
            CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(data.get().itemHandler)
        Prelude.defns.capItemStore -> Prelude.defns.capItemStore.cast(data.get().itemStore)
        else -> super.getCapability(capability, facing)
    }

    abstract class State(barrelBlock: BlockBarrel) : SyncContainer {
        protected val inventory: ObservableItemStore = ObservableItemStore(barrelBlock.config.capacity)
        protected val filter: ValueStateAtom<ItemKey?> = ValueStateAtom(null, ItemKey.Serializer.Nullable)

        val filterItem: ItemKey?
            get() = filter.value

        override val syncData: Piecewise = ListStateComposite.fromStates(inventory, filter)

        val itemStore: ItemStore = FilterItemStore()
        val itemHandler: IItemHandler = ItemStoreItemHandler(itemStore)

        private inner class FilterItemStore : ItemStore by inventory {
            override fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack {
                if (filter.value?.matches(stack) == false) return stack
                return inventory.insertItem(stack, simulate)
            }
        }
    }

    @ServerSide
    class Server(
        barrelBlock: BlockBarrel,
        private val state: ListStateComposite.Builder = ListStateComposite.Builder()
    ) : State(barrelBlock), TieredStorageContainer, ServerSideSerializable, Observable.Composite by state.build() {
        private val insertTimes: Object2LongMap<EntityPlayer> = Object2LongOpenCustomHashMap(IdentityHashStrategy())

        override var dropState: DropState = DropState.NORMAL

        init {
            state part inventory
            state part filter
        }

        private fun checkAndScrubDoubleInsert(player: EntityPlayer): Boolean {
            val now = player.world.totalWorldTime
            val iter = insertTimes.object2LongEntrySet().iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                if (entry.key == player) {
                    return now - entry.setValue(now) <= DOUBLE_INSERT_INTERVAL
                } else if (now - entry.longValue > DOUBLE_INSERT_INTERVAL) {
                    iter.remove()
                }
            }
            insertTimes[player] = now
            return false
        }

        fun insertItems(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand) {
            if (checkAndScrubDoubleInsert(player)) {
                if (insertAll(player)) {
                    world.playSound(null, pos, XpkStorage.defns.soundBarrelInsertAll, SoundCategory.BLOCKS, 1F, 1F)
                }
            } else {
                val stack = player.getHeldItem(hand)
                if (stack.isEmpty) return
                val rem = itemStore.insertItem(stack, false)
                if (rem.count >= stack.count) return
                player.setHeldItem(hand, rem)
                world.playSound(
                    null, pos, XpkStorage.defns.soundBarrelInsert, SoundCategory.BLOCKS,
                    1F, 0.9F + 0.2F * world.rand.nextFloat()
                )
            }
        }

        private fun insertAll(player: EntityPlayer): Boolean {
            val stored = inventory.storedItem ?: filter.value ?: return false
            val inv = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) ?: return false
            var success = false
            inv.forEachSlot {
                val simStack = inv.getStackInSlot(it)
                if (!stored.matches(simStack)) return@forEachSlot
                val toInsert = simStack.count - inventory.insertItem(simStack, true).count
                if (toInsert <= 0) return success // probably full, so abort
                val stack = inv.extractItem(it, toInsert, false)
                if (stack.isEmpty) return@forEachSlot
                success = true
                val rem = inventory.insertItem(stack, false)
                if (!rem.isEmpty) {
                    // there shouldn't be any remainder, but if there is, make the best effort to return it
                    inv.insertItem(it, rem, false)
                }
            }
            return success
        }

        fun extractItems(world: World, pos: BlockPos, player: EntityPlayer, face: EnumFacing) {
            val stored = inventory.storedItem ?: return
            val extracted = inventory.extractItem(if (player.isSneaking) stored.newStack(1).maxStackSize else 1, false)
            if (extracted.isEmpty) return
            world.playSound(
                null, pos, XpkStorage.defns.soundBarrelExtract, SoundCategory.BLOCKS,
                1F, 0.9F + 0.2F * world.rand.nextFloat()
            )
            player.inventory.addItemStackToInventory(extracted)
            if (extracted.isEmpty) return
            val entity = EntityItem(
                world, pos.x + 0.5 + face.xOffset * 0.6, pos.y + 0.5, pos.z + 0.5 + face.zOffset * 0.6, extracted
            )
            entity.motionX = face.xOffset * 0.08
            entity.motionY = 0.05
            entity.motionZ = face.zOffset * 0.08
            world.spawnEntity(entity)
        }

        fun toggleFilter(world: World, pos: BlockPos) {
            if (filter.value == null) {
                if (filter.update(inventory.storedItem)) {
                    world.playSound(null, pos, XpkStorage.defns.soundBarrelLock, SoundCategory.BLOCKS, 1F, 1F)
                }
            } else {
                filter.value = null
                world.playSound(null, pos, XpkStorage.defns.soundBarrelUnlock, SoundCategory.BLOCKS, 1F, 1F)
            }
        }

        override fun computeComparatorValue(): Int = inventory.computeComparatorValue()

        override fun dropContents(world: World, pos: BlockPos, block: BlockTieredStorage<*>, keepContents: Boolean) {
            if (dropState == DropState.UPGRADING) return
            if (keepContents) {
                if (inventory.isEmpty) {
                    if (dropState == DropState.NORMAL) {
                        InvHelper.dropItem(ItemStack(block), world, pos)
                    }
                } else {
                    val stack = ItemStack(block)
                    stack.tagCompound = NbtDsl.compound {
                        SER_INV tag inventory.writeToNbt()
                        filter.value?.let {
                            SER_FILTER tag ItemKey.Serializer.serializeToNbt(it)
                        }
                    }
                    InvHelper.dropItem(stack, world, pos)
                }
            } else {
                InvHelper.dropItems(inventory, world, pos)
            }
        }

        override fun restoreFromStack(stack: ItemStack) {
            stack.dataTagOrNull?.let { dto ->
                dto.getCompoundOrNull(SER_INV)?.let { inventory.readFromNbt(it) }
                filter.setValueFromSync(
                    dto.getCompoundOrNull(SER_FILTER)?.let { ItemKey.Serializer.deserializeFromNbt(it) }
                )
            }
        }

        fun copyState(store: ItemStore, filterItem: ItemKey?) {
            val storedItem = store.storedItem
            if (storedItem != null) {
                inventory.setItem(storedItem, store.count)
            } else {
                inventory.clearItem()
            }
            filter.setValueFromSync(filterItem)
        }

        override fun writeToNbtServerSide(dto: NBTTagCompound) {
            dto.setTag(SER_INV, inventory.writeToNbt())
            filter.value?.let {
                dto.setTag(SER_FILTER, ItemKey.Serializer.serializeToNbt(it))
            }
        }

        override fun readFromNbtServerSide(dto: NBTTagCompound) {
            inventory.readFromNbt(dto.getCompoundTag(SER_INV))
            filter.setValueFromSync(
                dto.getCompoundOrNull(SER_FILTER)?.let { ItemKey.Serializer.deserializeFromNbt(it) }
            )
        }
    }

    @ClientSide
    inner class Client(barrelBlock: BlockBarrel) : State(barrelBlock) {
        init {
            filter.observeAll(Observer.Simple.fixed {
                world.markBlockRangeForRenderUpdate(pos, pos)
            })
        }
    }
}
