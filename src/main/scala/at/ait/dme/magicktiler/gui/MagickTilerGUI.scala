package at.ait.dme.magicktiler.gui

import scala.swing._
import scala.swing.event._
import at.ait.dme.magicktiler._

/**
 * MagickTiler graphical user interface.
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
class MagickTilerGUI extends SimpleSwingApplication {
  val input:FileSelector = new FileSelector(25)
  val output:FileSelector = new FileSelector(25)
  val tilingSchemes:RadioButtonGroup = new RadioButtonGroup("TMS", "Zoomify", "PTIF")
  val tileFormats:RadioButtonGroup = new RadioButtonGroup("jpeg", "png")
  val backgroudColor:TextField = new TextField("white", 10)
  val generatePreview:CheckBox = new CheckBox("");
  val startButton:Button = new Button("Create those tiles, dude!") {
    	reactions+={
    		case ButtonClicked(b) => startTiler
    	}
  }
  
  def top = new MainFrame {
    title = "MagickTiler GUI"
    minimumSize = new Dimension(620, 230)
    
    var x=0; var y=0;
    val gridPanel = new GridBagPanel {
    	add(new Label("Input File or Directory:"))
	    add(input)
	    
	    add(new Label("Output File or Directory:"))
	    add(output)
	
	    add(new Label("Tiling Scheme:"))
	    add(tilingSchemes)
	    
	    add(new Label("Tile format:"))
	    add(tileFormats)
	
	    add(new Label("Background color:"))
	    add(new FlowPanel {contents+=backgroudColor;contents+=new Label("(e.g. 'white', 'rgb(255,255,255)', '#FFFFFF')")})
	    
	    add(new Label("Generate HTML preview:"))
	    add(generatePreview)
	    
	    add(new Label(""))
	    add(startButton)
	
	    def add(component:Component, anc:GridBagPanel.Anchor.Value = GridBagPanel.Anchor.West) {
	    	var constraints = new this.Constraints
	    	constraints.anchor = anc
	    	constraints.gridx = x % 2
	    	constraints.gridy = y % 8	    	
	    	
	    	add(component, constraints)
	    	
	    	x+=1;if(x%2==0) y+=1
	    }
    }
    contents = gridPanel
  }
  
  def startTiler() {
	  var tiler:MagickTiler = null;
	  println("starttiler:"+tilingSchemes.group.selected.get.text)
  }
}