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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.im4java.core.CompositeCmd;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.IdentifyCmd;
import org.im4java.core.MontageCmd;
import org.im4java.process.OutputConsumer;

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
	 * JPEG compression quality (in case of JPEG image format), default=75
	 */
	private int jpegQuality = 75;
	
	/**
	 * The default background color for montage operations
	 */
	private String backgroundColor;
	
	
	public ImageProcessor(ImageProcessingSystem processingSystem) {
		this.processingSystem = processingSystem;
	}
	
	public ImageProcessor(ImageProcessingSystem processingSystem, ImageFormat format) {
		this(processingSystem);
		this.format = format;
	}
	
	public ImageProcessor(ImageProcessingSystem processingSystem, ImageFormat format, String backgroundColor) {
		this(processingSystem, format);
		this.backgroundColor = backgroundColor;
	}

	public ImageProcessor(ImageProcessingSystem processingSystem, ImageFormat format, String backgroundColor, int jpegQuality) {
		this(processingSystem, format, backgroundColor);
		this.jpegQuality = jpegQuality;
	}

	
	public ImageProcessor(ImageProcessingSystem processingSystem, ImageFormat format, int jpegQuality) {
		this(processingSystem, format, null, jpegQuality);
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
		
		new ConvertCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);
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

		new ConvertCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);
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
		
		String color = backgroundColor;
		if(color.startsWith("#")) {
			color=color.substring(0,7);
		}
		op.addRawArgs("xc:" + color);
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
	 * Converts an image to the specified target format
	 * 
	 * @param src  absolute path to source image
	 * @param target  absolute path to target image
	 * @param rawArgs  args passed to graphics/imagemagick
	 * 
	 * @throws IM4JavaException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void convert(String src, String target, Map<String, String> rawArgs) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = new IMOperation();
		op.addImage(src);
		if(rawArgs!=null) {
			for(String rawArg : rawArgs.keySet())
			op.addRawArgs(rawArg, rawArgs.get(rawArg));
		}
		op.addImage(target);
		
		new ConvertCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);
	}
	
	/**
	 * Describes the format and characteristics of one or more image files.
	 * 
	 * @param src  absolute path to source image
	 * @return result as String (@see <a href="http://www.graphicsmagick.org/identify.html">docs</a>)
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws IM4JavaException
	 */
	public String identify(String src) throws IOException, InterruptedException, IM4JavaException {
		final StringBuffer result = new StringBuffer();
		IdentifyCmd identify = new IdentifyCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK);
		identify.setOutputConsumer(new OutputConsumer() {
			public void consumeOutput(InputStream in) throws IOException {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;
				while ((line = reader.readLine()) != null) {
					result.append(line);
				}
			}
		});

		IMOperation op = new IMOperation();
		op.addImage(src);
		identify.run(op);
		
		return result.toString();
	}
	
	/**
	 * Merges multiple images
	 * 
	 * @param srcs  list of source file images
	 * @param definition  definition for coders and decoders to use while reading and writing image data
	 * @param compression  compressions of the resulting image
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws IM4JavaException
	 */
	public void merge(List<String> srcs, String definition, String compression) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = createOperation();
		op.adjoin();
		op.define(definition);
		op.compress(compression);
		for (String src : srcs) {
			op.addImage(src);
		}
 		
		new ConvertCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);
	}
	
	/**
	 * Scales an image to the given size
	 * 
	 * @param src  absolute path to source image
	 * @param target  absolute path to target image
	 * @param width  width of the resulting image
	 * @param height  height of the resulting image
	 * 
	 * @throws IM4JavaException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void scale(String src, String target, int width, int height) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = createOperation();
		op.size(width, height);
		op.scale(width, height);
		op.addImage(src);
		op.addImage(target);

		new ConvertCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);
	}
	
	/**
	 * Creates a montage of the given images
	 * 
	 * @param srcs  list of source file images
	 * @param target  absolute path to target image
	 * @param xTiles  number of x tiles
	 * @param yTiles  number of y tiles
	 * @param rawArgs  raw args passed to graphics/imagemagick
	 * 
	 * @throws IM4JavaException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void montage(List<String> srcs, String target, int xTiles, int yTiles, Map<String, String> rawArgs) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = createOperation();
		op.tile(xTiles, yTiles);
		if(rawArgs!=null) {
			for(String rawArg : rawArgs.keySet())
			op.addRawArgs(rawArg, rawArgs.get(rawArg));
		}
		op.addImage(srcs.toArray(new String[srcs.size()]));
		op.addImage(target);
		
		new MontageCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);
	}
	
	/**
	 * Creates a montage of the given images
	 * 
	 * @param srcs  list of source file images
	 * @param target  absolute path to target image
	 * @param xTiles  number of x tiles
	 * @param yTiles  number of y tiles
	 * @param width  width of target image
	 * @param height  height of target image
	 * @param backgroundColor  the background color to use
	 * @param gravity  the gravity specifies the location of the image on the canvas
	 * 
	 * @throws IM4JavaException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void montage(List<String> srcs, String target, int xTiles, int yTiles, int width, int height, 
			String backgroundColor, String gravity) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = createOperation();
		op.tile(xTiles, yTiles);
		op.gravity(gravity);
		op.background(backgroundColor);
		op.geometry(width, height);
		op.addImage(srcs.toArray(new String[srcs.size()]));
		op.addImage(target);
		
		new MontageCmd(processingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);
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
