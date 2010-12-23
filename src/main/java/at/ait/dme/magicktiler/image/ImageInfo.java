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

package at.ait.dme.magicktiler.image;

import java.io.File;

import at.ait.dme.magicktiler.TilingException;
import at.ait.dme.magicktiler.image.ImageProcessor.ImageProcessingSystem;


/**
 * Image information for a specified file (currently width and height only).
 * 
 * @author Rainer Simon <magicktiler@gmail.com>
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class ImageInfo {
	
	/**
	 * Error message
	 */
	private static final String IDENTIFY_ERROR = "Error reading file information";
		
	/**
	 * The source image
	 */
	private File file;
	
	/**
	 * Image width
	 */
	private int width;
	
	/**
	 * Image height
	 */
	private int height;
	
	public ImageInfo(File image, ImageProcessingSystem imageProcessingSystem) throws TilingException {
		this.file = image;
		
		try {
			String result = new ImageProcessor(imageProcessingSystem).identify(image.getAbsolutePath());
			if (result == null || result.length() == 0) throw new TilingException(IDENTIFY_ERROR);
			
			// Parse console output
			String[] params = result.toString().split(" ");
			String size = params[2];
			if (size.indexOf('+') > -1) size = size.substring(0, size.indexOf('+'));
			width  = Integer.parseInt(size.substring(0, size.indexOf('x')));
			height = Integer.parseInt(size.substring(size.indexOf('x') + 1));
		} catch (Exception e) {
			throw new TilingException(e.getMessage());
		}
	}
	
	public File getFile() {
		return file;
	}

	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}
}
