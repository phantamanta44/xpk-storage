package st.evening.mc.xpk.storage

import net.minecraft.client.Minecraft
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.SoundEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import st.evening.mc.prelude.api.PreludeMod
import st.evening.mc.prelude.api.config.json.JsonConfig
import st.evening.mc.prelude.api.config.json.defaultInit
import st.evening.mc.prelude.api.network.PacketType
import st.evening.mc.prelude.api.registration.ContainerType
import st.evening.mc.prelude.api.registration.ModRegistrar
import st.evening.mc.prelude.api.registration.RegistrationHolder
import st.evening.mc.prelude.api.registration.TileEntityType
import st.evening.mc.prelude.api.registration.block
import st.evening.mc.prelude.api.registration.c2sPacket
import st.evening.mc.prelude.api.registration.container
import st.evening.mc.prelude.api.registration.creativeTab
import st.evening.mc.prelude.api.registration.item
import st.evening.mc.prelude.api.registration.on
import st.evening.mc.prelude.api.registration.soundEvent
import st.evening.mc.prelude.api.registration.tileEntity
import st.evening.mc.prelude.api.util.game.onPhysicalClient
import st.evening.mc.prelude.loader.PreludeLoader
import st.evening.mc.xpk.storage.backpack.ContainerBackpack
import st.evening.mc.xpk.storage.backpack.ItemBackpack
import st.evening.mc.xpk.storage.barrel.BlockBarrel
import st.evening.mc.xpk.storage.barrel.TileEntityBarrel
import st.evening.mc.xpk.storage.barrel.TileEntityBarrelRenderer
import st.evening.mc.xpk.storage.base.BlockTieredStorage
import st.evening.mc.xpk.storage.base.ItemTieredStorage
import st.evening.mc.xpk.storage.chest.BlockChezt
import st.evening.mc.xpk.storage.chest.ContainerChezt
import st.evening.mc.xpk.storage.chest.TileEntityChezt
import st.evening.mc.xpk.storage.chest.TileEntityCheztRenderer
import st.evening.mc.xpk.storage.handler.BlockInteractionHandler
import st.evening.mc.xpk.storage.handler.KeyHandler
import st.evening.mc.xpk.storage.handler.RecipeHandler
import st.evening.mc.xpk.storage.network.C2SExtractFromBarrel
import st.evening.mc.xpk.storage.network.C2SOpenBackpackInInv
import st.evening.mc.xpk.storage.tank.BlockTank
import st.evening.mc.xpk.storage.tank.TileEntityTank
import st.evening.mc.xpk.storage.tank.TileEntityTankRenderer

@PreludeLoader.Load(XpkStorageConsts.MOD_ID)
object XpkStorage : PreludeMod<XpkStorageDefns> {
    override val modId: String = XpkStorageConsts.MOD_ID
    private val jsonConfig: JsonConfig = JsonConfig(XpkStorageConsts.MOD_ID)
    val config: StorageConfig = StorageConfig(jsonConfig.root)
    val defns: XpkStorageDefns by PreludeLoader.inject()

    override fun register(reg: ModRegistrar): XpkStorageDefns {
        jsonConfig.defaultInit(reg, changeCallback = { defns.refreshConfigCache() })
        return XpkStorageDefns(reg)
    }
}

class XpkStorageDefns(reg: ModRegistrar) {
    val creativeTab: CreativeTabs = reg.creativeTab {
        when {
            chests.isNotEmpty() -> ItemStack(chests.values.first().getPropertyValue())
            backpacks.isNotEmpty() -> ItemStack(backpacks.values.first().getPropertyValue())
            barrels.isNotEmpty() -> ItemStack(barrels.values.first().getPropertyValue())
            tanks.isNotEmpty() -> ItemStack(tanks.values.first().getPropertyValue())
            else -> ItemStack(Blocks.CHEST)
        }
    }

    private val backpacks: MutableMap<String, RegistrationHolder<ItemBackpack>> = mutableMapOf()
    val containerBackpack: ContainerType by reg.container(ContainerBackpack.Factory)
    val c2sOpenBackpack: PacketType.C2S<C2SOpenBackpackInInv>
        by reg.c2sPacket(C2SOpenBackpackInInv.serializer, C2SOpenBackpackInInv.Handler)
    val soundBackpackOpen: SoundEvent by reg.soundEvent("item.backpack.open")
    val soundBackpackClose: SoundEvent by reg.soundEvent("item.backpack.close")

    private val barrels: MutableMap<String, RegistrationHolder<BlockBarrel>> = mutableMapOf()
    val teBarrel: TileEntityType<TileEntityBarrel>
        by reg.tileEntity("barrel") { _, state -> TileEntityBarrel(state.block as BlockBarrel) }
    val c2sExtractFromBarrel: PacketType.C2S<C2SExtractFromBarrel>
        by reg.c2sPacket(C2SExtractFromBarrel.Serializer, C2SExtractFromBarrel.Handler)
    val soundBarrelInsert: SoundEvent by reg.soundEvent("block.barrel.insert")
    val soundBarrelInsertAll: SoundEvent by reg.soundEvent("block.barrel.insert_all")
    val soundBarrelExtract: SoundEvent by reg.soundEvent("block.barrel.extract")
    val soundBarrelLock: SoundEvent by reg.soundEvent("block.barrel.lock")
    val soundBarrelUnlock: SoundEvent by reg.soundEvent("block.barrel.unlock")

    private val chests: MutableMap<String, RegistrationHolder<BlockChezt>> = mutableMapOf()
    val teChest: TileEntityType<TileEntityChezt>
        by reg.tileEntity("chest") { _, state -> TileEntityChezt(state.block as BlockChezt) }
    val containerChest: ContainerType by reg.container(ContainerChezt.Factory)

    private val tanks: MutableMap<String, RegistrationHolder<BlockTank>> = mutableMapOf()
    val teTank: TileEntityType<TileEntityTank>
        by reg.tileEntity("tank") { _, state -> TileEntityTank(state.block as BlockTank) }

    init {
        XpkStorage.config.backpacks.forEach { config ->
            backpacks[config.name] = reg.item("backpack_${config.name}") { ItemBackpack(config) }
        }
        XpkStorage.config.barrels.forEach { config ->
            barrels[config.name] = reg.block("barrel_${config.name}") { BlockBarrel(config) }
        }
        XpkStorage.config.chests.forEach { config ->
            chests[config.name] = reg.block("chest_${config.name}") { BlockChezt(config) }
        }
        XpkStorage.config.tanks.forEach { config ->
            tanks[config.name] = reg.block("tank_${config.name}") { BlockTank(config) }
        }
    }

    fun getBackpack(name: String): ItemBackpack? = backpacks[name]?.getPropertyValue()

    fun getBarrel(name: String): BlockBarrel? = barrels[name]?.getPropertyValue()

    fun getChest(name: String): BlockChezt? = chests[name]?.getPropertyValue()

    fun getTank(name: String): BlockTank? = tanks[name]?.getPropertyValue()

    init {
        BlockInteractionHandler.init(reg)
        RecipeHandler.init(reg)
        onPhysicalClient {
            KeyHandler.init(reg)
            reg.on<FMLInitializationEvent> {
                val mc = Minecraft.getMinecraft()
                val itemCols = mc.itemColors
                val blockCols = mc.blockColors
                backpacks.values.forEach { holder ->
                    val item = holder.getPropertyValue()
                    itemCols.registerItemColorHandler(ItemTieredStorage.ColourHandler(item), item)
                }
                barrels.values.forEach { holder ->
                    val block = holder.getPropertyValue()
                    val handler = BlockTieredStorage.ColourHandler(block)
                    itemCols.registerItemColorHandler(handler, block)
                    blockCols.registerBlockColorHandler(handler, block)
                }
                chests.values.forEach { holder ->
                    val block = holder.getPropertyValue()
                    val handler = BlockTieredStorage.ColourHandler(block)
                    itemCols.registerItemColorHandler(handler, block)
                    blockCols.registerBlockColorHandler(handler, block)
                }
                tanks.values.forEach { holder ->
                    val block = holder.getPropertyValue()
                    val handler = BlockTieredStorage.ColourHandler(block)
                    itemCols.registerItemColorHandler(handler, block)
                    blockCols.registerBlockColorHandler(handler, block)
                }
                ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBarrel::class.java, TileEntityBarrelRenderer)
                ClientRegistry.bindTileEntitySpecialRenderer(TileEntityChezt::class.java, TileEntityCheztRenderer)
                ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTank::class.java, TileEntityTankRenderer)
            }
        }
    }

    fun refreshConfigCache() {
        BlockInteractionHandler.refreshConfigCache()
        backpacks.values.forEach {
            it.getPropertyValue().refreshConfigCache()
        }
        barrels.values.forEach {
            it.getPropertyValue().refreshConfigCache()
        }
        chests.values.forEach {
            it.getPropertyValue().refreshConfigCache()
        }
        tanks.values.forEach {
            it.getPropertyValue().refreshConfigCache()
        }
    }
}
