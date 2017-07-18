/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Sep 2, 2009
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

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author daniel
 */
class BlendFilter extends FileFilter {
    //accept all directories and all blend files
    public boolean accept(File f) {
        if(f.isDirectory()) {
            return true;
        }

        String filename = f.getName();
        return filename.endsWith(".blend");
    }

    public String getDescription() {
        return "*.blend";
    }
}
