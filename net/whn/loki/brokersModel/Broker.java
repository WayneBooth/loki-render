/**
 *Project: Loki Render - A distributed job queue master.
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

import net.whn.loki.IO.MasterIOHelper;
import java.io.File;
import net.whn.loki.master.*;
import net.whn.loki.network.BrokerStreamSocket;
import net.whn.loki.network.Hdr;
import java.io.IOException;
import net.whn.loki.common.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.error.MasterFrozenException;
import net.whn.loki.messaging.*;

/**
 *
 * it sits on socket, waiting to receive
 * @author daniel
 */
public class Broker implements Runnable, ICommon {

    @Override
    public void run() {
        while (true) {

            try {
                //we'll block on this call until we receive a header from socket
                handleIncomingDelivery(bSSock.receiveDelivery());
            } catch (IOException ex) {
                if (!bSSock.isClosed()) {
                    //we're the first to hit the problem, so log it
                    log.throwing(className, "run()", ex);

                    bSSock.tryClose();  //and try to close the socket
                }
                break;  //now exit so this broker can die
            } catch (ClassNotFoundException ex) {
                try {
                    master.deliverMessage(
                            new FatalThrowableMsg(MsgType.FATAL_ERROR, ex));
                } catch (InterruptedException iex) {
                    //nothing to do here
                } catch (MasterFrozenException mfe) {
                    ErrorHelper.outputToLogMsgAndKill(null, false, log,
                            "fatal error. exiting.", ex);
                }
                log.throwing(className, "run()", ex);
                break;
            } catch (InterruptedException ex) {
                ErrorHelper.outputToLogMsgAndKill(null, false, log,
                        "fatal error. exiting.", ex);
                System.exit(-1);
                break;
            } catch (MasterFrozenException mfe) {
                ErrorHelper.outputToLogMsgAndKill(null, false, log,
                        "fatal error. exiting.", mfe);
                System.exit(-1);
            }
        }
        bSSock.tryClose();
        try {
            if(taskPending) {
                sendBusyGruntLostMsg();
            }
            sendRemoveGruntMsg();
            
        } catch (InterruptedException ex) {
            ErrorHelper.outputToLogMsgAndKill(null, false, log,
                        "fatal error. exiting.", ex);
                System.exit(-1);
        } catch (MasterFrozenException mfe) {
            ErrorHelper.outputToLogMsgAndKill(null, false, log,
                        "fatal error. exiting.", mfe);
                System.exit(-1);
        }
    }
    
    public void sendBusyGruntLostMsg() throws InterruptedException,
            MasterFrozenException {
        log.fine("sending lostBusyGruntMsg for grunt w/ id: " + gruntID);
        Msg lostBusyGruntMsg = new LostBusyGruntMsg(gruntID, assignedTask);
        master.deliverMessage(lostBusyGruntMsg);
    }

    public void sendRemoveGruntMsg() throws InterruptedException,
            MasterFrozenException {
        log.fine("sending removeGruntMsg for grunt w/ id: " + gruntID);
        Msg removeGruntMsg = new RemoveGruntMsg(gruntID, status, assignedTask);

        master.deliverMessage(removeGruntMsg);

    }

    public Object getValue(int column) {
        if (column == 0) {
            return machine.getHostname();
        } else if (column == 1) {
            return machine.getOsName();
        } else if (column == 2) {
            return machine.getProcessors();
        } else if (column == 3) {
            if (lastMachineUpdate == null) {
                return null;
            } else {
                return lastMachineUpdate.getMemUsageStr();
            }

        } else if (column == 4) {
            return lastTaskTime;
        } else if (column == 5) {
            return statusStr;
        } else {
            throw new IllegalArgumentException(Integer.toString(column));
        }
    }

    public GruntDetails getDetails() {
        return new GruntDetails(
                machine.getHostname(),
                machine.getOsName(),
                machine.getOsVersion(),
                machine.getOsArchitecture(),
                machine.getProcessors(),
                machine.getTotalMemory(),
                machine.getTotalSwap(),
                machine.getUserName(),
                machine.getUserHome(),
                machine.getCurrentWorkingDir());
    }

    /*BEGIN PACKAGE*/
    Broker(Socket gSocket, MasterR m, BrokersModel bModel)
            throws SocketException, IOException {
        gruntID = gruntIDCounter++;
        bSSock = new BrokerStreamSocket(gSocket);   //throws SocketException
        machine = new Machine("fetching...");
        master = m;
        lokiBaseDir = m.getLokiCfgDir();
        brokersModel = bModel;
        status = GruntStatus.BUSY;
        taskPending = false;
        statusStr = "unknown";
        assignedTask = null;
        lastMachineUpdate = null;
        lastTaskTime = null;

        //log.setLevel(Level.ALL);
    }

    int getCoreCount() {
        return machine.getProcessors();
    }

    /**
     * called by gruntsModel(aka master)
     * @return
     */
    long getGruntID() {
        return gruntID;
    }

    /**
     * only called by master
     * @return
     */
    GruntStatus getGruntStatus() {
        return status;
    }

    /**
     * only called by master
     * @param s
     */
    void setGruntStatus(GruntStatus s) {
        status = s;
        if (status == GruntStatus.IDLE) {
            statusStr = "idle";
        } else if (status == GruntStatus.BUSY) {
            statusStr = "busy";
        }
    }

    /**
     * cases like 'fetching...' or 'sending'; these are cosmetic and don't
     * affect internal behavior
     */
    void setStatusStr(String str) {
        statusStr = str;
    }

    void setThread(Thread bThread) {
        brokerThread = bThread;
    }

    Thread getThread() {
        return brokerThread;
    }

    /**
     * called by master to send a task to this broker's grunt
     * @param t
     * @throws IOException if we get socket IO problem - pass to calling method
     */
    synchronized void sendTaskAssign(Task t) throws IOException {
        Hdr h = null;
        try {
            //clone so i send unique task object for grunt, otherwise :-(moby
            h = new Hdr(HdrType.TASK_ASSIGN, t.clone());
            bSSock.sendHdr(h);
            assignedTask = t;
            taskPending = true;
        } catch (CloneNotSupportedException cex) {
            //TODO - um...
            log.severe("couldn't clone task!");
        }

    }

    synchronized void sendTaskAbort() throws IOException {
        Hdr h = new Hdr(HdrType.TASK_ABORT);
        bSSock.sendHdr(h);
    }

    synchronized void sendQuit() throws IOException {
        Hdr h = new Hdr(HdrType.QUIT_AFTER_TASK);
        bSSock.sendHdr(h);
    }

    /**
     * we send a message to the grunt that we're shutting down, then we
     * close the socket so the broker thread knows we're shutting down
     */
    synchronized void shutdown() throws IOException {
        Hdr h = new Hdr(HdrType.MASTER_SHUTDOWN);
        bSSock.sendHdr(h);
        bSSock.tryClose();
    }

    boolean isSocketClosed() {
        return bSSock.isClosed();
    }

    /**
     * called by file sender, not master
     * NOTE: synchronize on the broker object because the master may try to send
     * a shutdown message or something similar on the socket while we're sending
     * ; master needs to synchronize on the broker as well!
     * @param pFile
     * @throws IOException
     */
    synchronized void sendFile(ProjFile pFile) throws IOException {

        bSSock.sendHdr(new Hdr(HdrType.FILE_REPLY, pFile.getFileCacheType(),
                pFile.getMD5(), pFile.getSize()));
        MasterIOHelper.sendProjectFileToGrunt(pFile, bSSock);
    }

    /*BEGIN PRIVATE*/
    //logging
    private static final String className = "net.whn.loki.master.Broker";
    private static final Logger log = Logger.getLogger(className);
    private final File lokiBaseDir;
    private static long gruntIDCounter = 0;
    private long gruntID;
    private final MasterR master;
    private final BrokersModel brokersModel;
    private String lastTaskTime;
    private Machine machine;
    private MachineUpdate lastMachineUpdate;
    private GruntStatus status;
    private volatile String statusStr;
    private Thread brokerThread;
    private BrokerStreamSocket bSSock;
    private volatile Task assignedTask; //accessed by both master and broker
    private volatile boolean taskPending; //both master and broker

    /**
     * this is handling an incoming delivery on the gruntSocket
     * @param msg
     * @throws IOException
     * @throws SocketException
     * @throws InterruptedException if we timeout while delivering message
     * to master
     */
    private void handleIncomingDelivery(Hdr header) throws IOException,
            InterruptedException, MasterFrozenException {
        if (header.getType() == HdrType.IDLE) {
            handleIdle();
        } else if (header.getType() == HdrType.MACHINE_INFO) {
            handleMachineInfo(header);
        } else if (header.getType() == HdrType.MACHINE_UPDATE) {
            handleMachineUpdate(header);
        } else if (header.getType() == HdrType.TASK_REPORT) {
            handleTaskReport(header);   //throws InterruptedException
            taskPending = false;
        } else if (header.getType() == HdrType.FILE_REQUEST) {
            handleFileRequest(header);
        } else {
            log.severe("handleIncomingDelivery received and unknown header " +
                    header.getType().toString());
        }
    }

    /**
     * determines if this is our first machineInfo for this grunt then
     * passes this info on to master along with the machineinfo object
     * @param header
     */
    private void handleMachineInfo(Hdr header) throws InterruptedException,
        MasterFrozenException {
        log.finest("received machine info");
        boolean firstReceive = false;
        if (machine.getProcessors() == -1) {
            firstReceive = true;
        }

        machine = header.getMachine();
        master.deliverMessage(new UpdateGruntMsg(gruntID, firstReceive));
    }

    private void handleMachineUpdate(Hdr header) throws InterruptedException,
        MasterFrozenException {
        log.finest("received machine update");
        lastMachineUpdate = header.getMachineUpdate();
        master.deliverMessage(new UpdateGruntMsg(gruntID, false));
    }

    /**
     * looks at report, if done, receives accompanying file as well,
     * then passes taskreport to master
     * @param header
     * @throws InterruptedException if we timeout on the master's delivery
     */
    private void handleTaskReport(Hdr header) throws InterruptedException,
        MasterFrozenException {
        //cast
        TaskReportMsg reportMsg = new TaskReportMsg(header.getTaskReport());

        //insert gruntID into report
        reportMsg.getReport().setGruntID(gruntID);
        Task t = reportMsg.getReport().getTask();

        //we only get a file if task is done
        if (t.getStatus() == TaskStatus.DONE) {
            lastTaskTime = t.getTaskTime();
            if(t.isAutoFileTranfer()) {
                brokersModel.updateBrokerRow(gruntID, "sending");
                if(!MasterIOHelper.receiveOutputFileFromGrunt(
                        bSSock, t, lokiBaseDir)) {
                    //failed to receive or save output file
                    t.setStatus(TaskStatus.FAILED);
                    String error = "failed to receive file from grunt, or " +
                            "save file to the output directory. Check " +
                            "output directory permissions.";
                    log.warning(error);
                    t.setErrout(error);
                }
            }
        }
        t.setGruntName(machine.getHostname());

        master.deliverMessage(reportMsg);
    }

    private void handleIdle() throws InterruptedException,
            MasterFrozenException {
        master.deliverMessage(new IdleGruntMsg(gruntID));
    }

    /**
     * inserts gruntID then passes to master to send the file to grunt.
     * @param header
     * @throws InterruptedException
     */
    private void handleFileRequest(Hdr header) throws InterruptedException,
        MasterFrozenException {
        FileRequestMsg fRMsg = new FileRequestMsg(MsgType.FILE_REQUEST,
                header.getMD5(), gruntID);

        master.deliverMessage(fRMsg);
    }
}
