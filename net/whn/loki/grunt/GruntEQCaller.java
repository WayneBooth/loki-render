/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Sep 10, 2009
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

import net.whn.loki.common.*;
import java.awt.EventQueue;

/**
 *
 * @author daniel
 */
public class GruntEQCaller extends EQCaller {
    
    /**
     * updates the connection label on gruntForm
     * @param gForm
     * @param text
     */
    public static void invokeUpdateConnectionLbl(final GruntForm gForm,
            final String text) {

        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                gForm.setLblConnection(text);
            }
        });
    }

    /**
     * updates the status label on gruntForm
     * @param gForm
     * @param text
     */
    public static void invokeUpdateStatus(final GruntForm gForm,
            final GruntStatusText s) {

        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                gForm.setStatus(s);
            }
        });
    }

    /**
     * update the task progress bar
     * @param mForm
     * @param update
     */
    public static void invokeGruntUpdatePBar(final GruntForm gForm,
            final ProgressUpdate update) {

        uPB = new UpdateProgressBar(gForm, update);

        EventQueue.invokeLater(uPB);
    }

    /*BEGIN PRIVATE*/

    private static UpdateProgressBar uPB;

    private static class UpdateProgressBar implements Runnable {
        UpdateProgressBar(GruntForm gF, ProgressUpdate u) {
           gForm = gF;
           update = u;
        }

        @Override
        public void run()
        {
            gForm.updateProgressBar(update);
        }

        private final GruntForm gForm;
        private final ProgressUpdate update;
    }
}
