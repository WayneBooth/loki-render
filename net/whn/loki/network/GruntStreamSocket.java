/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 24, 2009
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
import java.net.InetAddress;
import java.net.Socket;

/**
 *
 * @author daniel
 */
public class GruntStreamSocket extends StreamSocketA {

    public GruntStreamSocket(InetAddress masterAddress, int connectPort)
            throws IOException {
        //throws IOE if socket creation fails
        socket = new Socket(masterAddress, connectPort);
        initStream();
    }

    /**
     * puts the specified Hdr object into the stream. trailing file should be
     * sent with the sendFile method
     * synchronized because both machine update and taskhandler thread want to
     * send on socket
     * @param h
     * @throws IOException if socket interaction fails; probably lost connection
     */
    @Override
    public synchronized void sendHdr(Hdr h) throws IOException {
        objectOut.writeObject(h);
        objectOut.flush();
        //objectOut.reset();
    }
}
