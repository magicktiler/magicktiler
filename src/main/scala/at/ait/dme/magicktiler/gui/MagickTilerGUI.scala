package at.ait.dme.magicktiler.gui

import scala.swing._
import scala.swing.event._

/**
 * MagickTiler graphical user interface.
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
class MagickTilerGUI extends SimpleSwingApplication {

  def top = new MainFrame {
    title = "MagickTiler GUI"
    minimumSize = new Dimension(600,200)
      
    val gridPanel = new GridBagPanel {
    	add(new Label("Input File or Directory:"), 0, 0)
	    add(new FileSelector(25), 1, 0)
	    
	    add(new Label("Output File or Directory:"), 0, 1)
	    add(new FileSelector(25), 1, 1)
	
	    add(new CheckBox("Generate HTML preview"), 1, 2)

	    add(new Label("Tiling Scheme:"), 0, 3)
	    add(new RadioButtonGroup("TMS", "Zoomify", "PTIF"), 1, 3)
	    
	    add(new Label("Tile format:"), 0, 4)
	    add(new RadioButtonGroup("jpeg", "png"), 1, 4)
		
	    def add(component:Component, x:Int, y:Int, anc:GridBagPanel.Anchor.Value = GridBagPanel.Anchor.West) {
	    	var constraints = new this.Constraints
	    	constraints.anchor = anc
	    	constraints.gridx = x
	    	constraints.gridy = y
	    	add(component, constraints)
	    }
    }
    contents = gridPanel
  }
}