/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 12, 2009
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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.whn.loki.error.MasterFrozenException;

/**
 *provides a blocking queue for messages
 * NOTE: I can easily add other methods for timeout waits, etc if needed.
 * @author daniel
 */
public abstract class MsgQueue {

    public MsgQueue(int mqSize){
        messageQueue = new LinkedBlockingQueue<Msg>(mqSize);
    }

    /**
     * blocks until it can place message on the queue
     * @param message
     * @throws InterruptedException if interrupted during block (shutdown)
     */
    public void deliverMessage(Msg message) throws InterruptedException,
        MasterFrozenException{

        if(!messageQueue.offer(message, 10, TimeUnit.SECONDS)) {
            log.severe("failed to delivermessage w/ 10 second time out.");
            throw new MasterFrozenException(null);
        }
    }

    /**
     * blocks until there is a message to be taken.
     * this is ok since all actions by the master are initiated
     * via a message
     * @return
     * @throws InterruptedException if thread is interrupted while waiting
     */
    public Msg fetchNextMessage() throws InterruptedException {
        return messageQueue.take();
    }

    /*BEGIN PRIVATE*/
    private final LinkedBlockingQueue<Msg> messageQueue;

    private static final Logger log =
            Logger.getLogger("net.whn.loki.messaging.MsgQueue");
}
