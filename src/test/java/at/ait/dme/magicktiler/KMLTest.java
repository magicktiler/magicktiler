package at.ait.dme.magicktiler;

import java.io.File;

import org.junit.Test;

import junit.framework.TestCase;

public class KMLTest extends TestCase {
	
	/**
	 * Define a custom working dir for this test
	 */
	private File workingDir = new File("test/kml");
	
	@Test 
	public void testTMSTiling() throws TilingException {
		// Delete the working dir
		deleteDir(workingDir);
		
		// Generate a TMS tileset from the test image
		KMLSuperOverlayTiler t = new KMLSuperOverlayTiler();
		t.setWorkingDirectory(workingDir);
		t.setBoundingBox(new BoundingBox(48, 38, 20, 0));
		t.convert(new File("src/test/resources/OrteliusWorldMap1570.jpg"));
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
