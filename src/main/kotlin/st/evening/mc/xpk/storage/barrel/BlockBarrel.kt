package st.evening.mc.xpk.storage.barrel

import net.minecraft.block.ITileEntityProvider
import net.minecraft.block.properties.PropertyBool
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.system.Magic
import st.evening.mc.prelude.api.util.world.findTileEntity
import st.evening.mc.prelude.api.util.world.onServer
import st.evening.mc.prelude.api.util.world.useTileEntity
import st.evening.mc.xpk.storage.StorageConfig
import st.evening.mc.xpk.storage.StorageStrings
import st.evening.mc.xpk.storage.XpkStorage
import st.evening.mc.xpk.storage.base.BlockTieredStorage
import st.evening.mc.xpk.storage.base.TieredStorageContainer
import st.evening.mc.xpk.storage.util.DropState
import st.evening.mc.xpk.storage.util.XpkStorageHelper

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE", "OVERRIDE_DEPRECATION")
@Magic.Use
class BlockBarrel(config: StorageConfig.Barrel) : BlockTieredStorage<StorageConfig.Barrel>(config), ITileEntityProvider,
    BlockSidedIfc by BlockSidedIfc.Delegate(Magic.getThis()) {
    companion object {
        val LOCKED: PropertyBool = PropertyBool.create("locked")

        fun cacheUpgrades(config: List<StorageConfig.Upgrade>): List<Pair<Ingredient, BlockBarrel>> =
            config.mapNotNull {
                Pair(
                    XpkStorageHelper.parseIngredient(it.item) ?: return@mapNotNull null,
                    XpkStorage.defns.getBarrel(it.upgradeTo) ?: return@mapNotNull null
                )
            }
    }

    private var _upgrades: List<Pair<Ingredient, BlockBarrel>>? = null
    val upgrades: List<Pair<Ingredient, BlockBarrel>>
        get() = _upgrades ?: cacheUpgrades(config.upgrades).also { _upgrades = it }

    override fun createBlockState(): BlockStateContainer = BlockStateContainer(this, BlockSidedIfc.PROP_FACING, LOCKED)

    override fun getActualState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState {
        world.useTileEntity<TileEntityBarrel>(pos) {
            return state.withProperty(LOCKED, it.data.get().filterItem != null)
        }
        return state
    }

    override fun createBlockItem(): ItemBlockBarrel = ItemBlockBarrel(this)

    override fun createNewTileEntity(world: World, meta: Int): TileEntity = TileEntityBarrel(this)

    @ServerSide
    override fun getStorageContainer(world: World, pos: BlockPos): TieredStorageContainer? =
        world.findTileEntity<TileEntityBarrel>(pos)?.data?.getServer()

    override fun refreshConfigCache() {
        super.refreshConfigCache()
        _upgrades = null
    }

    @ServerSide
    fun handleLeftClick(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer) {
        world.useTileEntity<TileEntityBarrel>(pos) {
            it.data.getServer().extractItems(world, pos, player, state.getValue(BlockSidedIfc.PROP_FACING).face)
        }
    }

    fun handleRightClick(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand): Boolean {
        if (player.isSneaking) {
            if (!player.getHeldItem(hand).isEmpty) return false
            world.onServer {
                world.useTileEntity<TileEntityBarrel>(pos) {
                    it.data.getServer().toggleFilter(world, pos)
                }
            }
            return true
        } else {
            world.onServer {
                world.useTileEntity<TileEntityBarrel>(pos) {
                    it.data.getServer().insertItems(world, pos, player, hand)
                }
            }
            return true
        }
    }

    override fun tryUpgrade(player: EntityPlayer, pos: BlockPos, state: IBlockState, stack: ItemStack): Boolean {
        upgrades.forEach { (ing, newBlock) ->
            if (!ing.apply(stack)) return@forEach
            val world = player.world
            world.onServer {
                world.useTileEntity<TileEntityBarrel>(pos) { oldTe ->
                    stack.shrink(1)
                    val oldTeServer = oldTe.data.getServer()
                    oldTeServer.dropState = DropState.UPGRADING
                    val newState = newBlock.defaultState
                        .withProperty(BlockSidedIfc.PROP_FACING, state.getValue(BlockSidedIfc.PROP_FACING))
                    world.setBlockState(pos, newState)
                    (world.getTileEntity(pos) as TileEntityBarrel).data.getServer()
                        .copyState(oldTeServer.itemStore, oldTeServer.filterItem)
                    world.playEvent(Constants.WorldEvents.BREAK_BLOCK_EFFECTS, pos, getStateId(newState))
                }
            }
            return true
        }
        return false
    }

    @ClientSide.Physical
    override fun getRenderLayer(): BlockRenderLayer = BlockRenderLayer.CUTOUT

    @ClientSide.Physical
    override fun addInformation(stack: ItemStack, world: World?, tooltip: MutableList<String>, flag: ITooltipFlag) {
        TileEntityBarrel.addStackTooltip(tooltip, stack)
        tooltip += "${TextFormatting.DARK_GRAY}${I18n.format(StorageStrings.TOOLTIP_BARREL_SIZE, config.capacity)}"
        super.addInformation(stack, world, tooltip, flag)
    }
}
