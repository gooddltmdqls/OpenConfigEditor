package xyz.icetang.lib.openconfigeditor

import org.bukkit.entity.Player
import xyz.icetang.lib.icemmand.icemmand
import org.bukkit.plugin.java.JavaPlugin
import xyz.icetang.lib.openconfigeditor.handlers.ConfigEditorScreenHandler

class OpenConfigEditor : JavaPlugin() {
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
                requires { sender is Player && sender.isOp }

                executes {
                    ConfigEditorScreenHandler.openScreen(player)
                }
            }
        }
    }

    companion object {
        lateinit var INSTANCE: OpenConfigEditor
    }
}