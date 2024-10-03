package xyz.icetang.lib.utils

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitTask
import xyz.icetang.lib.openconfigeditor.OpenConfigEditor

object ChatInputUtil {
    fun getChatInput(player: Player, message: Component, callback: (String?) -> Unit) {
        player.sendMessage(message)
        player.sendMessage(Component.text("Type \'+cancel\' to cancel.", NamedTextColor.YELLOW))
        var task: BukkitTask? = null
        val listener = object : Listener {
            @EventHandler
            fun onChat(event: AsyncChatEvent) {
                if (event.player != player) {
                    return
                }

                event.isCancelled = true
                val content = (event.message() as TextComponent).content()

                if (content == "+cancel") {
                    player.sendMessage(Component.text("Cancelled", NamedTextColor.RED))
                    Bukkit.getScheduler().runTask(OpenConfigEditor.INSTANCE, Runnable {
                        callback(null)
                    })
                } else {
                    Bukkit.getScheduler().runTask(OpenConfigEditor.INSTANCE, Runnable {
                        callback(content)
                    })
                }

                HandlerList.unregisterAll(this)

                if (task != null) Bukkit.getScheduler().cancelTask(task!!.taskId)
            }
        }

        Bukkit.getPluginManager().registerEvents(listener, OpenConfigEditor.INSTANCE)
        // If 30 seconds passed, unregister listener
        task = Bukkit.getScheduler().runTaskLater(OpenConfigEditor.INSTANCE, Runnable {
            HandlerList.unregisterAll(listener)
            player.sendMessage(Component.text("Timed out.", NamedTextColor.RED))

            Bukkit.getScheduler().runTask(OpenConfigEditor.INSTANCE, Runnable {
                callback(null)
            })
        }, 20 * 30)
    }
}