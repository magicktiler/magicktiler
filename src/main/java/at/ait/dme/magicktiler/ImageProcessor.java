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
	private boolean useGraphicsMagick;
	private TileFormat tileFormat;
	private int quality;
	private String background;

	public ImageProcessor(ImageProcessingSystem imageProcessingSystem, TileFormat tileFormat) {
		this.useGraphicsMagick = (imageProcessingSystem==ImageProcessingSystem.GRAPHICSMAGICK);
		this.tileFormat = tileFormat;
	}
	
	public ImageProcessor(ImageProcessingSystem imageProcessingSystem, TileFormat tileFormat, String background, int quality) {
		this(imageProcessingSystem, tileFormat);
		this.quality = quality;
		this.background = background;
	}
	
	public void crop(String src, String target, int width, int height) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = createOperation();
		op.addImage(src);
		op.crop(width, height);
		op.p_adjoin();
		op.addImage(target);
		
		ConvertCmd convert = new ConvertCmd(useGraphicsMagick);
		convert.run(op);
	}
	
	public void crop(String src, String target, int width, int height, String gravity, int x, int y) 
		throws IOException, InterruptedException, IM4JavaException {

		IMOperation op = createOperation();
		op.background(background);
		op.crop(width, height);
		op.p_adjoin();
		op.addImage(src);
		op.gravity(gravity);
		op.extent(x, y);
		op.addImage(target);

		ConvertCmd convert = new ConvertCmd(useGraphicsMagick);
		convert.run(op);
	}
	
	public void square(String src, String target, int dim) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = createOperation();
		op.addImage(src);
		op.gravity("Center");
		op.geometry(dim, dim);
		op.addRawArgs("xc:"+background);
		op.addImage(target);
		new CompositeCmd(useGraphicsMagick).run(op);
		
	}
	
	public void resize(String src, String target, int width, int height) 
		throws IOException, InterruptedException, IM4JavaException {
		
		IMOperation op = createOperation();
		op.addImage(src);
		op.resize(width, height);
		op.addImage(target);
		new ConvertCmd(useGraphicsMagick).run(op);		
	}
	
	private IMOperation createOperation() {
		IMOperation op = new IMOperation();
		if (tileFormat == TileFormat.JPEG) op.quality(new Double(quality));
		return op;
	}

	public boolean isGraphicsMagickUsed() {
		return useGraphicsMagick;
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
	
	public void setUseGraphicsMagick(boolean useGraphicsMagick) {
		this.useGraphicsMagick = useGraphicsMagick;
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
