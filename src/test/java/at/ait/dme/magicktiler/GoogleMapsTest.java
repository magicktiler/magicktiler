package at.ait.dme.magicktiler;

import java.io.File;

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
	public void testTMSTiling() throws TilingException {
		
		// Generate a Google Maps tileset from the test image
		MagickTiler t = new GoogleMapsTiler();
		t.setWorkingDirectory(workingDir);
		t.setGeneratePreviewHTML(true);
		TilesetInfo info = t.convert(new File("src/test/resources/Hong_Kong_Night_Skyline.jpg"));
	}
}
