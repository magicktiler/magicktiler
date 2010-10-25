package at.ait.dme.magicktiler;

import java.io.File;

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
	public void testTMSTiling() throws TilingException {	
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
	}
}
