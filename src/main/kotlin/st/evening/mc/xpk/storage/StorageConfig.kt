package st.evening.mc.xpk.storage

import net.minecraft.item.EnumRarity
import st.evening.mc.prelude.api.config.json.JsonConfig
import st.evening.mc.prelude.api.config.json.JsonConfig.ConfigObjectArray.EntryList
import st.evening.mc.xpk.storage.base.BlockTieredStorage

class StorageConfig(config: JsonConfig.ConfigObject) {
    class Upgrade(config: JsonConfig.ConfigObject) {
        var upgradeTo: String by config.string("example", "The tier to which this item upgrades the container.")

        var item: String by config.string(
            "ore:gemDiamond",
            """
            The item used for the upgrade, expressed as either "<mod-id>:<item-id>" or "ore:<ore-name>".
            """.trimIndent()
        )

        fun configureUpgrade(upgradeTo: String, item: String) {
            this.upgradeTo = upgradeTo
            this.item = item
        }
    }

    interface Storage {
        val colour: String

        val rarity: EnumRarity
    }

    interface Block : Storage {
        val material: BlockTieredStorage.MaterialType

        val upgrades: List<Upgrade>

        val keepContents: Boolean
    }

    val backpacks: EntryList<Backpack> by config.objArray(
        "backpack type configuration", ::Backpack,
        listOf(
            { it.configure("leather", "C65C35", 3, 3) },
            { it.configure("iron", "D8D8D8", 3, 5) },
            { it.configure("gold", "FDF55F", 3, 7) },
            { it.configure("diamond", "A1FBE8", 3, 9) }
        ),
        restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
    )

    class Backpack(config: JsonConfig.ConfigObject) : Storage {
        var name: String by config.string(
            "example", "The name of the backpack type. Changing this will destroy existing backpacks!",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART,
            validPattern = Regex(".+")
        )

        override var colour: String by config.string(
            "FFFFFF", "The backpack's theme colour, expressed as six hexadecimal digits.",
            validPattern = Regex("""[\da-f]{6}""", RegexOption.IGNORE_CASE)
        )

        override var rarity: EnumRarity by config.enum(EnumRarity.COMMON, "The backpack's item rarity.")

        var slotRows: Int by config.int(
            3, "The number of rows in the backpack's inventory.",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART,
            minValue = 1
        )

        var slotColumns: Int by config.int(
            9, "The number of columns in the backpack's inventory.",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART,
            minValue = 1,
        )

        fun getSlotCount(): Int = slotRows * slotColumns

        fun configure(
            name: String,
            colour: String,
            slotRows: Int,
            slotColumns: Int
        ) {
            this.name = name
            this.colour = colour
            this.slotRows = slotRows
            this.slotColumns = slotColumns
        }
    }

    val barrels: EntryList<Barrel> by config.objArray(
        "barrel type configuration", ::Barrel,
        listOf(
            {
                it.configure("stone", "999999", BlockTieredStorage.MaterialType.STONE, 2048) {
                    configureUpgrade("iron", "ore:blockIron")
                }
            },
            {
                it.configure("iron", "D8D8D8", BlockTieredStorage.MaterialType.METAL, 4096) {
                    configureUpgrade("gold", "ore:blockGold")
                }
            },
            {
                it.configure("gold", "FDF55F", BlockTieredStorage.MaterialType.METAL, 8192) {
                    configureUpgrade("diamond", "ore:blockDiamond")
                }
            },
            { it.configure("diamond", "A1FBE8", BlockTieredStorage.MaterialType.METAL, 16384) }
        ),
        restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
    )

    class Barrel(config: JsonConfig.ConfigObject) : Block {
        var name: String by config.string(
            "example", "The name of the barrel type. Changing this will destroy existing barrels!",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART,
            validPattern = Regex(".+")
        )

        override var colour: String by config.string(
            "FFFFFF", "The barrel's theme colour, expressed as six hexadecimal digits.",
            validPattern = Regex("""[\da-f]{6}""", RegexOption.IGNORE_CASE)
        )

        override var rarity: EnumRarity by config.enum(EnumRarity.COMMON, "The barrel's item rarity.")

        override var material: BlockTieredStorage.MaterialType by config.enum(
            BlockTieredStorage.MaterialType.WOOD, "The barrel's block material.",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )

        override val upgrades: EntryList<Upgrade> by config.objArray(
            "upgrade item configuration", ::Upgrade, 0,
            "Upgrade items for the barrel."
        )

        override var keepContents: Boolean by config.bool(true, "Does the barrel retain its contents when broken?")

        var capacity: Int by config.int(
            4096, "The number of items that the barrel can hold.",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART,
            minValue = 1
        )

        fun configure(
            name: String,
            colour: String,
            material: BlockTieredStorage.MaterialType,
            capacity: Int,
            configureUpgrade: (Upgrade.() -> Unit)? = null
        ) {
            this.name = name
            this.colour = colour
            this.material = material
            this.capacity = capacity
            if (configureUpgrade != null) {
                upgrades.configure(listOf(configureUpgrade))
            }
        }
    }

    val chests: EntryList<Chest> by config.objArray(
        "chest type configuration", ::Chest,
        listOf(
            {
                it.configure("stone", "999999", BlockTieredStorage.MaterialType.STONE, 4, 9) {
                    configureUpgrade("iron", "ore:blockIron")
                }
            },
            {
                it.configure("iron", "D8D8D8", BlockTieredStorage.MaterialType.METAL, 5, 10) {
                    configureUpgrade("gold", "ore:blockGold")
                }
            },
            {
                it.configure("gold", "FDF55F", BlockTieredStorage.MaterialType.METAL, 6, 11) {
                    configureUpgrade("diamond", "ore:blockDiamond")
                }
            },
            { it.configure("diamond", "A1FBE8", BlockTieredStorage.MaterialType.METAL, 7, 12) }
        ),
        restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
    )

    class Chest(config: JsonConfig.ConfigObject) : Block {
        var name: String by config.string(
            "example", "The name of the chest type. Changing this will destroy existing chests!",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART,
            validPattern = Regex(".+")
        )

        override var colour: String by config.string(
            "FFFFFF", "The chest's theme colour, expressed as six hexadecimal digits.",
            validPattern = Regex("""[\da-f]{6}""", RegexOption.IGNORE_CASE)
        )

        override var rarity: EnumRarity by config.enum(EnumRarity.COMMON, "The chest's item rarity.")

        val chestModelTexture: String by config.string(
            "default", "The chest's model texture, which should be a file in textures/entity/chest."
        )

        val chestModelTint: Boolean by config.bool(true, "Should the chest model be tinted with its theme colour?")

        override var material: BlockTieredStorage.MaterialType by config.enum(
            BlockTieredStorage.MaterialType.WOOD, "The chest's block material.",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )

        override val upgrades: EntryList<Upgrade> by config.objArray(
            "upgrade item configuration", ::Upgrade, 0,
            "Upgrade items for the chest."
        )

        override var keepContents: Boolean by config.bool(false, "Does the chest retain its contents when broken?")

        var checkLidObstruction: Boolean by config.bool(
            true, "Does opening the chest require the block above it to be clear?"
        )

        var slotRows: Int by config.int(
            3, "The number of rows in the chest's inventory.",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART,
            minValue = 1
        )

        var slotColumns: Int by config.int(
            9, "The number of columns in the chest's inventory.",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART,
            minValue = 1,
        )

        fun getSlotCount(): Int = slotRows * slotColumns

        fun configure(
            name: String,
            colour: String,
            material: BlockTieredStorage.MaterialType,
            slotRows: Int,
            slotColumns: Int,
            configureUpgrade: (Upgrade.() -> Unit)? = null
        ) {
            this.name = name
            this.colour = colour
            this.material = material
            this.slotRows = slotRows
            this.slotColumns = slotColumns
            if (configureUpgrade != null) {
                upgrades.configure(listOf(configureUpgrade))
            }
        }
    }

    val vanillaChestUpgrades: EntryList<Upgrade> by config.objArray(
        "upgrade item configuration", ::Upgrade,
        listOf {
            it.configureUpgrade("stone", "minecraft:furnace")
        },
        "Upgrade items for the vanilla chest."
    )

    val tanks: EntryList<Tank> by config.objArray(
        "tank type configuration", ::Tank,
        listOf(
            {
                it.configure("stone", "999999", BlockTieredStorage.MaterialType.STONE, 8000) {
                    configureUpgrade("iron", "ore:blockIron")
                }
            },
            {
                it.configure("iron", "D8D8D8", BlockTieredStorage.MaterialType.METAL, 16000) {
                    configureUpgrade("gold", "ore:blockGold")
                }
            },
            {
                it.configure("gold", "FDF55F", BlockTieredStorage.MaterialType.METAL, 32000) {
                    configureUpgrade("diamond", "ore:blockDiamond")
                }
            },
            { it.configure("diamond", "A1FBE8", BlockTieredStorage.MaterialType.METAL, 64000) }
        ),
        restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
    )

    class Tank(config: JsonConfig.ConfigObject) : Block {
        var name: String by config.string(
            "example", "The name of the tank type. Changing this will destroy existing tanks!",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART,
            validPattern = Regex(".+")
        )

        override var colour: String by config.string(
            "FFFFFF", "The tank's theme colour, expressed as six hexadecimal digits.",
            validPattern = Regex("""[\da-f]{6}""", RegexOption.IGNORE_CASE)
        )

        override var rarity: EnumRarity by config.enum(EnumRarity.COMMON, "The tank's item rarity.")

        override var material: BlockTieredStorage.MaterialType by config.enum(
            BlockTieredStorage.MaterialType.WOOD, "The tank's block material.",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
        )

        override val upgrades: EntryList<Upgrade> by config.objArray(
            "upgrade item configuration", ::Upgrade, 0,
            "Upgrade items for the tank."
        )

        override var keepContents: Boolean by config.bool(true, "Does the tank retain its contents when broken?")

        val fluidHandlerItem: Boolean by config.bool(
            true,
            """
            Can the tank item be used as a fluid handler?
            Enabling this makes the tank unstackable.
            If switching from off to on, existing tank stacks could be exploited for fluid duplication!
            """.trimIndent()
        )

        var capacity: Int by config.int(
            16000, "The amount of fluid, in mB, that the tank can hold.",
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART,
            minValue = 1
        )

        val inputRate: Int by config.int(
            -1,
            """
            The amount of fluid, in mB, that can be mechanically inserted into the tank in a single operation.
            Does not apply to players manually interacting with the tank!
            Set to -1 to disable the limit.
            """.trimIndent(),
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART,
            minValue = -1
        )

        val outputRate: Int by config.int(
            -1,
            """
            The amount of fluid, in mB, that can be mechanically extracted from the tank in a single operation.
            Does not apply to players manually interacting with the tank!
            Set to -1 to disable the limit.
            """.trimIndent(),
            restartRequirement = JsonConfig.RestartRequirement.REQUIRES_WORLD_RESTART,
            minValue = -1
        )

        fun configure(
            name: String,
            colour: String,
            material: BlockTieredStorage.MaterialType,
            capacity: Int,
            configureUpgrade: (Upgrade.() -> Unit)? = null
        ) {
            this.name = name
            this.colour = colour
            this.material = material
            this.capacity = capacity
            if (configureUpgrade != null) {
                upgrades.configure(listOf(configureUpgrade))
            }
        }
    }

    val exampleRecipes: Boolean by config.bool(
        true, "Should example recipes be added for the default configuration?",
        sync = false,
        restartRequirement = JsonConfig.RestartRequirement.REQUIRES_MC_RESTART
    )
}
