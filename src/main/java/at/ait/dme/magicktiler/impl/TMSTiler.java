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

package at.ait.dme.magicktiler.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

/**
 * A tiler that implements the TMS tiling scheme.
 * <br><br>
 * <b>Developer info...</b><br>
 * <em>If you just want to generate TMS tiles and use them, and 
 * don't need to understand how TMS works internally - <b>just ignore this section!</b></em>
 * <br><br>
 * The TMS tiling scheme
 * arranges tiles in the following folder/file structure:
 * <br><br>
 * /tileset-root/[zoomlevel]/[column]/[row].jpg (or .png)
 * <br><br>
 * The highest-resolution zoom level has the highest number. Column/row
 * numbering of tiles starts left/bottom, counting direction is upwards/right.
 * <br><br>
 * TMS does NOT allow irregularly sized tiles on the border! Each tile must
 * be rectangular. If the image width/height are not integer multiples of 
 * the tilesize, a background-color buffer must be added. TMS mandates this 
 * buffer to be added to the TOP and RIGHT of the image!   
 * <br><br>
 * The implemented tiling algorithm works as follows:
 * <ol>
 * <li>The base image is cut into vertical stripes; top/right color buffer
 * is added if necessary in the same step.</li>
 * <li>The base image stripes are cut to tiles.</li>
 * <li>For each additional zoom level, the stripes of the zoom level beneath 
 * are merged (adding color buffer if necessary) and cut to tiles.</li>
 * <li>tilemapresource.xml is generated.</li>
 * <li>HTML preview file is generated (if requested).</li>
 * </ol>
 * 
 * @author magicktiler@gmail.com
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class TMSTiler extends MagickTiler {

	/**
	 * XML descriptor file template 
	 */
	private static final String METADATA_TEMPLATE = 
		"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
		"<TileMap version=\"1.0.0\" tilemapservice=\"http://tms.osgeo.org/1.0.0\">\n" +
		"  <Title>@title@</Title>\n"  +
		"  <Abstract></Abstract>\n" +
		"  <SRS></SRS>\n" +
		"  <BoundingBox minx=\"-@height@.00000000000000\" miny=\"0.00000000000000\" maxx=\"0.00000000000000\" maxy=\"@width@.00000000000000\"/>\n" +
		"  <Origin x=\"-@height@.00000000000000\" y=\"0.00000000000000\"/>\n" +
		"  <TileFormat width=\"@tilewidth@\" height=\"@tileheight@\" mime-type=\"@mimetype@\" extension=\"@ext@\"/>\n" +
		"  <TileSets profile=\"raster\">\n" + 
		"@tilesets@" +
		"  </TileSets>\n" +
		"</TileMap>\n";
	
	/**
	 * XML tileset element template
	 */
	private static final String TILESET_TEMPLATE = 
		"    <TileSet href=\"@idx@\" units-per-pixel=\"@unitsPerPixel@.00000000000000\" order=\"@idx@\"/>\n";
	
	/**
	 * Log4j logger
	 */
	private static Logger log = Logger.getLogger(TMSTiler.class);
	
	public TMSTiler() {
		super();
		
		// Set default background color to white, fully transparent
		processor.setBackground("#ffffffff");
	}
	
	@Override
	protected TilesetInfo convert(File image, TilesetInfo info) throws TilingException {
		long startTime = System.currentTimeMillis();
		log.info("Generating TMS tiles for file " + image.getName() + ": " +
                info.getWidth() + "x" + info.getHeight() + ", " +
                info.getNumberOfXTiles(0) + "x" + info.getNumberOfYTiles(0) + " basetiles, " +
                info.getZoomLevels() + " zoom levels, " +
                info.getTotalNumberOfTiles() + " tiles total"
		);		
		
		String baseName = image.getName().substring(0, image.getName().lastIndexOf('.'));
		
		// Step 1 - stripe the base image
		log.debug("Striping base image");
		String basestripePrefix = baseName + "-0-";
		List<Stripe> baseStripes;
		try {
			baseStripes = stripeVertically(image, info, basestripePrefix);
		} catch (Exception e) {
			throw new TilingException(e.getMessage());
		} 
		
		// Step 2 - tile base image stripes
		log.debug("Tiling level 1");
		File baselayerDir = new File(tilesetRootDir, Integer.toString(info.getZoomLevels() - 1));
		createDir(baselayerDir);
		for (int i=0; i<baseStripes.size(); i++) {
			File targetDir = new File(baselayerDir, Integer.toString(i));
			createDir(targetDir);
			try {
				generateTMSTiles(baseStripes.get(i), info, targetDir);
			} catch (Exception e) {
				throw new TilingException(e.getMessage());
			}
		}
		
		// Step 3 - compute the pyramid
		List<Stripe> levelBeneath = baseStripes;
		List<Stripe> thisLevel = new ArrayList<Stripe>();
		for (int i=1; i<info.getZoomLevels(); i++) {
			log.debug("Tiling level " + (i + 1));
			File zoomLevelDir = new File(tilesetRootDir, Integer.toString(info.getZoomLevels() - i - 1));
			createDir(zoomLevelDir);
			
			for(int j=0; j<Math.ceil((double)levelBeneath.size() / 2); j++) {
				try {
					// Step 3a - merge stripes from level beneath
					Stripe stripe1 = levelBeneath.get(j * 2);
					Stripe stripe2 = ((j * 2 + 1) < levelBeneath.size()) ? levelBeneath.get(j * 2 + 1) : null;
					Stripe result = mergeStripes(stripe1, stripe2, baseName + "-" + i + "-" + j + ".tif");
					thisLevel.add(result);
					
					// Step 3b - tile result stripe
					File targetDir = new File(zoomLevelDir, Integer.toString(j));
					createDir(targetDir);
					generateTMSTiles(result, info, targetDir);
				} catch (Exception e) {
					throw new TilingException(e.getMessage());
				} 
			}
			
			for (Stripe s : levelBeneath) s.delete();
			levelBeneath = thisLevel;
			thisLevel = new ArrayList<Stripe>();
		}
		for (Stripe s : levelBeneath) s.delete();
		
		// Step 4 - generate tilemapresource.xml
		generateTilemapresourceXML(info);
		
		// Step 5 (optional) - generate OpenLayers preview
		if (generatePreview) {
			try {
				generatePreview(info);
			} catch (IOException e) {
				throw new TilingException("Error writing preview HTML: " + e.getMessage());
			}
		}

		log.info("Took " + (System.currentTimeMillis() - startTime) + " ms.");
		return info;
	}
	
	protected List<Stripe> stripeVertically(File image, TilesetInfo info, String outfilePrefix)
			throws IOException, InterruptedException, IM4JavaException {
	
		int canvasHeight = info.getHeight() + tileHeight - (info.getHeight() % tileHeight);
		processor.crop(image.getAbsolutePath(), 
				workingDirectory.getAbsolutePath() + File.separator + outfilePrefix + "%d.tif", 
				tileWidth, info.getHeight(), tileWidth, canvasHeight, ImageProcessor.GRAVITY_SOUTHWEST);

		// Assemble the list of Stripes
		List<Stripe> stripes = new ArrayList<Stripe>();
		for (int i=0; i<info.getNumberOfXTiles(0); i++) {
			// Somewhat risky to not check whether GM has generated all stripes correctly - but checking would take time...
			stripes.add(new Stripe(
					new File(workingDirectory, outfilePrefix + i + ".tif"),
					tileWidth, canvasHeight,
					Stripe.Orientation.VERTICAL)
			);
		}
		return stripes;
	}
	
	private void generateTMSTiles(Stripe stripe, TilesetInfo info, File targetDir)
			throws IOException, InterruptedException, IM4JavaException, TilingException {

		// Tile the stripe
		String filenamePattern = targetDir.getAbsolutePath() + File.separator + "tmp-%d." + 
			info.getTileFormat().getExtension();
		processor.crop(stripe.getImageFile().getAbsolutePath(), filenamePattern, tileWidth, tileHeight);

		// Rename result files (not nice, but seems to be the fastest way to do it)
		for (int i=0; i<(stripe.getHeight() / tileHeight); i++) {
			File fOld = new File(filenamePattern.replace("%d", Integer.toString(i)));
			File fNew = new File(filenamePattern.replace("tmp-%d", 
					Integer.toString((stripe.getHeight() / tileHeight) - i - 1)));
			if(!fOld.renameTo(fNew)) throw new TilingException("Failed to rename file:"+fOld);
		}
	}
	
	protected Stripe mergeStripes(Stripe stripe1, Stripe stripe2, String targetFile) 
			throws IOException, InterruptedException, IM4JavaException {
		
		int height = stripe1.getHeight() / 2;
		if ((stripe1.getHeight() / tileHeight) % 2 != 0) height += tileHeight / 2;
		
		if (stripe2 == null) {
			return stripe1.shrink(
					ImageProcessor.GRAVITY_SOUTHWEST, 
					tileWidth, 
					height,
					processor.getBackground(),
					new File(workingDirectory.getAbsolutePath() + File.separator + targetFile), 
					processor.getImageProcessingSystem());
		} else {
			return stripe1.merge(
					stripe2,
					ImageProcessor.GRAVITY_SOUTHWEST,
					tileWidth,
					height,
					processor.getBackground(),
					new File(workingDirectory.getAbsolutePath() + File.separator + targetFile),
					processor.getImageProcessingSystem());
		}
	}
	
	private void generateTilemapresourceXML(TilesetInfo info) {
		StringBuffer tilesets = new StringBuffer();
		for (int i=0; i<info.getZoomLevels(); i++) {
			tilesets.append(TILESET_TEMPLATE
					.replaceAll("@idx@", Integer.toString(i))
					.replaceAll("@unitsPerPixel@", Integer.toString((int)Math.pow(2, info.getZoomLevels() - i - 1)))
			);
		}
		
		String metadata = METADATA_TEMPLATE
			.replace("@title@", info.getImageFile().getName())
			.replace("@width@", Integer.toString(info.getWidth()))
			.replace("@height@", Integer.toString(info.getHeight()))
			.replace("@tilewidth@", Integer.toString(tileWidth))
			.replace("@tileheight@", Integer.toString(tileHeight))
			.replace("@mimetype@", info.getTileFormat().getMimeType())
			.replace("@ext@", info.getTileFormat().getExtension())
			.replace("@tilesets@", tilesets.toString());
		
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(tilesetRootDir, "tilemapresource.xml")));
			out.write(metadata);
		} catch (IOException e) {
			log.error("Error writing metadata XML: " + e.getMessage());
		} finally {
			if(out!=null)
				try {
					out.close();
				} catch (IOException e) {
					log.error("Error closing stream: " + e.getMessage());
				}
		}
	}
	
	private void generatePreview(TilesetInfo info) throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(this.getClass()
						.getResourceAsStream("tms-template.html")));
	
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			
			String html = sb.toString()
				.replace("@title@", info.getImageFile().getName())
				.replace("@width@", Integer.toString(info.getWidth()))
				.replace("@height@", Integer.toString(info.getHeight()))
				.replace("@maxZoom@", Integer.toString(info.getZoomLevels() - 1))
				.replace("@maxResolution@", Integer.toString((int) Math.pow(2, info.getZoomLevels() - 1)))
				.replace("@numZoomLevels@", Integer.toString(info.getZoomLevels()))
				.replace("@tilesetpath@", tilesetRootDir.getAbsolutePath().replace("\\", "/")+"/")
				.replace("@ext@", info.getTileFormat().getExtension());
			
			writeHtmlPreview(html);
		} finally {
			if(reader!=null) reader.close();
		}
	}
}
