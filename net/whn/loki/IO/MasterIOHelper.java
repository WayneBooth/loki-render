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
package net.whn.loki.IO;

import net.whn.loki.master.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.whn.loki.common.Config;
import net.whn.loki.common.ICommon.FileCacheType;
import net.whn.loki.common.ProjFile;
import net.whn.loki.common.Task;
import net.whn.loki.network.BrokerStreamSocket;

/**
 *
 * @author daniel
 */
public class MasterIOHelper extends IOHelper {

    public static Job copyJob(Job j) throws IOException, ClassNotFoundException {
        start = System.currentTimeMillis();
        Job job;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(j);
        byte[] buf = baos.toByteArray();
        oos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        ObjectInputStream ois = new ObjectInputStream(bais);
        job = (Job) ois.readObject();
        ois.close();
        log.fine("cloned job (via serialization) in (ms): " + (System.currentTimeMillis() - start));
        return job;
    }

    /**
     * master calls this when it got a new job; needs to add file to cache
     * @param fileCache
     * @param newJob
     * @param lokiCacheDir
     * @return md5 string of cached file, null otherwise
     */
    public static String newProjFileToCache(
            ConcurrentHashMap<String, ProjFile> fileCacheMap,
            String oFile, File lokiCacheDir, Config cfg) {

        byte[] buffer = new byte[BUFFER_SIZE];
        File origFile = new File(oFile);
        File tmpCacheFile = new File(lokiCacheDir, "tmp.file");
        String md5 = null;
        InputStream inFile = null;
        MessageDigest digest = null;
        OutputStream outFile = null;

        //copy to tmp.file, generate the md5 checksum
        start = System.currentTimeMillis();
        try {
            digest = MessageDigest.getInstance("MD5");
            inFile = new FileInputStream(origFile);
            outFile = new FileOutputStream(tmpCacheFile);

            int amountRead;
            while (true) {
                amountRead = inFile.read(buffer);
                if (amountRead == -1) {
                    break;
                }
                digest.update(buffer, 0, amountRead);
                outFile.write(buffer, 0, amountRead);
            }

            md5 = binToHex(digest.digest());
        } catch (Exception ex) {
            log.severe("failed to cache project file! " + ex.getMessage());
            return null;
        } finally {
            try {
                if (inFile != null) {
                    inFile.close();
                }
                if (outFile != null) {
                    outFile.close();
                }
            } catch (IOException ex) {
                //squelch this!! pointless...
            }
        }
        try {
            addTmpToCache(FileCacheType.BLEND, fileCacheMap, md5,
                    lokiCacheDir, tmpCacheFile, cfg);
        } catch (IOException ex) {
            log.throwing(className, "newFileToCache", ex);
            return null;
        }

        log.finest("finished newFileToCache method (ms): " +
                (System.currentTimeMillis() - start));

        return md5; //whether the file was new or not, we want this for the job
    }

    /*PRIVATE*/
    //logging
    private static final String className =
            "net.whn.loki.master.MasterIOHelper";
    private static final Logger log = Logger.getLogger(className);

    public static void sendProjectFileToGrunt(
            ProjFile pFile, BrokerStreamSocket bSSock) throws IOException {
        pFile.setInUse(true);
        byte[] buffer = new byte[BUFFER_SIZE];
        String md5 = null;
        InputStream inFile = null;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
            inFile = new FileInputStream(pFile.getProjFile());
            int amountRead;
            while (true) {
                amountRead = inFile.read(buffer);
                if (amountRead == -1) {
                    break;
                }
                digest.update(buffer, 0, amountRead);
                bSSock.sendFileChunk(buffer, amountRead);
            }
            bSSock.flushSockOut();
            //make sure the md5 is as it should be
            //make sure the md5 is as it should be
            md5 = binToHex(digest.digest());
            if (!md5.equals(pFile.getMD5())) {
                log.warning("file\'s md5 does not match it\'s pFile md5:" + md5 + "/" + pFile.getMD5());
            }
        } catch (NoSuchAlgorithmException ex) {
            //TODO what to do?
            log.severe("md5 algorithm not available!");
        } catch (FileNotFoundException ex) {
            //TODO what to do?
            //TODO what to do?
            log.severe("file to send not found: " + pFile.getProjFile().getAbsolutePath());
        } finally {
            if (inFile != null) {
                inFile.close();
            }
        }
        pFile.setInUse(false);
    }

    /**
     * receives output file from grunt via network, and adds to job's output dir.
     * if a tile, then puts in '<job>-<frame>' folder in tmp
     */
    public static boolean receiveOutputFileFromGrunt(BrokerStreamSocket bSock,
            Task t, File lokiBaseDir) {

        File outputDir;
        File outputFile;

        if (t.isTile()) {
            File tmpDir = new File(lokiBaseDir, "tmp");
            String dirStr = Long.toString(t.getJobID()) + "-" +
                    Integer.toString(t.getFrame());
            outputDir = new File(tmpDir, dirStr);

            if (!outputDir.isDirectory()) {
                //doesn't exist; create it
                if (!outputDir.mkdir()) {
                    log.severe("couldn't create directory:" + outputDir);
                }
            }
            outputFile = new File(outputDir, Integer.toString(t.getTile()) +
                    ".png");

        } else {
            outputDir = new File(t.getOutputDir());
            outputFile = new File(outputDir, t.getOutputFilePrefix() +
                    t.getOutputFileName());
        }

        long remaining = t.getOutputFileSize();
        byte[] buffer = new byte[BUFFER_SIZE];
        String md5;
        int amountRead;
        OutputStream outFile = null;
        MessageDigest digest;

        try {
            InputStream sockIn = bSock.getSockIn();
            outFile = new FileOutputStream(outputFile);
            digest = MessageDigest.getInstance("MD5");

            while (remaining > 0) {
                if (remaining > BUFFER_SIZE) {   //read up to a full buffer
                    amountRead = sockIn.read(buffer);
                } else {
                    amountRead = sockIn.read(buffer, 0, (int) remaining);
                }

                digest.update(buffer, 0, amountRead);
                outFile.write(buffer, 0, amountRead);
                remaining -= amountRead;
            }
        } catch (Exception ex) {
            log.severe("failed during receive/save of output file! " +
                    ex.getMessage());
            return false;
        } finally {
            try {
                if (outFile != null) {
                    outFile.close();
                }
            } catch (IOException ex) {
                //squelch this!! pointless...
            }

        }
        md5 = binToHex(digest.digest());
        if (!md5.equals(t.getOutputfileMD5())) {
            log.warning("file's md5 does not match grunt's reported md5:" +
                    md5 + "/" + t.getOutputfileMD5());
        }
        return true;
    }

    /**
     * checks if blendcache directory exists; if so, packs into zip file
     * and adds to loki file cache
     * @param lokigCfgBasedir, blendFile
     * @return md5 of resulting zip file in cache, null otherwise
     */
    public static String addBlendCacheToLokiCache(
            ConcurrentHashMap<String, ProjFile> fileCacheMap,
            File lokiCacheDir, String blendFileStr, Config cfg) {

        String md5 = null;

        File tmpZipFile = new File(lokiCacheDir, File.separator +
                "blendcache.zip");

        File blendFile = new File(blendFileStr);
        File parentDir = blendFile.getParentFile();
        String fileName = blendFile.getName();
        
        File cacheDir = new File(parentDir, 
                generateBlendCacheDirName(fileName));

        if (!cacheDir.isDirectory()) {
            return null;
        }

        String[] files = cacheDir.list();
        if (files.length < 1) {
            return null;
        }

        if (!zipDirectory(cacheDir, tmpZipFile)) {
            return null;
        }

        try {
            md5 = generateMD5(tmpZipFile);
            addTmpToCache(FileCacheType.BLEND_CACHE, fileCacheMap, md5,
                    lokiCacheDir, tmpZipFile, cfg);
            return md5;
        } catch (IOException ex) {
            log.warning("failed to generate md5 for " +
                    tmpZipFile.toString());
            return null;
        }
    }
}
