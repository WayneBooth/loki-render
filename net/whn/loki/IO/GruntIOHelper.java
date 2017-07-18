/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen, Gustavo Alejandro Moreno Martínez
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
package net.whn.loki.IO;

import net.whn.loki.grunt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.whn.loki.common.Config;
import net.whn.loki.IO.IOHelper;
import net.whn.loki.common.ICommon.FileCacheType;
import net.whn.loki.common.ProgressUpdate;
import net.whn.loki.common.ProjFile;
import net.whn.loki.common.Task;
import net.whn.loki.common.TileBorder;
import net.whn.loki.network.GruntStreamSocket;

/**
 *
 * @author daniel
 */
public class GruntIOHelper extends IOHelper {

    public static String generateHumanReadableFileSize(long size) {
        DecimalFormat fmt = new DecimalFormat("#0.0");
        final long bytesPerKB = 1024;
        final long bytesPerMB = 1048576;
        final long bytesPerGB = 1073741824;
        String txt;
        if (size < bytesPerKB) {
            txt = Long.toString(size) + " bytes";
        } else if (size < bytesPerMB) {
            double result = (double) size / (double) bytesPerKB;
            txt = fmt.format(result) + " KB";
        } else if (size < bytesPerGB) {
            double result = (double) size / (double) bytesPerMB;
            txt = fmt.format(result) + " MB";
        } else if (size >= bytesPerGB) {
            double result = (double) size / (double) bytesPerGB;
            txt = fmt.format(result) + " GB";
        } else {
            log.severe("freaky!");
            txt = null;
        }
        return "(" + txt + ")";
    }

    /**
     * receives file from broker via network, and adds to cache
     */
    public static boolean receiveFileFromBroker(
            FileCacheType fcType, GruntForm gruntForm,
            ConcurrentHashMap<String, ProjFile> fileCacheMap,
            GruntStreamSocket gSSock, File lokiCacheDir,
            long total, String expectedMD5, Config cfg)
            throws FileNotFoundException, NoSuchAlgorithmException,
            IOException {

        long remaining = total;
        byte[] buffer = new byte[BUFFER_SIZE];
        String md5;
        int amountRead;
        File tmpCacheFile = new File(lokiCacheDir, "tmp.file");
        OutputStream outFile = null;
        MessageDigest digest;

        try {
            InputStream sockIn = gSSock.getSockIn();
            outFile = new FileOutputStream(tmpCacheFile);
            digest = MessageDigest.getInstance("MD5");

            while (remaining > 0) {
                if (remaining > BUFFER_SIZE) {   //read full buffer
                    amountRead = sockIn.read(buffer);
                } else {
                    amountRead = sockIn.read(buffer, 0, (int) remaining);
                }


                digest.update(buffer, 0, amountRead);
                outFile.write(buffer, 0, amountRead);
                remaining -= amountRead;

                if (gruntForm != null) {
                    GruntEQCaller.invokeGruntUpdatePBar(gruntForm,
                            new ProgressUpdate(total, remaining));
                }
            }
            outFile.close();

            md5 = binToHex(digest.digest());
            if (!md5.equals(expectedMD5)) {
                log.warning("file's actual md5 does not match it's pFile md5:" +
                        md5 + "/" + expectedMD5);
            } else {
                try {
                    addTmpToCache(fcType, fileCacheMap, md5, lokiCacheDir,
                            tmpCacheFile, cfg);
                } catch (IOException ex) {
                    log.throwing(className, "receiveFileFromBroker", ex);
                    return false;
                }
            }
        } finally {
            try {
                if (outFile != null) {
                    outFile.close();
                }
            } catch (IOException ex) {
                //squelch this!! pointless...
            }
        }
        return true;
    }

    /**
     * makes the given blendCacheActive (if not already active) by deleting
     * the old directory, and unzipping from the file cache
     * @param lcDir
     * @param bcDirName
     */
    public static void handleActiveBlendCache(File lokiCacheDir,
            String newBCDir, String newMD5, Config cfg) {

        if (!newMD5.equals(cfg.getActiveBlendCacheMD5())) {
            //delete old one if it exists
            if (cfg.getActiveBlendCacheDir() != null) {
                File oldDir = new File(lokiCacheDir, cfg.getActiveBlendCacheDir());
                if (oldDir.isDirectory()) {
                    deleteDirectory(oldDir);
                }
            }

            File blendCacheDir = new File(lokiCacheDir, newBCDir);

            //unzip directory
            File blendCacheFile = new File(lokiCacheDir, newMD5);
            unzipDirectory(blendCacheFile, blendCacheDir);

            //add to cfg file
            cfg.setActiveBlendCacheDir(newBCDir);
            cfg.setActiveBlendCacheMD5(newMD5);
        }

    }

    public static void sendOutputFileToBroker(GruntForm gForm, File oFile, GruntStreamSocket gSock) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream inFile = null;
        long total = oFile.length();
        long remaining = total;
        if (!oFile.isFile()) {
            log.severe("can\'t find output file: " + oFile.getAbsolutePath());
            throw new IOException("can\'t find output file: " + oFile.getAbsolutePath());
        }
        try {
            inFile = new FileInputStream(oFile);
            int amountRead;
            synchronized (gSock) {
                //machine update sends on socket too
                while (true) {
                    amountRead = inFile.read(buffer);
                    if (amountRead == -1) {
                        break;
                    }
                    gSock.sendFileChunk(buffer, amountRead);
                    remaining -= amountRead;

                    if (gForm != null) {
                        GruntEQCaller.invokeGruntUpdatePBar(gForm,
                                new ProgressUpdate(total, remaining));
                    }
                }
                gSock.flushSockOut();
            }
        } catch (FileNotFoundException ex) {            //TODO what to do?
            log.severe("file to send not found: " + ex.getMessage());
        } finally {
            if (inFile != null) {
                inFile.close();
            }
        }
    }

    public static File blender_setupTileScript(File tmpDirFile, Task t)
            throws IOException {
        File script = new File(tmpDirFile, "setupTileBorder.py");

        if (script.isFile()) {
            if (!script.delete()) {
                log.warning("unable to delete renderTile.py");
            }
        }
        TileBorder b = t.getTileBorder();

        PrintWriter fout = new PrintWriter(new FileWriter(script));
        fout.println("import bpy");
        fout.println("import os");
        fout.println("left = " + Float.toString(b.getLeft()));
        fout.println("right = " + Float.toString(b.getRight()));
        fout.println("bottom = " + Float.toString(b.getBottom()));
        fout.println("top = " + Float.toString(b.getTop()));
        fout.println("scene  = bpy.context.scene");
        fout.println("render = scene.render");
        fout.println("render.use_border = True");
        fout.println("render.use_crop_to_border = True");
        fout.println("render.image_settings.file_format = 'PNG'");
        fout.println("render.image_settings.color_mode = 'RGBA'");
        fout.println("render.use_file_extension = True");
        fout.println("render.border_max_x = right");
        fout.println("render.border_min_x = left");
        fout.println("render.border_max_y = top");
        fout.println("render.border_min_y = bottom");
        // from the location of the fetched file (blendcache) to ../tmp/
        fout.println("render.filepath = os.path.dirname(bpy.data.filepath) + os.sep + \'..\' + os.sep + \'tmp\' + os.sep");
        fout.println("scene.frame_start = " + t.getFrame());
        fout.println("scene.frame_end = " + t.getFrame());
        // if it's not used this line, the scene uses their original parameters...
        fout.println("bpy.ops.render.render(animation=True)");
        fout.flush();
        fout.close();
        return script;
    }

    /*PRIVATE*/
    //logging
    private static final String className =
            "net.whn.loki.grunt.GruntIOHelper";
    private static final Logger log = Logger.getLogger(className);
}
