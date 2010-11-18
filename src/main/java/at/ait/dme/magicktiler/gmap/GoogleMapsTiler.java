package at.ait.dme.magicktiler.gmap;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.im4java.core.CompositeCmd;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;

import at.ait.dme.magicktiler.GoogleMapsTest;
import at.ait.dme.magicktiler.MagickTiler;
import at.ait.dme.magicktiler.TilesetInfo;
import at.ait.dme.magicktiler.TilingException;
import at.ait.dme.magicktiler.image.ImageInfo;

/**
 * A tiler that implements the Google Maps tiling scheme
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class GoogleMapsTiler extends MagickTiler {
	private static Logger log = Logger.getLogger(GoogleMapsTest.class);

	//TODO finish this implementation
			
	public GoogleMapsTiler() {

	}
	
	@Override
	protected void convert(File image, TilesetInfo info) throws TilingException {
		if (!workingDirectory.exists()) createDir(workingDirectory);
		
		String baseName = image.getName();
		baseName = baseName.substring(0, baseName.lastIndexOf('.'));
		createTargetDir(baseName);

		try {
			ImageInfo baseImage=resizeAndSquareImage(
					new ImageInfo(image, useGraphicsMagick), 
					baseName);
		} catch (Exception e) {
			log.error("Failed to resize image", e);
			throw new TilingException(e.getMessage());
		} 
	}
	

	private ImageInfo resizeAndSquareImage(ImageInfo imageInfo, String targetDir) throws IOException, 
			InterruptedException, IM4JavaException, TilingException {	
		String targetImageFileName = tilesetRootDir.getAbsolutePath()+"/base."+format.getExtension();
		
		int maxDim=Math.max(imageInfo.getHeight(), imageInfo.getWidth());
		// find the next multiple of 256 and the power of 2 
		int newMaxDim=0;
		for(int pow=0; newMaxDim<maxDim; pow++) {
			newMaxDim = 256 * (int)(Math.pow(2, pow));
		}
		
		// calculate the new height and width
		int newHeight=0,newWidth=0;
		if(maxDim==imageInfo.getHeight())  {
			newHeight=newMaxDim;
			newWidth=newHeight * (int)Math.ceil(((float)imageInfo.getWidth()/imageInfo.getHeight()));
		} else {
			newWidth=newMaxDim;
			newHeight=newWidth * (int)Math.ceil(((float)imageInfo.getHeight()/imageInfo.getWidth()));
		}
		
		// resize the image
		IMOperation op = new IMOperation();
		op.addImage(imageInfo.getFile().getAbsolutePath());
		op.resize(newWidth, newHeight);
		op.addImage(targetImageFileName);
		new ConvertCmd(useGraphicsMagick).run(op);
		
		// square the image
		op = new IMOperation();
		op.addImage(targetImageFileName);
		op.background(backgroundColor);
		op.gravity("Center");
		op.geometry(newMaxDim, newMaxDim);
		op.addRawArgs("xc:black");
		op.addImage(targetImageFileName);
		new CompositeCmd(useGraphicsMagick).run(op);
		
		return new ImageInfo(new File(targetImageFileName), useGraphicsMagick);
	}

}
