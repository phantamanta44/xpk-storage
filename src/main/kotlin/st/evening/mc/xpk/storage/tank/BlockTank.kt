package st.evening.mc.xpk.storage.tank

import net.minecraft.block.ITileEntityProvider
import net.minecraft.block.state.BlockFaceShape
import net.minecraft.block.state.IBlockState
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fluids.FluidUtil
import st.evening.mc.prelude.api.block.CustomItemBlock
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
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

@Suppress("OVERRIDE_DEPRECATION")
class BlockTank(config: StorageConfig.Tank) :
    BlockTieredStorage<StorageConfig.Tank>(config), ITileEntityProvider, CustomItemBlock {
    companion object {
        private val BOUNDING_BOX: AxisAlignedBB = AxisAlignedBB(0.125, 0.0, 0.125, 0.875, 1.0, 0.875)

        fun cacheUpgrades(config: List<StorageConfig.Upgrade>): List<Pair<Ingredient, BlockTank>> =
            config.mapNotNull {
                Pair(
                    XpkStorageHelper.parseIngredient(it.item) ?: return@mapNotNull null,
                    XpkStorage.defns.getTank(it.upgradeTo) ?: return@mapNotNull null
                )
            }
    }

    private var _upgrades: List<Pair<Ingredient, BlockTank>>? = null
    val upgrades: List<Pair<Ingredient, BlockTank>>
        get() = _upgrades ?: cacheUpgrades(config.upgrades).also { _upgrades = it }

    override fun createBlockItem(): ItemBlockTank = ItemBlockTank(this)

    override fun createNewTileEntity(world: World, meta: Int): TileEntity = TileEntityTank(this)

    @ServerSide
    override fun getStorageContainer(world: World, pos: BlockPos): TieredStorageContainer? =
        world.findTileEntity<TileEntityTank>(pos)?.data?.getServer()

    override fun refreshConfigCache() {
        super.refreshConfigCache()
        _upgrades = null
    }

    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = BOUNDING_BOX

    override fun onBlockActivated(
        world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand,
        facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        world.onServer {
            world.useTileEntity<TileEntityTank>(pos) {
                FluidUtil.interactWithFluidHandler(player, hand, it.data.getServer().syncData)
            }
        }
        return true
    }

    override fun tryUpgrade(player: EntityPlayer, pos: BlockPos, state: IBlockState, stack: ItemStack): Boolean {
        upgrades.forEach { (ing, newBlock) ->
            if (!ing.apply(stack)) return@forEach
            val world = player.world
            world.onServer {
                world.useTileEntity<TileEntityTank>(pos) { oldTe ->
                    stack.shrink(1)
                    val oldTeServer = oldTe.data.getServer()
                    oldTeServer.dropState = DropState.UPGRADING
                    val newState = newBlock.defaultState
                    world.setBlockState(pos, newState)
                    (world.getTileEntity(pos) as TileEntityTank).data.getServer().copyState(oldTeServer.syncData)
                    world.playEvent(Constants.WorldEvents.BREAK_BLOCK_EFFECTS, pos, getStateId(newState))
                }
            }
            return true
        }
        return false
    }

    override fun isOpaqueCube(state: IBlockState): Boolean = false

    override fun isFullCube(state: IBlockState): Boolean = false

    override fun getBlockFaceShape(
        world: IBlockAccess, state: IBlockState, pos: BlockPos, face: EnumFacing
    ): BlockFaceShape = when (face) {
        EnumFacing.UP, EnumFacing.DOWN -> BlockFaceShape.CENTER
        else -> BlockFaceShape.UNDEFINED
    }

    @ClientSide.Physical
    override fun getRenderLayer(): BlockRenderLayer = BlockRenderLayer.CUTOUT

    @ClientSide.Physical
    override fun addInformation(stack: ItemStack, world: World?, tooltip: MutableList<String>, flag: ITooltipFlag) {
        TileEntityTank.addStackTooltip(tooltip, stack)
        tooltip += "${TextFormatting.DARK_GRAY}${I18n.format(StorageStrings.TOOLTIP_TANK_SIZE, config.capacity)}"
        super.addInformation(stack, world, tooltip, flag)
    }
}
