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

import net.whn.loki.IO.MasterIOHelper;
import java.io.File;
import java.io.IOException;
import net.whn.loki.common.ProgressUpdate;
import java.io.Serializable;
import net.whn.loki.common.ICommon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import net.whn.loki.common.Task;
import net.whn.loki.common.TaskReport;

/**holds an array of job objects and serves as the model
 * for the jobs queue table
 *
 * DEVNOTE: uses a synchronized list so all access is thread-safe, except
 * any calls that fetch an iterator(for each loop), these calls must be
 * synchronized on the list
 * @author daniel
 */
public class JobsModel extends AbstractTableModel implements ICommon,
        Serializable {

    /**
     * called by AWT EQ
     * @param c
     * @return the name of given column
     */
    @Override
    public String getColumnName(int c) {
        return columnHeaders[c];
    }

    /**
     * called by AWT EQ
     * @return total number of columns
     */
    @Override
    public int getColumnCount() {
        return columnHeaders.length;
    }

    /**
     * called by AWT EQ
     * 
     * @return current row count of the model.
     */
    @Override
    public int getRowCount() {
        return jobsList.size();
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
        if (row < jobsList.size()) {
            return jobsList.get(row).getValue(column);
        } else {
            return "";
        }

        
    }

    /*BEGIN PACKAGE*/
    /**
     * initializes columnHeaders, from which is derived columnCount
     * NOTE: make sure to update Job.getValue() if you change headers.
     * ...it's probably best to keep everything we want here, then
     * just hide headers with the JTabel
     */
    public JobsModel(File lcd) {
        //TODO - perhaps there is a better way to handle headers?
        columnHeaders = populateColumnHeaders();
        jobsList = Collections.synchronizedList(new ArrayList<Job>());
        lokiCfgDir = lcd;
    }

    public boolean isPFileOrphaned(String md5) {
        boolean orphaned = true;
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (j.getPFileMD5().equals(md5)) {
                    orphaned = false;
                    break;
                }
            }
        }
        return orphaned;
    }

    public Job getJobDetails(int row) throws IOException,
            ClassNotFoundException {
        return MasterIOHelper.copyJob(jobsList.get(row));
    }

    ProgressUpdate getProgressBarUpdate() {
        int totalTasks = 0;
        int remaining = 0;
        synchronized (jobsList) {
            for (Job j : jobsList) {
                totalTasks += j.getTotalTasks();
                remaining += j.getRemainingTasks();
            }
        }
        return new ProgressUpdate(totalTasks, totalTasks - remaining);

    }

    /**
     * called by AWT EQ -> areSelectedJobsRunning() in MasterR
     * used if user requests exit
     * @return
     */
    boolean areJobsRunning() {
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (j.getStatus() == JobStatus.B || j.getStatus() == JobStatus.C) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean areSelectedJobsRunning(int[] selectedRows) {
        long[] selectedJobIDs = new long[selectedRows.length];
        synchronized (jobsList) {
            //populate our jobID array
            for (int i = 0; i < selectedRows.length; i++) {
                selectedJobIDs[i] = jobsList.get(selectedRows[i]).getJobID();
            }

            //now see if any of them are running
            for (int s = 0; s < selectedJobIDs.length; s++) {
                int jIndex = getJobIndex(selectedJobIDs[s]);
                if (jIndex != -1) {
                    Job j = jobsList.get(jIndex);
                    JobStatus iStatus = j.getStatus();
                    if (iStatus == JobStatus.B || iStatus == JobStatus.C) {
                        return true;
                    }
                }

            }
            return false;
        }
    }

    /**
     * called by master because we're going to remove the jobs which means we
     * first need to abort all running tasks in them.
     * NOTE! we're also setting taskstatus back to ready on the tasks!
     * @param selectedRows
     * @return
     */
    ArrayList<Long> getGruntIDsForSelectedRunningJobs(int[] selectedRows) {
        long[] selectedJobIDs = new long[selectedRows.length];
        ArrayList<Long> gruntIDlist = new ArrayList<Long>();

        synchronized (jobsList) {
            //populate our jobID array
            for (int i = 0; i < selectedRows.length; i++) {
                selectedJobIDs[i] = jobsList.get(selectedRows[i]).getJobID();
            }

            //now see if any of them are running
            for (int s = 0; s < selectedJobIDs.length; s++) {
                int jIndex = getJobIndex(selectedJobIDs[s]);
                if (jIndex != -1) {
                    Job j = jobsList.get(jIndex);
                    JobStatus iStatus = j.getStatus();
                    if (iStatus == JobStatus.B || iStatus == JobStatus.C) {
                        //found one, so add grunts to list
                        gruntIDlist = j.getGruntIDsForRunningTasks(gruntIDlist);
                    }
                }

            }
            return gruntIDlist;
        }
    }

    /**
     * called by master for abort all
     * NOTE! we're also setting taskstatus back to ready on the tasks!
     */
    ArrayList<Long> getGruntIDsForAllRunningTasks() {
        ArrayList<Long> gruntIDlist = new ArrayList<Long>();
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (j.getStatus() == JobStatus.B || j.getStatus() == JobStatus.C) {
                    gruntIDlist = j.getGruntIDsForRunningTasks(gruntIDlist);
                }
            }
        }
        fireTableDataChanged();
        return gruntIDlist;
    }

    long getJobID(int row) {
        return jobsList.get(row).getJobID();
    }

    /**
     * we're mutating the jobs, which AWT looks at
     * signals AWT
     * junit: yes
     * @param jID
     * @param task
     * @param status
     */
    void setTaskStatus(long jID, long taskID, TaskStatus status) {
        int jIndex = getJobIndex(jID);
        if (jIndex != -1) {
            jobsList.get(jIndex).setTaskStatus(taskID, status);
            fireTableRowsUpdated(jIndex, jIndex); //tell AWT.EventQueue
        }
    }

    void resetFailures(int[] rows) {
        for(int i = 0; i<rows.length; i++) {
            jobsList.get(rows[i]).resetFailures();
        }
        fireTableDataChanged();
    }

    /**
     * this updates the job with all the info just returned from the grunt
     * -status
     * -gruntID
     * -output
     * and also calls compositer if all tiles are done
     * @param r
     * @returns true if we need to composite tiles
     */
    boolean setReturnTask(TaskReport r) {
        TaskStatus status = null;
        Task t = r.getTask();
        int jIndex = getJobIndex(t.getJobID());
        if (jIndex != -1) {
            Job j = jobsList.get(jIndex);
            status = j.setReturnTask(r);
            fireTableRowsUpdated(jIndex, jIndex); //tell AWT.EventQueue

            if (t.getStatus() == TaskStatus.DONE && t.isTile() &&
                    AreFrameTilesDone(j, t.getFrame())) {
                return true;
            }
        }
        return false;
    }

    /**
     * adds given job to the end of jobsList, then tells AWT.EventQueue
     * called by addJob in master
     * junit: yes
     * @param j
     * @return true if job was successfully added, false if it wasn't
     */
    void addJob(Job j) {
        //TODO IndexOutOfBoundsException here
        jobsList.add(j);
        int newRow = jobsList.size() - 1;
        fireTableRowsInserted(newRow, newRow); //tell AWT.EventQueue
    }

    /**
     * removes the jobs selected in the table, excepting running jobs
     * junit: yes
     * @param selectedRows
     * @param masterForm
     */
    void removeJobs(int[] selectedRows) {
        //we need to remove by jobID because indexes will change as we remove
        long[] selectedJobIDs = new long[selectedRows.length];

        //for loops all over in here; must synchronize!
        synchronized (jobsList) {
            //pull the jobIDs for the selected rows
            for (int i = 0; i < selectedRows.length; i++) {
                selectedJobIDs[i] = jobsList.get(selectedRows[i]).getJobID();
            }

            int currentIndex = -1;

            for (int s = 0; s < selectedJobIDs.length; s++) { //for each jobID...
                for (int i = 0; i < jobsList.size(); i++) {
                    if (selectedJobIDs[s] == jobsList.get(i).getJobID()) {
                        currentIndex = i;
                        break;
                    }
                }   //we now have the currentIndex of the job in jobsList that we'll remove

                long jID = jobsList.get(currentIndex).getJobID();
                ImageHelper.deleteTileTmpFiles(lokiCfgDir, jID);
                jobsList.remove(currentIndex);
            }//end outer for
        }
        fireTableDataChanged(); //notify AWT
    }

    /**
     *called by MasterR->AWT EQ
     * @return next available Task, null if none
     */
    Task getNextTask() {
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (j.getStatus() == JobStatus.A || j.getStatus() == JobStatus.B) {
                    return j.getNextAvailableTask();
                }
            }
            return null;
        }
    }

    /**
     * checks that all jobs are done (i.e. all tasks are complete
     * no tasks are running.
     * @return true if all tasks are done or aborted (D), false if otherwise
     */
    boolean areAllJobsDone() {
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (j.getStatus() != JobStatus.D) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * test method
     * @param jobName
     * @return
     */
    long testGetJobID(String jobName) {
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (j.getJobName().equals(jobName)) {
                    return j.getJobID();
                }
            }
        }
        return -1;
    }

    /**
     * just for TEST
     */
    boolean testJobIDExists(long jID) {
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (j.getJobID() == jID) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * for test
     * @param jobName
     * @return
     */
    boolean testJobWithNameExists(String jobName) {
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (j.getJobName().equals(jobName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * called by master before adding a new job
     * @param nameToCheck
     * @return
     */
    boolean isJobNameUnique(String nameToCheck) {

        //iterating on jobsList, so must synchronize!
        synchronized (jobsList) {
            for (Job j : jobsList) {
                if (nameToCheck.equals(j.getJobName())) {
                    return false;   //oops, name already exists
                }
            }
        }
        return true;
    }

    /*BEGIN PRIVATE*/

    //logging
    private static final String className = "net.whn.loki.master.JobsModel";
    private static final Logger log = Logger.getLogger(className);

    final private List<Job> jobsList;
    final private String[] columnHeaders;
    final private File lokiCfgDir;

    private String[] populateColumnHeaders() {
        return new String[]{
                    "name",
                    "failed",
                    "remain",
                    "running",
                    "done",
                    "status"
                };
    }

    /**
     * pass in the jID to get it's current index
     * DEVNOTE: this may change if any jobs are removed!
     * @param jID
     * @return the Job's current index
     */
    private int getJobIndex(long jID) {
        synchronized (jobsList) {
            for (int i = 0; i < jobsList.size(); i++) {
                if (jobsList.get(i).getJobID() == jID) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean AreFrameTilesDone(Job j, int f) {
        Task[] tasks = j.getTasks();
        for (int t = 0; t < tasks.length; t++) {
            if (tasks[t].getFrame() == f) {
                if (tasks[t].getStatus() != TaskStatus.DONE) {
                    return false;
                }
            }
        }
        return true;
    }
}
