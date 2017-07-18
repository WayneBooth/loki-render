/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
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

import net.whn.loki.common.TileBorder;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Logger;
import net.whn.loki.IO.IOHelper;
import net.whn.loki.common.Task;
import net.whn.loki.common.ICommon;
import net.whn.loki.common.TaskReport;

/**
 *represents a job in the queue
 * 
 * @author daniel
 */
public class Job implements ICommon, Serializable {

    public Job(JobFormInput jI, String md5, String blendCacheMd5,
            long pFileSize) {
        jobID = jobIDCounter++;
        //TODO - temporary. change into enum later w/ enumComboBox
        if (jI.getTaskType().equalsIgnoreCase(JobType.BLENDER.toString())) {
            type = JobType.BLENDER;
        }
        name = jI.getName();
        origProjFile = new File(jI.getProjFileName());   //replace this with a projectFile object
        outputDirFile = new File(jI.getOutputDirName());
        pFileMD5 = md5;
        blendCacheMD5 = blendCacheMd5;
        filePrefix = jI.getFilePrefix();
        autoFileTransfer = jI.getAutoFileTransfer();

        running = 0;
        done = 0;
        failed = 0;
        status = JobStatus.A;

        String outputDirStr = jI.getOutputDirName();
        String blendCacheDirName = null;

        tileRender = jI.isTileEnabled();
        if(jI.getAutoFileTransfer()) {
            blendCacheDirName = IOHelper.generateBlendCacheDirName(
                pFileMD5);
        }
        

        if (tileRender) {
            tileMultiplier = jI.getTileMultiplier();
            int tilesPerFrame = tileMultiplier * tileMultiplier;
            firstFrame = jI.getFirstFrame();
            lastFrame = jI.getLastFrame();
            totalFrames = (lastFrame - firstFrame) + 1;
            tasks = new Task[totalFrames * tilesPerFrame];

            ready = tasks.length;

            int tCount = 0;
            TileBorder[] tileBorders = calcTileBorders(tileMultiplier);
            for (int f = 0; f < totalFrames; f++) {
                for (int t = 0; t < tilesPerFrame; t++) {
                    tasks[tCount] = new Task(type, firstFrame + f, jobID,
                            md5, blendCacheMd5, blendCacheDirName,
                            pFileSize, outputDirStr, filePrefix,
                            origProjFile, autoFileTransfer, true, t,
                            tilesPerFrame, tileBorders[t]);

                    tCount++;
                }
            }
        } else {    //not tile rendering
            tileMultiplier = -1;

            firstFrame = jI.getFirstFrame();
            lastFrame = jI.getLastFrame();
            totalFrames = (lastFrame - firstFrame) + 1;
            tasks = new Task[totalFrames];

            ready = tasks.length;    //initially, all tasks are READY

            for (int t = 0; t < tasks.length; t++) {
                tasks[t] = new Task(type, firstFrame + t, jobID, md5,
                        blendCacheMd5, blendCacheDirName, pFileSize,
                        outputDirStr, filePrefix, origProjFile,
                        autoFileTransfer, false, -1, -1, null);
            }
        }
    }

    /**
     * this is for the job details form
     * @return
     */
    public String getTileStr() {
        if (tileRender) {
            String m = Integer.toString(tileMultiplier);
            return m + " * " + m + " = " + Integer.toString(
                    tileMultiplier * tileMultiplier);
        } else {
            return "disabled";
        }
    }

    /**
     * this method grabs value specified by column
     * called by jobsModel for the GUI table when we have an update fired to AWT
     * @param column
     * @return
     */
    public Object getValue(int column) {
        if (column == 0) {
            return name;
        } else if (column == 1) {
            return Integer.toString(failed);
        } else if (column == 2) {
            return Integer.toString(ready);
        } else if (column == 3) {
            return Integer.toString(running);
        } else if (column == 4) {
            return Integer.toString(done);
        } else if (column == 5) {
            if (status == JobStatus.A) {
                return "ready";
            } else if (status == JobStatus.B || status == JobStatus.C) {
                return "running";
            } else if (status == JobStatus.D) {
                return "done";
            } else {
                log.severe("An unknown jobStatus value was encountered: " +
                        status);
                return null;
            }
        } else {
            log.severe("A value outside of table scope was requested: " +
                    Integer.toString(column));
            return null;
        }
    }

    /**
     * just for test
     * @return true if counters match, false if there is a discrepancy
     */
    public boolean test_tallyCounters() {
        int rdy = 0;
        int rnng = 0;
        int dn = 0;
        int fld = 0;

        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.READY) {
                rdy++;
            } else if (t.getStatus() == TaskStatus.RUNNING) {
                rnng++;
            } else if (t.getStatus() == TaskStatus.DONE) {
                dn++;
            } else if (t.getStatus() == TaskStatus.FAILED) {
                fld++;
            } else {
                log.severe("status outside of scope: " + t.getStatus());
            }
        }
        if (rdy != ready) {
            return false;
        }
        if (rnng != running) {
            return false;
        }
        if (dn != done) {
            return false;
        }
        if (fld != failed) {
            return false;
        }

        return true;
    }

    /**
     * just for test!
     * @param task
     * @return
     */
    Task test_returnTask(int task) {
        return tasks[getTaskIndex(task)];
    }
    
    public boolean isAutoFileTransfer() {
        return autoFileTransfer;
    }

    /**
     * called by Master once on shutdown to get current idcounter
     * @return the current job id counter
     */
    public static long getJobIDCounter() {
        return jobIDCounter;
    }

    /**
     * called by Master once on startup if we're loading from a cfg file
     * @param jCounter
     */
    public static void setJobIDCounter(long jCounter) {
        jobIDCounter = jCounter;
    }

    public File getOrigProjFile() {
        return origProjFile;
    }

    public String getPFileMD5() {
        return pFileMD5;
    }

    /*BEGIN PACKAGE*/
    String getJobName() {
        return name;
    }

    long getJobID() {
        return jobID;
    }

    JobStatus getStatus() {
        return status;
    }

    void resetFailures() {
        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.FAILED) {
                setTaskStatus(t.getTaskID(), TaskStatus.READY);
                t.setInitialOutput(null, null, null);
            }
        }
    }

    /**
     * called by getNextTask() in JobsModel; ancestor call is in MasterR
     * assignIdleGrunts();
     * finds the next task that is ready
     * @return
     */
    Task getNextAvailableTask() {
        for (Task t : tasks) {
            TaskStatus s = t.getStatus();
            if (s == TaskStatus.READY) {
                return t;
            }
        }
        return null;
    }

    /**
     * update task w/ returned values
     * @param r
     * @return
     */
    TaskStatus setReturnTask(TaskReport r) {
        Task t = r.getTask();
        int taskIndex = getTaskIndex(t.getTaskID());
        //must set status first, before dropping in new object!
        setTaskStatus(t.getTaskID(), t.getStatus());  //to update counters, not status
        tasks[taskIndex] = t;  //put the whole object in, rather than copying vars
        return tasks[taskIndex].getStatus();
    }

    /**
     * get gruntIDs for all grunts running tasks from this job
     * @param gIDList
     * @return
     */
    ArrayList<Long> getGruntIDsForRunningTasks(ArrayList<Long> gIDList) {
        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.RUNNING) {
                if (t.getGruntID() == -1) {
                    log.severe("running task has no associated grunt!");
                }
                gIDList.add(t.getGruntID());
                //setTaskStatus(t.getTaskID(), TaskStatus.READY);
            }
        }
        return gIDList;
    }

    /**
     * sets the specified task's status
     * @param task - this is the actual task value, not the index where
     * it's stored!
     * @param tStatus
     */
    void setTaskStatus(long taskID, TaskStatus tStatus)
            throws IndexOutOfBoundsException {

        int taskIndex = getTaskIndex(taskID);

        tasks[taskIndex].setStatus(tStatus);

        //and recalculate job status
        calcStatus();
    }

    JobType getJobType() {
        return type;
    }

    int getTotalTasks() {
        return tasks.length;
    }

    int getRemainingTasks() {
        return tasks.length - done;
    }

    File getOutputDirFile() {
        return outputDirFile;
    }

    String getFilePrefix() {
        return filePrefix;
    }

    int getFirstTask() {
        return firstFrame;
    }

    int getLastTask() {
        return lastFrame;
    }

    int getDone() {
        return done;
    }

    int getFailed() {
        return failed;
    }

    int getReady() {
        return ready;
    }

    int getRunning() {
        return running;
    }

    Task[] getTasks() {
        return tasks;
    }

    /*BEGIN PRIVATE*/
    //logging
    private static final String className = "net.whn.loki.master.Job";
    private static final Logger log = Logger.getLogger(className);
    private static long jobIDCounter = 0;
    final private long jobID;
    private JobType type;
    private final int firstFrame, lastFrame, totalFrames;
    private final String name;
    private final File origProjFile;
    private final File outputDirFile;   //broker saves output here, master verifies
    private final String filePrefix;    //broker uses this
    private final String pFileMD5;
    private final String blendCacheMD5;
    private final boolean autoFileTransfer;
    private int ready, running, done, failed;
    private final boolean tileRender;
    private final int tileMultiplier;
    private JobStatus status;
    private Task[] tasks;

    /**
     *
     * @param multiplier
     * @return an array of tileBorders representing all tiles for given
     * multiplier
     */
    private TileBorder[] calcTileBorders(int multiplier) {
        TileBorder[] tileBorders = new TileBorder[multiplier * multiplier];
        float chunk = (float) 1 / (float) multiplier;

        int t = 0;
        float left, bottom, right, top;
        for (int y = 1; y < multiplier + 1; y++) {
            for (int x = 1; x < multiplier + 1; x++) {

                //x coordinates
                if (x == 1) {  //left border tile
                    left = 0.0F;
                    right = chunk;
                } else if (x == multiplier) { //right border tile
                    left = chunk * (multiplier - 1);
                    right = 1.0F;
                } else {    //tile not on left or right border...
                    left = chunk * (float) (x - 1);
                    right = chunk * (float) x;
                }

                //y coordinates
                if (y == 1) {  //bottom border tile
                    bottom = 0.0F;
                    top = chunk;
                } else if (y == multiplier) { //top border tile
                    bottom = chunk * (multiplier - 1);
                    top = 1.0F;
                } else {    //tile not on bottom or top border...
                    bottom = chunk * (float) (y - 1);
                    top = chunk * (float) y;
                }

                tileBorders[t] = new TileBorder(left, right, bottom, top);
                t++;
            }
        }
        return tileBorders;
    }

    /**
     * finds the index in tasks for given task value
     * @param task
     * @return
     */
    private int getTaskIndex(long tID) {
        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i].getTaskID() == tID) {
                return i;
            }
        }
        return -1;
    }

    /**
     * determines job status based
     * on values of counters: ABORTED, DONE, READY, requested, RUNNING and FAILED.
     */
    private void calcStatus() {
        ready = 0;
        running = 0;
        done = 0;
        failed = 0;

        for (Task t : tasks) {
            if (t.getStatus() == TaskStatus.READY) {
                ready++;
            } else if (t.getStatus() == TaskStatus.RUNNING) {
                running++;
            } else if (t.getStatus() == TaskStatus.DONE) {
                done++;
            } else if (t.getStatus() == TaskStatus.FAILED) {
                failed++;
            } else {
                log.severe("unknown status type!");
            }
        }

        //logic to determine job status
        /**
         *A - remaining, stopped
         *B - remaining, tasks RUNNING
         *C - all assigned, RUNNING
         *D - all tasks finished or ABORTED
         */
        if (ready > 0 && running < 1) {
            status = JobStatus.A;
        } else if (ready > 0 && running > 0) {
            status = JobStatus.B;
        } else if (ready < 1 && running > 0) {
            status = JobStatus.C;
        } else if (ready < 1 && running < 1) {
            status = JobStatus.D;
        } else {
            log.severe("unknown job state!");
        }
    }
}
