package xyz.icetang.lib.openconfigeditor.handlers

import io.github.monun.invfx.InvFX
import io.github.monun.invfx.frame.InvFrame
import io.github.monun.invfx.openFrame
import net.kyori.adventure.text.Component
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
import xyz.icetang.lib.utils.ChatInputUtil
import java.io.File

object ConfigEditorScreenHandler {
    fun openScreen(player: Player) {
        player.openFrame(createFrame())
    }

    private fun createFrame(): InvFrame {
        val plugins = Bukkit.getPluginManager().plugins.toMutableList()

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
                                it.displayName(Component.text(plugin.name)
                                    .color(NamedTextColor.WHITE)
                                    .decoration(TextDecoration.ITALIC, false))
                                it.lore(listOf(Component.text("Click to edit config").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)))
                            }
                        }
                }

                onClickItem { x, y, item, event ->
                    val plugin = plugins.getOrNull(page.toInt() * 36 + y * 9 + x)

                    if (plugin != null) {
                        openPluginConfigScreen(plugin, event.whoClicked as Player)
                    }
                }
            }

            slot(0, 5) {
                item = ItemStack(Material.ARROW)
                    .apply {
                        editMeta {
                            it.displayName(Component.text("Back")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false))
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
                            it.displayName(Component.text("Next")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false))
                        }
                    }

                onClick {
                    pluginList.page += 1
                    pluginList.refresh()
                }
            }
        }
    }

    private fun openPluginConfigScreen(plugin: Plugin, player: Player, sectionPath: String = "@root", root: YamlConfiguration? = null, file: File? = null, changeLog: MutableMap<String, Pair<Any, Any>> = mutableMapOf()) {
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

        val keys = listOfNotNull(if (sectionPath != "@root") ".." else null, *section.getKeys(false).toTypedArray())

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

            val configList = list(0, 1, 8, 4, true, { keys }) {
                transform { key ->
                    // left click to edit, right click to restore
                    if (key == "..") {
                        ItemStack(Material.BARRIER)
                            .apply {
                                editMeta {
                                    it.displayName(Component.text("..")
                                        .color(NamedTextColor.WHITE)
                                        .decoration(TextDecoration.ITALIC, false))
                                    it.lore(listOf(Component.text("Click to go to parent section").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)))
                                }
                            }
                    } else {
                        val fullPath = listOfNotNull(if (sectionPath == "@root") null else sectionPath, key).joinToString(".")

                        val value = section.get(key)

                        val editedBefore = changeLog.containsKey(fullPath)

                        val displayName = miniMessage.deserialize("<reset>${if (editedBefore) "<yellow>" else "<white>"}$key${if (editedBefore) "*" else ""}").decoration(TextDecoration.ITALIC, editedBefore)

                        if (value is ConfigurationSection) {
                            ItemStack(Material.CHEST)
                                .apply {
                                    editMeta {
                                        it.displayName(displayName)
                                        it.lore(listOf(Component.text("Click to open").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)))
                                    }
                                }
                        } else {
                            ItemStack(Material.PAPER)
                                .apply {
                                    editMeta {
                                        it.displayName(displayName)
                                        it.lore(listOf(
                                            miniMessage.deserialize("<green>Current value: <white>${value}").decoration(TextDecoration.ITALIC, false),
                                            Component.text("Left click to edit").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                                            Component.text("Right click to restore").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                                        ))
                                    }
                                }
                        }
                    }
                }

                onClickItem { x, y, item, event ->
                    val key = keys[page.toInt() * 36 + y * 9 + x]

                    if (key == "..") {
                        val parentPath = section.currentPath!!.split(".").dropLast(1).joinToString(".")

                        openPluginConfigScreen(plugin, player, parentPath.ifEmpty { "@root" }, config, configFile, changeLog)
                    } else {
                        val fullPath = listOfNotNull(if (sectionPath == "@root") null else sectionPath, key).joinToString(".")

                        val value = section.get(key)!!

                        if (value is ConfigurationSection) {
                            openPluginConfigScreen(plugin, player, fullPath, config, configFile, changeLog)
                        } else {
                            val originalValue = changeLog[fullPath]?.first ?: value

                            if (event.isLeftClick) {
                                player.closeInventory()

                                ChatInputUtil.getChatInput(player, Component.text("Enter new value for $key")) {
                                    if (it != null) {
                                        val newValue = wrapTypeWith(it, originalValue)

                                        if (newValue == originalValue) {
                                            changeLog.remove(fullPath)
                                        } else {
                                            changeLog[fullPath] = originalValue to newValue
                                        }

                                        section.set(key, newValue)

                                        this.refresh()
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
                            it.displayName(Component.text("Back")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false))
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
                            it.displayName(Component.text("Next")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false))
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
                            it.displayName(Component.text("Save")
                                .color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false))
                        }
                    }

                onClick {
                    config.save(configFile)

                    changeLog.forEach {
                        player.sendMessage(Component.text("${it.key}: ${it.value.first} -> ${it.value.second}").color(NamedTextColor.GREEN))
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
            is List<*> -> value.toString().replaceFirst("[", "").split("]").dropLast(1).joinToString("]").split(",").joinToString(", ").split(", ").map { wrapTypeWith(it, origin[0]!!) }
            else -> value
        }
    }
}