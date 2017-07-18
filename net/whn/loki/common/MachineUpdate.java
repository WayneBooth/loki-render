/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Sep 28, 2009
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
import java.text.DecimalFormat;

/**
 *
 * @author daniel
 */
public class MachineUpdate implements Serializable {

    public MachineUpdate(double lAvg, long tMemory, long fMemory, long fSwap) {
        loadAvg = lAvg;
        totalMemory = tMemory;
        freeMemory = fMemory;
        freeSwap = fSwap;
        usedMemory = generateMemUsage();
    }

    public String getMemUsageStr() {
        return usedMemory;
    }

    public long getFreeSwap() {
        return freeSwap;
    }

    public double getLoadAvg() {
        return loadAvg;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    /*PRIVATE*/
    private static final long bytesPerGB = 1073741824;
    private static DecimalFormat gb = new DecimalFormat("#0.00");
    private final double loadAvg;
    private final long totalMemory;
    private final long freeMemory;
    private final long freeSwap;
    private final String usedMemory;

    private String generateMemUsage() {
        double total = (double) totalMemory / (double) bytesPerGB;
        String tmpUsed;
        if (totalMemory == freeMemory) {
            tmpUsed = "?";
        } else {
            double used = (double) (totalMemory - freeMemory) / (double) bytesPerGB;
            tmpUsed = gb.format(used);
        }
        String tmpTotal = gb.format(total);
        return tmpUsed + "/" + tmpTotal;
    }
}
