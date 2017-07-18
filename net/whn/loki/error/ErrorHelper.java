/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Oct 30, 2009
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
package net.whn.loki.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import net.whn.loki.common.LokiForm;

/**
 *
 * @author daniel
 */
public class ErrorHelper {

    /**
     * null lokiform is ok here; can handle it.
     * @param form
     * @param log
     * @param text
     * @param t
     */
    public static void outputToLogMsgAndKill(LokiForm form, boolean gruntcl, 
            Logger log, String text, Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String msg = text + t.toString() + "\n" + sw.toString();
        log.severe(msg);
        
        if(!gruntcl) {
            JOptionPane.showMessageDialog(form,
                    msg, "Fatal Error",
                    JOptionPane.ERROR_MESSAGE);
        } else
            System.out.println(msg);
        System.exit(-1);
    }
}
