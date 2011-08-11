package at.ait.dme.magicktiler.gmaps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

import scala.actors.threadpool.Arrays;
import at.ait.dme.magicktiler.TilesetInfo;
import at.ait.dme.magicktiler.ValidationFailedException;
import at.ait.dme.magicktiler.Validator;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Validator for the Google Maps tiling scheme.
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class GoogleMapsValidator implements Validator {
  private static Logger log = Logger.getLogger(GoogleMapsValidator.class);

  @Override
  public boolean isTilesetDir(File dir) {
    if (dir.isFile())
      return false;
    return (Arrays.asList(dir.list()).contains(GoogleMapsTiler.METADATA_FILE));
  }

  @Override
  public void validate(File dir) throws ValidationFailedException {
    if (!isTilesetDir(dir))
      throw new ValidationFailedException("Not a MagickTiler Google Maps tileset, "
          + "validation can not be continued.");

    TilesetInfo info = readMetadata(dir);
    if (info == null)
      throw new ValidationFailedException("Failed to read metadata");

    int filesVerified = 0;
    for (int z = 0; z < info.getZoomLevels(); z++) {
      for (int x = 0; x < info.getNumberOfXTiles(info.getZoomLevels() - 1 - z); x++) {
        for (int y = 0; y < info.getNumberOfYTiles(info.getZoomLevels() - 1 - z); y++) {
          String tile = z + "_" + x + "_" + y + "." + info.getFileExtension();
          if (!new File(dir.getAbsolutePath(), tile).exists())
            throw new ValidationFailedException("Files missing for zoom level " + z);

          filesVerified++;
        }
      }
    }

    if (filesVerified != info.getTotalNumberOfTiles())
      throw new ValidationFailedException("Not enough files generated for Tileset!");
  }

  private TilesetInfo readMetadata(File dir) throws ValidationFailedException {
    TilesetInfo info = null;
    FileInputStream metadata = null;
    try {
      metadata = new FileInputStream(dir.getAbsolutePath() + "/" + GoogleMapsTiler.METADATA_FILE);
      info = (TilesetInfo) new XStream(new DomDriver()).fromXML(metadata);
    } catch (FileNotFoundException e) {
      throw new ValidationFailedException("Metadata file not found!");
    } finally {
      try {
        if (metadata != null)
          metadata.close();
      } catch (IOException e) {
        log.error("Could not close metadata file!");
      }
    }
    return info;
  }
}
