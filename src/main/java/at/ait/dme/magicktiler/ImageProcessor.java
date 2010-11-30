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

import java.io.IOException;

import org.im4java.core.CompositeCmd;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;

/**
 * A wrapper for all image processing operations used.
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class ImageProcessor {
	
	/**
	 * Supported image processing systems: GraphicsMagick or ImageMagick.
	 * Please note that there are currently some issues with ImageMagick though.
	 * It is highly recommended to use this software with GraphicsMagick!
	 */
	public enum ImageProcessingSystem { GRAPHICSMAGICK,	IMAGEMAGICK }
	
	/**
	 * IM/GM gravity String constants
	 */
	public static final String GRAVITY_CENTER = "Center";
	public static final String GRAVITY_SOUTHWEST = "SouthWest";
	
	/**
	 * The processing system used by this ImageProcessor
	 */
	private ImageProcessingSystem processingSystem;
	
	/**
	 * The image format this processor will produce as output
	 */
	private ImageFormat format;
	
	/**
	 * JPEG compression quality (in case of JPEG image format)
	 */
	private int jpegQuality;
	
	/**
	 * The default background color for montage operations
	 */
	private String backgroundColor;
	
	public ImageProcessor(ImageProcessingSystem processingSystem, ImageFormat format) {
		this(processingSystem, format, 75, null);
	}
	
	public ImageProcessor(ImageProcessingSystem processingSystem, ImageFormat format, String backgroundColor) {
		this(processingSystem, format, 75, backgroundColor);
	}
	
	public ImageProcessor(ImageProcessingSystem processingSystem, ImageFormat format, int jpegQuality) {
		this(processingSystem, format, jpegQuality, null);
	}

	public ImageProcessor(ImageProcessingSystem processingSystem, ImageFormat format, int jpegQuality, 
			String backgroundColor) {
		this.processingSystem = processingSystem;
		this.format = format;
		this.jpegQuality = jpegQuality;
		this.backgroundColor = backgroundColor;
	}

	/**
	 * Crops an image using the width and height provided
	 * 
	 * @param src  absolute path to source image
	 * @param target  absolute path to target image
	 * @param width  the width of the resulting image
	 * @param height  the height of the resulting image
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws IM4JavaException
	 */
	public void crop(String src, String target, int width, int height) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = createOperation();
		op.addImage(src);
		op.crop(width, height);
		op.p_adjoin();
		op.addImage(target);
		
		ConvertCmd convert = new ConvertCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK);
		convert.run(op);
	}
	
	/**
	 * Crops an image using the width and height provided and places it on a
	 * canvas with the specified gravity, width and height.
	 * 
	 * @param src  absolute path to source image
	 * @param target  absolute path to target image
	 * @param width  the width of the resulting image
	 * @param height the height of the resulting image
	 * @param gravity  the gravity specifies the location of the image on the canvas
	 * @param canvasWidth  the width of the canvas
	 * @param canvasHeight the height of the canvas
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws IM4JavaException
	 */
	public void crop(String src, String target, int width, int height,
			int canvasWidth, int canvasHeight, String gravity) 
		throws IOException, InterruptedException, IM4JavaException {

		IMOperation op = createOperation();
		op.background(backgroundColor);
		op.crop(width, height);
		op.p_adjoin();
		op.addImage(src);
		op.gravity(gravity);
		op.extent(canvasWidth, canvasHeight);
		op.addImage(target);

		ConvertCmd convert = new ConvertCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK);
		convert.run(op);
	}
	
	/**
	 * Squares an image using the specified dimension
	 * 
	 * @param src  absolute path to source image
	 * @param target  absolute path to target image
	 * @param dim  dimension of the new image
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws IM4JavaException
	 */
	public void square(String src, String target, int dim) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = createOperation();
		op.addImage(src);
		op.gravity(GRAVITY_CENTER);
		op.geometry(dim, dim);
		op.addRawArgs("xc:" + backgroundColor);
		op.addImage(target);
		new CompositeCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);
	}
	
	/**
	 * Resizes as image to the specified width and height
	 * 
	 * @param src  absolute path to source image
	 * @param target  absolute path to target image
	 * @param width  the width of the resulting image
	 * @param height  the height of the resulting image
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws IM4JavaException
	 */
	public void resize(String src, String target, int width, int height) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = createOperation();
		op.addImage(src);
		op.resize(width, height);
		op.addImage(target);
		new ConvertCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);		
	}

	/**
	 * Converts an image to the target format
	 * 
	 * @param src  absolute path to source image
	 * @param target  absolute path to target image
	 * 
	 * @throws IM4JavaException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void convert(String src, String target) throws IOException, InterruptedException, IM4JavaException {
		IMOperation convert = new IMOperation();
		convert.addImage(src);
		convert.addImage(target);
		
		new ConvertCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(convert);
	}
	
	private IMOperation createOperation() {
		IMOperation op = new IMOperation();
		if (format == ImageFormat.JPEG) op.quality(new Double(jpegQuality));
		return op;
	}

	public ImageProcessingSystem getImageProcessingSystem() {
		return processingSystem;
	}
	
	public void setImageProcessingSystem(ImageProcessingSystem processingSystem) {
		this.processingSystem = processingSystem;
	}
	
	public ImageFormat getImageFormat() {
		return format;
	}
	
	public void setImageFormat(ImageFormat format) {
		this.format = format;
	}
	
	public int getJPEGQuality() {
		return jpegQuality;
	}
	
	public void setJPEGQuality(int quality) {
		if (quality < 0) throw new IllegalArgumentException("quality below 0");
		if (quality > 100) throw new IllegalArgumentException("quality above 100");
		this.jpegQuality = quality;		
	}
	
	public String getBackground() {
		return backgroundColor;
	}
	
	public void setBackground(String color) {
		this.backgroundColor = color;
	}
}
