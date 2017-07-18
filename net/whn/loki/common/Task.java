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
package net.whn.loki.common;

import net.whn.loki.IO.IOHelper;
import net.whn.loki.CL.CLHelper;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 *represents a task of one or more tasks within a job
 * @author daniel
 */
public class Task implements ICommon, Serializable, Cloneable {

    public Task(JobType t, int f, long jID, String m, String bcm, 
            String bcd, long s, String oDir, String oPrefix, 
            File oPFile, boolean aFileTransfer, boolean tRender,
            int tl, int tff,
            TileBorder tBorder) {
        taskID = taskIDCounter++;

        type = t;
        frame = f;
        jobID = jID;
        projectFileMD5 = m;
        blendCacheMD5 = bcm;
        blendCacheDirName = bcd;
        projectFileSize = s;
        outputDir = oDir;
        outputFilePrefix = oPrefix;
        origProjFile = oPFile;
        autoFileTransfer = aFileTransfer;
        tileRender = tRender;
        tile = tl;
        tilesForFrame = tff;
        tileBorder = tBorder;

        status = TaskStatus.READY;
        gruntID = -1;
        gruntName = "";

        //given values after task has run
        taskCL = null;
        stdout = null;
        errout = null;
        taskTime = null;
        tmpOutputFileName = null;
        outputFileExt = null;
        outputFileMD5 = null;
        outputFileSize = -1;

    }

    @Override
    public Task clone() throws CloneNotSupportedException {
        return (Task) super.clone();
    }

    /**
     * called by master once on shutdown to get current idcounter
     * @return the current job id counter
     */
    public static long getTaskIDCounter() {
        return taskIDCounter;
    }

    /**
     * called by master once on startup if we're loading from a cfg file
     * @param jCounter
     */
    public static void setTaskIDCounter(long jCounter) {
        taskIDCounter = jCounter;
    }

    public JobType getType() {
        return type;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus newStatus) {
        status = newStatus;
    }

    public int getFrame() {
        return frame;
    }

    public int getTile() {
        return tile;
    }

    public int getTilesPerFrame() {
        return tilesForFrame;
    }

    public boolean isTile() {
        return tileRender;
    }

    public TileBorder getTileBorder() {
        return tileBorder;
    }

    public long getTaskID() {
        return taskID;
    }

    public long getJobID() {
        return jobID;
    }

    public long getGruntID() {
        return gruntID;
    }

    public void setGruntID(long gID) {
        gruntID = gID;
    }

    public void setGruntName(String gName) {
        gruntName = gName;
    }

    public String getGruntName() {
        return gruntName;
    }

    public String getProjectFileMD5() {
        return projectFileMD5;
    }

    public String getBlendCacheMD5() {
        return blendCacheMD5;
    }

    public String getBlendCacheDirName() {
        return blendCacheDirName;
    }

    public long getProjectFileSize() {
        return projectFileSize;
    }

    public String[] getTaskCL() {
        return taskCL;
    }

    public String getStdout() {
        return stdout;
    }

    public String getErrOut() {
        return errout;
    }

    public String getTaskTime() {
        return taskTime;
    }

    public String getOutputFileName() {
        return tmpOutputFileName;
    }

    public String getOutputFileExt() {
        return outputFileExt;
    }

    public String getOutputfileMD5() {
        return outputFileMD5;
    }

    public long getOutputFileSize() {
        return outputFileSize;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getOutputFilePrefix() {
        return outputFilePrefix;
    }
    
    public File getOrigProjFile() {
        return origProjFile;
    }
    
    public boolean isAutoFileTranfer() {
        return autoFileTransfer;
    }
    
    public void setErrout(String eout) {
        errout = eout;
    }

    public void setInitialOutput(String[] tCL, String sOut, String eOut) {
        taskCL = tCL;
        stdout = sOut;
        errout = eOut;
    }

    public void determineStatus() {
        status = CLHelper.determineTaskReturn(type, stdout, errout);
    }

    public void populateDoneInfo()
            throws IOException {

        taskTime = CLHelper.extractBlenderRenderTime(stdout);
        File outputFile = new File(
                CLHelper.blender_getRenderedFileName(stdout));
        tmpOutputFileName = outputFile.getName();
        outputFileExt = tmpOutputFileName.substring(
                tmpOutputFileName.lastIndexOf('.') + 1, tmpOutputFileName.length());
        if(autoFileTransfer) {
            outputFileMD5 = IOHelper.generateMD5(outputFile);
        }
        outputFileSize = outputFile.length();
    }

    /*BEGIN PRIVATE*/
    private static long taskIDCounter = 0;
    final private long taskID;

    private final JobType type;
    private final int frame;
    private final long jobID;
    private final String projectFileMD5;
    private final String blendCacheMD5;
    private final String blendCacheDirName;
    private final long projectFileSize;
    private final String outputDir;
    private final String outputFilePrefix;
    private final File origProjFile;
    private final boolean autoFileTransfer;
    private final boolean tileRender;
    private final int tile;
    private final int tilesForFrame;
    private final TileBorder tileBorder;
    private volatile TaskStatus status;
    private long gruntID;
    private String gruntName;
    
    //output
    private String[] taskCL;
    private String stdout;
    private String errout;
    private String taskTime;
    private String tmpOutputFileName;
    private String outputFileExt;
    private String outputFileMD5;
    private long outputFileSize;
}
