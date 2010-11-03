package at.ait.dme.magicktiler.gui

import scala.swing.Button
import scala.swing.MainFrame
import scala.swing.Frame
import scala.swing.SimpleSwingApplication

/**
 * MagickTiler graphical user interface.
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
class MagickTilerGUI extends SimpleSwingApplication {

  def top = new MainFrame {
    title = "MagickTiler GUI"
    contents = new Button { text = "Click me"}
  }
}