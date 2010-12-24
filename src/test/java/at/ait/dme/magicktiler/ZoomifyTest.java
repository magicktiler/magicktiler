package at.ait.dme.magicktiler;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import at.ait.dme.magicktiler.zoomify.ZoomifyTiler;
import at.ait.dme.magicktiler.zoomify.ZoomifyValidator;

/**
 * Zoomify tiling tests
 * 
 * @author Rainer Simon <magicktiler@gmail.com>
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
		// Generate a Zoomify tileset from the test image
		MagickTiler t = new ZoomifyTiler();
		t.setWorkingDirectory(workingDir);
		t.setGeneratePreviewHTML(true);
		TilesetInfo info = t.convert(new File("src/test/resources/OrteliusWorldMap1570.jpg"));
		
		// Check if image metadata was read correctly
		assertEquals("Wrong width calculated for the Zoomify tileset!", 5816, info.getImageWidth());
		assertEquals("Wrong height calculated for the Zoomify tileset!", 3961, info.getImageHeight());
		
		// Check if tileset properties were computed correctly
		assertEquals("Wrong number of x-basetiles calculated for the Zoomify tileset!", 23, info.getNumberOfXTiles(0));
		assertEquals("Wrong number of y-basetiles calculated for the Zoomify tileset!", 16, info.getNumberOfYTiles(0));
		assertEquals("Wrong number of zoom levels calculated for the Zoomify tileset!", 6, info.getZoomLevels());
		assertEquals("Wrong number of tiles calculated for the Zoomify tileset!", 497, info.getTotalNumberOfTiles());
		
		// Check if tileset files were generated correctly
		File tilesetRoot = new File(workingDir, "OrteliusWorldMap1570");
		Collection<String> files = Arrays.asList(tilesetRoot.list());
		assertTrue("HTML preview missing!", files.contains("preview.html"));
		// Validate the tileset
		new ZoomifyValidator().validate(tilesetRoot);
	}
}
