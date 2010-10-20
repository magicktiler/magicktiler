package at.ait.dme.magicktiler;

import java.io.File;

import org.junit.Test;

import junit.framework.TestCase;

public class TMSTest extends TestCase {
	
	/**
	 * Define a custom working dir for this test
	 */
	private File workingDir = new File("test/tms");
	
	@Test 
	public void testTMSTiling() throws TilingException {
		// Delete the working dir
		deleteDir(workingDir);
		
		// Generate a TMS tileset from the test image
		MagickTiler t = new TMSTiler();
		t.setWorkingDirectory(workingDir);
		t.setGeneratePreviewHTML(true);
		// TilesetInfo info = t.convert(new File("src/test/resources/Hong_Kong_Night_Skyline.jpg"));
		TilesetInfo info = t.convert(new File("map.png"));

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
		String[] files = tilesetRoot.list();
		assertTrue("TMS tileset seems to be missing files!", files.length > 7);
		assertTrue("Wrong directory structure at top level!", checkTopLevel(files));
		assertTrue("Wrong directory structure in zoom level 0", checkZoomLevel(new File(tilesetRoot, "0"), 1, 1));
		assertTrue("Wrong directory structure in zoom level 1", checkZoomLevel(new File(tilesetRoot, "1"), 2, 1));
		assertTrue("Wrong directory structure in zoom level 2", checkZoomLevel(new File(tilesetRoot, "2"), 3, 1));
		assertTrue("Wrong directory structure in zoom level 3", checkZoomLevel(new File(tilesetRoot, "3"), 5, 2));
		assertTrue("Wrong directory structure in zoom level 4", checkZoomLevel(new File(tilesetRoot, "4"), 10, 4));
		assertTrue("Wrong directory structure in zoom level 5", checkZoomLevel(new File(tilesetRoot, "5"), 19, 8));
	}
	
	private boolean checkTopLevel(String[] files) {
		if (!files[0].equals("0")) return false;
		if (!files[1].equals("1")) return false;
		if (!files[2].equals("2")) return false;
		if (!files[3].equals("3")) return false;
		if (!files[4].equals("4")) return false;
		if (!files[5].equals("5")) return false;
		if (!files[6].equals("preview.html")) return false;
		if (!files[7].equals("tilemapresource.xml")) return false;
		return true;
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
	
	private void deleteDir(File path) {
	    if(path.exists()) {
	      File[] files = path.listFiles();
	      for (int i=0; i<files.length; i++) {
	         if(files[i].isDirectory()) {
	        	 deleteDir(files[i]);
	         } else {
	           files[i].delete();
	         }
	      }
	    }
	    path.delete();
	}

}
