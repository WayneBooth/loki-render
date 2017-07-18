/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 19, 2009
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
package net.whn.loki.brokersModel;

import net.whn.loki.error.LostGruntException;
import net.whn.loki.master.*;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import net.whn.loki.common.ICommon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import net.whn.loki.common.GruntDetails;
import net.whn.loki.common.ProjFile;
import net.whn.loki.common.Task;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.error.MasterFrozenException;
import net.whn.loki.messaging.FileRequestMsg;
import net.whn.loki.messaging.Msg;
import net.whn.loki.messaging.RemoveGruntMsg;

/**
 *
 * @author daniel
 */
public class BrokersModel extends AbstractTableModel implements ICommon {

    public BrokersModel(MasterR m, ConcurrentHashMap<String, ProjFile> fCache) {
        //TODO - perhaps there is a better way to handle headers?
        columnHeaders = new String[]{
                    "name",
                    "OS",
                    "cores",
                    "memory",
                    "last task",
                    "status"
                };

        master = m;
        fileSendPool = Executors.newFixedThreadPool(2);
        fileCacheMap = fCache;
    }

    /**
     * AWT
     * @param c
     * @return column name
     */
    @Override
    public String getColumnName(int c) {
        return columnHeaders[c];
    }

    /**
     * AWT
     * @return
     */
    @Override
    public int getColumnCount() {
        return columnHeaders.length;
    }

    /**
     * returns the current row count of the model.
     * AWT
     * @return
     */
    @Override
    public int getRowCount() {
        return brokersList.size();
    }

    /**
     * fetches the column value on specified row (job).
     * AWT
     * @param row
     * @param column
     * @return string value
     */
    @Override
    public Object getValueAt(int row, int column) {
        if (row < brokersList.size()) {
            return brokersList.get(row).getValue(column);
        } else {
            return "";
        }
    }

    public void handleFileRequest(Msg m) {
        FileRequestMsg frMsg = (FileRequestMsg) m;
        fileSendPool.submit(new FileToGruntTask(frMsg.getMD5(),
                frMsg.getGruntID()));
    }

    public int getCores() {
        int cores = 0;
        synchronized (brokersList) {
            for (Broker b : brokersList) {
                cores += b.getCoreCount();
            }
        }
        return cores;
    }

    public void abortTasksForGrunts(ArrayList<Long> gruntIDs)
            throws IOException, MasterFrozenException {
        for (Long gID : gruntIDs) {
            try {
                int brokerRow = getBrokerIndex(gID);
                if (brokerRow != -1) {
                    brokersList.get(brokerRow).sendTaskAbort();
                }
            } catch (IOException ex) {
                log.info("failed on send to grunt");
                try {
                    master.deliverMessage(new RemoveGruntMsg(gID));
                } catch (InterruptedException iex) {
                    log.severe("failed to deliver msg to master!"); //TODO
                }
            }
        }
    }

    public void addGrunt(Socket sock, ThreadGroup bTGroup)
            throws SocketException, IOException {
        //TODO IndexOutOfBoundsException here
        Broker broker = new Broker(sock, master, this);
        Thread brokerThread = new Thread(bTGroup, broker,
                "broker " + broker.getGruntID());
        broker.setThread(brokerThread);
        brokerThread.start();

        brokersList.add(broker);
        int newRow = brokersList.size() - 1;
        fireTableRowsInserted(newRow, newRow); //tell AWT EQ
    }

    /**
     * called by master
     */
    public void shutdown() {
        synchronized (brokersList) {
            for (Broker b : brokersList) {
                try {
                    b.shutdown();
                } catch (IOException ex) {
                    log.throwing(className, "shutdownAllGrunts()", ex);
                }
            }
        }
        fileSendPool.shutdownNow();
    }

    /**
     * if we have any changes to the grunt, we want to see the updates in
     * the table
     * run: master
     * @param gID
     */
    public void updateGruntRow(long gID) {
        int row = getBrokerIndex(gID);
        fireTableRowsUpdated(row, row); //tell AWT.EventQueue
    }

    /**
     * called by master to set grunt status
     * @param gID
     */
    public void setGruntStatusToIdle(long gID) {
        int index = getBrokerIndex(gID);
        if (index != -1) {
            brokersList.get(index).setGruntStatus(GruntStatus.IDLE);
            fireTableRowsUpdated(index, index);
        }
    }

    public void removeGrunt(long gruntID) {
        int brokerRow = getBrokerIndex(gruntID);
        if (brokerRow != -1) {
            brokersList.remove(brokerRow);
            fireTableRowsDeleted(brokerRow, brokerRow);
        }
    }

    public String getGruntName(long gID) {
        String name = "";
        int brokerRow = getBrokerIndex(gID);
        if (brokerRow != -1) {
            name = brokersList.get(brokerRow).getValue(0).toString();
        }
        return name;
    }

    public GruntDetails getGruntDetails(int row) {
        GruntDetails details = null;
        if (row < brokersList.size()) {
            details = brokersList.get(row).getDetails();
        }
        return details;
    }

    public void quitGrunt(int gruntRow) throws MasterFrozenException {
        try {
            if (gruntRow < brokersList.size()) {
                brokersList.get(gruntRow).sendQuit();
                updateBrokerRow(gruntRow, "last");
            }

        } catch (IOException ex) {
            log.info("failed on send to grunt");
            try {
                master.deliverMessage(new RemoveGruntMsg(
                        brokersList.get(gruntRow).getGruntID()));
            } catch (InterruptedException iex) {
                log.severe("failed to deliver msg to master!"); //TODO
            }
        }
    }

    public void quitAllGrunts() throws MasterFrozenException {
        synchronized (brokersList) {
            int row = 0;
            for (Broker b : brokersList) {
                try {
                    b.sendQuit();
                    updateBrokerRow(b.getGruntID(), "last");
                } catch (IOException ex) {
                    log.info("failed on send to grunt");
                    try {
                        master.deliverMessage(new RemoveGruntMsg(
                                b.getGruntID()));
                    } catch (InterruptedException iex) {
                        log.severe("failed to deliver msg to master!"); //TODO
                    }
                }
                row++;
            }
        }
    }

    /**
     * finds the next idle grunt and delivers task to it.
     * @param t
     * @param gruntID
     * @throws LostGruntException when send on socket fails, in which case we
     * exit this method - broker should be killed higher up
     * @return true if it successfully delivered task to an idle grunt; false
     * if there were no idle grunts
     */
    public boolean sendNextTask(Task t) throws LostGruntException {
        int row = 0;
        synchronized (brokersList) {
            for (Broker b : brokersList) {
                if (b.getGruntStatus() == GruntStatus.IDLE) {
                    try {
                        b.sendTaskAssign(t);
                        t.setGruntID(b.getGruntID());
                        b.setGruntStatus(GruntStatus.BUSY);
                        fireTableRowsUpdated(row, row);
                        return true;
                    } catch (IOException ex) {
                        if (!b.isSocketClosed()) {
                            //we're first to hit this problem, so log it
                            log.throwing(className, "sendNextTask(Task t)", ex);
                        }
                        throw new LostGruntException(b.getGruntID());
                    }
                }
                row++;
            }
        }
        return false;
    }

    /*PACKAGE*/
    /**
     * update grunt row in GUI w/ given status string
     * run: master
     * @param gID
     * @param newStatus
     */
    void updateBrokerRow(long gID, String newStatus) {
        int row = getBrokerIndex(gID);
        if (row != -1) {
            brokersList.get(row).setStatusStr(newStatus);
            fireTableRowsUpdated(row, row);
        }
    }

    /*PRIVATE*/
    //logging
    private static final String className = "net.whn.loki.master.GruntsModel";
    private static final Logger log = Logger.getLogger(className);
    private final MasterR master;
    private final List<Broker> brokersList =
            Collections.synchronizedList(new ArrayList<Broker>());
    private final ConcurrentHashMap<String, ProjFile> fileCacheMap;
    private final String[] columnHeaders;
    private ExecutorService fileSendPool;//for sending files to grunts
    //the master has better things to do

    /**
     * @param gID
     * @return the row of the broker for gruntID, -1 if it doesn't exist
     */
    private int getBrokerIndex(long gID) {

        synchronized (brokersList) {
            for (int r = 0; r < brokersList.size(); r++) {
                if (gID == brokersList.get(r).getGruntID()) {
                    return r;
                }
            }
        }
        return -1;
    }

    /**l
     * private inner class that sends file to grunt via the broker socket
     * run: fileSendPool
     */
    private class FileToGruntTask implements Runnable {

        FileToGruntTask(String m, long gID) {
            md5 = m;
            gruntIndex = getBrokerIndex(gID);
        }

        @Override
        public void run() {
            if (!fileCacheMap.containsKey(md5)) {
                log.severe("grunt requested a file I don't have: " +
                        md5);
            } else {
                pFile = fileCacheMap.get(md5);
                if (!pFile.getProjFile().exists()) {
                    log.severe("file for key " + md5 + " doesn't exist!");
                } else {
                    Broker b = brokersList.get(gruntIndex);
                    try {
                        brokersList.get(gruntIndex).setStatusStr("fetching");
                        fireTableRowsUpdated(gruntIndex, gruntIndex);
                        fileCacheMap.get(md5).updateTime();
                        b.sendFile(pFile);
                        brokersList.get(gruntIndex).setStatusStr("busy");
                        fireTableRowsUpdated(gruntIndex, gruntIndex);
                    } catch (IOException ex) {
                        log.warning("IO problem during file send: " +
                                ex.getMessage());
                        try {
                            b.sendRemoveGruntMsg();
                        } catch (InterruptedException iex) {
                            ErrorHelper.outputToLogMsgAndKill(null, false, log,
                                    "fatal error. exiting.", iex);
                        } catch (MasterFrozenException mfe) {
                            ErrorHelper.outputToLogMsgAndKill(null, false, log,
                                    "fatal error. exiting.", mfe);
                        }
                    }
                }
            }
        }

        /*PRIVATE*/
        private final String md5;
        private final int gruntIndex;
        private ProjFile pFile;
    }
}
