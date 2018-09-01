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

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

class ComfortZonePlayer(player: Player) {
    var state: State = State.NONE
    var pos1Block: Location? = null
    var pos2Block: Location? = null
    var player = player

    fun sendFrame(restore: Boolean) {
        val y = ComfortZone.instance!!.fakeFrameYPos.toDouble()
        val material = ComfortZone.instance!!.fakeFrameBlock

        // (x1,z1) -> (x2, z1)
        for (i in pos1Block!!.blockX..pos2Block!!.blockX) {
            if(restore) {
                val loc = Location(player.world, i.toDouble(), y, pos1Block!!.z)
                player.sendBlockChange(loc, loc.block.blockData)
            }
            else
                player.sendBlockChange(Location(player.world, i.toDouble(), y, pos1Block!!.z), material.createBlockData())
        }
        // (x1,z1) -> (x1, z2)
        for (i in pos1Block!!.blockZ..pos2Block!!.blockZ) {
            if(restore) {
                val loc = Location(player.world, pos1Block!!.x, y, i.toDouble())
                player.sendBlockChange(loc, loc.block.blockData)
            }
            else
                player.sendBlockChange(Location(player.world, pos1Block!!.x, y, i.toDouble()), material.createBlockData())
        }
        // (x1,z2) -> (x2, z2)
        for (i in pos1Block!!.blockX..pos2Block!!.blockX) {
            if(restore) {
                val loc = Location(player.world, i.toDouble(), y, pos2Block!!.z)
                player.sendBlockChange(loc, loc.block.blockData)
            }
            else
                player.sendBlockChange(Location(player.world, i.toDouble(), y, pos2Block!!.z), material.createBlockData())
        }
        // (x2,z1) -> (x2, z2)
        for (i in pos1Block!!.blockZ..pos2Block!!.blockZ) {
            if(restore) {
                val loc = Location(player.world, pos2Block!!.x, y, i.toDouble())
                player.sendBlockChange(loc, loc.block.blockData)
            }
            else
                player.sendBlockChange(Location(player.world, pos2Block!!.x, y, i.toDouble()), material.createBlockData())
        }
    }

    fun setCorner(pos1: Location, pos2: Location) {
        pos1Block = pos1
        pos2Block = pos2
    }

    fun setCornerFromRadius(radius: Int) {
        val x = player.location.blockX
        val z = player.location.blockZ
        val y = ComfortZone.instance!!.fakeFrameYPos.toDouble()
        var step1: Int = radius / 2
        var step2: Int = radius / 2
        if(radius % 2 == 0) {
            step2 -= 1
        }
        setCorner(Location(player.world, (x - step1).toDouble(), y, (z - step1).toDouble()), Location(player.world, (x + step2).toDouble(), y, (z + step2).toDouble()))
    }

    enum class State {
        NONE,
        WAIT_FOR_CONFIRMATION
    }
}