/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen
 *Created on Sep 3, 2009
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

import net.whn.loki.IO.IOHelper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import net.whn.loki.master.JobsModel;

/**
 *
 * @author daniel
 */
public class Config implements Serializable, ICommon {

    /**
     * called by main if no previous cfg exists
     */
    public Config(File lcDir) {
        log.setLevel(Level.FINE);

        //common
        lokiVer = "0.7.2";
        role = LokiRole.ASK;
        lokiCfgDir = lcDir;
        fileCacheMap = new ConcurrentHashMap<String, ProjFile>();
        autoFileHandling = true;
        cacheSizeLimit = 100 * bytesPerMB;

        //master
        jobIDCounter = 0;
        taskIDCounter = 0;
        jobsModel = new JobsModel(lokiCfgDir);
        projFile = new File("");
        outDirFile = new File("");
        filePrefix = "";

        //multicast
        gruntMulticastPort = 53913;
        String multicastString = "232.26.11.4";
        multicastAddress = null;
        multicastTTL = 1;
        announceInterval = 1000;
        try {
            multicastAddress = InetAddress.getByName(multicastString);
        } catch (UnknownHostException ex) {
            //TODO
            log.severe(ex.getMessage());
        }

        //TCP
        connectPort = 53914;
        
        //grunt
        blenderBin = "blender";
        blendCacheMD5 = null;
        autoDiscoverMaster = true;
        masterAddress = null;
        masterIPValid = false;
        
    }
    
    public void setAutoFileHandling(boolean auto) {
        autoFileHandling = auto;
    }
    
    public boolean getAutoFileHandling () {
        return autoFileHandling;
    }
    
    public boolean isMasterIPValid() {
        return masterIPValid;
    }
    
    public void setMasterIp(InetAddress mAddress) {
        masterAddress = mAddress;
        masterIPValid = true;
    }
    
    public InetAddress getMasterIp() {
        return masterAddress;
    }

    public boolean getAutoDiscoverMaster() {
        return autoDiscoverMaster;
    }
    
    public void setAutoDiscoverMaster(boolean auto) {
        autoDiscoverMaster = auto;
    }
    
    public String getLokiVer() {
        return lokiVer;
    }

    public LokiRole getRole() {
        return role;
    }

    public void setRole(LokiRole r) {
        role = r;
    }

    public InetAddress getMulticastAddress() {
        return multicastAddress;
    }

    public int getGruntMulticastPort() {
        return gruntMulticastPort;
    }

    public int getMulticastTTL() {
        return multicastTTL;
    }
    
    public int getConnectPort() {
        return connectPort;
    }

    public int getAnnounceInterval() {
        return announceInterval;
    }

    public File getLokiCfgDir() {
        return lokiCfgDir;
    }

    public long getJobIDCounter() {
        return jobIDCounter;
    }

    public long getTaskIDCounter() {
        return taskIDCounter;
    }

    public JobsModel getJobsModel() {
        return jobsModel;
    }

    public ConcurrentHashMap<String, ProjFile> getFileCacheMap() {
        return fileCacheMap;
    }

    public void setBlenderBin(String bBin) {
        blenderBin = bBin;
    }

    public String getBlenderBin() {
        return blenderBin;
    }

    public void setActiveBlendCacheMD5(String bcMD5) {
        blendCacheMD5 = bcMD5;
    }

    public String getActiveBlendCacheMD5() {
        return blendCacheMD5;
    }

    public void setActiveBlendCacheDir(String bcDir) {
        blendCacheDir = bcDir;
    }

    public String getActiveBlendCacheDir() {
        return blendCacheDir;
    }

    public File getOutDirFile() {
        return outDirFile;
    }

    public void setOutDirFile(File outDirFile) {
        this.outDirFile = outDirFile;
    }

    public File getProjFile() {
        return projFile;
    }

    public void setProjFile(File projFile) {
        this.projFile = projFile;
    }

    /**
     *
     * @return a formatted string in format #0.0
     */
    public String getCacheSizeStr() {
        double result = (double) getCacheSize() / (double) bytesPerMB;
        return MBformatter.format(result) + " MB";
    }

    public long getCacheSize() {
        updateCacheSize();
        return cacheSize;
    }

    public void setCacheSizeLimitMB(int limit) {
        cacheSizeLimit = (long) (limit * bytesPerMB);
    }

    public void setCacheSizeLimitBytes(long limit) {
        cacheSizeLimit = limit;
    }

    public int getCacheSizeLimitMB() {
        int cslMB = (int) (cacheSizeLimit / bytesPerMB);
        return cslMB;
    }

    public long getCacheSizeLimitBytes() {
        return cacheSizeLimit;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    /**
     * writes unique mastercfg data to loki.cfg. as a rule, any common data
     * that is not a reference to object is written by master
     * @param jIDCounter
     */
    public void setMasterCfg(long jIDCounter, long tIDCounter) {
        jobIDCounter = jIDCounter;
        taskIDCounter = tIDCounter;

        log.finest("master setting jobIDCounter");
    }

    /**
     * if a cfg file exists, then it reads the cfg object from it
     * @return cfg object if file is present, new cfg object otherwise
     */
    public synchronized static Config readCfgFile(File lokiCfgDir) {
        lokiCfg = new File(lokiCfgDir, "loki.cfg");
        Config c = null;
        if (lokiCfg.canRead()) {
            try {
                FileInputStream file = new FileInputStream(lokiCfg);
                BufferedInputStream buffer = new BufferedInputStream(file);
                InflaterInputStream iin = new InflaterInputStream(buffer);
                ObjectInputStream input = new ObjectInputStream(iin);

                time = System.currentTimeMillis();
                c = (Config) input.readObject();
                input.close();
                time = System.currentTimeMillis() - time;
                log.finest("config file read in (ms): " + Long.toString(time));
                return c;
            } catch (Exception ex) {
                log.warning("failed to read loki.cfg: " + ex.getMessage());
            }
        }
        return new Config(lokiCfgDir);
    }

    /**
     * write the current cfg object to the cfg file. this and the read method
     * are synchronized so we guarantee that two threads are never reading/
     * writing the config file at the same time
     * @param cfg
     * @throws FileNotFoundException
     * @throws IOException
     */
    public synchronized static void writeCfgToFile(File lcDir, Config cfg)
            throws FileNotFoundException, IOException {
        lokiCfg = new File(lcDir, "loki.cfg");
        FileOutputStream file = new FileOutputStream(lokiCfg);
        BufferedOutputStream buffer = new BufferedOutputStream(file);
        DeflaterOutputStream dout = new DeflaterOutputStream(buffer,
                fastDeflater);
        ObjectOutput objOut = new ObjectOutputStream(dout);

        time = System.currentTimeMillis();
        objOut.writeObject(cfg);
        objOut.flush();
        objOut.close();
        time = System.currentTimeMillis() - time;
        log.fine("config file written in (ms): " + Long.toString(time));
    }

    /*PRIVATE*/
    //logging
    private static final String className = "net.whn.loki.common.Configuration";
    private static final Logger log = Logger.getLogger(className);
    //common
    private final String lokiVer;
    private static File lokiCfgDir;
    private static File lokiCfg;    //static because it's methods are static
    private volatile LokiRole role; //master and grunt both set this
    private volatile long cacheSize; //in bytes
    private volatile long cacheSizeLimit; //in bytes
    private ConcurrentHashMap<String, ProjFile> fileCacheMap;
    private boolean autoFileHandling;
    //multicast
    private InetAddress multicastAddress;
    private int gruntMulticastPort;
    private int multicastTTL;
    private int announceInterval;
    //TCP
    private int connectPort;
    //master
    private long jobIDCounter;
    private long taskIDCounter;
    private JobsModel jobsModel;
    //addjobFrom memory
    private File projFile;
    private File outDirFile;
    private String filePrefix;
    //grunt
    private String blenderBin;
    private String blendCacheMD5;
    private String blendCacheDir;
    private boolean autoDiscoverMaster;
    private InetAddress masterAddress;
    private boolean masterIPValid;
    //IO
    private static final Deflater fastDeflater = new Deflater(1);
    private static long time;
    //formatting
    private static DecimalFormat MBformatter = new DecimalFormat("#0.0");
    private static final int bytesPerMB = 1048576;

    private void updateCacheSize() {
        cacheSize = IOHelper.getFileCacheSize(fileCacheMap);
    }
}
