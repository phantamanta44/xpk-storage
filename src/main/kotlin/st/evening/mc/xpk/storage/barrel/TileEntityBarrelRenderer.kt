package st.evening.mc.xpk.storage.barrel

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import st.evening.mc.prelude.api.block.prefab.BlockSidedIfc
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.RenderingHelper
import st.evening.mc.prelude.api.util.world.BlockSide

@ClientSide.Physical
object TileEntityBarrelRenderer : TileEntitySpecialRenderer<TileEntityBarrel>() {
    override fun render(
        te: TileEntityBarrel, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float
    ) {
        val data = te.data.get()
        val item = data.itemStore.storedItem ?: data.filterItem ?: return
        val state = te.world.getBlockState(te.pos)
        if (state.block !is BlockBarrel) return

        GlStateManager.depthMask(false)
        RenderingHelper.pushMatrix {
            GlStateManager.translate(x + 0.5, y, z + 0.5)
            when (state.getValue(BlockSidedIfc.PROP_FACING)) {
                BlockSide.NORTH -> {
                    GlStateManager.rotate(180F, 0F, 1F, 0F)
                    RenderingHelper.setLightmapCoordsPacked(world.getCombinedLight(te.pos.north(), 0))
                }
                BlockSide.EAST -> {
                    GlStateManager.rotate(90F, 0F, 1F, 0F)
                    RenderingHelper.setLightmapCoordsPacked(world.getCombinedLight(te.pos.east(), 0))
                }
                BlockSide.SOUTH -> RenderingHelper.setLightmapCoordsPacked(world.getCombinedLight(te.pos.south(), 0))
                BlockSide.WEST -> {
                    GlStateManager.rotate(270F, 0F, 1F, 0F)
                    RenderingHelper.setLightmapCoordsPacked(world.getCombinedLight(te.pos.west(), 0))
                }
            }
            RenderHelper.enableStandardItemLighting()
            GlStateManager.translate(0F, 1F, 0.503125F)
            GlStateManager.scale(0.5F, 0.5F, 0.01F)

            val mc = Minecraft.getMinecraft()
            RenderingHelper.pushMatrix {
                GlStateManager.translate(0F, -0.875F, 0F)
                mc.renderItem.renderItem(item.newStack(1), ItemCameraTransforms.TransformType.GUI)
            }

            GlStateManager.scale(0.03125F, -0.03125F, 1F)
            val fr = mc.fontRenderer
            val countStr = "%,d".format(data.itemStore.count)
            fr.drawString(countStr, -fr.getStringWidth(countStr) * 0.5F, 58F - fr.FONT_HEIGHT, 0xFFFFFF, false)
            RenderingHelper.resetColour()
        }
        GlStateManager.depthMask(true)
    }
}
