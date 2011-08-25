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
import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import scala.actors.threadpool.Arrays;
import at.ait.dme.magicktiler.gmaps.GoogleMapsTiler;
import at.ait.dme.magicktiler.gmaps.GoogleMapsValidator;
import at.ait.dme.magicktiler.image.ImageFormat;
import at.ait.dme.magicktiler.ptif.PTIFConverter;
import at.ait.dme.magicktiler.tms.TMSTiler;
import at.ait.dme.magicktiler.zoomify.ZoomifyTiler;
import at.ait.dme.magicktiler.zoomify.ZoomifyValidator;

/**
 * MagickTiler Command-line interface.
 * <br><br>
 * Example usage: <em>java -jar magicktiler.jar -s tms -f jpeg -p -i images</em><br><br>
 * The command will create TMS tilesets (with JPEG tiles) for each file in the
 * folder /images. A preview HTML file will be added to each tileset. 
 * <br><br>
 * Command options:<br>
 * -h   displays this help text<br>
 * -g   displays the GUI<br>
 * -s   tiling scheme ('tms', 'zoomify', 'gmap' or 'ptif')<br>
 * -f   tile format ('jpeg' or 'png')<br>
 * -b   background color<br>
 * -i	input file or directory<br>
 * -o   output directory (for tilesets) or file (for PTIF)<br>
 * -q   JPEG compression quality (0 - 100)<br>
 * -p   generate an HTML preview file<br>
 * -l   writes reporting information to a log file<br>
 * -v   validate the input instead of generating a tileset
 * 
 * @author Rainer Simon <magicktiler@gmail.com>
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
public class MagickTilerCLI {
  private static final String TARGET_SCHEME_TMS = "TMS tileset";
  private static final String TARGET_SCHEME_ZOOMIFY = "Zoomify tileset";
  private static final String TARGET_SCHEME_GMAP = "Google Maps tileset";
  private static final String TARGET_SCHEME_PTIF = "Pyramid TIFF";
  private static final String TARGET_FMT_JPEG = "(JPEG tiles)";
  private static final String TARGET_FMT_PNG = "(PNG tiles)";

  private static final String VERSION = "Version 0.9";
  private static final String WEBSITE = "http://code.google.com/p/magicktiler";

  private static final String USAGE_HEADER = "MagickTiler " + VERSION + "\n"
      + "Copyright (C) 2010 AIT Austrian Institute of Technology.\n" + "Additional licences apply to this software.\n"
      + "See " + WEBSITE + " for details.\n";
  private static final String USAGE_FOOTER = "Example: java -jar magicktiler.jar -s tms -f jpeg -i image.tif -p";

  private static final Options options = new Options() {
    private static final long serialVersionUID = 8442627813822171704L;
    {
      addOption(new Option("s", "scheme", "mandatory tiling scheme ('tms', 'zoomify', 'gmap' or 'ptif')", true));
      addOption(new Option("i", "input", "mandatory input file or directory", true));
      addOption(new Option("o", "output", "output directory (for tilesets) or file (for PTIF), default=.", false));
      addOption(new Option("f", "format", "tile format ('jpeg' or 'png'), default=jpeg", false));
      addOption(new Option("q", "quality", "JPEG compression quality (0 - 100), default=75", false));
      addOption(new Option("b", "color", "background color, default=white", false));
      addOption(new Option("p", null, "generate an HTML preview file", false));
      addOption(new Option("g", null, "displays the GUI (ignores all other parameters)", false));
      addOption(new Option("h", null, "displays this help text", false));
      addOption(new Option("l", null, "writes reporting information to a log file", false));
      addOption(new Option("v", null, "validate the input instead of generating a tileset", false));
    }
  };

  private static final Logger logger = Logger.getLogger(MagickTilerCLI.class);

  public static void main(String... args) throws IOException {
    if (showGui(args))
      return;

    try {
      CommandLine cmd = new BasicParser().parse(options, args);

      // Help
      if (cmd.hasOption("h")) {
        printUsage(options);
        return;
      }

      // Log on/off
      if (cmd.hasOption("l")) {
        logger.addAppender(new FileAppender(new PatternLayout(), "log.txt", false));
        logger.setLevel(Level.DEBUG);
      }

      // Convert or validate
      if (cmd.hasOption("v")) {
        validate(cmd);
      } else {
        convert(cmd);
      }
    } catch (ParseException e) {
      System.err.println("Failed to parse command line arguments: " + e.getMessage());
      printUsage(options);
    }
  }

  private static void convert(CommandLine cmd) throws IOException {
    MagickTiler tiler = null;
    String consoleOutScheme = null;
    String consoleOutFormat = "";

    // Tiling scheme
    String scheme = cmd.getOptionValue("s");
    if (scheme.equalsIgnoreCase("tms")) {
      tiler = new TMSTiler();
      consoleOutScheme = TARGET_SCHEME_TMS;
      consoleOutFormat = TARGET_FMT_JPEG;
    } else if (scheme.equalsIgnoreCase("zoomify")) {
      tiler = new ZoomifyTiler();
      consoleOutScheme = TARGET_SCHEME_ZOOMIFY;
      consoleOutFormat = TARGET_FMT_JPEG;
    } else if (scheme.equalsIgnoreCase("gmap")) {
      tiler = new GoogleMapsTiler();
      consoleOutScheme = TARGET_SCHEME_GMAP;
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
    String format = cmd.getOptionValue("f");
    if (format != null && format.equalsIgnoreCase("png")) {
      tiler.setTileFormat(ImageFormat.PNG);
      consoleOutFormat = TARGET_FMT_PNG;
    }

    // JPEG compression quality
    String quality = cmd.getOptionValue("q");
    if (quality != null) {
      try {
        int q = Integer.parseInt(quality);
        if ((q < 0) || (q > 100)) {
          System.out.println("Invalid JPEG compression setting: " + q + " (must be in the range 0 - 100)");
          return;
        }
        tiler.setJPEGCompressionQuality(q);
      } catch (NumberFormatException e) {
        System.out.println("Invalid JPEG compression setting: " + quality);
        return;
      }
    }

    // Background color
    String background = cmd.getOptionValue("b");
    if (background != null)
      tiler.setBackgroundColor(background);

    // Destination
    File destination = null;
    if (cmd.hasOption("o")) {
      destination = new File(cmd.getOptionValue("o"));
    }

    // HTML Preview
    tiler.setGeneratePreviewHTML(cmd.hasOption("p"));

    // Input filename
    File file = new File(cmd.getOptionValue("i"));
    if (!file.exists()) {
      System.out.println("File not found: " + file.getName());
      return;
    }

    generateTiles(tiler, file, destination, consoleOutScheme, consoleOutFormat);
  }

  private static void validate(CommandLine cmd) {
    Validator validator = null;
    boolean includeFiles = true;

    // Tiling scheme
    String scheme = cmd.getOptionValue("s");
    if (scheme.equalsIgnoreCase("zoomify")) {
      validator = new ZoomifyValidator();
      includeFiles = false; // Zoomify is directory-based -> don't include files in validation
    } else if (scheme.equalsIgnoreCase("gmap")) {
      validator = new GoogleMapsValidator();
    }

    if (validator == null) {
      System.out.println("No validation support for tiling scheme: " + scheme);
      return;
    }

    // Input filename
    File file = new File(cmd.getOptionValue("i"));
    if (!file.exists()) {
      System.out.println("File not found: " + file.getName());
      return;
    }

    if (validator.isTilesetDir(file)) {
      // Validate single tileset
      if (file.isDirectory() || includeFiles) {
        try {
          validator.validate(file);
          logger.info("[OK] (" + scheme + ") " + file.getName());
        } catch (ValidationFailedException e) {
          logger.info("[CORRUPT] (" + scheme + ") " + file.getName() + ": " + e.getMessage());
        }
      }
    } else {
      // Try children
      File[] children = file.listFiles();
      for (int i = 0; i < children.length; i++) {
        if (children[i].isDirectory() || includeFiles) {
          try {
            validator.validate(children[i]);
            logger.info("[OK] (" + scheme + ") " + children[i].getName());
          } catch (ValidationFailedException e) {
            logger.info("[CORRUPT] (" + scheme + ") " + children[i].getName() + ": " + e.getMessage());
          }
        }
      }
    }
  }

  private static void generateTiles(MagickTiler tiler, File input, File destination, String consoleOutScheme,
      String consoleOutFormat) {

    long startTime = System.currentTimeMillis();
    System.out.println("Generating " + consoleOutScheme + " from file " + input.getAbsolutePath() + " "
        + consoleOutFormat);
    if (destination != null) {
      System.out.println("Destination: " + destination.getAbsolutePath());
    }

    if (input.isFile()) {
      // Tile single file
      try {
        tiler.convert(input, destination);
      } catch (TilingException e) {
        System.out.println(e.getMessage());
      }
    } else {
      // Tile folder full of files
      long ctrFiles = 0;
      long ctrTilesets = 0;
      tiler.setWorkingDirectory(input);
      String files[] = input.list();
      logger.info(files.length + " files/subdirs in folder");
      logger.info("--------------------------------------------------------------");
      for (int i = 0; i < files.length; i++) {
        File child = new File(input, files[i]);
        try {
          if (child.isFile()) {
            long tileStartTime = System.currentTimeMillis();
            ctrFiles++;
            tiler.convert(child, destination);
            ctrTilesets++;
            logger.info("[DONE] " + child.getName() + " (" + (System.currentTimeMillis() - tileStartTime) + " ms)");
          }
        } catch (TilingException e) {
          logger.info("[SKIPPED] " + child.getName() + " - " + e.getMessage());
        }
      }

      long duration = (System.currentTimeMillis() - startTime) / 60000;
      logger.info("--------------------------------------------------------------");
      logger.info(ctrFiles + " files processed");
      logger.info(ctrTilesets + " tilesets created (" + duration + " min)");
    }
  }

  private static boolean showGui(String... args) {
    boolean displayGui = false;
    try {
      if (displayGui = Arrays.asList(args).contains("-g")) {
        // the gui can be removed from the build, which is why 
        // we try to load it dynamically here.
        Class<?> gui = Class.forName("at.ait.dme.magicktiler.gui.MagickTilerGUI");
        Method startup = gui.getMethod("startup", new Class[] { String[].class });
        startup.invoke(gui.newInstance(), new Object[] { args });
      }
    } catch (Exception e) {
      System.err.println("Failed to start GUI (did you exclude it from the build?): " + e);
    }
    return displayGui;
  }

  private static void printUsage(Options options) {
    System.out.println(USAGE_HEADER);
    new HelpFormatter().printHelp("java -jar magicktiler", "", options, USAGE_FOOTER, true);
  }

  private static class Option extends org.apache.commons.cli.Option {
    private static final long serialVersionUID = 2457352966511905835L;

    public Option(String opt, String argName, String description, boolean required) {
      super(opt, (argName != null), description);
      this.setRequired(required);
      this.setArgName(argName);
    }
  }
}
