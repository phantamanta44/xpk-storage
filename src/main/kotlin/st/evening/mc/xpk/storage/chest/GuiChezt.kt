package st.evening.mc.xpk.storage.chest

import st.evening.mc.prelude.api.container.prefab.PrefabEngineGuiContainer
import st.evening.mc.prelude.api.gui.drawable.prefab.PreludeDrawables
import st.evening.mc.prelude.api.gui.engine.LayoutEngine
import st.evening.mc.prelude.api.gui.engine.addChild
import st.evening.mc.prelude.api.gui.engine.prefab.BackgroundBox
import st.evening.mc.prelude.api.gui.engine.prefab.ColumnLayout
import st.evening.mc.prelude.api.gui.engine.prefab.InventorySlot
import st.evening.mc.prelude.api.gui.engine.prefab.PaddingBox
import st.evening.mc.prelude.api.gui.engine.prefab.TableLayout
import st.evening.mc.prelude.api.gui.engine.prefab.TextDisplay
import st.evening.mc.prelude.api.gui.engine.setChild
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.render.gui.DrawAlignment

@ClientSide.Strong
class GuiChezt(container: ContainerChezt) : PrefabEngineGuiContainer<ContainerChezt>(container) {
    override val layoutEngine: LayoutEngine = LayoutEngine.fromDsl(
        ColumnLayout(-1, -1, alignX = DrawAlignment.CENTER),
        { BackgroundBox(PaddingBox(it, 6, 7, 7, 7), PreludeDrawables.windowBg) }
    ) {
        addChild(
            PaddingBox(TextDisplay.fromString(container.chest.chestBlock.localizedName), padBottom = 2),
            align = DrawAlignment.START
        )
        val rowCount = container.chest.chestBlock.config.slotRows
        val colCount = container.chest.chestBlock.config.slotColumns
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
                        setChild(i, j, InventorySlot(i * 9 + j))
                    }
                }
            }
        }
    }
}
