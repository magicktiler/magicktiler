package at.ait.dme.magicktiler;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

/**
 * KML tiling tests
 * 
 * @author Rainer Simon <magicktiler@gmail.com>
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class KMLTest extends BaseTest {

  /**
   * Define a custom working dir for this test
   */
  private File workingDir = new File("test/kml");

  @Before
  public void setUp() {
    deleteDir(workingDir);
  }

  @Test
  public void testKMLTiling() throws TilingException {
    /* Generate a KML tileset from the test image
    KMLSuperOverlayTiler t = new KMLSuperOverlayTiler();
    t.setWorkingDirectory(workingDir);
    t.setBoundingBox(new BoundingBox(48, 38, 20, 0));
    t.convert(new File("src/test/resources/OrteliusWorldMap1570.jpg"));
    */
  }
}
