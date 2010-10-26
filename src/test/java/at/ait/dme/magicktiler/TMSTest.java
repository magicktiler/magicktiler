package at.ait.dme.magicktiler;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

/**
 * TMS tiling tests
 * 
 * @author aboutgeo@no5.at
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class TMSTest extends BaseTest {
	
	/**
	 * Define a custom working dir for this test
	 */
	private File workingDir = new File("test/tms");
	
	@Before
	public void setUp() {
		deleteDir(workingDir);
	}
	
	@Test 
	public void testTMSTiling() throws TilingException {
		Collection<String> expectedTopLevel = 
			Arrays.asList(new String[]{"0","1","2","3","4","5","preview.html","tilemapresource.xml"});
		
		// Generate a TMS tileset from the test image
		MagickTiler t = new TMSTiler();
		t.setWorkingDirectory(workingDir);
		t.setGeneratePreviewHTML(true);
		TilesetInfo info = t.convert(new File("src/test/resources/Hong_Kong_Night_Skyline.jpg"));

		// Check if image metadata was read correctly
		assertEquals("Wrong width calculated for the TMS tileset!", 4670, info.getWidth());
		assertEquals("Wrong height calculated for the TMS tileset!", 2000, info.getHeight());
		
		// Check if tileset properties were computed correctly
		assertEquals("Wrong number of x-basetiles calculated for the TMS tileset!", 19, info.getNumberOfXTiles(0));
		assertEquals("Wrong number of y-basetiles calculated for the TMS tileset!", 8, info.getNumberOfYTiles(0));
		assertEquals("Wrong number of zoom levels calculated for the TMS tileset!", 6, info.getZoomLevels());
		assertEquals("Wrong number of tiles calculated for the TMS tileset!", 208, info.getTotalNumberOfTiles());
		
		// Check if tileset files were generated correctly
		File tilesetRoot = new File(workingDir, "Hong_Kong_Night_Skyline");
		assertTrue("Tileset root directory not found!", tilesetRoot.exists());
		Collection<String> files = Arrays.asList(tilesetRoot.list());
		assertEquals("TMS tileset seems to be missing files!", files.size(), expectedTopLevel.size());
		assertTrue("Wrong directory structure at top level!", files.containsAll(expectedTopLevel));
		assertTrue("Wrong directory structure in zoom level 0", checkZoomLevel(new File(tilesetRoot, "0"), 1, 1));
		assertTrue("Wrong directory structure in zoom level 1", checkZoomLevel(new File(tilesetRoot, "1"), 2, 1));
		assertTrue("Wrong directory structure in zoom level 2", checkZoomLevel(new File(tilesetRoot, "2"), 3, 1));
		assertTrue("Wrong directory structure in zoom level 3", checkZoomLevel(new File(tilesetRoot, "3"), 5, 2));
		assertTrue("Wrong directory structure in zoom level 4", checkZoomLevel(new File(tilesetRoot, "4"), 10, 4));
		assertTrue("Wrong directory structure in zoom level 5", checkZoomLevel(new File(tilesetRoot, "5"), 19, 8));
	}
	
	private boolean checkZoomLevel(File zoomlevelRoot, int xTiles, int yTiles) {
		String files[] = zoomlevelRoot.list();
		if (files.length != xTiles) return false;
		for (int i=0; i<files.length; i++) {
			File colDir = new File(zoomlevelRoot, Integer.toString(i));
			if (!colDir.exists()) return false;
			String[] tiles = colDir.list();
			if (tiles.length != yTiles) return false;
		}
		return true;
	}
}
