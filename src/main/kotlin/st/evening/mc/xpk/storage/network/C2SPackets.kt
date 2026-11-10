package st.evening.mc.xpk.storage.network

import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.PacketBuffer
import net.minecraft.util.math.BlockPos
import st.evening.mc.prelude.api.data.ser.ConstantSerializer
import st.evening.mc.prelude.api.data.ser.NetworkSerializer
import st.evening.mc.prelude.api.network.C2SMessageHandler
import st.evening.mc.prelude.api.util.game.ServerSide
import st.evening.mc.prelude.api.util.network.onServerThread
import st.evening.mc.xpk.storage.backpack.ItemBackpack
import st.evening.mc.xpk.storage.barrel.BlockBarrel

object C2SOpenBackpackInInv {
    val serializer: NetworkSerializer<C2SOpenBackpackInInv> = ConstantSerializer(C2SOpenBackpackInInv)

    object Handler : C2SMessageHandler<C2SOpenBackpackInInv> {
        @ServerSide
        override fun handleC2SMessage(netHandler: NetHandlerPlayServer, message: C2SOpenBackpackInInv) {
            netHandler.onServerThread {
                ItemBackpack.tryOpenBackpack(netHandler.player)
            }
        }
    }
}

class C2SExtractFromBarrel(val pos: BlockPos) {
    object Serializer : NetworkSerializer<C2SExtractFromBarrel> {
        override fun serializeToNetwork(buf: PacketBuffer, x: C2SExtractFromBarrel) {
            buf.writeBlockPos(x.pos)
        }

        override fun deserializeFromNetwork(buf: PacketBuffer): C2SExtractFromBarrel =
            C2SExtractFromBarrel(buf.readBlockPos())
    }

    object Handler : C2SMessageHandler<C2SExtractFromBarrel> {
        @ServerSide
        override fun handleC2SMessage(netHandler: NetHandlerPlayServer, message: C2SExtractFromBarrel) {
            netHandler.onServerThread {
                val player = netHandler.player
                val world = player.world
                val pos = message.pos
                if (!world.isBlockLoaded(pos)) return@onServerThread
                val state = world.getBlockState(pos)
                val block = state.block
                if (block is BlockBarrel) {
                    block.handleLeftClick(world, pos, state, player)
                }
            }
        }
    }
}
