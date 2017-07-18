/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 29, 2009
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
public class ProgressUpdate {

    //constructor for grunt's values
    public ProgressUpdate(long total, long remaining) {
        double ratio = ((double) (total - remaining) / (double) total);
        max = 100;
        done = (int) (ratio * max);
    }

    //constructor for straight values
    public ProgressUpdate(int m, int d) {
        max = m;
        done = d;
    }

    public int getDone() {
        return done;
    }

    public int getMax() {
        return max;
    }

    


    /*BEGIN PRIVATE*/

    private final int max;
    private final int done;

}
