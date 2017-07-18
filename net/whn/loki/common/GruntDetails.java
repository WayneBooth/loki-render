/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Oct 13, 2009
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

/**
 *
 * @author daniel
 */
public class GruntDetails {

    public GruntDetails(
            String h,
            String oName,
            String osVer,
            String osArch,
            int cores,
            long tMemory,
            long tSwap,
            String uName,
            String uHome,
            String cwDir) {
        
        hostname = h;
        osName = oName;
        osVersion = osVer;
        osArchitecture = osArch;
        processors = cores;
        totalMemory = tMemory;
        totalSwap = tSwap;
        userName = uName;
        userHome = uHome;
        currentWorkingDir = cwDir;
    }

    public String getCurrentWorkingDir() {
        return currentWorkingDir;
    }

    public String getHostname() {
        return hostname;
    }

    public String getOsArchitecture() {
        return osArchitecture;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public int getProcessors() {
        return processors;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public long getTotalSwap() {
        return totalSwap;
    }

    public String getUserHome() {
        return userHome;
    }

    public String getUserName() {
        return userName;
    }
    
    /*PRIVATE*/
    private final String hostname;
    private final String osName;
    private final String osVersion;
    private final String osArchitecture;
    private final int processors;
    private final long totalMemory;
    private final long totalSwap;
    private final String userName;
    private final String userHome;
    private final String currentWorkingDir;

}
