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

package at.ait.dme.magicktiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.im4java.core.IM4JavaException;

import at.ait.dme.magicktiler.image.ImageInfo;
import at.ait.dme.magicktiler.image.ImageFormat;
import at.ait.dme.magicktiler.image.ImageProcessor;
import at.ait.dme.magicktiler.image.ImageProcessor.ImageProcessingSystem;
import at.ait.dme.magicktiler.Stripe.Orientation;

/**
 * The base class for all supported tile scheme implementations.
 * 
 * @author Rainer Simon <magicktiler@gmail.com>
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public abstract class MagickTiler {

  private static Logger log = Logger.getLogger(MagickTiler.class);

  /**
   * Image processor initialized with default values
   */
  protected ImageProcessor processor = new ImageProcessor(ImageProcessingSystem.GRAPHICSMAGICK, ImageFormat.JPEG,
      "#ffffffff");

  /**
   * Working directory (default: app root)
   */
  protected File workingDirectory = new File(".");

  /**
   * Root directory for the target tileset
   */
  protected File tilesetRootDir = null;

  /**
   * Tile width (default: 256)
   */
  protected int tileWidth = 256;

  /**
   * Tile height (default: 256)
   */
  protected int tileHeight = 256;

  /**
   * Flag indicating whether a HTML preview should be generated (default: false)
   */
  protected boolean generatePreview = false;

  /**
   * get the tileset root directory
   * 
   * @return tileset root dir
   */
  public File getTilesetRootDir() {
    return tilesetRootDir;
  }

  /**
   * Set the working directory for this tiler implementation. The working
   * directory is used to store intermediate files (if any). After the
   * tileset is rendered, the working directory will be emptied.
   * 
   * @param workingDirectory the working directory
   */
  public void setWorkingDirectory(File workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  /**
   * Sets the image processing system for this tiler implementation.
   * 
   * @param system the image processing system to use
   */
  public void setImageProcessingSystem(ImageProcessingSystem system) {
    processor.setImageProcessingSystem(system);
  }

  /**
   * Sets the tile file format for this tiler. Please note that
   * not all tilers may support all file formats!
   * 
   * @param format the tile format
   */
  public void setTileFormat(ImageFormat format) {
    processor.setImageFormat(format);
  }

  /**
   * Sets the background (i.e. 'transparency') color for this tiler
   * implementation.
   * 
   * @param color the background color
   */
  public void setBackgroundColor(String color) {
    processor.setBackground(color);
  }

  /**
   * Sets the compression quality for JPEG tile format. Compression
   * quality must be in the range from 0 (bad quality) to 100 (maximum
   * quality). 
   * 
   * @param quality the JPEG compression quality
   */
  public void setJPEGCompressionQuality(int quality) {
    processor.setJPEGQuality(quality);
  }

  /**
   * If set to true, an HTML file will be generated which
   * displays the rendered tileset in an OpenLayers map. 
   * @param generatePreview set to true to obtain an OpenLayers preview of the tileset
   */
  public void setGeneratePreviewHTML(boolean generatePreview) {
    this.generatePreview = generatePreview;
  }

  /**
   * Generate a new tile set from the specified image file.
   * The tileset will be produced in the same directory as the
   * image, in a folder named the same as the image.
   * 
   * @param image the image file
   * @return some information about the generated tileset
   * @throws TilingException if anything goes wrong
   */
  public TilesetInfo convert(File image) throws TilingException {
    return convert(image, tilesetRootDir);
  }

  /**
   * Generate a new tile set from the specified image file.
   * The tileset will be produced into the specified directory. 
   * 
   * @param image the image file
   * @param target the target directory for the tileset
   * @return some information about the generated tileset
   * @throws TilingException if anything goes wrong
   */
  public TilesetInfo convert(File image, File target) throws TilingException {
    TilesetInfo info = null;
    tilesetRootDir = target;

    if (!workingDirectory.exists())
      createDir(workingDirectory);
    String name = image.getName();
    String baseName = name.indexOf('.') > -1 ? name.substring(0, name.lastIndexOf('.')) : name;
    createTargetDir(baseName);

    if (image.getAbsolutePath().endsWith("jp2")) {
      try {
        long startTime = System.currentTimeMillis();
        log.info("JPEG 2000 - Converting to intermediate TIF for faster processing");
        File tif = convertToTIF(image);
        log.info("Took " + (System.currentTimeMillis() - startTime) + " ms.");

        info = convert(tif, new TilesetInfo(tif, tileWidth, tileHeight, processor));

        if (!tif.delete())
          log.error("Failed to delete TIF file:" + tif);
      } catch (Exception e) {
        throw new TilingException(e.getMessage());
      }
    } else {
      info = convert(image, new TilesetInfo(image, tileWidth, tileHeight, processor));
    }

    return info;
  }

  protected abstract TilesetInfo convert(File image, TilesetInfo info) throws TilingException;

  /**
   * Create the target tileset root directory
   * 
   * @param baseName
   * @throws TilingException
   */
  protected void createTargetDir(String baseName) throws TilingException {
    if (tilesetRootDir == null) {
      tilesetRootDir = new File(workingDirectory, baseName);
      if (tilesetRootDir.exists())
        throw new TilingException("directory '" + baseName + "' exists");
      createDir(tilesetRootDir);
    } else {
      if (!tilesetRootDir.exists())
        createDir(tilesetRootDir);
    }
  }

  /**
   * Create a directory and throw a {@link TilingException} when unsuccessful
   * 
   * @param directory
   * @throws TilingException
   */
  protected void createDir(File dir) throws TilingException {
    if (dir != null && !dir.mkdir())
      throw new TilingException("Problem creating directory:" + dir);
  }

  /**
   * Write the provided HTML string to the preview file
   * 
   * @param html
   * @throws IOException 
   */
  protected void writeHtmlPreview(String html) throws IOException {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(new File(tilesetRootDir, "preview.html")));
      out.write(html);
    } catch (IOException e) {
      log.error("Error writing openlayers preview HTML file: " + e.getMessage());
    } finally {
      if (out != null)
        out.close();
    }
  }

  /**
   * Stripe an image
   * 
   * @param image  the image {@link File}
   * @param orientation  the {@link Orientation} that should be used for creating the stripes
   * @param stripes  the number of stripes
   * @param width  the width of a stripe
   * @param height  the height of a stripe
   * @param outfilePrefix  the prefix of the output files
   * @return the list of {@link Stripe}s
   * 
   * @throws IOException
   * @throws InterruptedException
   * @throws IM4JavaException
   * @throws TilingException
   */
  protected List<Stripe> stripeImage(File image, Orientation orientation, int stripes, int width, int height,
      String outfilePrefix) throws IOException, InterruptedException, IM4JavaException, TilingException {

    return stripeImage(image, orientation, stripes, width, height, width, height, "", outfilePrefix);
  }

  /**
   * Stripes an image
   * 
   * @param image  the image {@link File}
   * @param orientation  the {@link Orientation} that should be used for creating the stripes
   * @param stripes  the number of stripes
   * @param width  the width of a stripe
   * @param height  the height of a stripe
   * @param canvasWidth  the width of the canvas
   * @param canvasHeight  the height of the canvas
   * @param gravity  the gravity specifies the location of the image on the canvas
   * @param outfilePrefix  the prefix of the output files
   * @return the list of {@link Stripe}s
   * @throws IOException
   * @throws InterruptedException
   * @throws IM4JavaException
   * @throws TilingException
   */
  protected List<Stripe> stripeImage(File image, Orientation orientation, int stripes, int width, int height,
      int canvasWidth, int canvasHeight, String gravity, String outfilePrefix) throws IOException,
      InterruptedException, IM4JavaException, TilingException {

    String targetPattern = workingDirectory.getAbsolutePath() + File.separator + outfilePrefix + "%d.tif";
    if (canvasHeight == height && canvasWidth == width) {
      processor.crop(image.getAbsolutePath(), targetPattern, width, height);
    } else {
      processor.crop(image.getAbsolutePath(), targetPattern, width, height, canvasWidth, canvasHeight, gravity);
    }

    // Assemble the list of Stripes
    List<Stripe> resultStripes = new ArrayList<Stripe>();
    int h = canvasHeight;
    int w = canvasWidth;
    for (int i = 0; i < stripes; i++) {
      // in case the last stripe has a different width or height
      if (i == (stripes - 1)) {
        ImageInfo lastStripe = new ImageInfo(new File(workingDirectory, outfilePrefix + i + ".tif"),
            processor.getImageProcessingSystem());
        h = lastStripe.getHeight();
        w = lastStripe.getWidth();
      }

      // Somewhat risky to not check whether GM has generated all stripes correctly - but checking would take time...
      resultStripes.add(new Stripe(new File(workingDirectory, outfilePrefix + i + ".tif"), w, h, orientation));
    }
    return resultStripes;
  }

  /**
   * Utility method that converts any supported input
   * file to TIF. This makes sense e.g. for JPEG 2000, since
   * handling of JP2 in GraphicsMagick is so incredibly slow
   * that converting first and then tiling the TIF is faster
   * overall.
   * 
   * @param file the input file
   * @return the TIF result file
   * @throws IM4JavaException 
   * @throws InterruptedException 
   * @throws IOException 
   */
  private File convertToTIF(File file) throws IOException, InterruptedException, IM4JavaException {
    String inFile = file.getAbsolutePath();
    String outFile = inFile.substring(0, inFile.lastIndexOf('.')) + ".tif";

    processor.convert(inFile, outFile, null);

    File out = new File(outFile);
    if (out.exists())
      return out;

    // No file created without Exception raised by IM4Java - should never happen
    throw new RuntimeException("Panic! Could not generate temporary TIF file.");
  }
}
