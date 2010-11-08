package at.ait.dme.magicktiler.gui

import scala.swing._

/**
 * A group of radio buttons
 * 
 * @author Christian Sadilek <christian.sadilek@gmail.com>
 */
class RadioButtonGroup(options:String*) extends FlowPanel {
	val initialButtons = for(o<-options) yield new RadioButton(o)
	val group = new ButtonGroup(initialButtons : _*)
	group.select(initialButtons.first);
	group.buttons.foreach((b) => contents+=b)
	
	def value = group.selected.get.text
}