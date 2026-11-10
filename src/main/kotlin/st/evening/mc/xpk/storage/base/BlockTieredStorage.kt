package st.evening.mc.xpk.storage.base

import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.color.IBlockColor
import net.minecraft.client.renderer.color.IItemColor
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.IRarity
import st.evening.mc.prelude.api.block.CustomItemBlock
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.world.onServer
import st.evening.mc.xpk.storage.StorageConfig
import st.evening.mc.xpk.storage.StorageStrings
import st.evening.mc.xpk.storage.XpkStorage
import st.evening.mc.xpk.storage.util.DropState

interface TieredStorageContainer {
    var dropState: DropState

    fun computeComparatorValue(): Int

    fun dropContents(world: World, pos: BlockPos, block: BlockTieredStorage<*>, keepContents: Boolean)

    fun restoreFromStack(stack: ItemStack)
}

@Suppress("OVERRIDE_DEPRECATION")
abstract class BlockTieredStorage<C : StorageConfig.Block>(val config: C) :
    Block(config.material.material), CustomItemBlock {

    var colour: Int = config.colour.toInt(16)
        private set

    init {
        val materialType = config.material
        setSoundType(materialType.soundType)
        setHardness(materialType.hardness)
        setCreativeTab(XpkStorage.defns.creativeTab)
    }

    override fun createBlockItem(): ItemBlockTieredStorage<C, *> = ItemBlockTieredStorage(this)

    @ServerSide
    protected abstract fun getStorageContainer(world: World, pos: BlockPos): TieredStorageContainer?

    open fun refreshConfigCache() {
        colour = config.colour.toInt(16)
    }

    override fun onBlockPlacedBy(
        world: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack
    ) {
        world.onServer {
            getStorageContainer(world, pos)?.restoreFromStack(stack)
        }
    }

    override fun getHarvestTool(state: IBlockState): String? = config.material.toolType

    open fun tryUpgrade(player: EntityPlayer, pos: BlockPos, state: IBlockState, stack: ItemStack): Boolean = false

    override fun hasComparatorInputOverride(state: IBlockState): Boolean = true

    override fun getComparatorInputOverride(state: IBlockState, world: World, pos: BlockPos): Int {
        world.onServer {
            getStorageContainer(world, pos)?.let {
                return it.computeComparatorValue()
            }
        }
        return 0
    }

    override fun dropBlockAsItemWithChance(
        world: World, pos: BlockPos, state: IBlockState, chance: Float, fortune: Int
    ) {
        if (!config.keepContents) {
            super.dropBlockAsItemWithChance(world, pos, state, chance, fortune)
        }
    }

    override fun onBlockHarvested(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer) {
        if (player.capabilities.isCreativeMode) {
            world.onServer {
                getStorageContainer(world, pos)?.dropState = DropState.BROKEN_BY_CREATIVE_PLAYER
            }
        }
    }

    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        world.onServer {
            getStorageContainer(world, pos)?.let {
                it.dropContents(world, pos, this, config.keepContents)
                world.updateComparatorOutputLevel(pos, this)
            }
        }
        super.breakBlock(world, pos, state)
    }

    @ClientSide.Physical
    override fun addInformation(stack: ItemStack, world: World?, tooltip: MutableList<String>, flag: ITooltipFlag) {
        if (config.keepContents) {
            tooltip += "${TextFormatting.GOLD}${I18n.format(StorageStrings.TOOLTIP_KEEP_CONTENTS)}"
        }
    }

    enum class MaterialType(
        val material: Material,
        val soundType: SoundType,
        val hardness: Float,
        val toolType: String?
    ) {
        WOOD(Material.WOOD, SoundType.WOOD, 3F, "axe"),
        STONE(Material.ROCK, SoundType.STONE, 4F, "pickaxe"),
        METAL(Material.IRON, SoundType.METAL, 5F, "pickaxe"),
        DIRT(Material.GROUND, SoundType.GROUND, 3F, "shovel"),
        CLOTH(Material.CLOTH, SoundType.CLOTH, 2.5F, null),
        GLASS(Material.GLASS, SoundType.GLASS, 2.5F, null)
    }

    @ClientSide.Physical
    class ColourHandler(private val block: BlockTieredStorage<*>) : IBlockColor, IItemColor {
        override fun colorMultiplier(state: IBlockState, worldIn: IBlockAccess?, pos: BlockPos?, tintIndex: Int): Int =
            if (tintIndex == 1) block.colour else -1

        override fun colorMultiplier(stack: ItemStack, tintIndex: Int): Int = if (tintIndex == 1) block.colour else -1
    }
}

open class ItemBlockTieredStorage<C : StorageConfig.Block, B : BlockTieredStorage<C>>(val storageBlock: B) :
    ItemBlock(storageBlock) {

    override fun getForgeRarity(stack: ItemStack): IRarity = storageBlock.config.rarity
}
