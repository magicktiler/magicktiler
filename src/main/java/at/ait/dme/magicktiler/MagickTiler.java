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

/**
 * The base class for all supported tile scheme implementations.
 * 
 * @author aboutgeo@no5.at
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
	 * Flag indicating whether a HTML preview should be generated (default: false)
	 */
	protected boolean generatePreview = false;
		
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
		TilesetInfo info = new TilesetInfo(image, tileWidth, tileHeight, format, useGraphicsMagick);
		convert(image, info, target);
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
	 * If set to true, an HTML file will be generated which
	 * displays the rendered tileset in an OpenLayers map. 
	 * @param generatePreview set to true to obtain an OpenLayers preview of the tileset
	 */
	public void setGeneratePreviewHTML(boolean generatePreview) {
		this.generatePreview = generatePreview;
	}
	
}
