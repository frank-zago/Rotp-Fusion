/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.events;

import rotp.model.empires.Empire;
import rotp.model.galaxy.SpaceMonster;
// BR: kept for backward compatibility
// default methods are now in the abstract class
// Without keeping the interface, adding method in the abstract lead to some
// backward compatibility issues.
public interface RandomEvent {
	int GNN_NEVER	= 0;
	int GNN_READY	= 10;
	int GNN_TARGET	= 20;
	int GNN_REDIR	= 30;
	int GNN_END		= 40;
    boolean goodEvent();
    boolean repeatable();
    boolean hasPendingEvents();
    boolean monsterEvent();
    String systemKey();
    String statusMessage();
    Empire getPendingEmpire();
    String notificationText();
    void trigger(Empire e);
    void addPendingEvents(Empire e);
    void nextTurn();
    int minimumTurn();
    int startTurn();
    default boolean techDiscovered()			{ return true; }
    default SpaceMonster monster(boolean track)	{ return null; }
    default void validateOnLoad()	{ };
}
