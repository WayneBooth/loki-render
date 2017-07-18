/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 8, 2009, 8:09:39 PM
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

import net.whn.loki.error.LostGruntException;
import net.whn.loki.IO.MasterIOHelper;
import net.whn.loki.brokersModel.BrokersModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import net.whn.loki.IO.IOHelper;
import net.whn.loki.common.EQCaller;
import net.whn.loki.common.ICommon;
import net.whn.loki.common.Config;
import net.whn.loki.common.GruntDetails;
import net.whn.loki.error.MasterFrozenException;
import net.whn.loki.common.ProjFile;
import net.whn.loki.common.Task;
import net.whn.loki.common.TaskReport;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.grunt.GruntR;
import net.whn.loki.messaging.*;

/**
 *This is the central control point for all job and grunt management
 * @author daniel
 */
public class MasterR extends MsgQueue implements Runnable, ICommon {

    /**
     *default constructor
     */
    public MasterR(File lokiBase, Config c, AnnouncerR a,
            int mQSize) {
        super(mQSize);

        //log.setLevel(Level.FINE);

        lokiCfgDir = lokiBase;
        lokiCacheDir = new File(lokiCfgDir, "fileCache");

        cfg = c;
        Job.setJobIDCounter(cfg.getJobIDCounter());
        Task.setTaskIDCounter(cfg.getTaskIDCounter());
        jobsModel = cfg.getJobsModel();
        fileCacheMap = cfg.getFileCacheMap();

        brokersModel = new BrokersModel(this, fileCacheMap);
        grunt = null;
        announcer = a;
        queueRunning = false;
        shutdown = false;
        lastTotalCores = -1;

        //listener setup here
        try {
            listener = new ListenerR(cfg.getConnectPort(), this);
        } catch (Exception ex) {   //TODO - hmmm...
            //if either of these fail, we have to quit
            //TODO - message for user
            log.severe("listener failed to setup!");
            throw new IllegalThreadStateException();
        }

        helpersThreadGroup = new ThreadGroup("helpers");
        brokersThreadGroup = new ThreadGroup("brokers");

        listenerThread = new Thread(helpersThreadGroup, listener, "listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        announcerThread = new Thread(helpersThreadGroup, announcer, "announcer");
        announcerThread.setDaemon(true);
        announcerThread.start();

        compositer = Executors.newSingleThreadExecutor();
    }

    /**
     * called by a local grunt (if one is present) right after startup
     * we need this so we can tell the local grunt to shutdown when master does
     * @param g
     */
    public void setGrunt(GruntR g) {
        grunt = g;
    }

    /**
     * @return the cfg object for this session
     */
    public Config getCfg() {
        return cfg;
    }

    public File getLokiCfgDir() {
        return lokiCfgDir;
    }

    /**
     * passes a handle of the masterForm object for master to use
     * @param mjForm
     */
    public void setMasterForm(MasterForm mjForm) {
        masterForm = mjForm;
        updateProgressBar();
    }

    /**
     * called by MasterForm (AWT) if user clicked stop
     */
    public void setQueueRunningFalse() {
        queueRunning = false;
    }

    @Override
    public void run() {
        /**
         * main loop for the master
         */
        while (!shutdown) {
            try {
                handleMessage(fetchNextMessage());
            } catch (InterruptedException IntEx) {
                /**
                 * signaled ourselves to shutdown
                 */
                break;
            } catch (IOException ex) {
                ErrorHelper.outputToLogMsgAndKill(masterForm, false, log,
                    "Loki encountered an error", ex);
            } catch (MasterFrozenException mfe) {
                //impossible
            }
        }
    }

    /*BEGIN PACKAGE*/
    /**
     * called by AWT for user exit request - are there any jobs running (tasks)
     * @return true if yes, false if none
     */
    boolean areJobsRunning() {
        return jobsModel.areJobsRunning();
    }

    boolean areJobsRunning(int[] selectedRows) {
        return jobsModel.areSelectedJobsRunning(selectedRows);
    }

    /**
     * used by masterForm to get handle on jobsModel
     * @return
     */
    JobsModel getJobsModel() {
        return jobsModel;
    }

    /**
     * used by masterForm to get handle on gruntsModel
     * @return
     */
    BrokersModel getBrokersModel() {
        return brokersModel;
    }

    /*BEGIN PRIVATE*/
    private final File lokiCfgDir;
    private final File lokiCacheDir;
    private final Config cfg;    //stores cfg data for persistence
    private volatile GruntR grunt;  //set by local grunt, read by master
    private final JobsModel jobsModel;  //holds all the jobs
    private final ConcurrentHashMap<String, ProjFile> fileCacheMap;  //map holding all projFiles
    private final BrokersModel brokersModel;  //holds all the brokers
    private MasterForm masterForm;        //handle for masterForm
    private AnnouncerR announcer;
    private ListenerR listener;
    private Thread listenerThread, announcerThread;
    private final ExecutorService compositer;
    private ThreadGroup helpersThreadGroup;
    private ThreadGroup brokersThreadGroup;
    private volatile boolean queueRunning;  //volatile since UI changes this
    private boolean shutdown;
    private int lastTotalCores;
    //logging
    private static final String className = "net.whn.loki.master.MasterR";
    private static final Logger log = Logger.getLogger(className);

    /**
     * main decision point for masterThread
     * @param m
     * @throws InterruptedException
     */
    private void handleMessage(Msg m) throws InterruptedException, IOException,
        MasterFrozenException {
        MsgType type = m.getType();

        if (type == MsgType.ADD_JOB) {
            addJob(m);
        } else if (type == MsgType.REMOVE_JOBS) {
            removeJobs(m);
        } else if (type == MsgType.VIEW_JOB) {
            viewJob(m);
        } else if (type == MsgType.ADD_GRUNT) {
            addGrunt(m);
        } else if (type == MsgType.SHUTDOWN) {
            shutdown();
        } else if (type == MsgType.UPDATE_GRUNT) {
            updateGrunt(m);
        } else if (type == MsgType.REMOVE_GRUNT) {
            removeGrunt(m);
        } else if (type == MsgType.VIEW_GRUNT) {
            viewGrunt(m);
        } else if (type == MsgType.QUIT_GRUNT) {
            quitGrunt(m);
        } else if (type == MsgType.QUIT_ALL_GRUNTS) {
            quitAllGrunts();
        } else if (type == MsgType.IDLE_GRUNT) {
            idleGrunt(m);
        } else if (type == MsgType.START_QUEUE) {
            queueStarting();
        } else if (type == MsgType.TASK_REPORT) {
            handleReport(m);
        } else if (type == MsgType.LOST_BUSY_GRUNT) {
            handleLostBusyGrunt(m);
        }else if (type == MsgType.RESET_FAILURES) {
            resetFailures(m);
        } else if (type == MsgType.FILE_REQUEST) {
            brokersModel.handleFileRequest(m);
        } else if (type == MsgType.ABORT_ALL) {
            abortAll_StopQueue();
        } else if (type == MsgType.FATAL_ERROR) {
            handleFatalError(m);
        } else {
            log.warning("master received unknown MsgType: " + type);
        }
    }

    /**
     * calls shutdown then throws up the stack
     * @param m
     */
    private void handleFatalError(Msg m) {
        FatalThrowableMsg fatalMsg = (FatalThrowableMsg) m;
        Throwable throwable = fatalMsg.getThrowable();

        ErrorHelper.outputToLogMsgAndKill(masterForm, false, log,
                "Loki encountered a fatal error.\n" +
                "Click OK to exit.", throwable);

        shutdown();
    }

    /**
     * passes new job to jobsModel
     * called by addJob in masterForm
     * @param j
     */
    private void addJob(Msg m) {
        final AddJobMsg message = (AddJobMsg) m;
        JobFormInput newJobInput = message.getJobInput();
        String md5 = null;
        String blendCacheMd5 = null;

        if (!jobsModel.isJobNameUnique(newJobInput.getName())) {
            EQCaller.showMessageDialog(masterForm, "Job name exists",
                    "A job with the name '" +
                    newJobInput.getName() +
                    "' already exists. Please use a unique name.",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            if(newJobInput.getAutoFileTransfer()) {
               //auto file transfer is enabled, so 
               //cache the project file and add to fileCacheMap
                md5 = MasterIOHelper.newProjFileToCache(fileCacheMap,
                    newJobInput.getProjFileName(), lokiCacheDir, cfg);
                blendCacheMd5 = MasterIOHelper.addBlendCacheToLokiCache(
                    fileCacheMap, lokiCacheDir, newJobInput.getProjFileName(),
                    cfg); 
            }
            
            File pFile = new File(newJobInput.getProjFileName());
            long size = pFile.length();
            
            if(newJobInput.getAutoFileTransfer()) {
               if (md5 == null) {
                //oops, we failed to cacheFile
                EQCaller.showMessageDialog(masterForm, "failed to add job",
                        "Adding a new job failed. Filesystem permission problem?",
                        JOptionPane.WARNING_MESSAGE);
                log.severe("unable to add new job.");

                } 
            }
            //if auto file transfer was on, then
            //file was successfully added to cache; create/add new job
            Job newJob = new Job(newJobInput, md5, blendCacheMd5, size);
            jobsModel.addJob(newJob);
            updateProgressBar();
        }
        message.disposeAddingJobForm();
    }

    private void resetFailures(Msg m) {
        final ResetFailuresMsg msg = (ResetFailuresMsg) m;
        jobsModel.resetFailures(msg.getRows());
    }

    /**
     * removes one or more jobs as selected by user in job table
     * @param m
     */
    private void removeJobs(Msg m) throws IOException, InterruptedException,
        MasterFrozenException {
        final RemoveJobsMsg message = (RemoveJobsMsg) m;
        //get gruntIDs and set tasks back to READY here:
        ArrayList<Long> gruntIDs =
                jobsModel.getGruntIDsForSelectedRunningJobs(
                message.getRowsToRemove());

        abortGrunts(gruntIDs);

        //now finally remove jobs
        jobsModel.removeJobs(message.getRowsToRemove());
        updateProgressBar();
    }

    private void abortAll_StopQueue() throws IOException, InterruptedException,
        MasterFrozenException {
        queueRunning = false;
        masterForm.stopQueue();
        ArrayList<Long> gruntIDs = jobsModel.getGruntIDsForAllRunningTasks();
        abortGrunts(gruntIDs);
    }

    private void abortGrunts(ArrayList<Long> gruntIDs)
            throws InterruptedException, IOException, MasterFrozenException {
        //tell the grunts to abort the tasks

        brokersModel.abortTasksForGrunts(gruntIDs);
    }

    /**
     * update main progress bar (tasks) on masterForm
     */
    private void updateProgressBar() {
        MasterEQCaller.invokeUpdatePBar(masterForm,
                jobsModel.getProgressBarUpdate());
    }

    /**
     * update totalcore count on masterForm if it's different then last update
     * this saves us unecessary EQ calls in this particular case
     */
    private void updateCoresDisplay() {
        int total = brokersModel.getCores();
        if (total != lastTotalCores) {
            MasterEQCaller.invokeUpdateCores(masterForm, total);
        }
    }

    /**
     * listener picked up a new grunt on the accept port, so let's
     * create a broker for it and add it to gruntsModel
     * @param m
     */
    private void addGrunt(Msg m) {
        AddGruntMsg message = (AddGruntMsg) m; //cast

        try {
            //throws IOE
            brokersModel.addGrunt(message.getGruntSocket(), brokersThreadGroup);
        } catch (IOException ex) {
            //we hadn't added to gruntsModel when ex thrown, so just log...
            log.throwing(className, "addGrunt(Msg m)", ex);
        }
    }

    /**
     * original message sent by user from MasterForm
     * @param m
     */
    private void shutdown() {
        queueRunning = false;
        masterForm.stopQueue();
        if (grunt != null) {
            Task t = grunt.getCurrentTask();
            if (t != null) {
                jobsModel.setTaskStatus(
                        t.getJobID(), t.getTaskID(), TaskStatus.READY);
            }
            grunt.abortCurrentTask(TaskStatus.LOCAL_ABORT);
        }

        //notify announcer and listener threads.
        helpersThreadGroup.interrupt();

        //first, send shutdown messages to each broker
        brokersModel.shutdown();

        //signal compositer to shutdown
        compositer.shutdownNow();

        try {
            //first put latest info into cfg object
            cfg.setMasterCfg(Job.getJobIDCounter(), Task.getTaskIDCounter());

            //now write it to file
            Config.writeCfgToFile(lokiCfgDir, cfg);
            log.finest("writing cfg to file, as always.");

            //wait up to 1 second for listenerThread
            listenerThread.join(1000);

        } catch (IOException ex) {
            MasterEQCaller.showMessageDialog(masterForm, "filesystem error",
                    "failed to write to loki.cfg.\n" +
                    "Check filesystem permissions.",
                    JOptionPane.WARNING_MESSAGE);
            log.warning("failed to write cfg to file:" + ex.getMessage());
        } catch (InterruptedException ex) {
            //do nothing...we're exiting
        }
        IOHelper.deleteRunningLock(lokiCfgDir);

        //finally, set shutdown to true so I exit main loop
        shutdown = true;
        try {
        Thread.sleep(1000); //make sure local grunt has time to shutdown
        } catch (InterruptedException ex) {
            //squelch...
        }
        System.exit(0);
    }

    /**
     * refresh the UI w/ the latest info for grunt x; also update cores
     * in case that changed
     * @param m
     */
    private void updateGrunt(Msg m) {
        log.finest("updating grunt");
        UpdateGruntMsg message = (UpdateGruntMsg) m;
        brokersModel.updateGruntRow(message.getGruntID());
        if (message.getFirstMachineReply()) {
            updateCoresDisplay();
        }
    }

    /**
     * grunt x notified us that it is idle
     * @param m
     * @throws InterruptedException
     */
    private void idleGrunt(Msg m) throws InterruptedException {
        IdleGruntMsg iMsg = (IdleGruntMsg) m;

        //set grunt's status back to idle
        brokersModel.setGruntStatusToIdle(iMsg.getGruntID());

        //give out next task if possible
        try {
            assignIdleGrunts();
        } catch (LostGruntException ex) {   //kill the lost grunt!
            log.fine("lost grunt with id: " + ex.getGruntID());
            try {
                deliverMessage(new RemoveGruntMsg(ex.getGruntID())); //notify myself
            } catch (MasterFrozenException mfe) {
                //impossible...will never detect it's own freezeup
            }
        }
    }

    /**
     * remove grunt from gruntsModel; update cores on UI
     * @param m
     */
    private void removeGrunt(Msg m) {
        RemoveGruntMsg message = (RemoveGruntMsg) m;
        if (message.getGruntStatus() == GruntStatus.BUSY) {
            Task t = message.getGruntTask();
            t.setGruntID(-1);   //we're losing the grunt association w/ task
        }
        brokersModel.removeGrunt(message.getGruntID());
        updateCoresDisplay();
    }

    /**
     * view grunt details
     */
    private void viewGrunt(Msg m) {
        SelectedGruntMsg message = (SelectedGruntMsg) m;
        GruntDetails details = brokersModel.getGruntDetails(message.getRow());

        MasterEQCaller.invokeViewGruntDetails(masterForm, details);
    }

    private void quitGrunt(Msg m) throws MasterFrozenException {
        SelectedGruntMsg message = (SelectedGruntMsg) m;
        brokersModel.quitGrunt(message.getRow());
    }

    private void quitAllGrunts() throws MasterFrozenException {
        brokersModel.quitAllGrunts();
    }

    private void viewJob(Msg m) {
        SelectedGruntMsg message = (SelectedGruntMsg) m;
        try {
            Job job = jobsModel.getJobDetails(message.getRow());
            MasterEQCaller.invokeViewJobDetails(masterForm, job);
        } catch (IOException ex) {
            log.severe("IOEx while trying to clone Job: " + ex.getMessage());
        } catch (ClassNotFoundException cex) {
            log.severe("failed trying to clone Job: " + cex.getMessage());
        }
    }

    /**
     *
     *this method should be called in 3 cases:
     * 1. user has pressed the start button
     * 2. a new (idle) grunt has connected
     * 3. a grunt is done with a task and now idle
     * 
     * @throws InterruptedException if inter-thread messaging times out
     */
    private void assignIdleGrunts() throws LostGruntException {
        boolean tryNextTask = true;
        while (tryNextTask && queueRunning) {
            Task t = jobsModel.getNextTask();   //try and get next task
            if (t != null) { //t is null if we couldn't find one
                if (brokersModel.sendNextTask(t)) {

                    //update the task status in the job
                    jobsModel.setTaskStatus(t.getJobID(), t.getTaskID(),
                            TaskStatus.RUNNING);
                } else {
                    tryNextTask = false;
                }
            } else { //last iteration failed to find any remaining tasks
                tryNextTask = false;
                if (jobsModel.areAllJobsDone()) { //all status D?
                    MasterEQCaller.invokeStop(masterForm);
                }
            }
        }
    }

    /**
     * called as a result of receiving a 'startQueue' message from UI
     * @throws InterruptedException if MQDelivery failed
     */
    private void queueStarting() throws InterruptedException {
        queueRunning = true;
        //TODO -update UI stuff here ...already done?
        //give out next task if possible
        try {
            assignIdleGrunts();
        } catch (LostGruntException ex) {   //kill the lost grunt!
            log.fine("lost grunt with id: " + ex.getGruntID());
            try {
                deliverMessage(new RemoveGruntMsg(ex.getGruntID())); //notify myself
            } catch (MasterFrozenException mfe) {
                //impossible
            }
        }
    }

    private void compositeTiles(Task t) {
        File tmpDir = new File(lokiCfgDir, "tmp");
        File tileDir = new File(tmpDir, t.getJobID() + "-" +
                t.getFrame());
        String fileName = t.getOutputFilePrefix() +
                t.getOutputFileName();

        CompositeTiles cTiles = new CompositeTiles(
                tileDir,
                t.getTilesPerFrame(),
                t.getOutputDir(),
                fileName);

        compositer.submit(cTiles);
    }
    
    //call when we lose contact with a busy grunt
    // - assume task is lost and reset given task to 'ready'
    private void handleLostBusyGrunt(Msg m) {
      LostBusyGruntMsg lostBusyGruntMsg = (LostBusyGruntMsg) m;
     
      Task t = lostBusyGruntMsg.getGruntTask();
      jobsModel.setTaskStatus(t.getJobID(), t.getTaskID(),
                    TaskStatus.READY);
    }

    /**
     * called when a grunt is done/failed with a task
     * @param m
     * @throws InterruptedException if delivery timed out
     */
    private void handleReport(Msg m) {
        TaskReportMsg reportMsg = (TaskReportMsg) m;    //cast back
        TaskReport tReport;
        tReport = reportMsg.getReport();

        if (tReport.getTask().getStatus() == TaskStatus.DONE) {
            //update the job w/ info reported
            if (jobsModel.setReturnTask(tReport)) {
                compositeTiles(tReport.getTask());
            }
            updateProgressBar();

        } else if (tReport.getTask().getStatus() == TaskStatus.FAILED) {
            //task failed
            Task t = tReport.getTask();
            jobsModel.setReturnTask(tReport);
            String failureMsg;
            if(t.getStdout().length() > 0) {
                failureMsg = t.getStdout();
            } else if (t.getErrOut().length() > 0) {
                failureMsg = t.getErrOut();
            } else
                failureMsg = "unknown";

            String failed = "Task failed for grunt '" +
                    brokersModel.getGruntName(t.getGruntID()) +
                    "' with the message:\n\"" + failureMsg + "\"";



            MasterEQCaller.invokeTaskFailureNotification(masterForm, failureMsg);
            log.warning(failed);

        } else if (tReport.getTask().getStatus() == TaskStatus.LOCAL_ABORT) {
            Task t = tReport.getTask();
            jobsModel.setTaskStatus(t.getJobID(), t.getTaskID(),
                    TaskStatus.READY);
            String aborted = "User aborted task and quit Loki on grunt '" +
                    brokersModel.getGruntName(t.getGruntID()) + "'.";
            MasterEQCaller.showMessageDialog(masterForm, "task aborted",
                    aborted, JOptionPane.INFORMATION_MESSAGE);

        } else if (tReport.getTask().getStatus() == TaskStatus.MASTER_ABORT) {
            Task t = tReport.getTask();
            jobsModel.setTaskStatus(t.getJobID(), t.getTaskID(),
                    TaskStatus.READY);
        } else {
            log.severe("don't know how to handle tasktype: " +
                    tReport.getTask().getStatus());
        }
    }

    private class CompositeTiles implements Runnable {

        CompositeTiles(File tlDir, int tpf, String outDir, String fName) {
            tileDir = tlDir;
            tilesPerFrame = tpf;
            File outputDir = new File(outDir);
            outputFile = new File(outputDir, fName);
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            ImageHelper.compositeTiles(tileDir, tilesPerFrame, outputFile);
            log.fine("composited " + outputFile.getAbsolutePath() +
                    " in (ms): " + Long.toString(
                    System.currentTimeMillis() - start));
        }

        /*PRIVATE*/
        final private File tileDir;
        final private int tilesPerFrame;
        final private File outputFile;
    }
}
