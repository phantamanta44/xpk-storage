package st.evening.mc.xpk.storage.chest

import net.minecraft.block.ITileEntityProvider
import net.minecraft.block.state.BlockFaceShape
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumBlockRenderType
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants
import st.evening.mc.prelude.api.block.CustomItemBlock
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.registration.openContainer
import st.evening.mc.prelude.api.resource
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
class BlockChezt(config: StorageConfig.Chest) : BlockTieredStorage<StorageConfig.Chest>(config), ITileEntityProvider,
    BlockSidedIfc by BlockSidedIfc.Delegate(Magic.getThis()), CustomItemBlock {
    companion object {
        private val BOUNDING_BOX: AxisAlignedBB = AxisAlignedBB(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375)

        fun cacheUpgrades(config: List<StorageConfig.Upgrade>): List<Pair<Ingredient, BlockChezt>> = config.mapNotNull {
            Pair(
                XpkStorageHelper.parseIngredient(it.item) ?: return@mapNotNull null,
                XpkStorage.defns.getChest(it.upgradeTo) ?: return@mapNotNull null
            )
        }
    }

    private var _upgrades: List<Pair<Ingredient, BlockChezt>>? = null
    val upgrades: List<Pair<Ingredient, BlockChezt>>
        get() = _upgrades ?: cacheUpgrades(config.upgrades).also { _upgrades = it }
    var chestTexture: ResourceLocation = XpkStorage.resource("textures/entity/chest/${config.chestModelTexture}.png")

    override fun createBlockState(): BlockStateContainer = BlockStateContainer(this, BlockSidedIfc.PROP_FACING)

    override fun createBlockItem(): ItemBlockChezt = ItemBlockChezt(this)

    override fun createNewTileEntity(world: World, meta: Int): TileEntity = TileEntityChezt(this)

    @ServerSide
    override fun getStorageContainer(world: World, pos: BlockPos): TieredStorageContainer? =
        world.findTileEntity<TileEntityChezt>(pos)?.data?.getServer()

    override fun refreshConfigCache() {
        super.refreshConfigCache()
        _upgrades = null
        chestTexture = XpkStorage.resource("textures/entity/chest/${config.chestModelTexture}.png")
    }

    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = BOUNDING_BOX

    override fun onBlockActivated(
        world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand,
        facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        world.onServer {
            if (config.checkLidObstruction) {
                val abovePos = pos.up()
                if (world.getBlockState(abovePos).doesSideBlockChestOpening(world, abovePos, EnumFacing.DOWN)) {
                    return true
                }
            }
            player.openContainer(XpkStorage.defns.containerChest, world, pos)
        }
        return true
    }

    override fun tryUpgrade(player: EntityPlayer, pos: BlockPos, state: IBlockState, stack: ItemStack): Boolean {
        upgrades.forEach { (ing, newBlock) ->
            if (!ing.apply(stack)) return@forEach
            val world = player.world
            world.onServer {
                world.useTileEntity<TileEntityChezt>(pos) { oldTe ->
                    stack.shrink(1)
                    val oldTeServer = oldTe.data.getServer()
                    oldTeServer.dropState = DropState.UPGRADING
                    val newState = newBlock.defaultState
                        .withProperty(BlockSidedIfc.PROP_FACING, state.getValue(BlockSidedIfc.PROP_FACING))
                    world.setBlockState(pos, newState)
                    (world.getTileEntity(pos) as TileEntityChezt).data.getServer().copyState(oldTeServer.inventory)
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
    ): BlockFaceShape = BlockFaceShape.UNDEFINED

    override fun getRenderType(state: IBlockState): EnumBlockRenderType = EnumBlockRenderType.ENTITYBLOCK_ANIMATED

    @ClientSide.Physical
    override fun hasCustomBreakingProgress(state: IBlockState): Boolean = true

    @ClientSide.Physical
    override fun addInformation(stack: ItemStack, world: World?, tooltip: MutableList<String>, flag: ITooltipFlag) {
        TileEntityChezt.addStackTooltip(tooltip, stack)
        tooltip += "${TextFormatting.DARK_GRAY}${I18n.format(StorageStrings.TOOLTIP_CHEST_SIZE, config.getSlotCount())}"
        super.addInformation(stack, world, tooltip, flag)
    }
}
