# MagickTiler - README

MagickTiler is a utility for converting image files into formats suitable for 
publishing them as zoomable Web images. 

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

* Converts to TMS and Zoomify tiling schemes
* Converts to Pyramid TIFF (PTIF) image format
* Fully embeddable in your own Java app
* Usable as a command-line utility 
* Extensible to other tiling schemes and formats

## CLI Usage:

MagickTiler can be used from the command line prompt using the command
'java -jar magicktiler.jar', plus appropriate options. 

Example:

	java -jar magicktiler.jar -s tms -f jpeg -p images

The example command will create TMS tilesets (with JPEG tiles) for each file
in the folder 'images'. A preview HTML file will be added to each tileset. 

Command options:

     -h   displays help text
     -i   input file or directory
     -s   selects the tiling scheme ('tms', 'zoomify', 'gmap', or 'ptif')
     -f   selects the tile image format ('jpeg' or 'png')
     -b   selects a background color (if applicable for the selected tile scheme)
     -o   output directory (for tilesets) or file (for PTIF)
     -p   generates an HTML preview file
     -g   displays the GUI
     -o   output directory (for tilesets) or file (for PTIF)
     -q   JPEG compression quality (0 - 100)``
