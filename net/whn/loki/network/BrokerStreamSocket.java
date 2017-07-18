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

package net.whn.loki.network;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 *provides functionality needed by the broker to communicate with the grunt
 * @author daniel
 */
public class BrokerStreamSocket extends StreamSocketA {

    /**
     *
     * @param gruntSocket
     * @throws SocketException if we have underlying protocol problem
     * @throws IOException if we failed to setup the streams on socket
     */
    public BrokerStreamSocket(Socket gruntSocket) throws SocketException,
        IOException {
        socket = gruntSocket;
        //socket.setSoTimeout(500);
        initStream();
    }

    /*BEGIN PRIVATE*/

}
