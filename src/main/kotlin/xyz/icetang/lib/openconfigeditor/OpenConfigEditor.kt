package xyz.icetang.lib.openconfigeditor

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.java.JavaPlugin
import xyz.icetang.lib.icemmand.icemmand
import xyz.icetang.lib.openconfigeditor.handlers.ConfigEditorScreenHandler

class OpenConfigEditor : JavaPlugin() {
    private val configEditorPermission = Permission("openconfigeditor.configeditor", PermissionDefault.OP)

    override fun onEnable() {
        super.onEnable()

        INSTANCE = this

        setupCommands()

        logger.info("Enabled!")
    }

    override fun onDisable() {
        super.onDisable()

        logger.info("Disabled!")
    }

    private fun setupCommands() {
        icemmand {
            register("configeditor") {
                requires { sender is Player && sender.hasPermission(configEditorPermission) }

                executes {
                    player.sendMessage(Component.text("Opening Config Editor..."))

                    ConfigEditorScreenHandler.openScreen(player)
                }
            }
        }
    }

    companion object {
        lateinit var INSTANCE: OpenConfigEditor
    }
}