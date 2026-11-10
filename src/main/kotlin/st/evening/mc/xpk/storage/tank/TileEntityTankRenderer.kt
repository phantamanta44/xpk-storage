package st.evening.mc.xpk.storage.tank

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import org.lwjgl.opengl.GL11
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.getStillSprite
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.render.tessellate

@ClientSide.Physical
object TileEntityTankRenderer : TileEntitySpecialRenderer<TileEntityTank>() {
    override fun render(
        te: TileEntityTank, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float
    ) {
        val data = te.data.get()
        RenderingHelper.pushMatrix {
            GlStateManager.translate(x, y, z)
            val fluid = data.syncData.fluid
            if (fluid != null && fluid.amount > 0) { // TODO could interpolate the fill amount
                RenderingHelper.setLightmapCoordsPacked(
                    te.world.getCombinedLight(te.pos, fluid.fluid.getLuminosity(fluid))
                )
                renderFluid(fluid, data.syncData.capacity)
            }
        }
    }

    private fun renderFluid(fluid: FluidStack, capacity: Int) {
        val sprite = fluid.getStillSprite()
        val fill = (fluid.amount / capacity.toFloat()).coerceAtMost(1F)

        RenderHelper.disableStandardItemLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
        RenderingHelper.setColourRgb(fluid.fluid.getColor(fluid))
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)

        val u1 = sprite.getInterpolatedU(2.0).toDouble()
        val u2 = sprite.getInterpolatedU(14.0).toDouble()
        val y1: Double
        val y2: Double
        val v1: Double
        val v2: Double
        if (fluid.fluid.isGaseous(fluid)) {
            y1 = 1.0 - fill.toDouble()
            y2 = 1.0
            v1 = sprite.getInterpolatedV(16.0 * (1.0 - fill)).toDouble()
            v2 = sprite.maxV.toDouble()
        } else {
            y1 = 0.0
            y2 = fill.toDouble()
            v1 = sprite.minV.toDouble()
            v2 = sprite.getInterpolatedV(16.0 * fill).toDouble()
        }

        val tess = Tessellator.getInstance()
        tess.tessellate(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_TEX) {
            pos(0.13125, y1, 0.13125).tex(u1, v2).endVertex()
            pos(0.13125, y2, 0.13125).tex(u1, v1).endVertex()
            pos(0.86875, y1, 0.13125).tex(u2, v2).endVertex()
            pos(0.86875, y2, 0.13125).tex(u2, v1).endVertex()
            pos(0.86875, y1, 0.86875).tex(u1, v2).endVertex()
            pos(0.86875, y2, 0.86875).tex(u1, v1).endVertex()
            pos(0.13125, y1, 0.86875).tex(u2, v2).endVertex()
            pos(0.13125, y2, 0.86875).tex(u2, v1).endVertex()
            pos(0.13125, y1, 0.13125).tex(u1, v2).endVertex()
            pos(0.13125, y2, 0.13125).tex(u1, v1).endVertex()
        }
        if (fill < 1F) {
            if (y1 > 0.0) {
                val v1t = sprite.getInterpolatedV(2.0).toDouble()
                val v2t = sprite.getInterpolatedV(14.0).toDouble()
                tess.tessellate(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX) {
                    pos(0.13125, y1, 0.13125).tex(u1, v1t).endVertex()
                    pos(0.86875, y1, 0.13125).tex(u2, v1t).endVertex()
                    pos(0.86875, y1, 0.86875).tex(u2, v2t).endVertex()
                    pos(0.13125, y1, 0.86875).tex(u1, v2t).endVertex()
                }
            } else {
                val v1t = sprite.getInterpolatedV(2.0).toDouble()
                val v2t = sprite.getInterpolatedV(14.0).toDouble()
                tess.tessellate(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX) {
                    pos(0.13125, y2, 0.13125).tex(u1, v1t).endVertex()
                    pos(0.13125, y2, 0.86875).tex(u1, v2t).endVertex()
                    pos(0.86875, y2, 0.86875).tex(u2, v2t).endVertex()
                    pos(0.86875, y2, 0.13125).tex(u2, v1t).endVertex()
                }
            }
        }

        RenderingHelper.resetColour()
        RenderHelper.enableStandardItemLighting()
    }

    object ItemRenderer : TileEntityItemStackRenderer() {
        override fun renderByItem(stack: ItemStack, partialTicks: Float) {
            val item = stack.item
            if (item !is ItemBlockTank) return

            GlStateManager.enableDepth()
            GlStateManager.depthFunc(GL11.GL_LEQUAL)
            GlStateManager.depthMask(true)

            val mc = Minecraft.getMinecraft()
            RenderingHelper.pushMatrix {
                GlStateManager.translate(0.5F, 0.5F, 0.5F)
                mc.renderItem.renderItem(
                    stack, mc.blockRendererDispatcher.getModelForState(item.tankBlock.defaultState)
                )
            }

            val fluid = TileEntityTank.readStackFluid(stack)
            if (fluid == null || fluid.amount <= 0) return
            GlStateManager.pushAttrib()
            renderFluid(fluid, item.tankBlock.config.capacity)
            GlStateManager.popAttrib()
        }
    }
}
