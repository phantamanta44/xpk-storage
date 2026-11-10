package st.evening.mc.xpk.storage.chest

import net.minecraft.client.model.ModelChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.SidednessAssertion
import st.evening.mc.prelude.api.util.game.assertLogicalClient
import st.evening.mc.prelude.api.util.math.MathsHelper
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.world.BlockSide

@ClientSide.Physical
object TileEntityCheztRenderer : TileEntitySpecialRenderer<TileEntityChezt>() {
    private val model: ModelChest = ModelChest()

    @OptIn(SidednessAssertion::class)
    override fun render(
        te: TileEntityChezt, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float
    ) {
        val state = te.world.getBlockState(te.pos)
        val block = state.block
        if (block !is BlockChezt) return

        if (destroyStage >= 0) {
            bindTexture(DESTROY_STAGES[destroyStage])
            GlStateManager.matrixMode(GL11.GL_TEXTURE)
            GlStateManager.pushMatrix()
            GlStateManager.scale(4F, 4F, 1F)
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F)
            GlStateManager.matrixMode(GL11.GL_MODELVIEW)
        } else {
            bindTexture(block.chestTexture)
            if (block.config.chestModelTint) {
                RenderingHelper.setColourRgb(block.colour, alpha)
            } else {
                GlStateManager.color(1F, 1F, 1F, alpha)
            }
        }

        RenderingHelper.pushMatrix {
            GlStateManager.enableRescaleNormal()
            GlStateManager.translate(x, y + 1.0, z + 1.0)
            GlStateManager.scale(1F, -1F, -1F)
            assertLogicalClient {
                when (state.getValue(BlockSidedIfc.PROP_FACING)) {
                    BlockSide.NORTH -> rotateModel(180F)
                    BlockSide.EAST -> rotateModel(270F)
                    BlockSide.SOUTH -> {}
                    BlockSide.WEST -> rotateModel(90F)
                }
                val teClient = te.data.getClient()
                val f = 1F - (teClient.prevLidAngle + (teClient.lidAngle - teClient.prevLidAngle) * partialTicks)
                model.chestLid.rotateAngleX = (f * f * f - 1F) * (MathsHelper.PI_F * 0.5F)
            }
            model.renderAll()
            GlStateManager.disableRescaleNormal()
        }

        if (destroyStage >= 0) {
            GlStateManager.matrixMode(GL11.GL_TEXTURE)
            GlStateManager.popMatrix()
            GlStateManager.matrixMode(GL11.GL_MODELVIEW)
        } else {
            RenderingHelper.resetColour()
        }
    }

    fun renderItem(block: BlockChezt) {
        GlStateManager.enableDepth()
        GlStateManager.depthFunc(GL11.GL_LEQUAL)
        GlStateManager.depthMask(true)

        bindTexture(block.chestTexture)
        if (block.config.chestModelTint) {
            RenderingHelper.setColourRgb(block.colour, 1F)
        }
        RenderingHelper.pushMatrix {
            GlStateManager.enableRescaleNormal()
            GlStateManager.translate(0F, 1F, 1F)
            GlStateManager.scale(1F, -1F, -1F)
            model.chestLid.rotateAngleX = 0F
            model.renderAll()
            GlStateManager.disableRescaleNormal()
        }
        RenderingHelper.resetColour()
    }

    private fun rotateModel(angle: Float) {
        GlStateManager.translate(0.5F, 0.5F, 0.5F)
        GlStateManager.rotate(angle, 0F, 1F, 0F)
        GlStateManager.translate(-0.5F, -0.5F, -0.5F)
    }

    object ItemRenderer : TileEntityItemStackRenderer() {
        override fun renderByItem(stack: ItemStack, partialTicks: Float) {
            renderItem((stack.item as? ItemBlock ?: return).block as? BlockChezt ?: return)
        }
    }
}
