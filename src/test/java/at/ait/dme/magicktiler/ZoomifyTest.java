package at.ait.dme.magicktiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

/**
 * Zoomify tiling tests
 * 
 * @author aboutgeo@no5.at
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class ZoomifyTest extends BaseTest {
	
	/**
	 * Define a custom working dir for this test
	 */
	private File workingDir = new File("test/zoomify");
	
	@Before
	public void setUp() {
		deleteDir(workingDir);
	}
	
	@Test 
	public void testZoomifyTiling() throws Exception {	
		Collection<String> expectedTopLevel = new ArrayList<String>() {{
			add("ImageProperties.xml");
			add("preview.html");
		}};
	
		// Generate a TMS tileset from the test image
		MagickTiler t = new ZoomifyTiler();
		t.setWorkingDirectory(workingDir);
		t.setGeneratePreviewHTML(true);
		TilesetInfo info = t.convert(new File("src/test/resources/OrteliusWorldMap1570.jpg"));
		
		// Check if image metadata was read correctly
		assertEquals("Wrong width calculated for the Zoomify tileset!", 5816, info.getWidth());
		assertEquals("Wrong height calculated for the Zoomify tileset!", 3961, info.getHeight());
		
		// Check if tileset properties were computed correctly
		assertEquals("Wrong number of x-basetiles calculated for the Zoomify tileset!", 23, info.getNumberOfXTiles(0));
		assertEquals("Wrong number of y-basetiles calculated for the Zoomify tileset!", 16, info.getNumberOfYTiles(0));
		assertEquals("Wrong number of zoom levels calculated for the Zoomify tileset!", 6, info.getZoomLevels());
		assertEquals("Wrong number of tiles calculated for the Zoomify tileset!", 497, info.getTotalNumberOfTiles());
		
		// Check if tileset files were generated correctly
		int expectedNumberOfTileGroups = calculateExpectedNumberOfTileGroups(info.getTotalNumberOfTiles());
		for(int i=0; i<expectedNumberOfTileGroups; i++) {
			expectedTopLevel.add(ZoomifyTiler.TILEGROUP+i);
		}
		File tilesetRoot = new File(workingDir, "OrteliusWorldMap1570");
		assertTrue("Tileset root directory not found!", tilesetRoot.exists());
		Collection<String> files = Arrays.asList(tilesetRoot.list());
		assertEquals("TMS tileset seems to be missing files!", files.size(), expectedTopLevel.size());
		assertTrue("Wrong directory structure at top level!", files.containsAll(expectedTopLevel));
		
		// Check tilegroup directories
		for(String tileGroupDir : files) {
			if(tileGroupDir.contains(ZoomifyTiler.TILEGROUP)) {
				String[] tiles = new File(tilesetRoot+"/"+tileGroupDir).list();
				if(!tileGroupDir.equals(ZoomifyTiler.TILEGROUP+(expectedNumberOfTileGroups-1))) {
					// check for max tiles per group
					assertEquals("Wrong number of tiles in directory:" + tileGroupDir, 
							tiles.length, ZoomifyTiler.MAX_TILES_PER_GROUP);
				} else {
					// check for the remainder of the tiles in the last group
					assertEquals("Wrong number of tiles in directory:" + tileGroupDir, 
							tiles.length, info.getTotalNumberOfTiles() % ZoomifyTiler.MAX_TILES_PER_GROUP);
				}
			}
		}
	}
	
	private int calculateExpectedNumberOfTileGroups(int totalNumberOfTiles) {
		int numberOfTileGroups = 0;

		numberOfTileGroups = totalNumberOfTiles / ZoomifyTiler.MAX_TILES_PER_GROUP;
		if (totalNumberOfTiles % ZoomifyTiler.MAX_TILES_PER_GROUP > 0) numberOfTileGroups++;
		
		return numberOfTileGroups;
	}
}
