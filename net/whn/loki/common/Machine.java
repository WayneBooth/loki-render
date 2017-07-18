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

package net.whn.loki.common;

import java.io.Serializable;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author daniel
 */
public class Machine implements Serializable {

    public Machine() {
        Runtime runtime = Runtime.getRuntime();
        osMxbean = (com.sun.management.OperatingSystemMXBean)
                    ManagementFactory.getOperatingSystemMXBean();

        String tmpHostname = "unknown";
        //first try to get local hostname
        try {
            InetAddress localMachine = InetAddress.getLocalHost();
            tmpHostname = localMachine.getHostName();
        } catch (UnknownHostException ex) {
            //don't do anything .. we'll just keep "unknown".
        }
        hostname = tmpHostname;

        //get OS info
        osName = System.getProperty("os.name");
        osVersion = System.getProperty("os.version");
        osArchitecture = System.getProperty("os.arch");

        //get user info
        userName = System.getProperty("user.name");
        userHome = System.getProperty("user.home");
        currentWorkingDir = System.getProperty("user.dir");

        //get number of processors
        processors = runtime.availableProcessors();

        //get total memory, total swap from mxbean
        totalMemory = osMxbean.getTotalPhysicalMemorySize();
        totalSwap = osMxbean.getTotalSwapSpaceSize();
    }

    /**
     * dummy constructor to pass to broker until it gets a Machine object
     * with real values from the grunt
     * @param value
     */
    public Machine(String value) {

        hostname = value;
        osMxbean = null;

        //get OS info
        osName = "";
        osVersion = "";
        osArchitecture = "";

        //get user info
        userName = "";
        userHome = "";
        currentWorkingDir = "";

        //get number of processors
        processors = -1;

        //get total memory, total swap from mxbean
        totalMemory = -1;
        totalSwap = -1;
    }

    public MachineUpdate getMachineUpdate() {
        return new MachineUpdate(
                osMxbean.getSystemLoadAverage(),
                totalMemory,
                osMxbean.getFreePhysicalMemorySize(),
                osMxbean.getFreeSwapSpaceSize()
                );
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

    /*BEGIN PRIVATE*/

    //these never change
    private final String hostname;
    private final String osName;
    private final String osVersion;
    private final String osArchitecture;
    private final String userName;
    private final String userHome;
    private final String currentWorkingDir;
    private final int processors;
    private final long totalMemory;
    private final long totalSwap;
    private final transient OperatingSystemMXBean osMxbean;
}
