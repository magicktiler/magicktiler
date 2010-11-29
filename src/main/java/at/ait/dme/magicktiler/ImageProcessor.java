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
	
	public static final String GRAVITY_CENTER = "Center";
	public static final String GRAVITY_SOUTHWEST = "SouthWest";
	
	private ImageProcessingSystem imageProcessingSystem = ImageProcessingSystem.GRAPHICSMAGICK;
	private TileFormat tileFormat;
	private int quality;
	private String background;

	public ImageProcessor(TileFormat tileFormat) {
		this.tileFormat = tileFormat;
	}
	
	public ImageProcessor(TileFormat tileFormat, String background, int quality) {
		this(tileFormat);
		this.quality = quality;
		this.background = background;
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
		
		ConvertCmd convert = new ConvertCmd(imageProcessingSystem == ImageProcessingSystem.GRAPHICSMAGICK);
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
	public void crop(String src, String target, int width, int height, String gravity, 
			int canvasWidth, int canvasHeight) 
		throws IOException, InterruptedException, IM4JavaException {

		IMOperation op = createOperation();
		op.background(background);
		op.crop(width, height);
		op.p_adjoin();
		op.addImage(src);
		op.gravity(gravity);
		op.extent(canvasWidth, canvasHeight);
		op.addImage(target);

		ConvertCmd convert = new ConvertCmd(imageProcessingSystem == ImageProcessingSystem.GRAPHICSMAGICK);
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
		op.addRawArgs("xc:"+background);
		op.addImage(target);
		new CompositeCmd(imageProcessingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);
		
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
		new ConvertCmd(imageProcessingSystem == ImageProcessingSystem.GRAPHICSMAGICK).run(op);		
	}
	
	private IMOperation createOperation() {
		IMOperation op = new IMOperation();
		if (tileFormat == TileFormat.JPEG) op.quality(new Double(quality));
		return op;
	}

	public ImageProcessingSystem getImageProcessingSystem() {
		return imageProcessingSystem;
	}

	public void setImageProcessingSystem(ImageProcessingSystem imageProcessingSystem) {
		this.imageProcessingSystem = imageProcessingSystem;
	}
	
	public TileFormat getTileFormat() {
		return tileFormat;
	}

	public String getExtension() {
		return tileFormat.getExtension();
	}
	
	public int getQuality() {
		return quality;
	}

	public String getBackground() {
		return background;
	}

	public void setTileFormat(TileFormat tileFormat) {
		this.tileFormat = tileFormat;
	}

	public void setQuality(int quality) {
		this.quality = quality;
	}

	public void setBackground(String background) {
		this.background = background;
	}
	
}
