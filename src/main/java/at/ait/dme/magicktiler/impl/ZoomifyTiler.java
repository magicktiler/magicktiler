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

import at.ait.dme.magicktiler.MagickTiler;
import at.ait.dme.magicktiler.Stripe;
import at.ait.dme.magicktiler.TilesetInfo;
import at.ait.dme.magicktiler.TilingException;
import at.ait.dme.magicktiler.Stripe.Orientation;

/**
 * A tiler that implements the Zoomify tiling scheme.
 * <br><br>
 * <b>Developer info...</b><br>
 * <em>If you just want to generate Zoomify tiles and use them, and 
 * don't need to understand how Zoomify works internally - <b>just 
 * ignore this section!</b></em>
 * <br><br>
 * The Zoomify tiling scheme arranges tiles in the following folder/file
 * structure:
 * <br><br>
 * /tileset-root/TileGroup[group-no]/[zoomlevel]-[column]-[row].jpg
 * <br><br>
 * The highest-resolution zoom level has the highest number. Column/row
 * numbering of tiles starts top/left, counting direction is right/downwards.
 * <br><br>
 * Zoomify allows irregularly sized tiles on the border: I.e. the tiles in the
 * last (=right-most) column and in the last (=bottom-most) row do not need to 
 * be rectangular.
 * <br><br>
 * <b>A note on the TileGroups:</b> Zoomify does not put all tiles in one directory,
 * but splits them up into groups of 256 tiles max. (This is tricky...) TileGroup0
 * contains the 'first' 256 tiles, starting with tile 0-0-0 (the lowest-resolution
 * zoom level). TileSet1 contains the 'next' 256, and so on. Counting is done starting
 * from the lowest-resolution zoom level, and inside the zoom level tiles are counted
 * from left to right (and top to bottom - i.e. reading direction).
 * <br><br>
 * The implemented tiling algorithm works as follows:
 * <ol>
 * <li>The base image is cut into horizontal stripes.</li>
 * <li>The base image (highest-resolution zoom level) stripes are cut to tiles and
 * placed into the correct TileGroup folders.</li>
 * <li>For each additional zoom level, the stripes of the zoom level beneath 
 * are merged, cut to tiles, and placed in the correct TileGroup folder.</li>
 * <li>ImageProperties.xml is generated.</li>
 * <li>HTML preview file is generated (if requested).</li>
 * </ol>
 * 
 * @author magicktiler@gmail.com
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class ZoomifyTiler extends MagickTiler {
	public static final int MAX_TILES_PER_GROUP = 256;

	/**
	 * TileGroup string constant
	 */
	public static final String TILEGROUP = "TileGroup";
	
	/**
	 * XML descriptor file template 
	 */
	private static final String METADATA_TEMPLATE = 
		"<IMAGE_PROPERTIES WIDTH=\"@width@\" HEIGHT=\"@height@\" NUMTILES=\"@numtiles@\"" +
		" NUMIMAGES=\"1\" VERSION=\"1.8\" TILESIZE=\"@tilesize@\" />";

	/**
	 * Log4j logger
	 */
	private static Logger log = Logger.getLogger(ZoomifyTiler.class);
	
	@Override
	protected TilesetInfo convert(File image, TilesetInfo info) throws TilingException {
		long startTime = System.currentTimeMillis();
		log.info("Generating Zoomify tiles for file " + image.getName() + ": " +
				info.getWidth() + "x" + info.getHeight() + ", " +
                info.getNumberOfXTiles(0) + "x" + info.getNumberOfYTiles(0) + " basetiles, " +
                info.getZoomLevels() + " zoom levels, " +
                info.getTotalNumberOfTiles() + " tiles total"
		);
		
		String baseName = image.getName().substring(0, image.getName().lastIndexOf('.'));
				
		// Step 1 - stripe the base image
		log.debug("Striping base image");
		List<Stripe> baseStripes;
		try {
			baseStripes = stripeImage(image, Orientation.HORIZONTAL, 
					info.getNumberOfYTiles(0), info.getWidth(), tileHeight, baseName + "-0-");
		} catch (Exception e) {
			throw new TilingException(e.getMessage());
		} 
		
		// Step 2 - tile base image stripes
		log.debug("Tiling level 1");
		int zoomlevelStartIdx = info.getTotalNumberOfTiles() - info.getNumberOfXTiles(0) * info.getNumberOfYTiles(0);
		int offset = zoomlevelStartIdx;
		for (int i=0; i<baseStripes.size(); i++) {
			try {
				generateZoomifyTiles(baseStripes.get(i), 
						info.getZoomLevels() - 1,
						info.getNumberOfXTiles(0),
						offset,
						i);
				offset += info.getNumberOfXTiles(0);
			} catch (Exception e) {
				throw new TilingException(e.getMessage());
			}
		}
		
		// Step 3 - compute the pyramid
		List<Stripe> levelBeneath = baseStripes;
		List<Stripe> thisLevel = new ArrayList<Stripe>();
 
		for (int i=1; i<info.getZoomLevels(); i++) {
			log.debug("Tiling level " + (i + 1));
			zoomlevelStartIdx -= info.getNumberOfXTiles(i) * info.getNumberOfYTiles(i);
			offset = zoomlevelStartIdx;
			for(int j=0; j<Math.ceil((double)levelBeneath.size() / 2); j++) {
				try {
					// Step 3a - merge stripes from level beneath
					Stripe stripe1 = levelBeneath.get(j * 2);
					Stripe stripe2 = ((j * 2 + 1) < levelBeneath.size()) ? levelBeneath.get(j * 2 + 1) : null;
					Stripe result = mergeStripes(stripe1, stripe2, baseName + "-" + i + "-" + j + ".tif");
					thisLevel.add(result);
					
					// Step 3b - tile result stripe
					generateZoomifyTiles(result,
							info.getZoomLevels() - i -1, 
							info.getNumberOfXTiles(i),
							offset,
							j);
					offset += info.getNumberOfXTiles(i);
				} catch (Exception e) {
					throw new TilingException(e.getMessage());
				}
			}
			
			for (Stripe s: levelBeneath) s.delete();
			levelBeneath = thisLevel;
			thisLevel = new ArrayList<Stripe>();
		}

		for (Stripe s : levelBeneath) s.delete();
		
		// Step 4 - generate ImageProperties.xml file
		generateImagePropertiesXML(info);
		
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
	
	private void generateZoomifyTiles(Stripe stripe, int zoomlevel, int xTiles, int startIdx, int rowNumber) 
			throws IOException, InterruptedException, IM4JavaException, TilingException {
		
		String filenamePattern = tilesetRootDir + File.separator + "tmp-%d.jpg";
		processor.crop(stripe.getImageFile().getAbsolutePath(), filenamePattern, tileWidth, tileHeight);

		// Rename result files (not nice, but seems to be the fastest way to do it)
		for (int idx=0; idx<xTiles; idx++) {
			int tileGroup = (startIdx + idx) / MAX_TILES_PER_GROUP;
			File tileGroupDir = new File(tilesetRootDir.getAbsolutePath() + File.separator + TILEGROUP + tileGroup);
			if (!tileGroupDir.exists()) createDir(tileGroupDir);
			
			File fOld = new File(filenamePattern.replace("%d", Integer.toString(idx)));
			
			File fNew = new File(filenamePattern.replace("tmp-%d", 
					TILEGROUP + tileGroup + 
					File.separator + 
					Integer.toString(zoomlevel) + "-" + (idx % xTiles) + "-" + rowNumber));

			if(!fOld.renameTo(fNew)) throw new TilingException("Failed to rename file:"+fOld);
		}
	}
	
	private Stripe mergeStripes(Stripe stripe1, Stripe stripe2, String targetFile) 
		throws IOException, InterruptedException, IM4JavaException {
		
		if (stripe2 == null) {
			return stripe1.shrink(new File(workingDirectory.getAbsolutePath() + File.separator + targetFile), 
					processor.getImageProcessingSystem());
		} else {
			return stripe1.merge(stripe2, new File(workingDirectory.getAbsolutePath() + File.separator + targetFile), 
					processor.getImageProcessingSystem());
		}
	}
	
	private void generateImagePropertiesXML(TilesetInfo info) {
		String metadata = METADATA_TEMPLATE
			.replace("@width@", Integer.toString(info.getWidth()))
			.replace("@height@", Integer.toString(info.getHeight()))
			.replace("@numtiles@", Integer.toString(info.getTotalNumberOfTiles()))
			.replace("@tilesize@", Integer.toString(tileHeight));
		
		// Write to file
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(new File(tilesetRootDir, "ImageProperties.xml")));
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
						.getResourceAsStream("zoomify-template.html")));
		
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			
			String html = sb.toString()
				.replace("@title@", info.getImageFile().getName())
				.replace("@tileset@", ".")
				.replace("@playerPath@", "file://" + System.getProperty("user.dir").replace("\\", "/") + "/zoomify/");
			
			writeHtmlPreview(html);
		} finally {
			if(reader!=null) reader.close();
		}
	}
}
