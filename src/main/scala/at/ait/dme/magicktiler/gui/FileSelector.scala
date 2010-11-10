package at.ait.dme.magicktiler.gui

import javax.swing.JDialog
import javax.swing.SwingUtilities
import java.awt.Color
import javax.swing.JLabel
import javax.swing.ImageIcon
import java.io.File
import scala.swing._
import scala.swing.event._

/**
 * A simple file selector
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
class FileSelector(cols:Int, buttonText:String, selectionMode:FileChooser.SelectionMode.Value,
   validate:Boolean) extends FlowPanel {

  def this(cols:Int, validate:Boolean) =
    this(cols, "Browse", FileChooser.SelectionMode.FilesAndDirectories, validate)

  val selection = new TextField {
    columns = cols;
    reactions += {
      case EditDone(selection) => if (validate) validate()
      case ValueChanged(selection) =>
        errorPopup.setVisible(false)
        background = Color.WHITE
    }
  }
  listenTo(selection)

  val button = new Button {
    text = buttonText
    reactions += {
      case ButtonClicked(b) =>
      	errorPopup.setVisible(false)
        selection.background = Color.WHITE
        
        val input = new FileChooser
        input.fileSelectionMode = selectionMode
        if (input.showDialog(this, "") == FileChooser.Result.Approve) {
          selection.text = input.selectedFile.toString
          selection.requestFocusInWindow
        } else {
          selection.text = ""
        }
    }
  }

  val errorPopup = new JDialog() {
    val messageLabel = new JLabel("The file or directory specified does not exist! ")
    setUndecorated(true)
    getContentPane().add(messageLabel);
  }

  def validate():Boolean = {
    if (!new File(selection.text).exists) {
      selection.background = Color.PINK
      errorPopup.setSize(0, 0)
      errorPopup.setLocationRelativeTo(selection.peer)
      val point = errorPopup.getLocation()
      val dim = selection.size
      errorPopup.setLocation((point.x - dim.getWidth() / 2).toInt, (point.y + dim.getHeight() / 2).toInt)
      errorPopup.getContentPane().setBackground(Color.YELLOW);
      errorPopup.setFocusableWindowState(false);
      errorPopup.pack()
      errorPopup.setVisible(true)
      errorPopup.setAlwaysOnTop(true)
      return false
    } 
    return true
  }
  
  contents += selection
  contents += button
}