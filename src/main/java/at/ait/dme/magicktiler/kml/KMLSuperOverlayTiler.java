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

package at.ait.dme.magicktiler.kml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.im4java.core.IM4JavaException;

import at.ait.dme.magicktiler.image.ImageProcessor;
import at.ait.dme.magicktiler.Stripe;
import at.ait.dme.magicktiler.TilesetInfo;
import at.ait.dme.magicktiler.TilingException;
import at.ait.dme.magicktiler.Stripe.Orientation;
import at.ait.dme.magicktiler.geo.BoundingBox;
import at.ait.dme.magicktiler.tms.TMSTiler;

/**
 * A tiler that generates a KML Superoverlay for Google Earth (unfinished!).
 * <br><br>
 * A KML Superoverlay is a hierarchy of regions and network links. Detail
 * information is here:
 * <br><br>
 * <a href="http://earth.google.com/kml/2.1">http://earth.google.com/kml/2.1</a>
 * <br><br>
 * Note: this KML Superoverlay implementation generates a standard TMS 
 * tile/directory structure, but adds appropriate KML files for each tile.
 * <br><br>
 * Additional Note: this implementation is currently UNFINISHED! 
 * <br><br>
 * TODO finish this implementation!
 * 
 * @author magicktiler@gmail.com
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class KMLSuperOverlayTiler extends TMSTiler {

	private static final String ROOT_KML_TEMPLATE = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
		"<kml xmlns=\"http://earth.google.com/kml/2.1\">\n" +
		"@network.link@" +
		"</kml>";

	private static final String TILE_KML_TEMPLATE =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
		"<kml xmlns=\"http://earth.google.com/kml/2.1\">\n" +
		"  <Document>\n" +
		"    <Region>\n" +
		"      <Lod>\n" +
		"        <minLodPixels>128</minLodPixels>\n" +
		"        <maxLodPixels>-1</maxLodPixels>\n" +
		"      </Lod>\n" +
		"      <LatLonAltBox>\n" +
		"        <north>@north@</north>\n" +
		"        <south>@south@</south>\n" +
		"        <east>@east@</east>\n" +
		"        <west>@west@</west>\n" +
		"      </LatLonAltBox>\n" +
		"    </Region>\n" +
		"@network.links@" +
		"    <GroundOverlay>\n" +
		"      <drawOrder>5</drawOrder>\n" +
		"      <Icon>\n" +
		"        <href>@img.href@</href>\n" +
		"      </Icon>\n" +
		"      <LatLonBox>\n" +
		"        <north>@north@</north>\n" +
		"        <south>@south@</south>\n" +
		"        <east>@east@</east>\n" +
		"        <west>@west@</west>\n" +
		"      </LatLonBox>\n" +
		"    </GroundOverlay>\n" +
		"  </Document>\n" +
		"</kml>";
	
	private static final String NETWORK_LINK_TEMPLATE =
		"    <NetworkLink>\n" +
		"      <name>@name@</name>\n" +
		"      <Region>\n" +
		"        <Lod>\n" +
		"          <minLodPixels>128</minLodPixels>\n" +
		"          <maxLodPixels>-1</maxLodPixels>\n" +
		"        </Lod>\n" +
		"        <LatLonAltBox>\n" +
		"          <north>@north@</north>\n" +
		"          <south>@south@</south>\n" +
		"          <east>@east@</east>\n" +
		"          <west>@west@</west>\n" +
		"        </LatLonAltBox>\n" +
		"      </Region>\n" +
		"      <Link>\n" +
		"        <href>@href@</href>\n" +
		"        <viewRefreshMode>onRegion</viewRefreshMode>\n" +
		"      </Link>\n" +
		"    </NetworkLink>\n";
	
	/**
	 * Log4j logger
	 */
	private static Logger log = Logger.getLogger(KMLSuperOverlayTiler.class);
	
	/**
	 * Geographical bounding box for this Superoverlay
	 */
	private BoundingBox bbox = null;
	
	@Override
	protected TilesetInfo convert(File image, TilesetInfo info) throws TilingException {
		if (bbox == null) throw new TilingException("No bounding box set!");
		
		long startTime = System.currentTimeMillis();
		log.info("Generating KML Superoverlay for file " + image.getName() + ": " +
                info.getImageWidth() + "x" + info.getImageHeight() + ", " +
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
			int canvasHeight = info.getImageHeight() + tileHeight - (info.getImageHeight() % tileHeight);
			baseStripes = stripeImage(image, Orientation.VERTICAL, 
					info.getNumberOfXTiles(0), tileWidth, info.getImageHeight(), 
					tileWidth, canvasHeight, ImageProcessor.GRAVITY_SOUTHWEST, basestripePrefix);
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
				generateLOD(baseStripes.get(i), info.getZoomLevels()-1, i);
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
					generateLOD(result, info.getZoomLevels()-i-1, j);
				} catch (Exception e) {
					throw new TilingException(e.getMessage());
				} 
			}
			
			for (Stripe s : levelBeneath) s.delete();
			levelBeneath = thisLevel;
			thisLevel = new ArrayList<Stripe>();
		}
		
		for (Stripe s : levelBeneath) s.delete();
		
		// Step 4 - generate the root KML file
		try {
			generateRootKMLFile(info);
		} catch (IOException e) {
			throw new TilingException(e.getMessage());
		}

		log.info("Took " + (System.currentTimeMillis() - startTime) + " ms.");		

		/*
		// Step 2 - tile base image stripes
		log.debug("Tiling level 1");
		int zoomlevelStartIdx = info.getTotalNumberOfTiles() - info.getNumberOfXTiles(0) * info.getNumberOfYTiles(0);
		int offset = zoomlevelStartIdx;
		for (int i=0; i<baseStripes.size(); i++) {
			try {
				generateLOD(
						baseStripes.get(i),
						info.getZoomLevels() - 1,
						info.getNumberOfXTiles(0),
						offset,
						i,
						tilesetRoot);
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
					generateLOD(
							result,
							info.getZoomLevels() - i -1, 
							info.getNumberOfXTiles(i),
							offset,
							j,
							tilesetRoot);
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
		*/	
		return info;
	}
	
	/**
	 * Sets the geographical bounding box for this Superoverlay
	 * @param bbox the bounding box
	 */
	public void setBoundingBox(BoundingBox bbox) {
		this.bbox = bbox;
	}
	
	private void generateLOD(Stripe stripe, int zoomlevel, int col) throws IOException, 
		InterruptedException, IM4JavaException, TilingException {
		
		// Tile the stripe
		String filenamePattern = tilesetRootDir.getAbsolutePath() + File.separator + "tmp-%d.jpg";
		processor.crop(stripe.getImageFile().getAbsolutePath(), filenamePattern, tileWidth, tileHeight);

		// Tile boundaries
		double width = bbox.getLonExtent() / Math.pow(2, zoomlevel);
		double height = bbox.getLatExtent() / Math.pow(2, zoomlevel);

		double north = bbox.getNorth();
		double west = bbox.getWest() + col * width;
		
		int rows = stripe.getHeight() / tileHeight;
		for (int i=0; i<rows; i++) {
			// Rename result files
			File fOld = new File(filenamePattern.replace("%d", Integer.toString(i)));
			File fNew = new File(filenamePattern.replace("tmp-%d", Integer.toString((stripe.getHeight() / tileHeight) - i - 1)));
			if(!fOld.renameTo(fNew)) throw new TilingException("Failed to rename file: " + fOld);
			
			// Generate KML
			generateTileKML(zoomlevel, col, rows - i - 1, north, west, width, height, fNew);
			north -= height;
		}
	}
	
	private void generateTileKML(int zoomlevel, int col, int row, double north, double west, double width, double height, File forTile) throws IOException {
		StringBuffer networkLinks = new StringBuffer();
		for (int x=0; x<2; x++) {
			for (int y=0; y<2; y++) {					
				String subRegion =
					"../../" +
					(zoomlevel + 1) + "/" +
					(col * 2 + x) + "/" +
					(row * 2 + y) + ".kml"; 
					
				networkLinks.append(NETWORK_LINK_TEMPLATE
						.replace("@name@", forTile.getName())
						.replace("@north@", Double.toString(north - height * y / 2))
						.replace("@south@", Double.toString(north - height * (1 + y) / 2))
						.replace("@west@", Double.toString(west + width * x / 2))
						.replace("@east@", Double.toString(west + width * (1 + x) / 2))
						.replace("@href@", subRegion));
			} 
		}
		
		String kml = TILE_KML_TEMPLATE
			.replace("@north@", Double.toString(north))
			.replace("@south@", Double.toString(north - height))
			.replace("@west@", Double.toString(west))
			.replace("@east@", Double.toString(west + width))
			.replace("@network.links@", networkLinks.toString())
			.replace("@img.href@", row + ".jpg");
		
		String file = forTile.getAbsolutePath();
		file = file.substring(0, file.lastIndexOf('.')) + ".kml";
		writeToFile(new File(file), kml);	
	}
	
	/*
	private void generateLOD(Stripe stripe, int zoomlevel, int xTiles, int startIdx, int rowNumber, File targetDirectory) throws IOException, InterruptedException, IM4JavaException {
		String filenamePattern = targetDirectory + File.separator + "tmp-%d.jpg";
		
		IMOperation op = new IMOperation();
		op.addImage(stripe.getImageFile().getAbsolutePath());
		op.crop(tileWidth, tileHeight);
		op.p_adjoin();
		op.addImage(filenamePattern);
		
		ConvertCmd convert = new ConvertCmd(useGraphicsMagick);
		convert.run(op);

		double dLat = bbox.getLatExtent() / Math.pow(2, zoomlevel);
		double dLon = bbox.getLonExtent() / Math.pow(2, zoomlevel);
		
		double north = bbox.getNorth() - rowNumber * dLat;
		double west = bbox.getWest();
		
		int colNumber;
		for (int idx=0; idx<xTiles; idx++) {
			// Rename result file
			int tileGroup = (startIdx + idx) / 256;
			File tileGroupDir = new File(targetDirectory.getAbsolutePath() + File.separator + TILEGROUP + tileGroup);
			if (!tileGroupDir.exists()) tileGroupDir.mkdir();
			
			colNumber = idx % xTiles;
			String filename = 
				Integer.toString(zoomlevel) + "-" + 
				Integer.toString(colNumber) + "-" + 
				Integer.toString(rowNumber);
			
			File fOld = new File(filenamePattern.replace("%d", Integer.toString(idx)));
			
			File fNew = new File(filenamePattern.replace("tmp-%d", 
					TILEGROUP + tileGroup + 
					File.separator + 
					filename));

			fOld.renameTo(fNew);
			
			// Generate KML
			StringBuffer networkLinks = new StringBuffer();
			
			for (int x=0; x<2; x++) {
				for (int y=0; y<2; y++) {
					String kmlfile =
						(zoomlevel + 1) + "-" +
						(colNumber * 2 + x) + "-" +
						(rowNumber * 2 + y); 
						
					networkLinks.append(NETWORK_LINK_TEMPLATE
							.replace("@name@", kmlfile)
							.replace("@north@", Double.toString(north - dLat * y / 2))
							.replace("@south@", Double.toString(north - dLat * (1 + y) / 2))
							.replace("@west@", Double.toString(west + dLon * x / 2))
							.replace("@east@", Double.toString(west + dLon * (1 + x) / 2))
							.replace("@href@", "../" + TILEGROUP + tileGroup + "/" + kmlfile + ".kml"));
				} 
			}
			
			String kml = TILE_KML_TEMPLATE
				.replace("@north@", Double.toString(north))
				.replace("@south@", Double.toString(north - dLat))
				.replace("@west@", Double.toString(west))
				.replace("@east@", Double.toString(west + dLon))
				.replace("@network.links@", networkLinks.toString())
				.replace("@img.href@", filename + ".jpg");
			
			String fKml = fNew.getAbsolutePath();
			fKml = fKml.substring(0, fKml.lastIndexOf('.')) + ".kml";
			writeToFile(new File(fKml), kml);
			
			west += dLon;
		}
	}
	*/
	
	private void generateRootKMLFile(TilesetInfo info) throws IOException {
		String name = info.getImageFile().getName();
		name = name.substring(0, name.lastIndexOf('.'));
		
		String networkLink = NETWORK_LINK_TEMPLATE
			.replace("@name@", name)
			.replace("@north@", Double.toString(bbox.getNorth()))
			.replace("@south@", Double.toString(bbox.getSouth()))
			.replace("@east@", Double.toString(bbox.getEast()))
			.replace("@west@", Double.toString(bbox.getWest()))
			.replace("@href@", "0/0/0.kml");

		writeToFile(new File(tilesetRootDir, name + ".kml"), ROOT_KML_TEMPLATE.replace("@network.link@", networkLink));
	}
	
	private void writeToFile(File f, String s) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(f));
	    out.write(s);
	    out.close();
	}
	
}
