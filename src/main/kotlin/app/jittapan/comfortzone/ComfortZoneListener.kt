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

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class ComfortZoneListener: Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerQuitEvent) {
        ComfortZone.instance?.clearPlayerState(event.player)
    }
}