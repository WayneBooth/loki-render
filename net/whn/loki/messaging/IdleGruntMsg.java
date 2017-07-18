/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Sep 24, 2009
 */

/**
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation, either version 3 of the License, or
 *(at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.whn.loki.messaging;

import net.whn.loki.common.ICommon;

/**
 *
 * @author daniel
 */
public class IdleGruntMsg extends Msg implements ICommon {
    public IdleGruntMsg(long gID) {
        super(MsgType.IDLE_GRUNT);
        gruntID = gID;
    }

    public long getGruntID() {
        return gruntID;
    }

    /*BEGIN PRIVATE*/

    private final long gruntID;
}
