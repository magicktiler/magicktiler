/*
 * Copyright 2010 Austrian Institute of Technology
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package at.ait.dme.magicktiler;

import java.io.File;
import java.util.HashMap;

/**
 * MagickTiler Command-line interface.
 * <br><br>
 * Example usage: <em>java -jar magicktiler.jar -s tms -f jpeg -p images</em><br><br>
 * The command will create TMS tilesets (with JPEG tiles) for each file in the
 * folder /images. A preview HTML file will be added to each tileset. 
 * <br><br>
 * Command options:<br>
 * -h   displays this help text<br>
 * -s   tiling scheme ('tms', 'zoomify' or 'ptif')<br>
 * -f   tile format ('jpeg' or 'png')<br>
 * -b   background color<bt>
 * -o   output directory (for tilesets) or file (for PTIF)<br>
 * -p   generate an HTML preview file
 * 
 * @author aboutgeo@no5.at
 */
public class MagickTilerCLI {
	
	/**
	 * Console output constants
	 */
	private static final String TARGET_SCHEME_TMS = "TMS tileset";
	private static final String TARGET_SCHEME_ZOOMIFY = "Zoomify tileset";
	private static final String TARGET_SCHEME_PTIF = "Pyramid TIFF";
	
	private static final String TARGET_FMT_JPEG = "(JPEG tiles)";
	private static final String TARGET_FMT_PNG = "(PNG tiles)";
	
	private static final String VERSION = "Version 0.1-SNAPSHOT";
	private static final String WEBSITE = "http://code.google.com/p/magicktiler";
	
	private static final String HELP =
		"MagickTiler " + VERSION + "\n" + 
		"Copyright (C) 2010 AIT Austrian Institute of Technology.\n" +
		"Additional licences apply to this software.\n" +
		"See " + WEBSITE + " for details.\n\n" +
		"  Example usage: java -jar magicktiler.jar -s tms -f jpeg -p image.tif\n\n" +
		"  Available options:\n" +
		"  -h   displays this help text\n" +
		"  -s   tiling scheme ('tms', 'zoomify' or 'ptif')\n" +
		"  -f   tile format ('jpeg' or 'png')\n" +
		"  -b   background color\n" +
		"  -o   output directory (for tilesets) or file (for PTIF)\n" +
		"  -p   generate an HTML preview file\n";
		
	/**
	 * @param args
	 * @throws TilingException 
	 */
	public static void main(String[] args) throws TilingException {
		if (args.length < 2) {
			System.out.println(HELP);
			return;
		}
	
		HashMap<String, String> argMap = parseArguments(args);

		// Tiling scheme (mandatory)
		String scheme = argMap.get("-s");
		if (scheme == null) {
			System.out.println("Please specify a tiling scheme using the -s option.");
			return;
		}
		
		String consoleOutScheme = null;
		String consoleOutFormat = "";
		MagickTiler tiler = null;
		if (scheme.equalsIgnoreCase("tms")) {
			tiler = new TMSTiler();
			consoleOutScheme = TARGET_SCHEME_TMS;
			consoleOutFormat = TARGET_FMT_JPEG;
		} else if (scheme.equalsIgnoreCase("zoomify")) {
			tiler = new ZoomifyTiler();
			consoleOutScheme = TARGET_SCHEME_ZOOMIFY;
			consoleOutFormat = TARGET_FMT_JPEG;
		} else if (scheme.equalsIgnoreCase("ptif")) {
			tiler = new PTIFConverter();
			consoleOutScheme = TARGET_SCHEME_PTIF;
		}
		
		if (tiler == null) {
			System.out.println("Unsupported tiling scheme: " + scheme);
			return;
		}
		
		// Tile format
		String format = argMap.get("-f");
		if (format != null && format.equalsIgnoreCase("png")) {
			tiler.setTileFormat(TileFormat.PNG); 
			consoleOutFormat = TARGET_FMT_PNG;
		}
		
		// Background color
		String background = argMap.get("-b");
		if (background != null) tiler.setBackgroundColor(background);
		
		// Destination
		File destination = null;
		if (argMap.containsKey("-o")) {
			destination = new File(argMap.get("-o"));
		}
		
		// HTML Preview
		tiler.setGeneratePreviewHTML(argMap.containsKey("-p"));
		
		// Filename
		File file = new File(args[args.length - 1]);
		if (!file.exists()) {
			System.out.println("File not found: " + file.getName());
			return;
		}
		
		// Generate tiles!
		long startTime = System.currentTimeMillis();
		System.out.println("Generating " + consoleOutScheme + " from file " + file.getAbsolutePath()+ " " + consoleOutFormat);
		if (destination != null) System.out.println("Destination: " + destination.getAbsolutePath());
		
		if (file.isFile()) {
			// Tile single file
			tiler.convert(file, destination);
		} else {
			// Tile folder full of files
			tiler.setWorkingDirectory(file);
			String files[] = file.list();
			for (int i=0; i<files.length; i++) {
				File child = new File(file, files[i]);
				if (child.isFile()) tiler.convert(child, destination);
			}
		}
		
		System.out.println("Done. Took " + (System.currentTimeMillis() - startTime) + " ms.");
	}
	
	private static HashMap<String, String> parseArguments(String[] args) {
		HashMap<String, String> argMap = new HashMap<String, String>();
		
		String key;
		String val = null;
		int ctr = 0;
		
		while (ctr < args.length) {
			if (args[ctr].startsWith("-")) {
				// Command parameter name
				key = args[ctr];
				if (ctr + 1 < args.length && !args[ctr + 1].startsWith("-")) {
					// Command parameter value (optional)
					val = args[ctr + 1];
					ctr ++;
				} else {
					val = null;
				}
				argMap.put(key, val);
			}
			ctr++;
		}
		
		return argMap;
	}

}
