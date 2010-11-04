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

    contents = new BoxPanel(Orientation.Vertical) {
    	contents+=new FileSelector("Input File or Directory:")
    	contents+=new FileSelector("Output File or Directory:");
    }
  }
}