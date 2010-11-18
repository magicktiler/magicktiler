package at.ait.dme.magicktiler.gmap;

import java.io.File;

import at.ait.dme.magicktiler.MagickTiler;
import at.ait.dme.magicktiler.TilesetInfo;
import at.ait.dme.magicktiler.TilingException;

/**
 * A tiler that implements the Google Maps tiling scheme
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class GoogleMapsTiler extends MagickTiler {

	//TODO finsish this implementation
			
	public GoogleMapsTiler() {

	}
	
	@Override
	protected void convert(File image, TilesetInfo info) throws TilingException {
		if (!workingDirectory.exists()) createDir(workingDirectory);
		
		// Store 'base name' (= filename without extension)
		String baseName = image.getName();
		baseName = baseName.substring(0, baseName.lastIndexOf('.'));
		createTargetDir(baseName);

	}

}
