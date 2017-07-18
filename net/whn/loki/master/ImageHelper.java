/**
 *Project: Loki Render - A distributed job queue manager.
 *Version 0.7.2
 *Copyright (C) 2014 Daniel Petersen, Gustavo Alejandro Moreno Martínez
 *Created on Oct 20, 2009
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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author daniel
 */
public class ImageHelper {

    public static boolean compositeTiles(File tileDir, int tileCount,
            File outputFile) {

        long start = System.currentTimeMillis();

        // first check that it has all images
        File[] inputFiles = new File[tileCount];
        for (int i = 0; i < tileCount; i++) {
            inputFiles[i] = new File(tileDir, Integer.toString(i) + ".png");
            if (!inputFiles[i].isFile()) {
                log.warning("expected tile file doesn't exist: " +
                        inputFiles[i].getAbsolutePath());
                return false;
            }
        }

        try {
            int divisions = (int)Math.sqrt((double)inputFiles.length);
            int numImage = 0;
            // create an array of BufferedImages from the input files inverting the order in the rows
            // (it's cropped from bottom to top but it's composited from top to bottom)
            BufferedImage[] bufferedImages = new BufferedImage[inputFiles.length];  
            for (int row = divisions - 1; row >= 0; row--)
                for (int order = 0; order < divisions; order++)
                    bufferedImages[numImage++] = ImageIO.read(inputFiles[row*divisions + order]);  
            
            BufferedImage image = combineImages(bufferedImages);
            ImageIO.write(image, "png", outputFile);

        } catch (IOException ex) {
            log.warning("failed during tile compositing: "  + ex.getMessage());
            return false;
        }
        cleanup(tileDir, inputFiles, tileCount);

        log.fine("composited " + Integer.toString(tileCount) +
                " tiles in (ms): " +
                Long.toString(System.currentTimeMillis() - start));

        return true;
    }

    public static void deleteTileTmpFiles(File lcd, long jID) {
        File tmpDir = new File (lcd, "tmp");
        String prefix = Long.toString(jID) + "-";
        File[] tmpFiles = tmpDir.listFiles();
        for(File tmpTileDir: tmpFiles) {
            if(tmpTileDir.isDirectory() && tmpTileDir.getName().startsWith(prefix)) {
                File[] tileFiles = tmpTileDir.listFiles();
                for(File tilefile: tileFiles) {
                    if(!tilefile.delete()) {
                        log.warning("unable to delete tile file: " +
                                tilefile.getAbsolutePath());
                    }
                }
                if(!tmpTileDir.delete()) {
                    log.warning("unable to delete tile dir: " +
                                tmpTileDir.getAbsolutePath());
                }
            }
        }
    }

    private static void cleanup(File tileDir, File[] inputFiles, int tileCount) {
        for(File f: inputFiles) {
            if(!f.delete()) {
                log.warning("unable to delete tmp tile file: " +
                        f.getAbsolutePath());
            }
        }
        if(!tileDir.delete()) {
            log.warning("unable to delete tmp tile dir: " +
                    tileDir.getAbsolutePath());
        }
    }

    /*PRIVATE*/
    //logging
    private static final String className =
            "net.whn.loki.master.ImageHelper";
    private static final Logger log = Logger.getLogger(className);

    private static BufferedImage combineImages(BufferedImage bufferedImages[]) {
        int divisions = (int)Math.sqrt((double)bufferedImages.length);
        int actualImage = 0;
        // first we stablish the width and height of the final image
        int finalWidth = 0;
        int finalHeight = 0;
        for (int i = 0; i < divisions; i++){
            finalWidth += bufferedImages[i].getWidth();
            finalHeight += bufferedImages[i*divisions].getHeight();
        }
//        BufferedImage finalImg = new BufferedImage(finalWidth, finalHeight, bufferedImages[0].getType());
        BufferedImage finalImg = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_ARGB);

        int rowWidth = 0;
        int rowHeight = 0;
        for (int heightImage = 0; heightImage < divisions; heightImage++) {
            for (int widthImage = 0; widthImage < divisions; widthImage++) {
                // check every image
                if (bufferedImages[actualImage] == null) {
                    log.warning("bufferedImages element has null parameter");
                    return null;
                }
                // adding to the final image
                finalImg.createGraphics().drawImage(bufferedImages[actualImage], rowWidth, rowHeight, null);  
                rowWidth += bufferedImages[actualImage].getWidth();
                actualImage++;  
            }  
            // after processing the row we get the height of the last processed image 
            // (it's the same for all in the row) and locate at the begining of the row
            rowHeight += bufferedImages[actualImage - 1].getHeight();
            rowWidth = 0;
        }  
        
        return finalImg;
    }
}