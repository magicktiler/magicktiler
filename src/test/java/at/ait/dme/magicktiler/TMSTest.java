package at.ait.dme.magicktiler;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import at.ait.dme.magicktiler.impl.TMSTiler;

/**
 * TMS tiling tests
 * 
 * @author magicktiler@gmail.com
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
		assertEquals("Wrong width calculated for the TMS tileset!", 4670, info.getImageWidth());
		assertEquals("Wrong height calculated for the TMS tileset!", 2000, info.getImageHeight());
		
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
		
		for(int i=0; i<info.getZoomLevels(); i++) {
			assertTrue("Wrong directory structure in zoom level "+i, 
					checkZoomLevel(info,
							new File(tilesetRoot, new Integer(i).toString()), 
							info.getNumberOfXTiles(info.getZoomLevels()-1-i), 
							info.getNumberOfYTiles(info.getZoomLevels()-1-i)));
		}
	}
	
	private boolean checkZoomLevel(TilesetInfo info, File zoomlevelRoot, int xTiles, int yTiles) {
		String ext = getFileExtension(info);
		
		String files[] = zoomlevelRoot.list();
		if (files.length != xTiles) return false;
		for (int col=0; col<xTiles; col++) {
			File colDir = new File(zoomlevelRoot, Integer.toString(col));
			if (!colDir.exists()) return false;
			String[] tiles = colDir.list();
			if (tiles.length != yTiles) return false;
			for (int row=0; row<yTiles; row++) {
				File tile = new File(colDir, Integer.toString(row)+"."+ext);
				if (!tile.exists()) return false;
			}
		}
		return true;
	}
}
