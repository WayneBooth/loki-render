/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Sep 16, 2009
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

import java.io.File;
import java.io.Serializable;
import net.whn.loki.common.ICommon.FileCacheType;

/**
 *
 * @author daniel
 */
public class ProjFile implements Serializable {

    public ProjFile(FileCacheType fcType, File file, String m) {
       fileCacheType = fcType;
       projFile = file;
       md5 = m;
       timeLastUsed = System.currentTimeMillis();
       inUse = false;
    }

    public File getProjFile() {
        return projFile;
    }

    public long getSize() {
        return projFile.length();
    }

    public String getMD5() {
        return md5;
    }

    public FileCacheType getFileCacheType() {
        return fileCacheType;
    }

    public long getTimeLastUsed() {
        return timeLastUsed;
    }

    public void updateTime() {
        this.timeLastUsed = System.currentTimeMillis();
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    /*BEGIN PRIVATE*/

    private final ICommon.FileCacheType fileCacheType;
    private final File projFile;
    private final String md5;
    private long timeLastUsed;
    private volatile boolean inUse;
}
