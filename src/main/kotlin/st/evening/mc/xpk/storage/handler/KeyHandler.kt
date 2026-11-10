package st.evening.mc.xpk.storage.handler

import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.client.settings.KeyModifier
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.util.game.ClientSide
import st.evening.mc.prelude.api.util.game.onClient
import st.evening.mc.xpk.storage.XpkStorage
import st.evening.mc.xpk.storage.XpkStorageConsts
import st.evening.mc.xpk.storage.network.C2SOpenBackpackInInv

@ClientSide.Physical
object KeyHandler {
    const val CATEGORY: String = "key.categories.${XpkStorageConsts.MOD_ID}"
    const val KEY: String = "key.${XpkStorageConsts.MOD_ID}"

    val keyOpenBackpack: KeyBinding =
        KeyBinding("$KEY.open_backpack", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, Keyboard.KEY_E, CATEGORY)

    fun init(reg: ModRegistrar) {
        ClientRegistry.registerKeyBinding(keyOpenBackpack)
        reg.on<InputEvent.KeyInputEvent> {
            onClient {
                if (Keyboard.getEventKeyState() && keyOpenBackpack.isActiveAndMatches(Keyboard.getEventKey())) {
                    XpkStorage.defns.c2sOpenBackpack.sendToServer(C2SOpenBackpackInInv)
                }
            }
        }
    }
}
