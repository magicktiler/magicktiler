package at.ait.dme.magicktiler;

import java.io.File;

import org.junit.Test;

import at.ait.dme.magicktiler.impl.PTIFConverter;

import junit.framework.TestCase;

/**
 * PTIF tiling tests
 * 
 * @author magicktiler@gmail.com
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class PTIFTest extends TestCase {

	@Test 
	public void testPTIFTiling() throws TilingException {		
		// Generate a Pyramid TIF from the test image
		File result = new File("test/pyramid-tif.ptif");
		
		PTIFConverter ptiffer = new PTIFConverter();
		TilesetInfo info = ptiffer.convert(
				new File("src/test/resources/Hong_Kong_Night_Skyline.jpg"),
				result
		);
		
		// Check if image metadata was read correctly
		assertEquals("Wrong width calculated for the PTIF tileset!", 4670, info.getImageWidth());
		assertEquals("Wrong height calculated for the PTIF tileset!", 2000, info.getImageHeight());
		
		// Check if tileset properties were computed correctly
		assertEquals("Wrong number of zoom levels calculated for the PTIF tileset!", 6, info.getZoomLevels());
		
		// Check if the file was created
		assertTrue(result.exists());
	}
}
