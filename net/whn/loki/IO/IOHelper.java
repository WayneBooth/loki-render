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
package net.whn.loki.IO;

import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.whn.loki.common.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import net.whn.loki.common.ICommon.FileCacheType;

/**
 *
 * @author daniel
 */
public class IOHelper {

    /**
     * creates the _lokiconf dir in the user's home dir if it doesn't already
     * exist, also checks read/write permissions to make sure we're ok to
     * proceed w/ all other filesystem activities.
     * same stuff for child dir 'fileCache' as well
     * @return File of lokiBaseDir, null if failed somewhere
     */
    public static File setupLokiCfgDir() {
        File lokiBaseDir;
        File fileCacheDir;
        File tmpDir;
        String lokiConfDir = ".loki";

        //first let's retrieve the user's home directory
        String userHomeDir = System.getProperty("user.home");

        if (userHomeDir != null) {
            lokiBaseDir = new File(userHomeDir, lokiConfDir);
            log.finest("lokiBaseDir: " + lokiBaseDir.getAbsolutePath());
        } else {
            log.severe("couldn't retrieve user.home path!");
            return null;
        }

        //now check if base dir already exists; if not, create it
        if (!lokiBaseDir.isDirectory()) {
            //doesn't exist; create it
            if (!lokiBaseDir.mkdir()) {
                log.severe("couldn't create directory:" + lokiConfDir);
                return null;
            }
        }

        //now check if it's writable for files and directories
        if (!isDirWritable(lokiBaseDir)) {
            log.severe("couldn't write to directory: " + lokiConfDir);
            return null;
        }

        fileCacheDir = new File(lokiBaseDir, "fileCache");

        //now let's check if fileCache dir already exists; if not create it
        if (!fileCacheDir.isDirectory()) {
            //doesn't exist; create it
            if (!fileCacheDir.mkdir()) {
                log.severe("couldn't create directory:" + fileCacheDir.toString());
                return null;
            }
        }

        if (!isDirWritable(fileCacheDir)) {
            log.severe("couldn't write to directory:" + fileCacheDir.toString());
            return null;
        }

        tmpDir = new File(lokiBaseDir, "tmp");

        //if tmp dir exists, delete it and any contents it may have
        if(tmpDir.isDirectory()) {
            deleteDirectory(tmpDir);
        }
        
        //create empty tmpDir
        if (!tmpDir.mkdir()) {
            log.severe("couldn't create directory:" + tmpDir.toString());
            return null;
        }

        if (!isDirWritable(tmpDir)) {
            log.severe("couldn't write to directory:" + tmpDir.toString());
            return null;
        }

        //everything checked out, return
        return lokiBaseDir;
    }

    public static void deleteRunningLock(File lokiCfgDir) {
        File runningFile = new File(lokiCfgDir, ".runningLock");
        if (runningFile.exists()) {
            runningFile.delete();
        }
    }

    /**
     * generates MD5 for given file.
     * @param oFile
     * @return md5 as hex string, or null if failed
     * @throws IOException
     */
    public static String generateMD5(File oFile) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        InputStream inFile = null;
        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance("MD5");
            inFile = new FileInputStream(oFile);

            int amountRead;
            while (true) {
                amountRead = inFile.read(buffer);
                if (amountRead == -1) {
                    break;
                }
                digest.update(buffer, 0, amountRead);
            }
            return binToHex(digest.digest());

        } catch (NoSuchAlgorithmException ex) { //TODO what to do?
            log.severe("md5 algorithm not available!");
        } catch (FileNotFoundException ex) {    //TODO what to do?
            log.severe("file not found: " + ex.getMessage());
        } finally {
            if (inFile != null) {
                inFile.close();
            }
        }
        return null;
    }

    public static long getFileCacheSize(
            ConcurrentHashMap<String, ProjFile> fCMap) {
        long total = 0;
        ProjFile currentpFile;
        Iterator it = fCMap.entrySet().iterator();
        Map.Entry pair;
        while (it.hasNext()) {
            pair = (Map.Entry) it.next();
            currentpFile = (ProjFile) pair.getValue();
            total += currentpFile.getSize();
        }
        return total;
    }

    /**
     *
     * @param lokiCfgDir
     * @return false if .runningLock file didn't exist, false otherwise
     * @throws IOException
     */
    public static boolean setupRunningLock(File lokiCfgDir) throws IOException {
        File runningLock = new File(lokiCfgDir, ".runningLock");

        if (runningLock.createNewFile()) {
            runningLock.deleteOnExit();
            return false;
        } else {
            return true;   //oops; loki is already running on this system
        }
    }

    /**
     * zips up a given directory. skips subdirectories!
     * @param dir
     * @param outputZipFile
     * @return
     */
    public static boolean zipDirectory(File dir, File outputZipFile) {
        if (outputZipFile.exists()) {
            outputZipFile.delete();
            log.warning(outputZipFile.toString() + " already exists: overwriting");
        }
        IOHelper.start = System.currentTimeMillis();
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputZipFile));
            out.setLevel(1);
            addDir(dir, out);
            out.close();
            log.info("zipped blendcache in (ms): " + (System.currentTimeMillis() - IOHelper.start));
            return true;
        } catch (Exception ex) {
            log.warning("failed to zip blendcache directory: " + ex.toString());
        }
        return false;
    }

    /**
     * creates the output directory, and unzips files contents into it.
     * @param zipFile contains contents to be unzipped
     * @param outputDir the directory to create and unzip contents into
     * @return true if succeeds, false otherwise
     */
    public static boolean unzipDirectory(File zipFile, File outputDir) {
        try {
            if (!zipFile.isFile()) {
                return false;
            }
            //create directory
            if (!outputDir.mkdir()) {
                return false;
            }

            start = System.currentTimeMillis();
            ZipInputStream zin =
                    new ZipInputStream(new FileInputStream(zipFile));
            FileOutputStream fos = null;

            ZipEntry zipEntry;
            File fileEntry;
            while ((zipEntry = zin.getNextEntry()) != null) {
                int read;
                byte[] buffer = new byte[4096];

                fileEntry = new File(outputDir, zipEntry.getName());
                fos = new FileOutputStream(fileEntry);
                while ((read = zin.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                }
                fos.close();
            }

            zin.close();

            return true;
        } catch (Exception ex) {
            log.warning("failed to unpack zip file: " + zipFile.toString());
        }
        return false;
    }

    public static boolean deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (dir.delete());
    }

    public static String generateBlendCacheDirName(String blendFileName) {
        int dotIndex = blendFileName.lastIndexOf('.');

        if (dotIndex != -1) {
            blendFileName = blendFileName.substring(0, dotIndex);
        }

        return "blendcache_" + blendFileName;
    }

    /*PROTECTED*/
    protected static final int BUFFER_SIZE = 8192;
    protected static long start;

    /**
     * converts bytes to a hex string
     * @param bytes
     * @return
     */
    protected static String binToHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

    /**
     * adds a previously copied tmp file into the cache (if it's unique)
     * @param fileCacheMap
     * @param md5
     * @param lokiCacheDir
     * @param tmpCacheFile
     */
    protected static void addTmpToCache(FileCacheType fcType,
            ConcurrentHashMap<String, ProjFile> fileCacheMap,
            String md5, File lokiCacheDir, File tmpCacheFile, Config cfg)
            throws IOException {

        File md5File = null;
        ProjFile pFile = null;

        //check if file already exists in cache
        if (!fileCacheMap.containsKey(md5)) {
            log.finest("unique md5; adding to cache");
            //new file, so rename and add to map:

            //rename file
            if(fcType == FileCacheType.BLEND) {
                md5File = new File(lokiCacheDir, md5 + ".blend");
            } else {
                md5File = new File(lokiCacheDir, md5);
            }
            
            if (md5File.exists()) {
                log.warning("fileCache key set is out of sync w/ files:\n" +
                        "File: " + md5File.getAbsolutePath() +
                        " already exists; overwriting.");
                md5File.delete();
            }
            int limit = 0;
            while (!tmpCacheFile.renameTo(md5File)) {
                if (limit > 9) {
                    log.severe("failed to rename CacheFile: " +
                            tmpCacheFile.getAbsolutePath() + " to " +
                            md5File.getAbsolutePath());
                    throw new IOException("failed to rename CacheFile!");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    break;
                }
                limit++;
            }

            //create new ProjFile object
            pFile = new ProjFile(fcType, md5File, md5);

            //insert it into the fileCache
            fileCacheMap.put(md5, pFile);

            if (pFile.getSize() > cfg.getCacheSizeLimitBytes()) {
                cfg.setCacheSizeLimitBytes((pFile.getSize() * 4));
                log.info("increasing cache limit to: " + pFile.getSize() * 4);
            }
            manageCacheLimit(fileCacheMap, cfg);
        } else {
            //file is already in cache, so delete tmp file
            tmpCacheFile.delete();
            //that's all we need to do. md5 string will be returned and
            //placed in job
            log.finest("md5 not unique; we already have the file!");
        }
    }

    /*PRIVATE*/
    //logging
    private static final String className =
            "net.whn.loki.common.LokiFileHelper";
    private static final Logger log = Logger.getLogger(className);

    /**
     * should be called after a new file has been added to the cache.
     * if we're over the limit, should iteratively remove oldest used files
     * until we meet the limit constraint.
     */
    private static void manageCacheLimit(
            ConcurrentHashMap<String, ProjFile> fileCacheMap, Config cfg) {

        //we need to delete files using a "longest ago used" algorithm
        //until we fall under the limit
        if (!fileCacheMap.isEmpty()) {
            while (cfg.getCacheSize() > cfg.getCacheSizeLimitBytes()) {
                ProjFile oldestPFile = null;
                Iterator it = fileCacheMap.entrySet().iterator();
                long lowestTime = System.currentTimeMillis() + 1000000;
                Map.Entry pair;

                while (it.hasNext()) {
                    pair = (Map.Entry) it.next();
                    ProjFile currentPFile = (ProjFile) pair.getValue();
                    if (currentPFile.getTimeLastUsed() < lowestTime) {
                        oldestPFile = currentPFile;
                        lowestTime = oldestPFile.getTimeLastUsed();
                    }
                }
                //we now have our delete candidate, so delete it.

                if (!oldestPFile.isInUse() &&
                        cfg.getJobsModel().isPFileOrphaned(
                        oldestPFile.getMD5())) {
                    if (!oldestPFile.getProjFile().delete()) {
                        log.severe("failed to delete cache file");
                    }
                    fileCacheMap.remove(oldestPFile.getMD5());
                    log.finer("deleting file: " + oldestPFile.getMD5());
                } else {
                    log.fine("manageCacheLimit wanted to delete file in use!");
                    break;
                }
            }
        }
    }

    private static boolean isDirWritable(File bDir) {
        boolean ok = true;

        String tDir = "lokiDir";

        try {
            if (!bDir.isDirectory()) {
                ok = false;
            }
            //can I write a file to current working dir?
            File tempFile = new File(bDir, "loki.tmp");

            if (!tempFile.createNewFile()) {
                ok = false; //couldn't create file
            } else {  //file was created
                if (!tempFile.delete()) {
                    ok = false; //couldn't delete the file
                }
            }

            if (ok) {
                //can I write a dir to current working dir?
                File tempDir = new File(bDir, tDir);
                if (!tempDir.mkdir()) {
                    ok = false; //couldn't create dir
                } else {  //dir was created
                    if (!tempDir.delete()) {
                        ok = false; //couldn't delete dir
                    }
                }
            }
        } catch (IOException ex) {
            log.severe("couldnt write to directory!" +
                    ex.getMessage());
            ok = false;
        }

        return ok;
    }

    private static void addDir(File dir, ZipOutputStream out) throws IOException {
        File[] files = dir.listFiles();
        byte[] tmpBuf = new byte[4096];
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                //addDir(files[i], out);
                continue;
            }
            FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
            //out.putNextEntry(new ZipEntry(files[i].getAbsolutePath()));
            out.putNextEntry(new ZipEntry(files[i].getName()));
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
    }
    
    
}
