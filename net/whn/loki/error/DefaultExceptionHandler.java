/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Oct 27, 2009
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
import net.whn.loki.common.*;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author daniel
 */
public class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {

    public DefaultExceptionHandler(LokiForm lForm) {
        form = lForm;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
       JOptionPane.showMessageDialog(form, "Loki has encountered an error:\n" +
               e.toString() + "\nPlease view the log for details.\n" +
               "Wisdom would dictate restarting Loki at this point.",
               "Loki Render Error", JOptionPane.ERROR_MESSAGE);
       
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
       
        log.warning("uncaught throwable: " + "\n" + sw.toString());
    }

    //logging
    private static final String className =
            "net.whn.loki.common.DefaultExceptionHandler";
    private static final Logger log = Logger.getLogger(className);

    private final LokiForm form;

}
