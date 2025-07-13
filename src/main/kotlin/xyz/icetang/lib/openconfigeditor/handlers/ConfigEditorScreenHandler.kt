package xyz.icetang.lib.openconfigeditor.handlers

import xyz.icetang.lib.invfx.InvFX
import xyz.icetang.lib.invfx.frame.InvFrame
import xyz.icetang.lib.invfx.openFrame
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import xyz.icetang.lib.openconfigeditor.OpenConfigEditor
import xyz.icetang.lib.utils.ChatInputUtil
import java.io.File
import java.nio.file.Files
import org.bukkit.configuration.serialization.ConfigurationSerializable

object ConfigEditorScreenHandler {
    fun openScreen(player: Player) {
        Bukkit.getScheduler().runTaskAsynchronously(OpenConfigEditor.INSTANCE, Runnable {
            val frame = createFrame()

            Bukkit.getScheduler().runTask(OpenConfigEditor.INSTANCE, Runnable {
                player.openFrame(frame)
            })
        })
    }

    private fun createFrame(): InvFrame {
        val plugins = Bukkit.getPluginManager().plugins.toMutableList()
            .filter {
                it.dataFolder.exists()
            }

        return InvFX.frame(6, Component.text("Edit config")) {
            for (x in 0..8) {
                val lineItem = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
                    .apply {
                        editMeta {
                            it.displayName(Component.text(""))
                        }
                    }

                item(x, 0, lineItem)
                item(x, 5, lineItem)
            }
            val pluginList = list(0, 1, 8, 4, true, { plugins }) {
                transform { plugin ->
                    ItemStack(Material.PAPER)
                        .apply {
                            editMeta {
                                it.displayName(
                                    Component.text(plugin.name)
                                        .color(NamedTextColor.WHITE)
                                        .decoration(TextDecoration.ITALIC, false)
                                )
                                it.lore(
                                    listOf(
                                        Component.text("Left click to edit config.yml").color(NamedTextColor.YELLOW)
                                            .decoration(TextDecoration.ITALIC, false),
                                        Component.text("Right click to open data folder").color(NamedTextColor.YELLOW)
                                            .decoration(TextDecoration.ITALIC, false)
                                    )
                                )
                            }
                        }
                }

                onClickItem { x, y, _, event ->
                    val plugin = plugins.getOrNull(page.toInt() * 36 + y * 9 + x)

                    if (plugin != null) {
                        if (event.isLeftClick) {
                            openPluginConfigScreen(plugin, event.whoClicked as Player)
                        } else if (event.isRightClick) {
                            openDataFolder(plugin, event.whoClicked as Player)
                        }
                    }
                }
            }

            slot(0, 5) {
                item = ItemStack(Material.ARROW)
                    .apply {
                        editMeta {
                            it.displayName(
                                Component.text("Back")
                                    .color(NamedTextColor.WHITE)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                    }

                onClick {
                    pluginList.page -= 1
                    pluginList.refresh()
                }
            }

            slot(8, 5) {
                item = ItemStack(Material.ARROW)
                    .apply {
                        editMeta {
                            it.displayName(
                                Component.text("Next")
                                    .color(NamedTextColor.WHITE)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                    }

                onClick {
                    pluginList.page += 1
                    pluginList.refresh()
                }
            }
        }
    }

    private fun openDataFolder(plugin: Plugin, player: Player, folder: File? = null) {
        player.sendMessage(Component.text("Opening data folder for ${plugin.name}..."))
        val dataFolder = folder ?: plugin.dataFolder

        if (!dataFolder.exists()) {
            player.sendMessage(
                Component.text("Data folder for ${plugin.name} does not exist!").color(NamedTextColor.RED)
            )

            return
        }
        val isRoot = dataFolder.absolutePath == plugin.dataFolder.absolutePath
        val filesInDir =
            Files.list(dataFolder.toPath()).map { it.toFile() }.filter { it.extension == "yml" || it.isDirectory }.toList()
        val folders = filesInDir.filter { it.isDirectory }.map { it.absolutePath }.sorted().toTypedArray()
        val files =
            filesInDir.filter { it.extension == "yml" }.map { it.absolutePath }.sorted().toTypedArray()
        val paths = listOfNotNull(
            if (isRoot) null else "..", *folders, *files
        )
        val frame = InvFX.frame(6, Component.text("Data folder for ${plugin.name}")) {
            for (x in 0..8) {
                val lineItem = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
                    .apply {
                        editMeta {
                            it.displayName(Component.text(""))
                        }
                    }

                item(x, 0, lineItem)
                item(x, 5, lineItem)
            }

            slot(8, 0) {
                item = ItemStack(Material.RED_STAINED_GLASS_PANE)
                    .apply {
                        editMeta {
                            it.displayName(
                                Component.text("Go back to plugin list")
                                    .color(NamedTextColor.WHITE)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                    }

                onClick {
                    openScreen(player)
                }
            }

            val fileList = list(0, 1, 8, 4, true, { paths }) {
                transform { path ->
                    val file = File(if (path == "..") dataFolder.parentFile.absolutePath else path)

                    ItemStack(if (path == "..") Material.FEATHER else if (file.isDirectory) Material.CHEST else Material.PAPER)
                        .apply {
                            editMeta {
                                it.displayName(
                                    Component.text(if (this.type == Material.FEATHER) ".." else file.name)
                                        .color(NamedTextColor.WHITE)
                                        .decoration(TextDecoration.ITALIC, false)
                                )
                                it.lore(
                                    listOf(
                                        Component.text(if (this.type == Material.FEATHER) "Click to go to parent directory" else "Click to open")
                                            .color(NamedTextColor.YELLOW)
                                            .decoration(TextDecoration.ITALIC, false)
                                    )
                                )
                            }
                        }
                }

                onClickItem { _, _, item, _ ->
                    val file = File(if (item.first == "..") dataFolder.parentFile.absolutePath else item.first)

                    if (file.isDirectory) {
                        openDataFolder(plugin, player, file)
                    } else {
                        openPluginConfigScreen(plugin, player, "@root", null, file, mutableMapOf(), true)
                    }
                }
            }

            slot(0, 5) {
                item = ItemStack(Material.ARROW)
                    .apply {
                        editMeta {
                            it.displayName(
                                Component.text("Back")
                                    .color(NamedTextColor.WHITE)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                    }

                onClick {
                    fileList.page -= 1
                    fileList.refresh()
                }
            }

            slot(8, 5) {
                item = ItemStack(Material.ARROW)
                    .apply {
                        editMeta {
                            it.displayName(
                                Component.text("Next")
                                    .color(NamedTextColor.WHITE)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                    }

                onClick {
                    fileList.page += 1
                    fileList.refresh()
                }
            }
        }

        player.openFrame(frame)
    }

    private fun openPluginConfigScreen(
        plugin: Plugin,
        player: Player,
        sectionPath: String = "@root",
        root: YamlConfiguration? = null,
        file: File? = null,
        changeLog: MutableMap<String, Pair<Any, Any>> = mutableMapOf(),
        fromDataFolderScreen: Boolean = false
    ) {
        val miniMessage = MiniMessage.miniMessage()
        val configFile = file ?: File(plugin.dataFolder, "config.yml")

        if (!configFile.exists()) {
            player.sendMessage(
                Component.text("Config file for ${plugin.name} does not exist!").color(NamedTextColor.RED)
            )

            return
        }

        val config =
            root ?: YamlConfiguration.loadConfiguration(configFile)
        var section: ConfigurationSection = config

        if (sectionPath != "@root") {
            section = config.getConfigurationSection(sectionPath)!!
        }
        val maps = section.getKeys(false).filter { section.get(it) is ConfigurationSection }.sorted().toTypedArray()
        val values = section.getKeys(false).filter { section.get(it) !is ConfigurationSection }.sorted().toTypedArray()
        val keys = listOfNotNull(if (sectionPath != "@root") ".." else null, *maps, *values)

        player.openFrame(InvFX.frame(6, Component.text("Edit config for ${plugin.name}")) {
            for (x in 0..8) {
                val lineItem = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
                    .apply {
                        editMeta {
                            it.displayName(Component.text(""))
                        }
                    }

                item(x, 0, lineItem)
                item(x, 5, lineItem)
            }
            slot(8, 0) {
                item = ItemStack(Material.RED_STAINED_GLASS_PANE)
                    .apply {
                        editMeta {
                            it.displayName(
                                Component.text("Go back to ${if (fromDataFolderScreen) "data folder" else "plugin list"}")
                                    .color(NamedTextColor.WHITE)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                    }

                onClick {
                    if (fromDataFolderScreen) {
                        openDataFolder(plugin, player, file?.parentFile)
                    } else {
                        openScreen(player)
                    }
                }
            }

            val configList = list(0, 1, 8, 4, true, { keys }) {
                transform { key ->
                    // left click to edit, right click to restore
                    if (key == "..") {
                        ItemStack(Material.FEATHER)
                            .apply {
                                editMeta {
                                    it.displayName(
                                        Component.text("..")
                                            .color(NamedTextColor.WHITE)
                                            .decoration(TextDecoration.ITALIC, false)
                                    )
                                    it.lore(
                                        listOf(
                                            Component.text("Click to go to parent section").color(NamedTextColor.YELLOW)
                                                .decoration(TextDecoration.ITALIC, false)
                                        )
                                    )
                                }
                            }
                    } else {
                        val fullPath =
                            listOfNotNull(if (sectionPath == "@root") null else sectionPath, key).joinToString(".")
                        val value = section.get(key)
                        val editedBefore = changeLog.containsKey(fullPath)
                        val displayName =
                            miniMessage.deserialize("<reset>${if (editedBefore) "<yellow>" else "<white>"}$key${if (editedBefore) "*" else ""}")
                                .decoration(TextDecoration.ITALIC, editedBefore)

                        if (value is ConfigurationSection) {
                            ItemStack(Material.CHEST)
                                .apply {
                                    editMeta {
                                        it.displayName(displayName)
                                        it.lore(
                                            listOf(
                                                Component.text("Click to open").color(NamedTextColor.YELLOW)
                                                    .decoration(TextDecoration.ITALIC, false)
                                            )
                                        )
                                    }
                                }
                        } else {
                            ItemStack(Material.PAPER)
                                .apply {
                                    editMeta {
                                        it.displayName(displayName)
                                        it.lore(
                                            listOf(
                                                miniMessage.deserialize("<green>Current value: <white>${value}")
                                                    .decoration(TextDecoration.ITALIC, false),
                                                Component.text("Left click to edit").color(NamedTextColor.YELLOW)
                                                    .decoration(TextDecoration.ITALIC, false),
                                                Component.text("Right click to restore").color(NamedTextColor.YELLOW)
                                                    .decoration(TextDecoration.ITALIC, false)
                                            )
                                        )
                                    }
                                }
                        }
                    }
                }

                onClickItem { _, _, item, event ->
                    val key = item.first

                    if (key == "..") {
                        val parentPath = section.currentPath!!.split(".").dropLast(1).joinToString(".")

                        openPluginConfigScreen(
                            plugin,
                            player,
                            parentPath.ifEmpty { "@root" },
                            config,
                            configFile,
                            changeLog,
                            fromDataFolderScreen
                        )
                    } else {
                        val fullPath =
                            listOfNotNull(if (sectionPath == "@root") null else sectionPath, key).joinToString(".")
                        val value = section.get(key)!!

                        if (value is ConfigurationSerializable) {
                            player.sendMessage(Component.text("[WARNING] The value of $key is a ConfigurationSerializable object, and can only be edited with a text editor, not OpenConfigEditor.", NamedTextColor.YELLOW))

                            return@onClickItem
                        }

                        if (value is ConfigurationSection) {
                            openPluginConfigScreen(plugin, player, fullPath, config, configFile, changeLog)
                        } else {
                            val originalValue = changeLog[fullPath]?.first ?: value

                            if (event.isLeftClick) {
                                if (originalValue is Boolean) {
                                    val newValue = !(value as Boolean)

                                    if (newValue == originalValue) {
                                        changeLog.remove(fullPath)
                                    } else {
                                        changeLog[fullPath] = originalValue to newValue
                                    }

                                    section.set(key, newValue)

                                    this.refresh()

                                    return@onClickItem
                                }

                                player.closeInventory()

                                ChatInputUtil.getChatInput(
                                    player,
                                    Component.text("Enter new value for $key.")
                                        .appendNewline()
                                        .append(
                                            Component.text("[Click to copy current value]", NamedTextColor.GREEN)
                                                .decoration(TextDecoration.ITALIC, false)
                                                .clickEvent(ClickEvent.copyToClipboard(originalValue.toString()))
                                        )
                                ) {
                                    if (it != null) {
                                        try {
                                            val newValue = wrapTypeWith(it, originalValue)

                                            if (newValue == originalValue) {
                                                changeLog.remove(fullPath)
                                            } else {
                                                changeLog[fullPath] = originalValue to newValue
                                            }

                                            section.set(key, newValue)

                                            this.refresh()
                                        } catch (e: Exception) {
                                            player.sendMessage(
                                                Component.text("Invalid value!").color(NamedTextColor.RED)
                                            )
                                        }
                                    }

                                    player.openFrame(this@frame)
                                }
                            } else if (event.isRightClick) {
                                if (changeLog.containsKey(fullPath)) {
                                    changeLog[fullPath]?.let {
                                        section.set(key, it.first)
                                    }

                                    changeLog.remove(fullPath)
                                }

                                this.refresh()
                            }
                        }
                    }
                }
            }

            slot(0, 5) {
                item = ItemStack(Material.ARROW)
                    .apply {
                        editMeta {
                            it.displayName(
                                Component.text("Back")
                                    .color(NamedTextColor.WHITE)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                    }

                onClick {
                    configList.page -= 1
                    configList.refresh()
                }
            }

            slot(8, 5) {
                item = ItemStack(Material.ARROW)
                    .apply {
                        editMeta {
                            it.displayName(
                                Component.text("Next")
                                    .color(NamedTextColor.WHITE)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                    }

                onClick {
                    configList.page += 1
                    configList.refresh()
                }
            }

            slot(4, 5) {
                item = ItemStack(Material.GREEN_STAINED_GLASS_PANE)
                    .apply {
                        editMeta {
                            it.displayName(
                                Component.text("Save")
                                    .color(NamedTextColor.WHITE)
                                    .decoration(TextDecoration.ITALIC, false)
                            )
                        }
                    }

                onClick {
                    config.save(configFile)

                    changeLog.forEach {
                        player.sendMessage(
                            Component.text("${it.key}: ${it.value.first} -> ${it.value.second}")
                                .color(NamedTextColor.GREEN)
                        )
                    }

                    changeLog.clear()

                    player.sendMessage(Component.text("Config for ${plugin.name} saved!").color(NamedTextColor.GREEN))

                    configList.refresh()
                }
            }
        })
    }

    private fun wrapTypeWith(value: Any, origin: Any): Any {
        return when (origin) {
            is Int -> value.toString().toInt()
            is Double -> value.toString().toDouble()
            is Float -> value.toString().toFloat()
            is Long -> value.toString().toLong()
            is Short -> value.toString().toShort()
            is Byte -> value.toString().toByte()
            is Boolean -> value.toString().toBoolean()
            is List<*> -> value.toString().replaceFirst("[", "").split("]").dropLast(1).joinToString("]").split(",")
                .joinToString(", ").split(", ").map { wrapTypeWith(it, origin[0]!!) }
            else -> value
        }
    }
}