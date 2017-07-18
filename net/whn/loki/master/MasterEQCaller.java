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
package net.whn.loki.master;

import net.whn.loki.common.*;
import java.awt.EventQueue;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author daniel
 */
public class MasterEQCaller extends EQCaller {

    /**
     * tell UI we're stopping the queue
     * @param mForm
     */
    public static void invokeStop(final MasterForm mForm) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                mForm.stopQueue();
            }
        });
    }

    /**
     * update the totalCore count on the UI
     * @param mForm
     * @param cores
     */
    public static void invokeUpdateCores(final MasterForm mForm,
            final int cores) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                mForm.updateCores(cores);
            }
        });
    }

    public static void invokeViewGruntDetails(final MasterForm mForm,
            final GruntDetails details) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                mForm.viewGruntDetails(details);
            }
        });
    }

    public static void invokeViewJobDetails(final MasterForm mForm,
            final Job job) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                mForm.viewJobDetails(job);
            }
        });
    }

    /**
     * update the task progress bar
     * @param mForm
     * @param update
     */
    public static void invokeUpdatePBar(final MasterForm mForm,
            final ProgressUpdate update) {

        uPB = new UpdateProgressBar(mForm, update);

        EventQueue.invokeLater(uPB);
    }

    public static void invokeTaskFailureNotification(final MasterForm mForm,
            final String failureStr) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Object[] options = {"OK", "Stop the Queue"};
                String prelude =
                        "One or more tasks in the job queue have failed.\n" +
                        "Below is output from the first failed task.\n" +
                        "You can view job\n details for more information.\n" +
                        "Viewing the log in <userdir>/.loki may also help.\n\n\"";
                if (!failureMsgOpen) {
                    failureMsgOpen = true;
                    int result = JOptionPane.showOptionDialog(
                            mForm,
                            prelude + failureStr + "\"",
                            "task failed",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            options,
                            options[0]);

                    if (result == 1) {
                        mForm.stopQueue();
                    }
                    failureMsgOpen = false;
                }
            }
        });
    }

    /*BEGIN PRIVATE*/
    private static UpdateProgressBar uPB;
    private static boolean failureMsgOpen = false;

    private static class UpdateProgressBar implements Runnable {

        UpdateProgressBar(MasterForm mF, ProgressUpdate u) {
            mForm = mF;
            update = u;
        }

        @Override
        public void run() {
            mForm.updateProgressBar(update);
        }
        private final MasterForm mForm;
        private final ProgressUpdate update;
    }
}
