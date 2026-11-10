package st.evening.mc.xpk.storage.util

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.util.ResourceLocation
import st.evening.mc.prelude.api.util.game.ClientSide

@ClientSide.Physical
object XpkStorageClientHelper {
    val CHEST_MODEL: ModelResourceLocation =
        ModelResourceLocation(ResourceLocation("minecraft", "chest"), "inventory")
}
