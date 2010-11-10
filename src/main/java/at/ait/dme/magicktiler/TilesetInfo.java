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

import java.io.File;
import java.util.ArrayList;

/**
 * Tileset data for a specified image file.
 * 
 * TODO add methods that provide information about 
 * individual zoom levels (xTiles, yTiles)
 * 
 * @author magicktiler@gmail.com
 */
public class TilesetInfo {
	
	/**
	 * Tile width and height for this tileset
	 */
	private int tileWidth;
	private int tileHeight;
	
	/**
	 * The tile file format
	 */
	private TileFormat format;
	
	/**
	 * Image information
	 */
	private ImageInfo imgInfo;
	
	/**
	 * Zoomlevel dimensions (starting with highest-resolution level)
	 */
	private ArrayList<Dimension> zoomlevels = new ArrayList<Dimension>();
		
	/**
	 * Total number of tiles in this set
	 */
	private int tilesTotal;
	
	public TilesetInfo(File image, int tileWidth, int tileHeight, TileFormat format, boolean useGraphicsMagick) throws TilingException {
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.format = format;
		this.imgInfo = new ImageInfo(image, useGraphicsMagick);
		
		// Compute no. of tiles in base layer
		int xBaseTiles = (int) Math.ceil((float) imgInfo.getWidth() / tileWidth);
		int yBaseTiles = (int) Math.ceil((float) imgInfo.getHeight() / tileHeight);
		zoomlevels.add(new Dimension(xBaseTiles, yBaseTiles));
		
		// Compute no. of zoom levels
		double maxTiles = (xBaseTiles > yBaseTiles) ? xBaseTiles : yBaseTiles;
		int numberOfZoomlevels = (int) Math.ceil(Math.log(maxTiles) / Math.log(2)) + 1;
		
		// Compute zoomlevel dimensions and total amount of tiles
		double x = xBaseTiles;
		double y = yBaseTiles;
		tilesTotal = xBaseTiles * yBaseTiles;
		for (int i=1; i<numberOfZoomlevels; i++) {
			x = Math.ceil(x / 2);
			y = Math.ceil(y / 2);
			tilesTotal += x * y;
			zoomlevels.add(new Dimension((int) x, (int) y));
		}
	}
		
	public File getImageFile() {
		return imgInfo.getImageFile();
	}
	
	public int getTileWidth() {
		return tileWidth;
	}
	
	public int getTileHeight() {
		return tileHeight;
	}
	
	public TileFormat getTileFormat() {
		return format;
	}
	
	public int getWidth() {
		return imgInfo.getWidth();
	}
	
	public int getHeight() {
		return imgInfo.getHeight();
	}
	
	public int getNumberOfXTiles(int zoomlevel) {
		return zoomlevels.get(zoomlevel).x;
	}
	
	public int getNumberOfYTiles(int zoomlevel) {
		return zoomlevels.get(zoomlevel).y;
	}

	public int getZoomLevels() {
		return zoomlevels.size();
	}
	
	public int getTotalNumberOfTiles() {
		return tilesTotal;
	}
	
	/**
	 * Simple wrapper for the 'dimension' of a zoomlevel (i.e. number of
	 * tiles in X/Y directions)
	 * 
	 * @author magicktiler@gmail.com
	 */
	private class Dimension {
	
		int x,y;
		
		Dimension(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
	}
	
}
