/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Oct 12, 2009
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

import net.whn.loki.common.ICommon;

/**
 *
 * @author daniel
 */
public class GruntStatusText implements ICommon {

    GruntStatusText(GruntTxtStatus sText) {
        statusText = sText;
        fileSize = 0;
    }

    GruntStatusText(GruntTxtStatus sText, long fSize) {
        this(sText);
        fileSize = fSize;
    }

    GruntTxtStatus getStatus() {
        return statusText;
    }

    long getFileSize() {
        return fileSize;
    }

    private final GruntTxtStatus statusText;
    private long fileSize;
}
