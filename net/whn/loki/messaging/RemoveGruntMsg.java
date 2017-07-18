/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 25, 2009
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

import net.whn.loki.common.Task;

/**
 *
 * @author daniel
 */
public class RemoveGruntMsg extends Msg {

    public RemoveGruntMsg(long gID) {
        super(MsgType.REMOVE_GRUNT);
        gruntID = gID;
        status = GruntStatus.IDLE;
        task = null;
    }

    public RemoveGruntMsg(long gID, GruntStatus gStatus, Task t) {
        super(MsgType.REMOVE_GRUNT);
        gruntID = gID;
        status = gStatus;
        task = t;
    }

    public long getGruntID() {
        return gruntID;
    }

    public GruntStatus getGruntStatus() {
        return status;
    }

    public Task getGruntTask() {
        return task;
    }

    /*BEGIN PRIVATE*/
    final private long gruntID;
    final private GruntStatus status;
    final private Task task;
}
