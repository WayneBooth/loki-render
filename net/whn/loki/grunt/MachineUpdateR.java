/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Sep 27, 2009
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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.whn.loki.common.ICommon.HdrType;
import net.whn.loki.common.Machine;
import net.whn.loki.common.MachineUpdate;
import net.whn.loki.network.GruntStreamSocket;
import net.whn.loki.network.Hdr;

/**
 *
 * @author daniel
 */
public class MachineUpdateR implements Runnable {

    MachineUpdateR(GruntStreamSocket g) throws IOException {
        log.finest("constructor called");

        gSSock = g;
        gSSock.sendHdr(new Hdr(HdrType.MACHINE_INFO, machine));
        //log.setLevel(Level.ALL);
    }

    @Override
    public void run() {
        MachineUpdate update = machine.getMachineUpdate();
        String currentMemUsage = update.getMemUsageStr();

        if (!currentMemUsage.equals(lastMemUsage) && !gSSock.isClosed()) {
            log.finest("sending a new mem update: " + currentMemUsage + "|" +
                    lastMemUsage);
            Hdr iHdr = new Hdr(HdrType.MACHINE_UPDATE, update);

            //throws IOE if socket has problem
            try {
                gSSock.sendHdr(iHdr);
                lastMemUsage = currentMemUsage;
            } catch (IOException ex) {
                log.info("lost connection: " + ex.getMessage());
                gSSock.tryClose();
            }
        }
    }
    //logging
    private static final String className = "net.whn.loki.grunt.MachineUpdateR";
    private static final Logger log = Logger.getLogger(className);

    private static final Machine machine = new Machine();

    private final GruntStreamSocket gSSock;
    private static String lastMemUsage = null;
}
