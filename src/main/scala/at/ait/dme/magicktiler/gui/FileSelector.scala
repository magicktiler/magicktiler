package at.ait.dme.magicktiler.gui

import scala.swing._
import scala.swing.event._

/**
 * A simple file selector
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
class FileSelector(labelText:String, buttonText:String, selectionMode:FileChooser.SelectionMode.Value) extends FlowPanel {
	
	def this(labelText:String) = this(labelText, "Browse", FileChooser.SelectionMode.FilesAndDirectories)
	
	private val label = new Label {
    	text = labelText
    	preferredSize = new Dimension(170,15)
    }
    
	private val selection = new TextField {
    	columns = 25
    }
    
    private val button = new Button {
    	text = buttonText
    	reactions+= {
    		case ButtonClicked(inputButton) => 
    			val input = new FileChooser
    			input.fileSelectionMode = selectionMode
    			if(input.showDialog(this,"")==FileChooser.Result.Approve)
    				selection.text = input.selectedFile.toString
    			else
    				selection.text = ""
    	}
    }
    
    contents+=label
    contents+=selection
    contents+=button 
}