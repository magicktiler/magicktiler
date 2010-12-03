package at.ait.dme.magicktiler.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.im4java.core.IM4JavaException;

import at.ait.dme.magicktiler.ImageProcessor;
import at.ait.dme.magicktiler.MagickTiler;
import at.ait.dme.magicktiler.Stripe;
import at.ait.dme.magicktiler.TilesetInfo;
import at.ait.dme.magicktiler.TilingException;
import at.ait.dme.magicktiler.Stripe.Orientation;

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
 * <li>For performance reasons, the base image is cut into horizontal or vertical stripes (depending on the format). 
 * Thereby the image is also squared adding background-color buffer so that the new dimension 
 * is a multiple of 256 and the power of 2.</li>
 * <li>For all zoomlevels, the stripes of the zoom level beneath (if any) are merged and are then cut to 
 * tiles (The number of tiles per zoomlevel is 4^zoomlevel)</li>
 * <li>The HTML preview file is generated (if requested).</li>
 * </ol>
 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class GoogleMapsTiler extends MagickTiler {
	private static Logger log = Logger.getLogger(GoogleMapsTiler.class);

	@Override
	protected TilesetInfo convert(File image, TilesetInfo info) throws TilingException {
		long startTime = System.currentTimeMillis();
		log.info("Generating Google Map tiles for file " + image.getName());	
		
		try {
			log.debug("Striping base image");
			
			// Step 1: cut the image into stripes, thereby creating a squared result image 
			// with a dimension of the closest multiple of 256 and the power of 2
			List<Stripe> stripes=stripeBaseImage(info);
			List<Stripe> allStripes = stripes;
			for(int z=info.getZoomLevels()-1;z>=0;z--) {
				log.debug("Tiling level " + z);
				
				// Step 2: create the tiles for this zoom level
				String tilesBaseFileName = tilesetRootDir.getAbsolutePath()+File.separator+z;
				for (int s=0; s<stripes.size(); s++) {
					Stripe stripe = stripes.get(s);
					processor.crop(stripe.getImageFile().getAbsolutePath(), 
							tilesBaseFileName+"_"+"%d"+"."+processor.getImageFormat().getExtension(), 
							tileWidth, 
							tileHeight);
					
					int tiles=(stripe.getOrientation()==Orientation.HORIZONTAL)?
							stripe.getWidth()/tileWidth : stripe.getHeight()/tileHeight;
					
					for(int t=0;t<tiles;t++) {
						int column = (stripe.getOrientation()==Orientation.HORIZONTAL)?t:s;
						int row = (stripe.getOrientation()==Orientation.HORIZONTAL)?s:t;
						
						File fOld = new File(tilesBaseFileName+"_"+t+"."+processor.getImageFormat().getExtension());
						File fNew = new File(tilesBaseFileName+"_"+column+"_"+row+"."+processor.getImageFormat().getExtension());
						if(!fOld.renameTo(fNew)) throw new TilingException("Failed to rename file:"+fOld);
					}
				}
				
				stripes=createStripesForNextZoomLevel(stripes, image.getName(), info.getZoomLevels()-z);
				allStripes.addAll(stripes);
			}
			for(Stripe s : allStripes)
				if(!s.getImageFile().delete()) log.error("Could not delete file:"+s.getImageFile());
				
			//step 3: optionally create preview.html
			if(generatePreview) generatePreview(info);
			log.info("Took " + (System.currentTimeMillis() - startTime) + " ms.");
		} catch (Exception e) {
			log.error("Failed to tile image", e);
			throw new TilingException(e.getMessage());
		} 
		
		return info;
	}
	
	private List<Stripe> stripeBaseImage(TilesetInfo info) 
		throws IOException, InterruptedException, IM4JavaException, TilingException {
		
		String prefix = info.getImageFile().getName().substring(0, 
				info.getImageFile().getName().lastIndexOf('.'))+"-0-";
		
		// find the closest multiple of 256 and the power of 2
		int maxDim = Math.max(info.getWidth(), info.getHeight());
		int newMaxDim=0,prevMaxDim=0;
		for(int pow=0; newMaxDim<maxDim; pow++) {
			prevMaxDim = newMaxDim;
			newMaxDim = 256 * (int)(Math.pow(2, pow));
		}
		if(Math.abs(maxDim-prevMaxDim) < Math.abs(maxDim-newMaxDim)) newMaxDim = prevMaxDim;
		
		Orientation orientation;
		int tileWidth, tileHeight, canvasWidth, canvasHeight, stripes;
		if(info.getWidth()>info.getHeight()){
			orientation=Orientation.VERTICAL;
			tileWidth=canvasWidth=this.tileWidth;
			stripes = newMaxDim / tileWidth;
			tileHeight=info.getHeight();
			canvasHeight = newMaxDim;
		} else {
			orientation=Orientation.HORIZONTAL;
			tileHeight=canvasHeight=this.tileHeight;
			stripes = newMaxDim / tileHeight;
			tileWidth=info.getWidth();
			canvasWidth=newMaxDim;
		}
	
		info.setDimension(newMaxDim, newMaxDim);
		return stripeImage(info.getImageFile(), orientation, stripes, 
				tileWidth, tileHeight, canvasWidth, canvasHeight, ImageProcessor.GRAVITY_CENTER, prefix);
	}

	private List<Stripe> createStripesForNextZoomLevel(List<Stripe> stripes, String baseFileName, int z) 
		throws IOException, InterruptedException, IM4JavaException {
	
		String baseName = baseFileName.substring(0, baseFileName.lastIndexOf('.'));
	
		List<Stripe> nextLevel = new ArrayList<Stripe>();
		for(int i=0; i<Math.ceil((double)stripes.size() / 2); i++) {
			File targetStripe = 
				new File(workingDirectory.getAbsolutePath() + File.separator + baseName + "-" + z + "-" + i + ".tif");
			Stripe stripe1 = stripes.get(i * 2);
			Stripe stripe2 = ((i * 2 + 1) < stripes.size()) ? stripes.get(i * 2 + 1) : null;
			
			Stripe result = null;
			if(stripe2==null) {
				result=stripe1.shrink(targetStripe, processor.getImageProcessingSystem());
			} else {
				result=stripe1.merge(stripe2, targetStripe, processor.getImageProcessingSystem());
			}
			nextLevel.add(result);
		}
		return nextLevel;
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
				.replace("@zoomlevels@", Integer.toString(info.getZoomLevels() ))
				.replace("@maxzoom@", Integer.toString(info.getZoomLevels() - 1))
				.replace("@tilesetpath@", tilesetRootDir.getAbsolutePath().replace("\\", "/")+"/")
				.replace("@ext@", info.getTileFormat().getExtension());
			
			writeHtmlPreview(html);
		} finally {
			if(reader!=null) reader.close();
		}
	}
}
