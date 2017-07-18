/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 17, 2009
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
package net.whn.loki.grunt;

import net.whn.loki.IO.GruntIOHelper;
import net.whn.loki.CL.CLHelper;
import net.whn.loki.master.MasterEQCaller;
import java.io.File;
import java.io.FileNotFoundException;
import net.whn.loki.network.GruntStreamSocket;
import net.whn.loki.network.Hdr;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import net.whn.loki.CL.ProcessHelper;
import net.whn.loki.IO.IOHelper;
import net.whn.loki.common.*;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.master.MasterR;

/**
 *
 * @author daniel
 */
public class GruntR implements Runnable, ICommon {

    public GruntR(MasterR m, File lokiBase, Config c) {

        //log.setLevel(Level.FINE);
        lokiCfgDir = lokiBase;
        lokiCacheDir = new File(lokiCfgDir, "fileCache");
        lokiTmpDir = new File(lokiCfgDir, "tmp");
        master = m;
        previousMD5Request = null;


        if (master != null) {
            master.setGrunt(this); //we're w/ the manager on this computer
        }

        cfg = c;

        //grunt specific

        //null values for start
        gruntForm = null;
        masterLokiVer = null;
        masterName = null;
        mSock = null;

        localShutdown = false;
        gruntQuitting = false;

        taskHandler = Executors.newSingleThreadExecutor();

    }

    //this is the 'receiver' thread.
    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        do {    //while !localShutdown
            if (getMasterAddress()) {
                try {   //initial setup steps in this try
                    
                    boolean connected = false;
                    while(!connected && !localShutdown) {
                        try {
                           //throws IOE if get any problems w/ socket/stream setup
                            gruntStreamSock =
                            new GruntStreamSocket(cfg.getMasterIp(),
                            cfg.getConnectPort());
                            connected = true;
                        } catch (IOException ioex) {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException ex) {
                                
                            }
                        }
                    }
                    
                    if(!localShutdown) {
                        
                        machineUpdateHandler =
                                Executors.newSingleThreadScheduledExecutor();

                        masterName = cfg.getMasterIp().getHostName();
                        String msg = "online with master '" + masterName + "'";
                        if (gruntForm != null) {
                            GruntEQCaller.invokeUpdateConnectionLbl(gruntForm,
                                    msg);
                        } else {
                            System.out.println(msg);
                        }

                        final MachineUpdateR muR = new MachineUpdateR(
                                gruntStreamSock);

                        machineUpdateHandler.scheduleWithFixedDelay(muR, 0, 5,
                                TimeUnit.SECONDS);

                        if (task == null) { //tell master we're idle
                            gruntStreamSock.sendHdr(new Hdr(HdrType.IDLE));
                        } else {  //didn't conclude last task so do it now
                            AssignedTask myTask = new AssignedTask();
                            runningTask = new FutureTask<String>(myTask);
                            taskHandler.submit(runningTask);
                        }

                        //main receive loop
                        do {
                            //we block on the receiveDelivery; break out when
                            //we lost connection
                        } while (!handleDelivery(gruntStreamSock.receiveDelivery()));

                    }
                    if(machineUpdateHandler != null) {
                        machineUpdateHandler.shutdownNow();
                    }
                    if(gruntStreamSock != null) {
                        gruntStreamSock.tryClose();
                    }
                    

                    //these four are all runtime problems - fatal
                } catch (NoSuchAlgorithmException ex) {
                    handleFatalException(ex);
                } catch (ClassNotFoundException ex) {
                    handleFatalException(ex);
                } catch (InvalidClassException ex) {
                    handleFatalException(ex);
                } catch (StreamCorruptedException ex) {
                    handleFatalException(ex);
                } catch (OptionalDataException ex) {
                    handleFatalException(ex);
                } catch (IOException ex) {
                    /**
                     * come here if:
                     * 1. socket was closed by user initiated shutdown
                     * 2. we lost socket because of other IOE or task handler
                     * closed socket
                     */
                    if (!localShutdown) { //case 1
                        //user closed the socket; shutdown
                        //not the user, so we lost socket from IOE; cleanup
                        //and go back to the beginning: findmaster, etc..
                        log.throwing(className, "run()", ex);
                        gruntStreamSock.tryClose();
                    }
                }
                String msg = "attempting to connect with master...";
                if (gruntForm != null) {
                    GruntEQCaller.invokeUpdateConnectionLbl(gruntForm, msg);
                } else {
                    System.out.print(msg);
                }


            } else {  //findmaster() failed: IOE or user signalled quit
                localShutdown = true;   //either case is shutdown
            }
        } while (!localShutdown);
        shutdown(false);
    }

    public void setGruntForm(GruntForm gForm) {
        gruntForm = gForm;
        gruntcl = true;
    }

    public Config getCfg() {
        return cfg;
    }

    /**
     * could be called by AWT EQ (user abort/shutdown), master, or receiverThread
     * (master abort)
     */
    public void abortCurrentTask(TaskStatus abortType) {
        if (task != null) {
            if (task.getStatus() == TaskStatus.READY) {
                task.setStatus(abortType);
                TaskReport report = new TaskReport(task);
                Hdr reportHdr = new Hdr(HdrType.TASK_REPORT, report);
                sendHdr(reportHdr);
            }
            task.setStatus(abortType);
            if (!runningTask.cancel(true)) {
                log.warning("failed to cancel running task");
            }
        } else {
            log.fine("told to abort but no task running.");
        }
        if (abortType == TaskStatus.LOCAL_ABORT) {
            try {
                taskHandler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                //nothing to do here..continue
            }
            signalShutdown();
            if (gruntForm != null) {
                gruntForm.exitNoQuery();
            }
        }
    }

    public Task getCurrentTask() {
        return task;
    }

    /*BEGIN PACKAGE*/
    boolean isBusy() {
        if (status == GruntStatus.BUSY) {
            return true;
        } else {
            return false;
        }
    }

    void handleFatalException(Exception ex) {
        //TODO - any general stuff in here?
        ErrorHelper.outputToLogMsgAndKill(gruntForm, gruntcl, log,
                "Fatal error. Click ok to exit.", ex);

        shutdown(false);
    }

    /**
     * user via AWT or local master - this is responsible for:
     * 1. setting localShutdown to true
     * 2. trying to close the gruntStreamSock (interrupts gruntreceiveThread)
     * if w/ localMaster, then we should abort task as well
     *
     */
    void signalShutdown() {
        localShutdown = true;   //flag for both receiver thread and task thread
        tryCloseMSock();    //just in case it wasn't closed after discovery
        if (gruntStreamSock != null) {
            gruntStreamSock.tryClose();
        }
        log.finest("signalShutdown()");
    }

    /*BEGIN PRIVATE*/
    //logging
    private static final String className = "net.whn.loki.grunt.GruntR";
    private static final Logger log = Logger.getLogger(className);
    //general
    private static File lokiCfgDir; //base path for '.loki' dir
    private static File lokiCacheDir;
    private static File lokiTmpDir;
    private static MasterR master;
    private static Config cfg;
    private static String masterLokiVer;  //TODO - use this. (version check)
    private String masterName;
    private static GruntForm gruntForm;
    private static boolean gruntcl = false;
    private static GruntStatus status;
    private static volatile boolean localShutdown;
    private final ExecutorService taskHandler;
    FutureTask<String> runningTask;
    private ScheduledExecutorService machineUpdateHandler;
    private volatile Task task; //receiver puts, taskHandler null when done
    private volatile String previousMD5Request; //both taskHandler and grunt
    private static boolean gruntQuitting;
    //multicast
    private MulticastSocket mSock;
    //socket - stream
    private GruntStreamSocket gruntStreamSock;
    
    
    

    /**
     * parses the header object which specifies the action to take: get more
     * files from the stream; pass task to taskPool.
     *
     * @param msg
     * @throws SocketException
     * @return true if master is quitting, false otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean handleDelivery(Hdr h) throws IOException,
            NoSuchAlgorithmException {
        if (h.getType() == HdrType.TASK_ASSIGN) {
            if (task != null) {
                log.severe("received another task before first was done!");
            } else {    //we're ok
                task = h.getTask();
                AssignedTask myTask = new AssignedTask();
                runningTask = new FutureTask<String>(myTask);
                taskHandler.submit(runningTask);
            }
        } else if (h.getType() == HdrType.MASTER_SHUTDOWN) {
            log.finer("received notice that master is shutting down");
            return true;
        } else if (h.getType() == HdrType.FILE_REPLY) {
            status = GruntStatus.BUSY;
            receiveFile(h); //fetch the file  -TODO check return?
            AssignedTask myTask = new AssignedTask();
            runningTask = new FutureTask<String>(myTask);
            taskHandler.submit(runningTask);
        } else if (h.getType() == HdrType.TASK_ABORT) {
            abortCurrentTask(TaskStatus.MASTER_ABORT);
        } else if (h.getType() == HdrType.QUIT_AFTER_TASK) {
            gruntQuitting = true;
            if (task == null) {
                shutdown(false);
            } else {
                shutdown(true);
            }
        } else {
            log.severe("handleDelivery received an unknown Hdr type: " +
                    h.getType());
        }
        return false;
    }

    private void shutdown(boolean patient) {
        log.finest("entering shutdown -->");
        //TODO - query user if busy w/ task

        taskHandler.shutdown();
        if (machineUpdateHandler != null) {
            machineUpdateHandler.shutdown();
            try {
                machineUpdateHandler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                //nothing here
            }
            machineUpdateHandler.shutdownNow();
        }
        try {
            if (patient) {
                while (!taskHandler.isTerminated()) {
                    //patiently waiting...
                }
            } else {
                taskHandler.awaitTermination(1, TimeUnit.SECONDS);
            }

            taskHandler.shutdownNow();
            signalShutdown();
        } catch (InterruptedException ex) {
            //we're shutting down, so nothing to do here
            log.finest("grunt interrupted during shutdown - weird");
        }

        try {
            if (master == null) { //if we're NOT with localMaster, write to file
                Config.writeCfgToFile(lokiCfgDir, cfg);
                IOHelper.deleteRunningLock(lokiCfgDir);
            }
        } catch (IOException ex) {
            String msg = "failed to write " +
                    "to loki.cfg.  Check filesystem permissions.";

            if (gruntForm != null) {
                MasterEQCaller.showMessageDialog(gruntForm, "Error",
                        msg, JOptionPane.WARNING_MESSAGE);
            } else {
                System.out.println(msg);
            }

            log.warning("failed to write cfg to file:" + ex.getMessage());
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            //squelch
        }

        if (gruntForm != null) {
            gruntForm.dispose();
        }

        if (master == null) {
            System.exit(0);
        }
    }

    /**
     *
     * @param h
     * @return
     * @throws NoSuchAlgorithmException if system doesn't support MD5
     * @throws IOException if our socket op failed/closed
     */
    private boolean receiveFile(Hdr h) throws NoSuchAlgorithmException,
            IOException {
        long size = h.getSize();
        if (!previousMD5Request.equals(h.getMD5())) {
            log.warning("received file taskhandler didn't ask for: " +
                    previousMD5Request + "/" + h.getMD5());
        }
        previousMD5Request = null;

        updateStatus(GruntTxtStatus.FETCH, size);

        try {
            if (GruntIOHelper.receiveFileFromBroker(h.getFileCacheType(),
                    gruntForm, cfg.getFileCacheMap(),
                    gruntStreamSock, lokiCacheDir, size, h.getMD5(), cfg)) {

                return true;
            }
        } catch (FileNotFoundException ex) {
            log.warning("file IO failed during file receive: " +
                    ex.getMessage());
        }
        return false;   //failed
    }

    /**
     * finds master's IP address, or gets from cfg if set manually
     * @return true if address was found, false if network failed, or
     * we received an interrupt(shutdown). if false then localShutdown = true
     */
    private boolean getMasterAddress() {
        if(cfg.getAutoDiscoverMaster() == true) {
            
            try {
                //throws Socket, IO exceptions
                DatagramPacket packet = listenForMaster();

                if(!localShutdown) {
                    cfg.setMasterIp(packet.getAddress());
                    
                    String masterInfo = new String(packet.getData());
                    StringTokenizer st = new StringTokenizer(masterInfo, ";");
                    masterName = st.nextToken();
                    masterLokiVer = st.nextToken();
                }
                
            } catch (IOException ex) {
                /**
                 * either we:
                 * 1. received a fatal IOE and should shutdown or
                 * 2. user set localShutdown = true, and closed port
                 * so in either case, we shutdown.
                 */
                if (!localShutdown) {
                    //if UI didn't signal, this was fatal IOE, and we should
                    //tell the user and try to close the socket
                    
                    log.warning("Loki is unable to setup multicast\n" +
                            "to discover the master. Verify that your\n" +
                            "network is properly configured. (Hint:\n" +
                            "does your network have a default route?)");
                    
                    handleFatalException(ex);
                    

                    log.throwing(className, "findMaster()", ex);
                }
            } 
        } 
            //String test = cfg.getMasterIp().getHostAddress();

        //all is well, so let's continue
        if (!localShutdown) {
            return true;
        } else //local shutdown
        {
            return false;
        }
    }
    
    private DatagramPacket listenForMaster() throws SocketException, IOException {
        byte[] buf = new byte[256];
            DatagramPacket pkt = new DatagramPacket(buf, buf.length);
            
        boolean packetReceived = false;
        
        received:
            while(!packetReceived) {
                Enumeration<NetworkInterface> nets = 
                   NetworkInterface.getNetworkInterfaces();
                
                for (NetworkInterface netint : Collections.list(nets)){
                    Enumeration<InetAddress> addresses = netint.getInetAddresses();
                    if(!netint.isLoopback()) {

                        for (InetAddress inetAddress : Collections.list(addresses)) {
                            if(inetAddress instanceof Inet4Address) {
                                MulticastSocket s = 
                                new MulticastSocket(cfg.getGruntMulticastPort());

                                //throws IOE
                                s.setInterface(inetAddress);
                                s.joinGroup(cfg.getMulticastAddress());
                                s.setSoTimeout(1000);
                                try {
                                    s.receive(pkt);  //blocks here until we get a packet
                                    packetReceived = true;
                                    s.close();
                                    break received;
                                } catch (SocketTimeoutException sockEx) {
                                    //go again
                                }
                                if(localShutdown) {
                                    break received;
                                }
                                s.close();
                            } 
                        }
                    }
                }//end for
            }
        return pkt;
    }

    /**
     * closes the multicast socket if not already closed
     * called by the UI for a shutdown, or by receiverThread during findMaster
     * if we get a socket IOE
     */
    private void tryCloseMSock() {
        if (mSock != null) {
            if (!mSock.isClosed()) {
                mSock.close();
            }
        }
    }

    private void sendHdr(Hdr h) {
        try {
            gruntStreamSock.sendHdr(h);
        } catch (IOException ex) {
            /**
             * come here if:
             * 1. we lost socket because of other IOE
             * 2. user closed socket
             *
             */
            if (!localShutdown) {    //case 1
                gruntStreamSock.tryClose();
                log.throwing(className, "run()", ex);
            } else {
                log.fine("socket closed");
                //user shutdown so they already closed socket; do nothing
                }
        }
    }

    private void updateStatus(GruntTxtStatus currentStatus) {
        if (gruntForm != null) {
            GruntEQCaller.invokeUpdateStatus(gruntForm,
                    new GruntStatusText(currentStatus));
        } else {
            System.out.println(currentStatus.toString());
        }
    }

    private void updateStatus(GruntTxtStatus currentStatus, long val) {
        if (gruntForm != null) {
            GruntEQCaller.invokeUpdateStatus(gruntForm,
                    new GruntStatusText(currentStatus, val));
        } else {
            System.out.println(currentStatus.toString() + " (bytes) :" +
                    Long.toString(val));
        }
    }

    /**
     * private inner class that handles a given assigned task
     */
    private class AssignedTask implements Callable {

        @Override
        public String call() {
            if (task == null) {
                log.severe("task is null!");
            } else {    //let's get to work
                status = GruntStatus.BUSY;
                
                if(task.isAutoFileTranfer()) {  //loki handles file tranfer
                    if (AreFilesInCache()) {   //have file? if not, request it
                        //we have the project file, so continue...
                        if (task.getStatus() == TaskStatus.DONE ||
                                task.getStatus() == TaskStatus.FAILED) {
                            //nothing to do here
                        } else {    //task hasn't run yet
                            try {
                                cfg.getFileCacheMap().get(
                                        task.getProjectFileMD5()).setInUse(true);
                                task.setStatus(TaskStatus.RUNNING);
                                runTaskWrapper(task.isAutoFileTranfer());
                                cfg.getFileCacheMap().get(
                                        task.getProjectFileMD5()).setInUse(false);

                                cfg.getFileCacheMap().get(
                                        task.getProjectFileMD5()).updateTime();
                            } catch (IOException ex) {
                                log.warning(ex.getMessage());
                            }
                        }
                        TaskReport report = new TaskReport(task);

                        if (!gruntStreamSock.isClosed()) {
                            Hdr reportHdr = new Hdr(HdrType.TASK_REPORT, report);
                            sendHdr(reportHdr);

                            if (task.getStatus() == TaskStatus.DONE) {
                                //if successful, send a file too
                                sendOutputFile();
                                log.finer("sent output file");
                                log.finer("task set to null");
                                task = null; //task done and sent, so set to null
                                if(!gruntQuitting) {
                                    sendHdr(new Hdr(HdrType.IDLE));
                                }
                                
                            } else if (task.getStatus() == TaskStatus.LOCAL_ABORT) {
                                task = null;    //ditch current task
                                signalShutdown();
                            } else if (task.getStatus() == TaskStatus.MASTER_ABORT) {
                                task = null;    //ditch current task
                                sendHdr(new Hdr(HdrType.IDLE));
                            } else if (task.getStatus() == TaskStatus.FAILED) {
                                task = null;
                                sendHdr(new Hdr(HdrType.IDLE));
                            }
                        } else {
                            updateStatus(GruntTxtStatus.PENDING_SEND);

                            log.fine("socket closed! will try and send next connect");
                        }
                    }
                } else {    //no file caching and transfer - network share
                    
                    if (task.getStatus() == TaskStatus.DONE ||
                            task.getStatus() == TaskStatus.FAILED) {
                        //nothing to do here
                    } else {    //task hasn't run yet
                        try {
                            task.setStatus(TaskStatus.RUNNING);
                            runTaskWrapper(task.isAutoFileTranfer());

                        } catch (IOException ex) {
                            log.warning(ex.getMessage());
                        }
                    }
                    TaskReport report = new TaskReport(task);

                    if (!gruntStreamSock.isClosed()) {
                        Hdr reportHdr = new Hdr(HdrType.TASK_REPORT, report);
                        sendHdr(reportHdr);

                        if (task.getStatus() == TaskStatus.DONE) {
                            
                            if(task.isAutoFileTranfer()) {
                                 //if successful, send a file too
                                sendOutputFile();
                                log.finer("sent output file");
                            }
                            //task done (and sent if auto)set to null
                            task = null; 
                            log.finer("task set to null");
                            sendHdr(new Hdr(HdrType.IDLE));
                        } else if (task.getStatus() == TaskStatus.LOCAL_ABORT) {
                            task = null;    //ditch current task
                            signalShutdown();
                        } else if (task.getStatus() == TaskStatus.MASTER_ABORT) {
                            task = null;    //ditch current task
                            sendHdr(new Hdr(HdrType.IDLE));
                        } else if (task.getStatus() == TaskStatus.FAILED) {
                            task = null;
                            sendHdr(new Hdr(HdrType.IDLE));
                        }
                    } else {
                        updateStatus(GruntTxtStatus.PENDING_SEND);

                        log.fine("socket closed! will try and send next connect");
                    }
                }
            }
            status = GruntStatus.IDLE;

            return "done";
        }

        /*BEGIN PRIVATE*/
        private void runTaskWrapper(boolean autoFileTransfer) 
                throws IOException {
            String[] taskCL;
            
            taskCL = CLHelper.generateFileTaskCL(cfg.getBlenderBin(),
                    lokiCfgDir, task);
            

            String[] result = runTask(taskCL);
            task.setInitialOutput(taskCL, result[0], result[1]);
            if (task.getStatus() != TaskStatus.LOCAL_ABORT &&
                    task.getStatus() != TaskStatus.MASTER_ABORT) {
                task.determineStatus();
            }

            if (task.getStatus() == TaskStatus.DONE) {
                try {
                    task.populateDoneInfo();
                } catch (IOException ex) {
                    //this is from the file IO while generating md5
                    log.severe("failed file IO task: " + ex.getMessage());
                    //TODO - handle this somehow
                }
                updateStatus(GruntTxtStatus.IDLE);

            } else if (task.getStatus() == TaskStatus.LOCAL_ABORT) {
                //nothing to do here...
            } else if (task.getStatus() == TaskStatus.MASTER_ABORT) {
                //nothing to do here...
            } else if (task.getStatus() == TaskStatus.FAILED) {
                updateStatus(GruntTxtStatus.ERROR);
                log.warning("task failed with output: " + task.getStdout() +
                        "\n" + task.getErrOut());
            } else {
                log.warning("unknown task type in runTaskWrapper");
            }

        }

        private String[] runTask(String[] taskCL) {
            String result[] = {"", ""};
            ProcessHelper processHelper = new ProcessHelper(taskCL);

            updateStatus(GruntTxtStatus.BUSY);

            //block here until process returns
            result = processHelper.runProcess();

            if (result[1].contains("IOException")) {
                updateStatus(GruntTxtStatus.ERROR);

                if (result[1].contains("No such file or directory")) {
                    //task executable wasn't found
                    result[0] = "task executable not found (e.g. blender)!";
                    log.warning("task executable not found for task type:" +
                            task.getType());

                }
            } else if (result[1].contains("InterruptedException")) {
                updateStatus(GruntTxtStatus.ABORT);

                if (task.getStatus() == TaskStatus.MASTER_ABORT) {
                    result[0] = "master aborted task";
                    log.info("task aborted by master");
                } else {    //local abort
                    task.setStatus(TaskStatus.LOCAL_ABORT);
                    result[0] = "local user aborted task";
                    log.info("task aborted by local user");

                }
            } else if (result[1].contains("ExecutionException")) {
                updateStatus(GruntTxtStatus.ABORT);
            }
            return result;
        }

        private void sendOutputFile() {
            try {
                File tmpOutFile = new File(lokiTmpDir,
                        task.getOutputFileName());
                updateStatus(GruntTxtStatus.SEND, tmpOutFile.length());
                GruntIOHelper.sendOutputFileToBroker(gruntForm,
                        tmpOutFile, gruntStreamSock);

                //successful, so delete tmp output file
                tmpOutFile.delete();
            } catch (IOException ex) {
                log.throwing(masterName, "run()", ex);
                //TODO have no idea how to handle this right now...
            }
            updateStatus(GruntTxtStatus.IDLE);
        }

        /**
         * check to see if file(s) is in fileCache; if not, request it
         * FUTURE: if handle more files in the future, need loop in here
         * @return true if we have all file deps met, false if we had to
         * request one
         */
        private boolean AreFilesInCache() {
            if (!cfg.getFileCacheMap().containsKey(task.getProjectFileMD5())) {
                //don't have this file; request it:
                if (previousMD5Request == null) {
                    previousMD5Request = task.getProjectFileMD5(); //broker checks on receive
                } else {
                    log.warning("previousMD5Request not null for handler!");
                }
            } else if (task.getBlendCacheMD5() != null) {

                if (!cfg.getFileCacheMap().containsKey(
                        task.getBlendCacheMD5())) {
                    //don't have this file; request it:
                    if (previousMD5Request == null) {
                        previousMD5Request = task.getBlendCacheMD5(); //broker checks on receive
                    } else {
                        log.warning("previousMD5Request not null for handler!");
                    }
                } else {    //we have in cache, so make sure it's active
                    updateStatus(GruntTxtStatus.PREP_CACHE);
                    GruntIOHelper.handleActiveBlendCache(lokiCacheDir,
                            task.getBlendCacheDirName(),
                            task.getBlendCacheMD5(), cfg);

                }
            }

            if (previousMD5Request != null) {
                Hdr FileRequestHdr = new Hdr(HdrType.FILE_REQUEST,
                        previousMD5Request);
                sendHdr(FileRequestHdr);
                return false;
            } else {
                return true;
            }
        }
    }
}
