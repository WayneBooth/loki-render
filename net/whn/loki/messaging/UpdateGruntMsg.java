/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 22, 2009
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

import net.whn.loki.common.*;

/**
 *
 * @author daniel
 */
public class UpdateGruntMsg extends Msg implements ICommon{

    public UpdateGruntMsg(long gID, boolean first) {
        super(MsgType.UPDATE_GRUNT);
        gruntID = gID;
        firstMachineReply = first;
    }

    public long getGruntID() {
        return gruntID;
    }

    public boolean getFirstMachineReply() {
        return firstMachineReply;
    }

    /*BEGIN PRIVATE*/
    private final long gruntID;
    private final boolean firstMachineReply;
}
