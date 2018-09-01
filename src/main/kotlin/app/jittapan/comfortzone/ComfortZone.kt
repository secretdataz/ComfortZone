/*
 * (C) Copyright 2018 Jittapan Pluemsumran <secret@jittapan.app>
 *
 * Licensed under GNU Affero General Public License, version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.jittapan.comfortzone

import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.sk89q.worldguard.WorldGuard
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap

class ComfortZone: JavaPlugin() {
    companion object {
        var instance: ComfortZone? = null
        var worldGuard: WorldGuard? = null
        val listeningExecutorService: ListeningExecutorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
        fun prefixMessage(msg: String): String {
            return "" + ChatColor.DARK_GRAY + "[" + ChatColor.RED + "ComfortZone" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', msg)
        }

        fun prefixMessageAsTextComponent(msg: String): Array<BaseComponent> {
            return TextComponent.fromLegacyText(prefixMessage(msg))
        }
    }

    private val playerStates = HashMap<UUID, ComfortZonePlayer>()
    private val messages = HashMap<String, String>()
    var claimSize: Int = 0
    var allowOverlap: Boolean = false
    var fakeFrameBlock: Material = Material.AIR
    var fakeFrameYPos: Int = 0
    var parentRegion: String? = null

    private val defaultMessages: HashMap<String, String> = hashMapOf(
            "messages.noConsole" to "This command can only be ran from in game.",
            "messages.frameAppeared" to "A glowstone frame has appeared above the designated region.",
            "messages.claimConfirm" to "Do you want to claim this region? Please double check the area as there will be no reversal of your action.",
            "messages.overlap" to "This region overlaps with someone else's region. Please move to another area and try again.",
            "messages.havePendingRegion" to "You already have a pending region for claiming.",
            "messages.noPendingRegion" to "You do not have any pending region for claiming.",
            "messages.notAllowedDimension" to "You are not allowed to claim a region in this dimension.",
            "messages.alreadyHaveRegion" to "You have already claimed a region.",
            "messages.noClaimedRegion" to "You do not have any claimed region.",
            "messages.confirm" to "Confirm",
            "messages.cancel" to "Cancel",
            "messages.created" to "Your region has been created.",
            "messages.cancelled" to "Cancelled your claim request.",
            "messages.greetingsFlag" to "&bEntering &9{player}&b's land",
            "messages.leavingFlag" to "&bEntering wilderness",
            "messages.unexpectedError" to "Unexpected error occurred.",
            "messages.reloaded" to "Reloaded ComfortZone configuration.",
            "messages.addedMember" to "{player} has been added as a member of your region.",
            "messages.removedMember" to "{player} is no longer a member of your region."
    )

    fun getPlayerState(bukkitPlayer: Player): ComfortZonePlayer {
        if(playerStates.containsKey(bukkitPlayer.uniqueId))
            return playerStates[bukkitPlayer.uniqueId]!!
        else {
            val ret = ComfortZonePlayer(bukkitPlayer)
            playerStates.put(bukkitPlayer.uniqueId, ret)
            return ret
        }
    }

    fun hasPlayerState(bukkitPlayer: Player): Boolean {
        return playerStates.contains(bukkitPlayer.uniqueId)
    }

    fun clearPlayerState(bukkitPlayer: Player) {
        playerStates.remove(bukkitPlayer.uniqueId)
    }

    fun getMessage(key: String): String {
        return messages[key] ?: key
    }

    fun initCfg() {
        config.addDefault("claimSize", 32)
        config.addDefault("allowOverlap", false)
        config.addDefault("fakeFrameBlock", "GLOWSTONE")
        config.addDefault("fakeFrameYPos", 127)
        config.addDefault("parentRegion", "member")

        defaultMessages.forEach {
            config.addDefault(it.key, it.value)
        }

        config.options().copyDefaults(true)
        saveConfig()
    }

    fun reloadCfg() {
        reloadConfig()
        messages.clear()
        defaultMessages.forEach {
            messages.put(it.key.replace("messages.", ""), config.getString(it.key))
        }

        claimSize = config.getInt("claimSize")
        allowOverlap = config.getBoolean("allowOverlap")
        fakeFrameBlock = Material.getMaterial(config.getString("fakeFrameBlock"))
        fakeFrameYPos = config.getInt("fakeFrameYPos")
        parentRegion = config.getString("parentRegion")
    }

    override fun onEnable() {
        instance = this
        var wgPlugin = WorldGuard.getInstance()
        if(wgPlugin == null || wgPlugin !is WorldGuard)
            throw Exception("ComfortZone failed to get a reference to WorldGuard instance. Is it loaded?")
        worldGuard = wgPlugin
        initCfg()
        reloadCfg()
        getCommand("comfortzone").executor = ComfortZoneCommands()
        server.pluginManager.registerEvents(ComfortZoneListener(), this)
    }
}
