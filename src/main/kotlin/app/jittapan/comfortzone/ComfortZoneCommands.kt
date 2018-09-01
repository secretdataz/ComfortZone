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

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.sk89q.worldedit.BlockVector
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.domains.DefaultDomain
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion
import com.sk89q.worldguard.protection.util.DomainInputResolver
import com.sk89q.worldguard.util.profile.resolver.ProfileService
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ComfortZoneCommands: CommandExecutor {
    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {
        if(sender == null)
            return false
        if(args == null || args.isEmpty()) {
            sender.sendMessageWithColorCodes("Usage: /comfortzone <claim | addmember | removemember | reload >")
            return true
        }
        when (args[0]) {
            "claim" -> {
                if(sender is Player)
                    handleClaim(sender)
                else
                    sender.sendComfortZoneMessage("noConsole")
            }
            "confirm" -> {
                if(sender is Player)
                    handleConfirm(sender)
                else
                    sender.sendComfortZoneMessage("noConsole")
            }
            "cancel" -> {
                if(sender is Player)
                    handleCancel(sender)
                else
                    sender.sendComfortZoneMessage("noConsole")
            }
            "addmember" -> {
                if(sender !is Player) {
                    sender.sendComfortZoneMessage("noConsole")
                } else {

                    if (args.size < 2) {
                        sender.sendMessageWithColorCodes("Usage: /comfortzone addmember <Name>")
                    }
                    handleMember(sender, args[1], false)
                }
            }
            "removemember" -> {
                if(sender !is Player) {
                    sender.sendComfortZoneMessage("noConsole")
                } else {

                    if (args.size < 2) {
                        sender.sendMessageWithColorCodes("Usage: /comfortzone removemember <Name>")
                    }
                    handleMember(sender, args[1], true)
                }
            }
            "reload" -> {
                if(sender.hasPermission("comfortzone.admin")) {
                    ComfortZone.instance?.reloadCfg()
                } else {
                    sender.sendMessageWithColorCodes("&cYou do not have permission to do that.")
                }
            }
        }
        return true
    }

    fun handleClaim(player: Player) {
        if(player.world.environment != World.Environment.NORMAL) {
            player.sendComfortZoneMessage("notAllowedDimension")
            return
        }

        val container = ComfortZone.worldGuard!!.platform.regionContainer
        val regions = container.get(BukkitAdapter.adapt(player.world))
        if(regions == null) {
            player.sendComfortZoneMessage("unexpectedError")
            return
        }
        if(regions.hasRegion(player.uniqueId.toString() + "_land")) {
            player.sendComfortZoneMessage("alreadyHaveRegion")
            return
        }

        val state = ComfortZone.instance!!.getPlayerState(player)
        if(state.state == ComfortZonePlayer.State.WAIT_FOR_CONFIRMATION) {
            player.sendComfortZoneMessage("havePendingRegion")
            return
        }
        state.setCornerFromRadius(ComfortZone.instance!!.claimSize)

        val ground = state.pos1Block!!.clone()
        ground.y = 0.0
        val sky = state.pos2Block!!.clone()
        sky.y = 255.0
        val region = ProtectedCuboidRegion("CZ_OVERLAPTEST", ground.toWEBlockVector(), sky.toWEBlockVector())
        val applicableRegions = regions.getApplicableRegions(region)
        if(applicableRegions.size() > 0) {
            player.sendComfortZoneMessage("overlap")
        } else {
            state.state = ComfortZonePlayer.State.WAIT_FOR_CONFIRMATION
            state.sendFrame(false)
            player.sendComfortZoneMessage("frameAppeared")
            player.sendComfortZoneMessage("claimConfirm")

            val prompt = TextComponent("Click to proceed --> ")
            prompt.color = ChatColor.WHITE
            val message = TextComponent("[")
            message.color = ChatColor.DARK_GRAY
            val confirm = TextComponent(ComfortZone.instance!!.getMessage("confirm"))
            confirm.color = ChatColor.GREEN
            confirm.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cz confirm")
            confirm.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("Claim this region!").create())
            message.addExtra(confirm)
            message.addExtra("]")
            message.addExtra("       [");
            val cancel = TextComponent(ComfortZone.instance!!.getMessage("cancel"))
            cancel.color = ChatColor.RED
            cancel.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cz cancel")
            cancel.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("Cancel this request").create())
            message.addExtra(cancel)
            message.addExtra("]")
            prompt.addExtra(message)
            player.spigot().sendMessage(prompt)
        }
    }

    fun handleConfirm(player: Player) {
        val state = ComfortZone.instance?.getPlayerState(player)
        if(state?.state == ComfortZonePlayer.State.WAIT_FOR_CONFIRMATION) {
            // Create region and add flags
            val container = ComfortZone.worldGuard!!.platform.regionContainer
            val regions = container.get(BukkitAdapter.adapt(player.world))
            if(regions == null) {
                player.sendComfortZoneMessage("unexpectedError")
                return
            }

            val ground = state.pos1Block!!.clone()
            ground.y = 0.0
            val sky = state.pos2Block!!.clone()
            sky.y = 255.0
            val region = ProtectedCuboidRegion(player.uniqueId.toString() + "_land", ground.toWEBlockVector(), sky.toWEBlockVector())
            val applicableRegions = regions.getApplicableRegions(region)
            if(applicableRegions.size() > 0) {
                player.sendComfortZoneMessage("overlap")
            } else {
                region.setFlag(Flags.GREET_MESSAGE, ChatColor.translateAlternateColorCodes('&', ComfortZone.instance!!.getMessage("greetingsFlag").replace("{player}", player.displayName)))
                region.setFlag(Flags.FAREWELL_MESSAGE, ChatColor.translateAlternateColorCodes('&', ComfortZone.instance!!.getMessage("leavingFlag")))
                region.parent = regions.getRegion(ComfortZone.instance!!.parentRegion)
                region.owners.addPlayer(player.uniqueId)
                regions.addRegion(region)
                player.sendComfortZoneMessage("created")
            }

            state.sendFrame(true)
            ComfortZone.instance!!.clearPlayerState(player)
        } else {
            player.sendComfortZoneMessage("noPendingRegion")
        }
    }

    fun handleCancel(player: Player) {
        val state = ComfortZone.instance?.getPlayerState(player)
        if(state?.state == ComfortZonePlayer.State.WAIT_FOR_CONFIRMATION) {
            state.sendFrame(true)
            ComfortZone.instance!!.clearPlayerState(player)
            player.sendComfortZoneMessage("cancelled")
        } else {
            player.sendComfortZoneMessage("noPendingRegion")
        }
    }

    fun handleMember(player: Player, name: String, remove: Boolean) {
        val container = ComfortZone.worldGuard!!.platform.regionContainer
        val regions = container.get(BukkitAdapter.adapt(player.world))
        if(regions == null) {
            player.sendComfortZoneMessage("unexpectedError")
            return
        }
        if(!regions.hasRegion(player.uniqueId.toString() + "_land")) {
            player.sendComfortZoneMessage("noClaimedRegion")
            return
        }
        val region = regions.getRegion(player.uniqueId.toString() + "_land")
        val input: Array<String> = arrayOf(name)
        val profiles: ProfileService = ComfortZone.worldGuard!!.profileService
        val resolver = DomainInputResolver(profiles, input)
        resolver.locatorPolicy = DomainInputResolver.UserLocatorPolicy.UUID_AND_NAME
        val future = ComfortZone.listeningExecutorService.submit(resolver)
        Futures.addCallback(future, object: FutureCallback<DefaultDomain> {
            override fun onSuccess(result: DefaultDomain?) {
                if(remove) {
                    region!!.members.removeAll(result)
                    player.sendMessageWithColorCodes(ComfortZone.instance!!.getMessage("removedMember").replace("{player}", name))
                }
                else {
                    region!!.members.addAll(result)
                    player.sendMessageWithColorCodes(ComfortZone.instance!!.getMessage("addedMember").replace("{player}", name))
                }
            }

            override fun onFailure(t: Throwable) {
                player.sendMessageWithColorCodes("Failed to resolve UUID of $name.")
            }
        })
    }
}

fun CommandSender.sendComfortZoneMessage(key: String) {
    this.sendMessage(ComfortZone.prefixMessage(ComfortZone.instance!!.getMessage(key)))
}

fun CommandSender.sendMessageWithColorCodes(msg: String) {
    this.sendMessage(ComfortZone.prefixMessage(msg))
}

fun Location.toWEBlockVector(): BlockVector {
    return BlockVector(this.x, this.y, this.z)
}