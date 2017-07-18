/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 18, 2009
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

import java.net.Socket;
import net.whn.loki.common.ICommon.MsgType;

/**
 *
 * @author daniel
 */
public class AddGruntMsg extends Msg {

    public AddGruntMsg(Socket gSock) {
        super(MsgType.ADD_GRUNT);
        gruntSocket = gSock;
    }

    public Socket getGruntSocket() {
        return gruntSocket;
    }

    /*BEGIN PRIVATE*/
    private Socket gruntSocket;
}
