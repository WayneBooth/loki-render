/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Oct 28, 2009
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
package net.whn.loki.CL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

/**
 *
 * @author daniel
 */
public class ProcessHelper {

    public ProcessHelper(String[] tCL) {
        taskCL = tCL;
    }

    public String[] runProcess() {
        String result[] = {"", ""};
        try {
            //process = Runtime.getRuntime().exec(taskCL);
            process = new ProcessBuilder(taskCL).start();

            Output stdout = new Output(process.getInputStream());
            FutureTask<String> stdTask = new FutureTask<String>(stdout);
            Thread stdoutThread = new Thread(stdTask);
            stdoutThread.start();

            Output errout = new Output(process.getErrorStream());
            FutureTask<String> errTask = new FutureTask<String>(errout);
            Thread erroutThread = new Thread(errTask);
            erroutThread.start();

            process.waitFor();

            result[0] = stdTask.get();
            result[1] = errTask.get();
            
            process.getOutputStream().close();

        } catch (IOException ex) {
            result[1] = "IOException: " + ex.getMessage();
            log.warning(ex.getMessage());

        } catch (InterruptedException ex) {
            result[1] = "InterruptedException: " + ex.getMessage();
            process.destroy();
            log.fine("finished interruptedException handling");
            
        } catch (ExecutionException ex) {
            result[1] = "ExecutionException: " + ex.getMessage();
            log.warning(ex.getMessage());
        }

        return result;
    }
    /*PRIVATE*/
    //logging
    private static final String className = "net.whn.loki.CL";
    private static final Logger log = Logger.getLogger(className);

    private final String[] taskCL;
    private Process process;

    private class Output implements Callable<String> {

        Output(InputStream is) {
            this.is = is;
        }

        @Override
        public String call() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        is));
                StringBuilder sb = new StringBuilder();

                String line = null;
                while ((line = in.readLine()) != null) {
                    sb.append(line + "\n");
                }
                in.close();
                
                return sb.toString();

            } catch (IOException ex) {
                log.warning("unable to grab output from process: " +
                        ex.getMessage());
            }
            return null;
        }

        /*PRIVATE*/
        private final InputStream is;
    }
}
