package at.ait.dme.magicktiler.gmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.im4java.core.CompositeCmd;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;

import at.ait.dme.magicktiler.ImageInfo;
import at.ait.dme.magicktiler.MagickTiler;
import at.ait.dme.magicktiler.TileFormat;
import at.ait.dme.magicktiler.TilesetInfo;
import at.ait.dme.magicktiler.TilingException;

/**
 * A tiler that implements the Google Maps tiling scheme
 * <br><br>
 * <b>Developer info...</b><br>
 * <em>If you just want to generate Google Map tiles and use them, and 
 * don't need to understand how it works internally - <b>just 
 * ignore this section!</b></em>
 * <br><br>
 * The Google Maps tiling scheme
 * arranges tiles in the following folder/file structure:
 * <br><br>
 * /tileset-root/[zoomlevel]-[column]-[row].jpg (or .png)
 * <br><br>
 * The highest-resolution zoom level has the highest number. Column/row
 * numbering of tiles starts top/left, counting direction is right/downwards.
 * <br><br>
 * The implemented tiling algorithm works as follows:
 * <ol>
 * <li>Resize the image to the closest multiple of 256 and the power of 2</li>
 * <li>Then square the image using the longest dimension, 
 * filling the empty space with the background color. </li>
 * <li>For all zoomlevels we create a base image with a width/height of 256*2^zoomlevel</li>
 * <li>These base images are cut into tiles: The number of tiles per zoomlevel is 4^(zoomlevel)</li>
 * <li>HTML preview file is generated (if requested).</li>
 * </ol>
 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class GoogleMapsTiler extends MagickTiler {
	private static Logger log = Logger.getLogger(GoogleMapsTiler.class);

	//TODO finish this implementation
	
	@Override
	protected TilesetInfo convert(File image, TilesetInfo info) throws TilingException {
		long startTime = System.currentTimeMillis();
		log.info("Generating Google Map tiles for file " + image.getName());	
		
		if (!workingDirectory.exists()) createDir(workingDirectory);
		
		String baseName = image.getName();
		baseName = baseName.substring(0, baseName.lastIndexOf('.'));
		createTargetDir(baseName);

		try {
			log.debug("Resizing and squaring base image");
			
			String baseImageFileName = tilesetRootDir.getAbsolutePath()+"/base."+format.getExtension();
			
			// Step 1: resize to the closest multiple of 256 and the power of 2
			ImageInfo resizedImage=resizeBaseImage(image,info,baseImageFileName);
			
			// Step 2: square the image
			ImageInfo squaredImage=squareBaseImage(resizedImage, baseImageFileName);
			
			// reinitialize the tileset info based on the new base image
			info = new TilesetInfo(squaredImage.getFile(), tileWidth, tileHeight, format, useGraphicsMagick);	
		
			for(int z=0;z<info.getZoomLevels();z++) {
				log.debug("Tiling level " + z);
				String tilesBaseFileName = tilesetRootDir.getAbsolutePath()+File.separator+z;
				
				// Step 3: create a base image for each zoomlevel
				int dim = 256*(int)Math.pow(2, z);
				baseImageFileName = tilesetRootDir.getAbsolutePath()+"/base"+z+"."+format.getExtension();
				resizeImage(squaredImage.getFile().getAbsolutePath(), baseImageFileName, dim, dim);
				File baseImageFile = new File(baseImageFileName);
					
				// Step 4: create the tiles for each zoomlevel
				TilesetInfo baseInfo = 
					new TilesetInfo(baseImageFile, tileWidth, tileHeight, format, useGraphicsMagick);	
				
				IMOperation op = new IMOperation();
				// as the offset is omitted, this will create tiles for the entire input image, which we will rename.
				// this is also a performance improvement compared to generating each tile separately.
				op.crop(tileWidth, tileHeight);
				if (format == TileFormat.JPEG) op.quality(new Double(jpegQuality));
				op.addImage(baseImageFileName);
				op.addImage(tilesBaseFileName+"_"+"%d"+"."+format.getExtension());
				
				ConvertCmd convert = new ConvertCmd(useGraphicsMagick);
				convert.run(op);
				
				for(int x=0,i=0;x<baseInfo.getNumberOfXTiles(0);x++) {
					for(int y=0;y<baseInfo.getNumberOfYTiles(0);y++,i++) {
						File fOld = new File(tilesBaseFileName+"_"+i+"."+format.getExtension());
						File fNew = new File(tilesBaseFileName+"_"+x+"_"+y+"."+format.getExtension());
						if(!fOld.renameTo(fNew)) throw new TilingException("Failed to rename file:"+fOld);
					}
				}
				if(!baseImageFile.delete()) log.error("could not delete file:"+baseImageFile);
			}
			if(!squaredImage.getFile().delete()) log.error("could not delete file:"+squaredImage.getFile());
			
			//step 5: optionally create preview.html
			if(generatePreview) generatePreview(info);
			log.info("Took " + (System.currentTimeMillis() - startTime) + " ms.");
		} catch (Exception e) {
			log.error("Failed to resize image", e);
			throw new TilingException(e.getMessage());
		} 
		
		return info;
	}
	

	private ImageInfo squareBaseImage(ImageInfo imageInfo, String targetImageFileName) 
		throws IOException, InterruptedException, IM4JavaException, TilingException {	
		
		int maxDim=Math.max(imageInfo.getHeight(), imageInfo.getWidth());
		
		IMOperation op = new IMOperation();
		op.addImage(imageInfo.getFile().getAbsolutePath());
		op.gravity("Center");
		op.geometry(maxDim, maxDim);
		op.addRawArgs("xc:"+backgroundColor);
		if (format == TileFormat.JPEG) op.quality(new Double(jpegQuality));
		op.addImage(targetImageFileName);
		new CompositeCmd(useGraphicsMagick).run(op);
		
		return new ImageInfo(new File(targetImageFileName), useGraphicsMagick);
	}

	private ImageInfo resizeImage(String src, String target, int width, int height) 
		throws IOException, InterruptedException, IM4JavaException, TilingException {
		
		IMOperation op = new IMOperation();
		op.addImage(src);
		op.resize(width, height);
		if (format == TileFormat.JPEG) op.quality(new Double(jpegQuality));
		op.addImage(target);
		new ConvertCmd(useGraphicsMagick).run(op);
		
		return new ImageInfo(new File(target), useGraphicsMagick);
	}
	
	private ImageInfo resizeBaseImage(File image, TilesetInfo info, String targetFileName) 
		throws IOException, InterruptedException, IM4JavaException, TilingException {
		
		// find the closest multiple of 256 and the power of 2
		int maxDim = Math.max(info.getWidth(), info.getHeight());
		int newMaxDim=0,prevMaxDim=0;
		for(int pow=0; newMaxDim<maxDim; pow++) {
			prevMaxDim = newMaxDim;
			newMaxDim = 256 * (int)(Math.pow(2, pow));
		}
		if(Math.abs(maxDim-prevMaxDim) < Math.abs(maxDim-newMaxDim)) newMaxDim = prevMaxDim;
		
		// calculate the new height and width
		int newHeight=0,newWidth=0;
		if(maxDim==info.getHeight())  {
			newHeight=newMaxDim;
			newWidth=newHeight * (int)Math.ceil(((float)info.getWidth()/info.getHeight()));
		} else {
			newWidth=newMaxDim;
			newHeight=newWidth * (int)Math.ceil(((float)info.getHeight()/info.getWidth()));
		}
		
		return resizeImage(image.getAbsolutePath(), targetFileName, newWidth, newHeight);
	}
	
	private void generatePreview(TilesetInfo info) throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(this.getClass()
						.getResourceAsStream("gmap-template.html")));
	
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			
			String html = sb.toString()
				.replace("@title@", info.getImageFile().getName())
				.replace("@tilesetpath@", tilesetRootDir.getAbsolutePath().replace("\\", "/")+"/")
				.replace("@ext@", info.getTileFormat().getExtension());
			
			writeHtmlPreview(html);
		} finally {
			if(reader!=null) reader.close();
		}
	}
}
