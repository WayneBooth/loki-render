/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 22, 2009
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
package net.whn.loki.network;

import net.whn.loki.common.*;
import java.io.Serializable;

/**
 *
 * @author daniel
 */
public class Hdr implements Serializable, ICommon {

    public Hdr(HdrType t) {
        type = t;
        fcType = null;
        report = null;
        machine = null;
        machineUpdate = null;
        task = null;
        md5 = null;
        size = -1;
    }

    //constructor for MACHINE_INFO
    public Hdr(HdrType t, Machine m) {
        type = t;
        fcType = null;
        report = null;
        machine = m;
        machineUpdate = null;
        task = null;
        md5 = null;
        size = -1;
    }

    //constructor for MACHINE_UPDATE
    public Hdr(HdrType t, MachineUpdate mu) {
        type = t;
        fcType = null;
        report = null;
        machine = null;
        machineUpdate = mu;
        task = null;
        md5 = null;
        size = -1;
    }

    //constructor for TASK_SEND
    public Hdr(HdrType t, Task tk) {
        type = t;
        fcType = null;
        report = null;
        machine = null;
        machineUpdate = null;
        task = tk;
        md5 = null;
        size = -1;
    }

    //constructor for FILE_REQUEST
    public Hdr(HdrType t, String m) {
        type = t;
        fcType = null;
        report = null;
        machine = null;
        machineUpdate = null;
        task = null;
        md5 = m;
        size = -1;
    }

    //constructor for FILE_REPLY
    public Hdr(HdrType t, FileCacheType fct, String m, long s) {
        type = t;
        fcType = fct;
        report = null;
        machine = null;
        machineUpdate = null;
        task = null;
        md5 = m;
        size = s;
    }

    //constructor for TASK_REPORT
    public Hdr(HdrType t, TaskReport r) {
        type = t;
        fcType = null;
        report = r;
        machine = null;
        machineUpdate = null;
        task = null;
        md5 = null;
        size = -1;
    }

    public HdrType getType() {
        return type;
    }

    public FileCacheType getFileCacheType() {
        return fcType;
    }

    public Machine getMachine() {
        return machine;
    }

    public MachineUpdate getMachineUpdate() {
        return machineUpdate;
    }

    public Task getTask() {
        return task;
    }

    public TaskReport getTaskReport() {
        return report;
    }

    public String getMD5() {
        return md5;
    }

    public long getSize() {
        return size;
    }

    /*BEGIN PRIVATE*/
    private final HdrType type;
    private final TaskReport report;
    private final Machine machine;
    private final MachineUpdate machineUpdate;
    private final Task task;
    private final String md5;
    private final FileCacheType fcType;
    private final long size;
}
