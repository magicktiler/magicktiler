/*
 * Copyright 2010 Austrian Institute of Technology
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package at.ait.dme.magicktiler.ptif;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.im4java.core.IM4JavaException;

import at.ait.dme.magicktiler.MagickTiler;
import at.ait.dme.magicktiler.TilesetInfo;
import at.ait.dme.magicktiler.TilingException;

/**
 * A converter that implements conversion to the Pyramid TIFF (PTIF) format.
 * <br><br>
 * Please note that the default PTIF conversion of ImageMagick/GraphicsMagick...
 * <br><br>
 * <em>convert [image] -define tiff:tile-geometry=256x256 -compress jpeg 'ptif:output.tif'</em>
 * <br><br>
 * produces a result which has <strong>one zoom level too many</strong> - which
 * may cause problems with some viewers.
 * <br><br> 
 * The implemented conversion algorithm works around this as follows:
 * <ol>
 * <li>Each pyramid level is computed and stored as a temporary file</li>
 * <li>The temporary files are merged into a single (temporary) multi-level TIFF
 * with JPEG compression and 256x256 tile geometry.</li>
 * <li>The temporary PTIF file is renamed to the specified output file name.
 * (Note: this step is necessary - otherwise IM/GM would fall back to it's default
 * PTIF behavior in case the user specified an output file name with .ptif 
 * extension!</li>
 * <li>Temporary files are deleted.</li>
 * </ol>
 * 
 * @author Rainer Simon <magicktiler@gmail.com>
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class PTIFConverter extends MagickTiler {

  /**
   * Standard tile width and height for PTIF files = 256
   */
  private static final int TILE_SIZE = 256;

  /**
   * Log4j logger
   */
  private static Logger log = Logger.getLogger(PTIFConverter.class);

  @Override
  protected TilesetInfo convert(File image, TilesetInfo info) throws TilingException {
    long startTime = System.currentTimeMillis();
    log.info("Generating PTIF for file " + image.getName() + ": " + info.getImageWidth() + "x" + info.getImageHeight()
        + ", " + info.getZoomLevels() + " zoom levels");

    try {
      // Step 1 - compute pyramid
      log.debug("Computing pyramid");
      List<String> levels = computePyramid(info);

      // Step 2 - merge
      log.debug("Merging");
      File tempFile = new File(image.getParent(), "tmp.tif");
      levels.add(tempFile.getAbsolutePath());
      processor.merge(levels, "tiff:tile-geometry=" + TILE_SIZE + "x" + TILE_SIZE, "jpeg");

      // Step 3 - rename
      File emptyDir = null;
      if (!tilesetRootDir.getName().endsWith(".tif") && !tilesetRootDir.getName().endsWith(".ptif")) {
    	  emptyDir = new File(tilesetRootDir.getAbsolutePath());
    	  tilesetRootDir = new File(tilesetRootDir.getAbsolutePath() + ".tif");
      }
      
      if (emptyDir != null)
    	  emptyDir.delete();
    	  
      if (tilesetRootDir.exists()) {
        if (!tilesetRootDir.delete())
          throw new TilingException("Failed to delete directory:" + tilesetRootDir);
      }
      if (!tempFile.renameTo(tilesetRootDir))
        throw new TilingException("Failed to rename directory:" + tempFile);

      // Step 4 - remove temporary files
      for (int i = 1; i < levels.size() - 1; i++) {
        tempFile = new File(levels.get(i));
        if (!tempFile.delete())
          log.error("Failed to delete temp file:" + tempFile);
      }
    } catch (Exception e) {
      throw new TilingException(e.getMessage());
    }

    log.info("Took " + (System.currentTimeMillis() - startTime) + " ms.");
    return info;
  }

  private List<String> computePyramid(TilesetInfo info) throws IOException, InterruptedException, IM4JavaException {
    ArrayList<String> pyramid = new ArrayList<String>();

    String inputFile = info.getImageFile().getAbsolutePath();
    pyramid.add(inputFile);

    String tempFilePrefix = inputFile.substring(0, inputFile.lastIndexOf('.'));

    int w = info.getImageWidth();
    int h = info.getImageHeight();

    String previousLevel = inputFile;
    String thisLevel;

    for (int i = 1; i < info.getZoomLevels(); i++) {
      w /= 2;
      h /= 2;
      thisLevel = tempFilePrefix + "-" + i + ".tif";

      processor.scale(previousLevel, thisLevel, w, h);
      pyramid.add(thisLevel);
      previousLevel = thisLevel;
    }
    return pyramid;
  }
}
