/**
 *Project: Loki Render - A distributed job queue master.
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
package net.whn.loki.master;

import net.whn.loki.common.ICommon;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.logging.Logger;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.error.MasterFrozenException;
import net.whn.loki.messaging.AddGruntMsg;
import net.whn.loki.messaging.FatalThrowableMsg;

/**
 *
 * @author daniel
 */
public class ListenerR implements Runnable, ICommon {

    public void run() {
        Socket gruntSocket = null;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                try {
                    gruntSocket = listenSocket.accept();
                    master.deliverMessage(new AddGruntMsg(gruntSocket));
                } catch (InterruptedException ex) {
                    //master says shutdown
                    break;
                } catch (SocketTimeoutException ex) {
                    //don't do anything. we just wanted to get
                    //unblocked so we can periodically check the interrupt
                } catch (MasterFrozenException mfe) {
                    ErrorHelper.outputToLogMsgAndKill(null, false, log,
                            "fatal error. exiting.", mfe.getCause());
                }
            } catch (IOException ex) {
                //hopefully next time succeeds!
                //TEST
                System.out.println("failed to accept new grunt.");
            }
        }
        //we received a shutdown signal
        if (listenSocket.isBound()) {
            try {
                listenSocket.close();
            } catch (IOException ex) {
                //do nothing...we're exiting
            }
        }
    }

    /*BEGIN PACKAGE*/
    ServerSocket listenSocket;
    MasterR master;

    ListenerR(int gPort, MasterR m) throws InterruptedException, IOException {
        listenSocket = new ServerSocket(gPort);
        listenSocket.setSoTimeout(1000);

        master = m;
    }
    /*PRIVATE*/
    //logging
    private static final String className = "net.whn.loki.master.ListenerR";
    private static final Logger log = Logger.getLogger(className);
}
