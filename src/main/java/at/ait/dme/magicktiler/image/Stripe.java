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
import java.io.IOException;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.MontageCmd;

import at.ait.dme.magicktiler.TilingException;

/**
 * To speed up the MagickTiler tiling process, images are (for most tiling schemes)
 * first split into a sequence of 'stripes'. Depending on the tiling scheme, striping
 * is done either vertically or horizontally. This class is a utility class for handling
 * and manipulating image stripes.
 * 
 * @author magicktiler@gmail.com
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
	 * @param useGraphicsMagick flag indicating whether GM should be used for processing
	 * @return the result stripe
	 * @throws IOException if something goes wrong
	 * @throws InterruptedException if something goes wrong
	 * @throws IM4JavaException if something goes wrong
	 */
	public Stripe merge(Stripe stripe, File targetFile, boolean useGraphicsMagick)
			throws IOException, InterruptedException, IM4JavaException {
	
		return merge(stripe, null, -1, -1, null, targetFile, useGraphicsMagick);
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
	 * @param gravity the gravity to use when compositing the images on the background canvas
	 * @param xExtent the width of the result stripe canvas
	 * @param yExtent the height of the resul stripe canvas
	 * @param backgroundColor the background color of the canvas
	 * @param targetFile the file which will hold the result stripe image
	 * @param useGraphicsMagick flag indicating whether GM should be used for processing
	 * @return the result stripe
	 * @throws IOException if something goes wrong
	 * @throws InterruptedException if something goes wrong
	 * @throws IM4JavaException if something goes wrong
	 */
	public Stripe merge(Stripe stripe, String gravity, int xExtent, int yExtent,
			String backgroundColor, File targetFile, boolean useGraphicsMagick) 
		throws IOException, InterruptedException, IM4JavaException {
		
		if (stripe.orientation != orientation) throw new IllegalArgumentException(DIFFERENT_ORIENTATION_ERROR);

		IMOperation op = new IMOperation();

		if (orientation == Orientation.HORIZONTAL) {
			op.tile(1, 2);
		} else {
			op.tile(2, 1);
		}

		int w, h;
		if ((xExtent > -1) && (yExtent > -1)) {
			op.gravity(gravity);
			op.background(backgroundColor);
			w = xExtent;
			h = yExtent;
			op.geometry(w / 2, h);
		} else {
			w = (orientation == Orientation.HORIZONTAL) ? width / 2 : (width  + stripe.getWidth()) / 2;
			h = (orientation == Orientation.HORIZONTAL) ? (height + stripe.getHeight()) / 4 : height / 2;
			
			op.addRawArgs("-geometry", "+0+0");
			op.addRawArgs("-resize", "50%x50%");
		}

		op.addImage(file.getAbsolutePath());
		op.addImage(stripe.getImageFile().getAbsolutePath());
		op.addImage(targetFile.getAbsolutePath());
		
		MontageCmd montage = new MontageCmd(useGraphicsMagick);
		montage.run(op);

		return new Stripe(targetFile, w, h, orientation);
	}
	
	/**
	 * Shrinks this stripe 50% to the resolution of the next zoom level.
	 * 
	 * @param targetFile the file which will hold the result stripe image
	 * @param useGraphicsMagick flag indicating whether GM should be used for processing
	 * @return the result stripe
	 * @throws IOException if something goes wrong
	 * @throws InterruptedException if something goes wrong
	 * @throws IM4JavaException if something goes wrong
	 */
	public Stripe shrink(File targetFile, boolean useGraphicsMagick) throws IOException,
			InterruptedException, IM4JavaException {
		
		return shrink(null, -1, -1, null, targetFile, useGraphicsMagick);
	}
	
	/**
	 * Shrinks this stripe 50% to the resolution of the next zoom level.
	 * This method allows to create a background color buffer around the stripe, in case
	 * the employed tiling scheme mandates certain image resolution constraints (e.g.
	 * width/height must be integer multiples of the tile-size). 
	 * 
	 * @param gravity the gravity to use when compositing the images on the background canvas
	 * @param xExtent the width of the result stripe canvas
	 * @param yExtent the height of the resul stripe canvas
	 * @param backgroundColor the background color of the canvas
	 * @param targetFile the file which will hold the result stripe image
	 * @param useGraphicsMagick flag indicating whether GM should be used for processing
	 * @return the result stripe
	 * @throws IOException if something goes wrong
	 * @throws InterruptedException if something goes wrong
	 * @throws IM4JavaException if something goes wrong
	 */
	public Stripe shrink(String gravity, int xExtent, int yExtent, String backgroundColor,
			File targetFile, boolean useGraphicsMagick) 
		throws IOException, InterruptedException, IM4JavaException {
	
		IMOperation op = new IMOperation();
		if (xExtent > -1 && yExtent > -1){
			op.gravity(gravity);
			op.background(backgroundColor);
			
			if (orientation == Orientation.HORIZONTAL) {
				op.tile(1, 2);
			} else {
				op.tile(2, 1); 
			}
			
			op.geometry(xExtent / 2, yExtent);
			op.addImage(file.getAbsolutePath());
			op.addImage("null:");
			op.addImage(targetFile.getAbsolutePath());
			
			MontageCmd montage = new MontageCmd(useGraphicsMagick);
			montage.run(op);

			return new Stripe(targetFile, xExtent, yExtent, orientation);
		} else {
			op.addRawArgs("-scale", "50%x50%");
			op.addImage(file.getAbsolutePath());
			op.addImage(targetFile.getAbsolutePath());
			
			ConvertCmd convert = new ConvertCmd(useGraphicsMagick);
			convert.run(op);
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
