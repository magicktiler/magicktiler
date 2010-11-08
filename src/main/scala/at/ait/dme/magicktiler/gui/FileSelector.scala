package at.ait.dme.magicktiler.gui

import scala.swing._
import scala.swing.event._

/**
 * A simple file selector
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
class FileSelector(cols:Int, buttonText:String, selectionMode:FileChooser.SelectionMode.Value) extends FlowPanel {
	
	def this(cols:Int) = this(cols, "Browse", FileChooser.SelectionMode.FilesAndDirectories)
    
	val selection = new TextField {
		columns = cols;
	}
    
    val button = new Button {
    	text = buttonText
    	reactions+= {
    		case ButtonClicked(b) => 
    			val input = new FileChooser
    			input.fileSelectionMode = selectionMode
    			if(input.showDialog(this,"")==FileChooser.Result.Approve)
    				selection.text = input.selectedFile.toString
    			else
    				selection.text = ""
    	}
    }
    
   contents+=selection
   contents+=button
}