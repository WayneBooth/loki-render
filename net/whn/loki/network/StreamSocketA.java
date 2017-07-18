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

import net.whn.loki.common.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;

/**
 * provides common socket and stream functionality specific to communication
 * between the broker and grunt.
 * notes on behavior:
 * @author daniel
 */
public abstract class StreamSocketA implements ICommon {

    /**
     * configures the socket and initializes stream objects for interacting
     * with the socket.  must be called before the socket read/write!
     * @throws SocketException if underlying protocol has a problem
     * @throws IOException if socket is not connected or we get an I/O from
     * attempting to create input/outputstream
     */
    public void initStream() throws SocketException, IOException {
        sockOut = socket.getOutputStream();
        sockIn = socket.getInputStream();
        objectOut = new ObjectOutputStream(sockOut);
        objectOut.flush();
        objectIn = new ObjectInputStream(sockIn);
    }

    public InputStream getSockIn() {
        return sockIn;
    }

    /**
     * puts the specified Hdr object into the stream. trailing file should be
     * sent with the sendFile method
     * @param h
     * @throws IOException if socket interaction fails; probably lost connection
     */
    public void sendHdr(Hdr h) throws IOException {
        objectOut.writeObject(h);
        objectOut.flush();
    }

    public void sendFileChunk(byte[] buffer, int amountToSend)
        throws IOException {
        sockOut.write(buffer, 0, amountToSend);
    }

    public void flushSockOut() throws IOException {
        sockOut.flush();
    }

    /**
     * grabs the next object from the stream, assumes it will be of type Hdr
     * TODO - change to handle files when we get that far
     * @return Hdr object taken from the stream, unless we have an Exception:-)
     * @throws IOException if we have a socket problem
     * @throws ClassNotFoundException if we don't recognize the received class
     */
    public Hdr receiveDelivery() throws ClassNotFoundException,
        InvalidClassException, StreamCorruptedException, OptionalDataException,
        IOException {
        return (Hdr) objectIn.readObject();
    }

    /**
     * attempts to close the socket if it isn't already closed
     */
    public void tryClose() {
        if (!socket.isClosed()) {
            try {
                socket.close();
            }
            catch (IOException ex) {
                log.throwing(className, "tryClose()", ex);
            }
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    /*BEGIN PROTECTED*/
    protected Socket socket;
    protected InputStream sockIn;
    protected OutputStream sockOut;
    protected ObjectInputStream objectIn;
    protected ObjectOutputStream objectOut;   

    /*BEGIN PRIVATE*/

    //logging
    private static final String className = "net.whn.loki.network.AStreamSocket";
    private static final Logger log = Logger.getLogger(className);
}
