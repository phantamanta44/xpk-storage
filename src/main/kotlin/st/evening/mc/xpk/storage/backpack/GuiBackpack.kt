package st.evening.mc.xpk.storage.backpack

import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import st.evening.mc.prelude.api.container.prefab.PrefabEngineGuiContainer
import st.evening.mc.prelude.api.gui.drawable.prefab.PreludeDrawables
import st.evening.mc.prelude.api.gui.engine.GuiContext
import st.evening.mc.prelude.api.gui.engine.GuiPart
import st.evening.mc.prelude.api.gui.engine.LayoutEngine
import st.evening.mc.prelude.api.gui.engine.addChild
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiElement
import st.evening.mc.prelude.api.gui.engine.prefab.AbstractGuiPart
import st.evening.mc.prelude.api.gui.engine.prefab.BackgroundBox
import st.evening.mc.prelude.api.gui.engine.prefab.ColumnLayout
import st.evening.mc.prelude.api.gui.engine.prefab.InventorySlot
import st.evening.mc.prelude.api.gui.engine.prefab.PaddingBox
import st.evening.mc.prelude.api.gui.engine.prefab.TableLayout
import st.evening.mc.prelude.api.gui.engine.prefab.TextDisplay
import st.evening.mc.prelude.api.gui.engine.setChild
import st.evening.mc.prelude.api.util.game.ClientHelper
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.getColouredTooltip
import st.evening.mc.prelude.api.util.math.containsPoint
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment
import st.evening.mc.xpk.storage.XpkStorage
import java.util.Random

@ClientSide.Strong
class GuiBackpack(container: ContainerBackpack) : PrefabEngineGuiContainer<ContainerBackpack>(container) {
    companion object {
        private val rand: Random = Random()
    }

    private var firstInit: Boolean = true

    override fun initGui() {
        super.initGui()
        if (firstInit) {
            firstInit = false
            mc.soundHandler.playSound(
                PositionedSoundRecord.getRecord(XpkStorage.defns.soundBackpackOpen, 0.95F + rand.nextFloat() * 0.1F, 1F)
            )
        }
    }

    override val layoutEngine: LayoutEngine = LayoutEngine.fromDsl(
        ColumnLayout(-1, -1, alignX = DrawAlignment.CENTER),
        { BackgroundBox(PaddingBox(it, 6, 7, 7, 7), PreludeDrawables.windowBg) }
    ) {
        addChild(
            PaddingBox(
                TextDisplay.fromString(container.backpackItem.getItemStackDisplayName(container.backpackStack)),
                padBottom = 2
            ),
            align = DrawAlignment.START
        )
        val rowCount = container.backpackItem.config.slotRows
        val colCount = container.backpackItem.config.slotColumns
        addChild(TableLayout(rowCount, colCount, -1, -1)) {
            for (i in 0..<rowCount) {
                for (j in 0..<colCount) {
                    setChild(i, j, InventorySlot(36 + i * colCount + j))
                }
            }
        }
        addChild(ColumnLayout(-1, -1)) {
            addChild(PaddingBox(TextDisplay.constant(container.playerInv.displayName), padTop = 3, padBottom = 1))
            addChild(TableLayout(4, 9, -1, -1)) {
                configure {
                    configureRow(3, startPadding = 4, flex = 1F)
                }
                for (i in 0..<4) {
                    for (j in 0..<9) {
                        val slotIndex = i * 9 + j
                        setChild(
                            i, j,
                            if (container.getSlot(slotIndex).isEnabled) {
                                InventorySlot(slotIndex)
                            } else {
                                LockedInventorySlot(container.backpackStack)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        if (mc.player.openContainer != inventorySlots) {
            mc.soundHandler.playSound(
                PositionedSoundRecord.getRecord(
                    XpkStorage.defns.soundBackpackClose, 0.95F + rand.nextFloat() * 0.1F, 1F
                )
            )
        }
    }

    private class LockedInventorySlot(private val stack: ItemStack) : AbstractGuiElement() {
        override val contentWidth: Int
            get() = 18
        override val contentHeight: Int
            get() = 18

        override fun bakeDimensioned(posX: Int, posY: Int, width: Int, height: Int): GuiPart = Part(posX, posY)

        private inner class Part(posX: Int, posY: Int) : AbstractGuiPart(posX, posY) {
            override val width: Int
                get() = 18
            override val height: Int
                get() = 18

            override fun drawForeground(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int) {
                GlStateManager.enableLighting()
                GlStateManager.enableDepth()
                val mc = context.gui.mc
                mc.renderItem.renderItemAndEffectIntoGUI(mc.player, stack, posX + 1, posY + 1)
                mc.renderItem.renderItemOverlayIntoGUI(mc.fontRenderer, stack, posX + 1, posY + 1, null)
                GlStateManager.disableDepth()
                GlStateManager.disableLighting()
            }

            override fun drawTooltip(context: GuiContext, partialTicks: Float, mouseX: Int, mouseY: Int): Boolean {
                if (!containsPoint(mouseX, mouseY)) return false
                val tooltip = mutableListOf<String>()
                stack.getColouredTooltip(tooltip, ClientHelper.tooltipFlags)
                context.gui.drawHoveringText(tooltip, mouseX, mouseY)
                return true
            }
        }
    }
}
