/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Sep 20, 2009
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


/**
 *An intermediate container to hold input from addjobform until we create an
 * job object
 * @author daniel
 */
public class JobFormInput {
    JobFormInput(String tType, String n, String pFile, String oFile, String p,
            int fTask, int lTask, int aFailures, boolean tEnabled, int m,
            boolean aFileTransfer) {

        taskType = tType;
        name = n;
        projFileName = pFile;   //replace this with a projectFile object
        outputDirName = oFile;
        filePrefix = p;
        firstTask = fTask;
        lastTask = lTask;
        totalTasks = (lastTask - firstTask) + 1;
        allowedFailures = aFailures;
        tileEnabled = tEnabled;
        tileMultiplier = m;
        autoFileTransfer = aFileTransfer;

    }
    
    public boolean getAutoFileTransfer() {
        return autoFileTransfer;
    }

    public int getAllowedFailures() {
        return allowedFailures;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public int getFirstFrame() {
        return firstTask;
    }

    public int getLastFrame() {
        return lastTask;
    }

    public String getName() {
        return name;
    }

    public String getProjFileName() {
        return projFileName;
    }

    public String getOutputDirName() {
        return outputDirName;
    }

    public String getTaskType() {
        return taskType;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public boolean isTileEnabled() {
        return tileEnabled;
    }

    public int getTileMultiplier() {
        return tileMultiplier;
    }

    /*BEGIN PRIVATE*/

    private String taskType;
    private String name;
    private String projFileName;
    private String outputDirName;
    private String filePrefix;
    private final int firstTask;
    private final int lastTask;
    private final int totalTasks;
    private final int allowedFailures;
    private final boolean tileEnabled;
    private final int tileMultiplier;
    private final boolean autoFileTransfer;
}