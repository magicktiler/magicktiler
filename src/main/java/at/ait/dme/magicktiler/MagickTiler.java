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

import java.io.File;
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
	 * Log4j logger
	 */
	private Logger log = Logger.getLogger(MagickTiler.class);
		
	/**
	 * Generate a new tile set from the specified image file.
	 * The tileset will be produced in the same directory as the
	 * image, in a folder named the same as the image.
	 * @param image the image file
	 * @return some information about the generated tileset
	 * @throws TilingException if anything goes wrong
	 */
	public TilesetInfo convert(File image) throws TilingException {
		return convert(image, null);
	}
	
	/**
	 * Generate a new tile set from the specified image file.
	 * The tileset will be produced into the specified directory. 
	 * @param image the image file
	 * @param target the target directory for the tileset
	 * @return some information about the generated tileset
	 * @throws TilingException if anything goes wrong
	 */
	public TilesetInfo convert(File image, File target) throws TilingException {
		TilesetInfo info;
		
		if (image.getAbsolutePath().endsWith("jp2")) {
			try {
				long startTime = System.currentTimeMillis();
				log.info("JPEG 2000 - Converting to intermediate TIF for faster processing");
				File tif = convertToTIF(image); 
				log.info("Took " + (System.currentTimeMillis() - startTime) + " ms.");
				
				info = new TilesetInfo(tif, tileWidth, tileHeight, format, useGraphicsMagick);
				convert(tif, info, target);
				
				tif.delete();
			} catch (Exception e) {
				throw new TilingException(e.getMessage());
			}
		} else {
			info = new TilesetInfo(image, tileWidth, tileHeight, format, useGraphicsMagick);	
			convert(image, info, target);
		}
		
		return info;
	}
	
	protected abstract void convert(File image, TilesetInfo info, File tilesetRoot) throws TilingException;

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
	 * @param system the image processing system to use
	 */
	public void setImageProcessingSystem(ImageProcessingSystem system) {
		useGraphicsMagick = (system == ImageProcessingSystem.GRAPHICSMAGICK) ? true : false;
	}
	
	/**
	 * Sets the tile file format for this tiler. Please note that
	 * not all tilers may support all file formats!
	 * @param format the tile format
	 */
	public void setTileFormat(TileFormat format) {
		this.format = format;
	}
	
	/**
	 * Sets the background (i.e. 'transparency') color for this tiler
	 * implementation.
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
		if (quality < 0) throw new IllegalArgumentException();
		if (quality > 100) throw new IllegalArgumentException();
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
	 * Utility method that converts any supported input
	 * file to TIF. This makes sense e.g. for JPEG 2000, since
	 * handling of JP2 in GraphicsMagick is so incredibly slow
	 * that converting first and then tiling the TIF is faster
	 * overall.
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
