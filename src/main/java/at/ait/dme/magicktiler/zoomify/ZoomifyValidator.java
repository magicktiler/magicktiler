package at.ait.dme.magicktiler.zoomify;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.ait.dme.magicktiler.ValidationFailedException;
import at.ait.dme.magicktiler.Validator;

/**
 * Validation class for the Zoomify tiling scheme.
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 * @author Rainer Simon <magicktiler@gmail.com>
 */
public class ZoomifyValidator implements Validator {
	
	/**
	 * File name of the descriptor file
	 */
	private static final String IMAGE_PROPERTIES = "ImageProperties.xml";
	
	/**
	 * Number of expected TileGroup directories
	 */
	private int tileGroups;
	
	/**
	 * Number of tiles in the last TileGroup
	 */
	private int tilesInLastGroup;
	
	/**
	 * Tile size
	 */
	private int tileSize;
	
	/**
	 * Number of tiles in the base layer in x-direction
	 */
	private List<Integer> xTiles = new ArrayList<Integer>();
	
	/**
	 * Number of tiles in the base layer in y-direction
	 */
	private List<Integer> yTiles = new ArrayList<Integer>();
	
	/**
	 * Number of zoomlevels in this tileset
	 */
	private int zoomLevels;
	
	@Override
	public boolean isTilesetDir(File dir) {
		if (dir.isFile())
			return false;
		
		return Arrays.asList(dir.list()).contains(IMAGE_PROPERTIES); 
	}
	
	@Override
	public void validate(File dir) throws ValidationFailedException {
		if (dir.isFile())
			throw new ValidationFailedException("Not a zoomify tileset");
		
		List<String> children = Arrays.asList(dir.list());

		if (!children.contains(IMAGE_PROPERTIES)) 
			throw new ValidationFailedException("Not a Zoomify tileset - missing ImageProperties.xml");
		
		try {
			BufferedReader r = new BufferedReader(
					new FileReader(new File(dir, children.get(children.indexOf(IMAGE_PROPERTIES)))));
			
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = r.readLine()) != null) {
				sb.append(line);
			}
			
			parseImageProperties(sb.toString());
			checkTileDirectories(dir);
		} catch (IOException e) {
			throw new ValidationFailedException(e.getMessage());
		}
	}
	
	/**
	 * Parses the Zoomify XML descriptor. Example:
	 * 
	 * <IMAGE_PROPERTIES WIDTH="1414" HEIGHT="1100" NUMTILES="44" 
	 *                   NUMIMAGES="1" VERSION="1.8" TILESIZE="256" />
	 *                   
	 * @param xml
	 */
	private void parseImageProperties(String xml) throws ValidationFailedException {
		xml = xml.toLowerCase();
		int beginIdx, endIdx;
		
		try {
			// Width
			beginIdx = xml.indexOf("width=") + 7;
			endIdx = xml.indexOf("\"", beginIdx + 1);
			int width = Integer.parseInt(xml.substring(beginIdx, endIdx));
			
			// Height
			beginIdx = xml.indexOf("height=") + 8;
			endIdx = xml.indexOf("\"", beginIdx + 1);
			int height = Integer.parseInt(xml.substring(beginIdx, endIdx));
			
			// Numtiles
			beginIdx = xml.indexOf("numtiles=") + 10;
			endIdx = xml.indexOf("\"", beginIdx + 1);
			int numtiles = Integer.parseInt(xml.substring(beginIdx, endIdx));
			
			// Tilesize
			beginIdx = xml.indexOf("tilesize=") + 10;
			endIdx = xml.indexOf("\"", beginIdx + 1);
			tileSize = Integer.parseInt(xml.substring(beginIdx, endIdx));
			
			tileGroups = (int) Math.ceil((double) numtiles / ZoomifyTiler.MAX_TILES_PER_GROUP);
			tilesInLastGroup = (int) ((double) numtiles % tileSize);

			double xBaseTiles = (int) Math.ceil((float) width / tileSize);
			double yBaseTiles = (int) Math.ceil((float) height / tileSize);
			
			double maxTiles = (xBaseTiles > yBaseTiles) ? xBaseTiles : yBaseTiles;
			zoomLevels = (int) Math.ceil(Math.log(maxTiles) / Math.log(2)) + 1;
			
			xTiles.clear();
			yTiles.clear();
			double x = xBaseTiles;
			double y = yBaseTiles; 
			for (int i=0; i<zoomLevels; i++) {
				xTiles.add(new Integer((int) x));
				yTiles.add(new Integer((int) y));
				
				x = (int) Math.ceil(x / 2);
				y = (int) Math.ceil(y / 2);
			}
		} catch (Throwable t) {
			throw new ValidationFailedException("Ill-formed descriptor file: " + t.getMessage());
		}
	}
	
	private void checkTileDirectories(File tilesetDir) throws ValidationFailedException {
		Map<Integer, Collection<String>> allTiles = new HashMap<Integer, Collection<String>>();
		
		String[] children = tilesetDir.list();
		for (int i=0; i<children.length; i++) {
			if (children[i].contains(ZoomifyTiler.TILEGROUP)) {
				String[] tiles = new File(tilesetDir, children[i]).list();
				int tileGroup = Integer.parseInt(children[i].substring(ZoomifyTiler.TILEGROUP.length()));
				if (tileGroup < tileGroups - 2) {
					// check for max tiles per group
					if (tiles.length < tileSize)
						throw new ValidationFailedException(
							"Missing tiles in directory " + children[i] + " (" + tiles.length + " instead of " + 
								ZoomifyTiler.MAX_TILES_PER_GROUP + ")");
				} else {
					// check for the remainder of the tiles in the last group
					if (tiles.length < tilesInLastGroup)
						throw new ValidationFailedException(
							"Missing tiles in directory " + children[i] + " (" + tiles.length + " instead of " + 
								tilesInLastGroup + ")");
				}
				allTiles.put(tileGroup, Arrays.asList(tiles));
			}
		}
		checkForEachTile(allTiles);
	}
	
	private void checkForEachTile(Map<Integer, Collection<String>> allTiles) throws ValidationFailedException {	
		int tile = 0;
		for (int zoomLevel=zoomLevels-1; zoomLevel>=0; zoomLevel--) {
			for (int row=0; row<yTiles.get(zoomLevel); row++) {
				for(int col=0; col<xTiles.get(zoomLevel); col++,tile++) {
					String tileName = (zoomLevels - 1 - zoomLevel) + "-" + col + "-" + row + ".jpg";
					Collection<String> tiles = allTiles.get(tile / ZoomifyTiler.MAX_TILES_PER_GROUP);
					if(!tiles.contains(tileName)) {
						throw new ValidationFailedException("Missing tile: " + tileName);
					}
				}
			}
		}
	}
}
