/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Aug 14, 2009
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

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author daniel
 */
public class EQCaller implements ICommon {

    public EQCaller() {
    }

    /**
     * display message dialog
     * @param cmd
     */
    public static void showMessageDialog(final JFrame frame, final String title,
            final String msg, final int messageType) {
        EventQueue.invokeLater(new
            Runnable()
            {
                public void run()
                { JOptionPane.showMessageDialog(frame, msg, title,
                          messageType); }
            });
    }
}
