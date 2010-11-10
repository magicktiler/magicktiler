package at.ait.dme.magicktiler.gui

import scala.swing._
import scala.swing.event._
import scala.actors.Actor

import at.ait.dme.magicktiler._
import java.io.File

/**
 * MagickTiler graphical user interface.
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 * @author magicktiler@gmail.com
 */
class MagickTilerGUI extends SimpleSwingApplication {
  val input:FileSelector = new FileSelector(25, true)
  val output:FileSelector = new FileSelector(25, false)
 
  val tilingSchemes:RadioButtonGroup = new RadioButtonGroup("TMS", "Zoomify", "PTIF")
  val tileFormats:RadioButtonGroup = new RadioButtonGroup("jpeg", "png")
  val jpegQuality:Slider = new Slider()
  	{min=0;max=100;value=75}
  
  val backgroundColor:TextField = new TextField("white", 10)
 
  val generatePreview:CheckBox = new CheckBox("")
  
  val progressBar:ProgressBar = new ProgressBar() 
  	{indeterminate=true;visible=false;preferredSize=new Dimension(300,20)}
  
  val startButton:Button = new Button("Create those tiles, dude!") {
    reactions += {
      case ButtonClicked(b) => if(input.validate) TilingActor ! "start"
    }
  }

  def top = new MainFrame {
    title = "MagickTiler GUI"
    minimumSize = new Dimension(630, 300)

    var x = 0; var y = 0;
    val gridPanel = new GridBagPanel {
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
      add(new FlowPanel { contents += backgroundColor; contents += new Label("(e.g. 'white', 'rgb(255,255,255)', '#FFFFFF')") })

      add(new Label("Generate HTML preview:"))
      add(generatePreview)

      addSeparator();
      add(startButton)
      add(progressBar)

      def add(component: Component, anc: GridBagPanel.Anchor.Value = GridBagPanel.Anchor.West) {
        var constraints = new this.Constraints
        constraints.anchor = anc
        constraints.gridx = x % 2
        constraints.gridy = y % 11

        add(component, constraints)
        x += 1; if (x % 2 == 0) y += 1
      }
     
      def addSeparator() {
    	var constraints = new this.Constraints
    	constraints.gridx = 0
        constraints.gridy = y
    	constraints.fill = GridBagPanel.Fill.Horizontal
    	constraints.gridwidth=2;

    	add(new Separator(), constraints)
        y=y+1
      }
    }
    contents = gridPanel
  }

  def startTiler() {
    startButton.enabled = false;
    progressBar.visible = true;
    
    var tiler: MagickTiler = null
    var inputFile: File = null
    var outputFile: File = null

    tilingSchemes.value match {
      case "TMS" => tiler = new TMSTiler()
      case "Zoomify" => tiler = new ZoomifyTiler()
      case "PTIF" => tiler = new PTIFConverter()
    }
    tileFormats.value match {
      case "jpeg" => tiler.setTileFormat(TileFormat.JPEG)
      case "png" => tiler.setTileFormat(TileFormat.PNG)
    }
    tiler.setJPEGCompressionQuality(jpegQuality.value)
    tiler.setBackgroundColor(backgroundColor.text)

    if (output.selection.text != null)
      outputFile = new File(output.selection.text)

    if (input.selection.text != null)
      inputFile = new File(input.selection.text)

    tiler.setGeneratePreviewHTML(generatePreview.selected);
    tiler.convert(inputFile, outputFile)
  
    progressBar.visible = false;
    startButton.enabled = true;
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