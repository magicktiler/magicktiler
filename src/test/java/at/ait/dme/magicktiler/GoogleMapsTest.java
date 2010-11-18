package at.ait.dme.magicktiler;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import at.ait.dme.magicktiler.gmap.GoogleMapsTiler;

/**
 * Google Maps tiling tests
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class GoogleMapsTest extends BaseTest {
	//TODO finish this implementation
	
	private File workingDir = new File("test/gmap");
	
	@Before
	public void setUp() {
		deleteDir(workingDir);
	}
	
	@Test 
	public void testGoogleMapsTiling() throws TilingException {
		Collection<String> expectedTopLevel = 
			Arrays.asList(new String[]{"base.jpg"});
	
		// Generate a Google Maps tileset from the test image
		MagickTiler t = new GoogleMapsTiler();
		t.setWorkingDirectory(workingDir);
		t.setGeneratePreviewHTML(true);
		TilesetInfo info = t.convert(new File("src/test/resources/Hong_Kong_Night_Skyline.jpg"));
		
		// Check if tileset files were generated correctly
		File tilesetRoot = new File(workingDir, "Hong_Kong_Night_Skyline");
		assertTrue("Tileset root directory not found!", tilesetRoot.exists());
		Collection<String> files = Arrays.asList(tilesetRoot.list());
		assertEquals("TMS tileset seems to be missing files!", files.size(), expectedTopLevel.size());
		assertTrue("Wrong directory structure at top level!", files.containsAll(expectedTopLevel));
	}
}
