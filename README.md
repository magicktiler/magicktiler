# MagickTiler - README

MagickTiler is a Java library and utility for converting image files into formats 
suitable for publishing them as high-resolution, zoomable Web images. It includes 
options for batch processing and quality control, and supports a wide range of 
image formats.

## Dependencies

MagickTiler uses GraphicsMagick to perform image manipulation (resizing, cropping,
etc.) Make sure you have GraphicsMagick installed on your system!

http://www.graphicsmagick.org/

For convenience, the free Zoomify Express Viewer is packaged with MagickTiler.
Note that this software has its own licensing terms defined in the accompanying
'license_Zoomify.txt' file in the 'zoomify' folder. You can always download the
latest version of this viewer for free at:

http://www.zoomify.com

## License

MagickTiler is licensed under the EUPL, Version 1.1 (http://ec.europa.eu/idabc/eupl).

IMPORTANT: The 'zoomify' folder contains a Flash (*.swf) file that is not part of the
MagickTiler software. This file is copyright by Zoomify Inc. and covered by its own 
licensing terms. Please refer to the accompanying Zoomify License Agreement
'license_Zoomify.txt' for details.

## Features

* Converts to TMS, Zoomify, and Google Maps tiling schemes
* Converts to Pyramid TIFF (PTIF) image format
* Fully embeddable in your own Java app
* Usable as a command-line utility 
* Extensible to other tiling schemes and formats

## CLI and GUI Usage

MagickTiler can be used from the command line prompt using the command
'java -jar magicktiler.jar', plus appropriate options. 

Example:

	java -jar magicktiler.jar -s tms -f jpeg -p images

The example command will create TMS tilesets (with JPEG tiles) for each file
in the folder 'images'. A preview HTML file will be added to each tileset. 

Command options:

    -h .... displays help text
    -i .... input file or directory
    -o .... output directory (for tilesets) or file (for PTIF)
    -s .... selects the tiling scheme ('tms', 'zoomify','gmap' or 'ptif')
    -f .... selects the tile image format ('jpeg' or 'png')
    -q .... sets the JPEG compression quality from 0 (low) to 100 (high)
    -b .... selects a background color (if applicable for the selected tile scheme)
    -p .... generates an HTML preview file
    -g .... starts the GUI
    -l .... writes all relevant reporting information to a log file
    -v .... validate instead of convert: checks existing tilesets and generates a report about their correctness/integrity
   
## Library Usage

MagickTiler can also be used as a Java library. Just add the magicktiler-lib-<version>.jar to your
build path and you're good to go. Here is a simple code example for generating Google Map tiles. 
It works in the same way for all other tiling schemes (just replace the GoogleMapsTiler with a 
TMSTiler, ZoomifyTiler, PTIFTiler, etc.).

```java
public static void main(String... args) {
    try {
        File input = new File("/path/to/your/image.jpg");
        File output = new File("/tileset/output/path");
    
        MagickTiler tiler = new GoogleMapsTiler();
        // optional settings (these are the default values)
        tiler.setTileFormat(ImageFormat.JPEG);
        tiler.setJPEGCompressionQuality(75);
        tiler.setBackgroundColor("#ffffffff");
        tiler.setWorkingDirectory(new File("."));
        tiler.setGeneratePreviewHTML(true);
              
        // create the tiles
        TilesetInfo info = tiler.convert(input, output);
    } catch (TilingException te) {
        te.printStackTrace();
    }
}
```

## Build Instructions

MagickTiler is built using Ant. The following build targets exist:

    - build:dist ... creates the MagickTiler distribution bundle
    - build:lib .... creates the embeddable library file without dependencies
    - test ......... runs JUnit tests
    - doc .......... generates JavaDoc

The distribution bundle includes an executable .jar file (called magicktiler.jar) with all 
dependencies included. Use this file in case you want to use MagickTiler as a command-line tool.

The MagickTiler embeddable library file (called magicktiler-lib-<version>.jar) is for use in 
your own Java applications. It does not include any external dependencies. To use MagickTiler 
in your own applications, you need to add the embeddable library .jar and the following 
dependencies to your project build path:

* im4java-1.1.0.jar
* log4j-1.2.15.jar
* commons-cli-1.2.jar
* xstream-1.3.1.jar
* scala-library.jar (optional)
* scala-swing.jar (optional)

All these files can be found in the /lib folder of the MagickTiler project. If you like to build 
the executable or the embeddable jar without the dependency to Scala, you will have to exclude 
the GUI from the build. You can achieve this by passing the ```-Dinclude.gui=false``` property to Ant.