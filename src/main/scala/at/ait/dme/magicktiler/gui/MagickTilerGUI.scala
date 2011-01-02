package at.ait.dme.magicktiler.gui

import java.awt.Desktop
import javax.swing.JOptionPane
import scala.swing._
import scala.swing.event._
import scala.actors.Actor

import at.ait.dme.magicktiler._
import at.ait.dme.magicktiler.image._
import at.ait.dme.magicktiler.gmaps._
import at.ait.dme.magicktiler.kml._
import at.ait.dme.magicktiler.ptif._
import at.ait.dme.magicktiler.tms._
import at.ait.dme.magicktiler.zoomify._

import java.io.File

/**
 * MagickTiler graphical user interface.
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 * @author magicktiler@gmail.com
 */
class MagickTilerGUI extends SimpleSwingApplication {
  val input: FileSelector = new FileSelector(25, true)
  val output: FileSelector = new FileSelector(25, false)

  val tilingSchemes: RadioButtonGroup = new RadioButtonGroup("TMS", "Zoomify", "GMAP", "PTIF")
  val tileFormats: RadioButtonGroup = new RadioButtonGroup("jpeg", "png")

  val jpegQuality: Slider = new Slider() { min = 0; max = 100; value = 75 }

  val backgroundColor: TextField = new TextField("white", 10)

  val generatePreview: CheckBox = new CheckBox("")

  val progressBar: ProgressBar = new ProgressBar() {
    indeterminate = true; visible = false; preferredSize = new Dimension(300, 20)
  }

  val startButton: Button = new Button("Create those tiles, dude!") {
    reactions += {
      case ButtonClicked(b) => if (input.validate) TilingActor ! "start"
    }
  }

  def top = new MainFrame {
    title = "MagickTiler GUI"
    minimumSize = new Dimension(630, 330)

    var x = 0; var y = 0;
    val gridPanel = new GridBagPanel {
      var rows = 11; var cols = 2;

      add(new Label("Input File or Directory:"))
      add(input)
     
      add(new Label("Output File or Directory:"))
      add(output)
      addSeparator();

      add(new Label("Tiling Scheme:"))
      add(tilingSchemes)

      add(new Label("Tile format:"))
      add(tileFormats)

      add(new Label("JPEG Quality:"))
      add(jpegQuality)
      addSeparator();

      add(new Label("Background color:"))
      add(new FlowPanel {
        contents += backgroundColor;
        contents += new Label("(e.g. 'white', 'rgb(255,255,255)', '#FFFFFF')")
      })

      add(new Label("Generate HTML preview:"))
      add(generatePreview)
      addSeparator();
      
      add(startButton)
      add(progressBar)

      def add(component: Component, anc: GridBagPanel.Anchor.Value = GridBagPanel.Anchor.West) {
        var constraints = new this.Constraints
        constraints.anchor = anc
        constraints.gridx = x % cols
        constraints.gridy = y % rows

        add(component, constraints)
        x += 1; if (x % cols == 0) y += 1
      }

      def addSeparator() {
        var constraints = new this.Constraints
        constraints.gridx = 0
        constraints.gridy = y
        constraints.fill = GridBagPanel.Fill.Horizontal
        constraints.gridwidth = cols;

        add(new Separator(), constraints)
        y += 1 
      }
    }
    contents = gridPanel
    input.button.requestFocus()
  }

  def startTiler() {
    startButton.enabled = false
    progressBar.visible = true

    var tiler: MagickTiler = null
    var inputFile: File = null
    var outputFile: File = null

    tilingSchemes.value match {
      case "TMS" => tiler = new TMSTiler()
      case "Zoomify" => tiler = new ZoomifyTiler()
      case "GMAP" => tiler = new GoogleMapsTiler()
      case "PTIF" => tiler = new PTIFConverter()
    }
    tileFormats.value match {
      case "jpeg" => tiler.setTileFormat(ImageFormat.JPEG)
      case "png" => tiler.setTileFormat(ImageFormat.PNG)
    }
    tiler.setJPEGCompressionQuality(jpegQuality.value)
    tiler.setBackgroundColor(backgroundColor.text)

    if (!output.selection.text.isEmpty) outputFile = new File(output.selection.text)
    if (input.selection.text != null) inputFile = new File(input.selection.text)

    tiler.setGeneratePreviewHTML(generatePreview.selected);
    try {
      tiler.convert(inputFile, outputFile)
      
      // display the result (either the directory or the HTML preview)
      var result = tiler.getTilesetRootDir().getAbsolutePath()
      if (generatePreview.selected) result += File.separator + "preview.html";
      Desktop.getDesktop.open(new File(result))
    } catch {
      case e =>
        JOptionPane.showMessageDialog(null, "Sorry, something went wrong here. " +
          "For now we only have these details:\n" + e.getMessage,
          "Error", JOptionPane.ERROR_MESSAGE)
    }
    finally {
      progressBar.visible = false;
      startButton.enabled = true;
    }
  }

  object TilingActor extends Actor {
    def act() {
      loop {
        react {
          case "start" => startTiler()
          case "stop" => exit()
        }
      }
    }
    start()
  }

  override def shutdown() {
    TilingActor ! "stop"
  }
}