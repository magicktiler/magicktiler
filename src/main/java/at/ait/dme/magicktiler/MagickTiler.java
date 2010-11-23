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

import org.apache.log4j.Logger;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;


/**
 * The base class for all supported tile scheme implementations.
 * 
 * @author magicktiler@gmail.com
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public abstract class MagickTiler {
	private static Logger log = Logger.getLogger(MagickTiler.class);
	
	
	/**
	 * Gravity constant for WEST gravity orientation
	 */
	protected static final String GRAVITY_WEST = "West";
	
	/**
	 * Gravity constant for SOUTHWEST gravity orientation
	 */
	protected static final String GRAVITY_SOUTH_WEST = "SouthWest";
	
	/**
	 * Working directory (default: app root)
	 */
	protected File workingDirectory = new File(".");
	
	/**
	 * Root directory for the target tileset
	 */
	protected File tilesetRootDir = null;
	
	/**
	 * Image processing system flag - true=GraphicsMagick, false=ImageMagick (default: true) 
	 */
	protected boolean useGraphicsMagick = true;

	/**
	 * Tile width and height (default: 256x256)
	 */
	protected int tileWidth = 256;
	protected int tileHeight = 256;
	
	/**
	 * Tile file format (default: JPEG)
	 */
	protected TileFormat format = TileFormat.JPEG;
	
	/**
	 * Background color (default: white)
	 */
	protected String backgroundColor = "white";
	
	/**
	 * JPEG compression quality (range 0 - 100, default: 75)
	 */
	protected int jpegQuality = 75;
	
	/**
	 * Flag indicating whether a HTML preview should be generated (default: false)
	 */
	protected boolean generatePreview = false;
	
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
		
		if (!workingDirectory.exists()) createDir(workingDirectory);
		String baseName = image.getName().substring(0, image.getName().lastIndexOf('.'));
		createTargetDir(baseName);

		if (image.getAbsolutePath().endsWith("jp2")) {
			try {
				long startTime = System.currentTimeMillis();
				log.info("JPEG 2000 - Converting to intermediate TIF for faster processing");
				File tif = convertToTIF(image); 
				log.info("Took " + (System.currentTimeMillis() - startTime) + " ms.");
				
				info = convert(tif, new TilesetInfo(tif, tileWidth, tileHeight, format, useGraphicsMagick));
				
				if(!tif.delete()) log.error("Failed to delete TIF file:"+tif);
			} catch (Exception e) {
				throw new TilingException(e.getMessage());
			}
		} else {
			info = convert(image, new TilesetInfo(image, tileWidth, tileHeight, format, useGraphicsMagick));
		}
		
		return info;
	}
	
	protected abstract TilesetInfo convert(File image, TilesetInfo info) throws TilingException;

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
		useGraphicsMagick = (system == ImageProcessingSystem.GRAPHICSMAGICK) ? true : false;
	}
	
	/**
	 * Sets the tile file format for this tiler. Please note that
	 * not all tilers may support all file formats!
	 * 
	 * @param format the tile format
	 */
	public void setTileFormat(TileFormat format) {
		this.format = format;
	}
	
	/**
	 * Sets the background (i.e. 'transparency') color for this tiler
	 * implementation.
	 * 
	 * @param color the background color
	 */
	public void setBackgroundColor(String color) {
		this.backgroundColor = color;
	}

	/**
	 * Sets the compression quality for JPEG tile format. Compression
	 * quality must be in the range from 0 (bad quality) to 100 (maximum
	 * quality). 
	 * 
	 * @param quality the JPEG compression quality
	 */
	public void setJPEGCompressionQuality(int quality) {
		if (quality < 0) throw new IllegalArgumentException("quality below 0");
		if (quality > 100) throw new IllegalArgumentException("quality above 100");
		this.jpegQuality = quality;
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
	 * Create the target tileset root directory
	 * 
	 * @param baseName
	 * @throws TilingException
	 */
	protected void createTargetDir(String baseName) throws TilingException {
		if (tilesetRootDir == null) { 
			tilesetRootDir = new File(workingDirectory, baseName);
			if (tilesetRootDir.exists()) 
				throw new TilingException("There is already a directory named " + baseName + "!");
			createDir(tilesetRootDir);
		} else {
			if (!tilesetRootDir.exists()) createDir(tilesetRootDir);
		}
	}
	
	/**
	 * Create a directory and throw a {@link TilingException} when unsuccessful
	 * 
	 * @param directory
	 * @throws TilingException
	 */
	protected void createDir(File dir) throws TilingException {
		if(dir!=null && !dir.mkdir()) 
			throw new TilingException("Problem creating directory:"+dir);
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
			if(out!=null) out.close();
		}
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
	
		IMOperation convert = new IMOperation();
		convert.addImage(inFile);
		convert.addImage(outFile);
		
		ConvertCmd convertCmd = new ConvertCmd(useGraphicsMagick);
		convertCmd.run(convert);
		
		File out = new File(outFile);
		if (out.exists()) 
			return out;
			
		// No file created without Exception raised by IM4Java - should never happen
		throw new RuntimeException("Panic! Could not generate temporary TIF file.");
	}
}
