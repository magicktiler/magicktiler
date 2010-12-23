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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.im4java.core.IM4JavaException;

import at.ait.dme.magicktiler.image.ImageProcessor;
import at.ait.dme.magicktiler.image.ImageProcessor.ImageProcessingSystem;

/**
 * To speed up the MagickTiler tiling process, images are (for most tiling schemes)
 * first split into a sequence of 'stripes'. Depending on the tiling scheme, striping
 * is done either vertically or horizontally. This class is a utility class for handling
 * and manipulating image stripes.
 * 
 * @author Rainer Simon <magicktiler@gmail.com>
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class Stripe {
	
	/**
	 * Merge error message
	 */
	private static final String DIFFERENT_ORIENTATION_ERROR =
		"Cannot merge. Stripes have different orientation";
	
	/**
	 * Enum: possible stripe orientations
	 */
	public enum Orientation { HORIZONTAL, VERTICAL };
	
	/**
	 * The stripe image file
	 */
	private File file;
	
	/**
	 * The width of this stripe in pixel
	 */
	private int width;
	
	/**
	 * The height of this stripe in pixel
	 */
	private int height;
	
	/**
	 * This stripe's orientation
	 */
	private Orientation orientation;
	
	public Stripe(File file, int width, int height, Orientation orientation) {
		this.file = file;
		this.width = width;
		this.height = height;
		this.orientation = orientation;
	}
	
	public File getImageFile() {
		return file;
	}

	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}

	public Orientation getOrientation() {
		return orientation;
	}
	
	/**
	 * Merges this stripe with another one into a single stripe, scaled according to
	 * the resolution of the next pyramid zoom layer. (I.e. the two original stripes
	 * will be joined next to each other, and the resulting image will be down-scaled
	 * by 50%).
	 * 
	 * @param stripe the stripe to join with this stripe
	 * @param targetFile the file which will hold the result stripe image
	 * @param system the ImageProcessingSystem to use (ImageMagick or GraphicsMagick)
	 * @return the result stripe
	 * @throws IOException if something goes wrong
	 * @throws InterruptedException if something goes wrong
	 * @throws IM4JavaException if something goes wrong
	 */
	public Stripe merge(Stripe stripe, File targetFile, ImageProcessingSystem system)
			throws IOException, InterruptedException, IM4JavaException {
	
		return merge(stripe, null, -1, -1, null, targetFile, system);
	}
	
	/**
	 * Merges this stripe with another one into a single stripe, scaled according to
	 * the resolution of the next pyramid zoom layer. (I.e. the two original stripes
	 * will be joined next to each other, and the resulting image will be down-scaled
	 * by 50%).
	 * This method allows to create a background color buffer around the stripe, in case
	 * the employed tiling scheme mandates certain image resolution constraints (e.g.
	 * width/height must be integer multiples of the tile-size). 
	 * 
	 * @param stripe the stripe to join with this stripe
	 * @param gravity the gravity to use when composing the images on the background canvas
	 * @param xExtent the width of the result stripe canvas
	 * @param yExtent the height of the result stripe canvas
	 * @param backgroundColor the background color of the canvas
	 * @param targetFile the file which will hold the result stripe image
	 * @param system the ImageProcessingSystem to use (ImageMagick or GraphicsMagick)
	 * @return the result stripe
	 * @throws IOException if something goes wrong
	 * @throws InterruptedException if something goes wrong
	 * @throws IM4JavaException if something goes wrong
	 */
	public Stripe merge(Stripe stripe, String gravity, int xExtent, int yExtent,
			String backgroundColor, File targetFile, ImageProcessingSystem system) 
		throws IOException, InterruptedException, IM4JavaException {
		
		if (stripe.orientation != orientation) throw new IllegalArgumentException(DIFFERENT_ORIENTATION_ERROR);

		List<String> srcs = new ArrayList<String>();
		srcs.add(file.getAbsolutePath());
		srcs.add(stripe.getImageFile().getAbsolutePath());
	
		int xTiles=1,yTiles=2;
		if (orientation == Orientation.VERTICAL) {
			xTiles=2;yTiles=1;
		}

		int w, h;
		if ((xExtent > -1) && (yExtent > -1)) {
			w = xExtent;
			h = yExtent;
			w = w / 2;
			new ImageProcessor(system).montage(srcs, targetFile.getAbsolutePath(), xTiles, yTiles, 
					w, h, backgroundColor, gravity);
		} else {
			w = (orientation == Orientation.HORIZONTAL) ? width / 2 : (width  + stripe.getWidth()) / 2;
			h = (orientation == Orientation.HORIZONTAL) ? (height + stripe.getHeight()) / 4 : height / 2;
			
			Map<String, String>  rawArgs = new HashMap<String, String>();
			rawArgs.put("-geometry", "+0+0");
			rawArgs.put("-resize", "50%x50%");
			new ImageProcessor(system).montage(srcs, targetFile.getAbsolutePath(), xTiles, yTiles, rawArgs);
		}

		return new Stripe(targetFile, w, h, orientation);
	}
	
	/**
	 * Shrinks this stripe 50% to the resolution of the next zoom level.
	 * 
	 * @param targetFile the file which will hold the result stripe image
	 * @param system the ImageProcessingSystem to use (ImageMagick or GraphicsMagick)
	 * @return the result stripe
	 * @throws IOException if something goes wrong
	 * @throws InterruptedException if something goes wrong
	 * @throws IM4JavaException if something goes wrong
	 */
	public Stripe shrink(File targetFile, ImageProcessingSystem system) throws IOException,
			InterruptedException, IM4JavaException {
		
		return shrink(null, -1, -1, null, targetFile, system);
	}
	
	/**
	 * Shrinks this stripe 50% to the resolution of the next zoom level.
	 * This method allows to create a background color buffer around the stripe, in case
	 * the employed tiling scheme mandates certain image resolution constraints (e.g.
	 * width/height must be integer multiples of the tile-size). 
	 * 
	 * @param gravity the gravity to use when composing the images on the background canvas
	 * @param xExtent the width of the result stripe canvas
	 * @param yExtent the height of the result stripe canvas
	 * @param backgroundColor the background color of the canvas
	 * @param targetFile the file which will hold the result stripe image
	 * @param system the ImageProcessingSystem to use (ImageMagick or GraphicsMagick)
	 * @return the result stripe
	 * @throws IOException if something goes wrong
	 * @throws InterruptedException if something goes wrong
	 * @throws IM4JavaException if something goes wrong
	 */
	public Stripe shrink(String gravity, int xExtent, int yExtent, String backgroundColor,
			File targetFile, ImageProcessingSystem system) 
		throws IOException, InterruptedException, IM4JavaException {

		List<String> srcs = new ArrayList<String>();
		if (xExtent > -1 && yExtent > -1) {
			srcs.add(file.getAbsolutePath());
			srcs.add("null:");

			int xTiles=1,yTiles=2;
			if (orientation == Orientation.VERTICAL) {
				xTiles=2;yTiles=1;
			}
			
			new ImageProcessor(system).montage(srcs, targetFile.getAbsolutePath(), xTiles, yTiles, 
					xExtent / 2, yExtent, backgroundColor, gravity);
			
			return new Stripe(targetFile, xExtent, yExtent, orientation);
		} else {
			Map<String, String>  rawArgs = new HashMap<String, String>();
			rawArgs.put("-scale", "50%x50%");
			
			new ImageProcessor(system).convert(file.getAbsolutePath(), targetFile.getAbsolutePath(), rawArgs);

			return new Stripe(targetFile, width / 2, height / 2, orientation);
		}
	}
	
	/**
	 * Removes this stripe's image file from the file system.
	 * (Note that stripes are normally used as temporary files only!) 
	 * 
	 * @throws TilingException 
	 */
	public void delete() throws TilingException {
		if(!file.delete()) throw new TilingException("Could not delete file:"+file);
	}
	
}
