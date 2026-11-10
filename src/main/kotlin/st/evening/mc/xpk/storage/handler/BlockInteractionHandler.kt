package st.evening.mc.xpk.storage.handler

import net.minecraft.block.Block.getStateId
import net.minecraft.block.BlockChest
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.items.wrapper.InvWrapper
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.event.ClientClickBlockEvent
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.assertLogicalClient
import st.evening.mc.prelude.api.util.game.onPhysicalClient
import st.evening.mc.prelude.api.util.world.BlockSide
import st.evening.mc.prelude.api.util.world.onServer
import st.evening.mc.prelude.api.util.world.useTileEntity
import st.evening.mc.xpk.storage.XpkStorage
import st.evening.mc.xpk.storage.barrel.BlockBarrel
import st.evening.mc.xpk.storage.barrel.TileEntityBarrel
import st.evening.mc.xpk.storage.base.BlockTieredStorage
import st.evening.mc.xpk.storage.chest.BlockChezt
import st.evening.mc.xpk.storage.chest.TileEntityChezt
import st.evening.mc.xpk.storage.network.C2SExtractFromBarrel

object BlockInteractionHandler {
    private var _vanillaChestUpgrades: List<Pair<Ingredient, BlockChezt>>? = null
    private val vanillaChestUpgrades: List<Pair<Ingredient, BlockChezt>>
        get() = _vanillaChestUpgrades ?: BlockChezt.cacheUpgrades(XpkStorage.config.vanillaChestUpgrades)

    fun refreshConfigCache() {
        _vanillaChestUpgrades = null
    }

    fun init(reg: ModRegistrar) {
        onPhysicalClient {
            reg.on<ClientClickBlockEvent> @OptIn(SidednessAssertion::class) { event ->
                assertLogicalClient {
                    val world = Minecraft.getMinecraft().world
                    val state = world.getBlockState(event.pos)
                    if (state.block is BlockBarrel && state.getValue(BlockSidedIfc.PROP_FACING).face == event.face) {
                        world.useTileEntity<TileEntityBarrel>(event.pos) {
                            if (!it.data.get().itemStore.isEmpty) {
                                event.clickResult = EnumActionResult.SUCCESS
                                XpkStorage.defns.c2sExtractFromBarrel.sendToServer(C2SExtractFromBarrel(event.pos))
                            }
                        }
                    }
                }
            }
        }
        reg.on<PlayerInteractEvent.RightClickBlock> { event ->
            val player = event.entityPlayer
            val world = player.world
            val pos = event.pos
            val state = world.getBlockState(pos)
            if (
                tryBarrelRightClick(world, pos, event.face!!, state, player, event.hand) ||
                tryUpgrade(pos, state, player, player.getHeldItem(event.hand))
            ) {
                event.isCanceled = true
                event.cancellationResult = EnumActionResult.SUCCESS
            }
        }
    }

    private fun tryBarrelRightClick(
        world: World, pos: BlockPos, face: EnumFacing, state: IBlockState, player: EntityPlayer, hand: EnumHand
    ): Boolean {
        val block = state.block
        return block is BlockBarrel && state.getValue(BlockSidedIfc.PROP_FACING).face == face &&
            block.handleRightClick(world, pos, player, hand)
    }

    private fun tryUpgrade(pos: BlockPos, state: IBlockState, player: EntityPlayer, heldStack: ItemStack): Boolean =
        !heldStack.isEmpty && player.isSneaking && when (val block = state.block) {
            is BlockTieredStorage<*> -> block.tryUpgrade(player, pos, state, heldStack)
            is BlockChest -> tryUpgradeVanillaChest(player, pos, state, heldStack)
            else -> false
        }

    private fun tryUpgradeVanillaChest(
        player: EntityPlayer,
        pos: BlockPos,
        state: IBlockState,
        stack: ItemStack
    ): Boolean {
        vanillaChestUpgrades.forEach { (ing, newBlock) ->
            if (!ing.apply(stack)) return@forEach
            val world = player.world
            world.onServer {
                world.useTileEntity<TileEntityChest>(pos) { oldTe ->
                    stack.shrink(1)
                    val newState = newBlock.defaultState.withProperty(
                        BlockSidedIfc.PROP_FACING,
                        BlockSide.fromFace(state.getValue(BlockChest.FACING))
                            ?: throw IllegalStateException(state.getValue(BlockChest.FACING).toString())
                    )
                    world.removeTileEntity(pos) // prevent the chest from dropping its contents
                    world.setBlockState(pos, newState)
                    (world.getTileEntity(pos) as TileEntityChezt).data.getServer().copyState(InvWrapper(oldTe))
                    world.playEvent(Constants.WorldEvents.BREAK_BLOCK_EFFECTS, pos, getStateId(newState))
                }
            }
            return true
        }
        return false
    }
}
