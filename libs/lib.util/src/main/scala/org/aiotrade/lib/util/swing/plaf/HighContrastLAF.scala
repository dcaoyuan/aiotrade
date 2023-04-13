/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.lib.util.swing.plaf;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.Properties;
import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalIconFactory;
import javax.swing.plaf.metal.MetalLookAndFeel;

object HighContrastLAF {
  val properties = new Properties
  try {
    properties.load(classOf[HighContrastLAF].getResourceAsStream("WillaHighContracstLAF.properties"))
  } catch {case ioe:IOException => println("Could not load HighContrastLAF.properties.")}
  MetalLookAndFeel.setCurrentTheme(new HighContrastTheme)

}

class HighContrastLAF extends MetalLookAndFeel {
  import HighContrastLAF._
  
  override def getName: String = {
    properties.getProperty("name")
  }

  override def getDescription: String =  {
    properties.getProperty("description")
  }

  override def getID: String = {
    getClass.getName
  }

  override protected def initComponentDefaults(table:UIDefaults): Unit = {
    super.initComponentDefaults(table)
    var iconMagnification = 1.0
    try {
      iconMagnification = properties.getProperty("iconMagnificationFactor", "1").toDouble
    } catch {case ex: Exception =>}

    val defaults = Array[Object](
      "ComboBox.selectionForeground", MetalLookAndFeel.getHighlightedTextColor,
      "Panel.font", MetalLookAndFeel.getControlTextFont,
      "CheckBox.icon", new MagnifiedIcon(MetalIconFactory.getCheckBoxIcon, iconMagnification),
      "RadioButton.icon", new MagnifiedIcon(MetalIconFactory.getRadioButtonIcon, iconMagnification),
      "Menu.checkIcon", new MagnifiedIcon(MetalIconFactory.getMenuItemCheckIcon, iconMagnification),
      "Menu.arrowIcon", new MagnifiedIcon(MetalIconFactory.getMenuArrowIcon, iconMagnification),
      "CheckBoxMenuItem.checkIcon", new MagnifiedIcon(MetalIconFactory.getCheckBoxMenuItemIcon, iconMagnification),
      "CheckBoxMenuItem.arrowIcon", new MagnifiedIcon(MetalIconFactory.getMenuItemArrowIcon, iconMagnification),
      "RadioButtonMenuItem.checkIcon", new MagnifiedIcon(MetalIconFactory.getRadioButtonMenuItemIcon, iconMagnification),
      "RadioButtonMenuItem.arrowIcon", new MagnifiedIcon(MetalIconFactory.getMenuItemArrowIcon, iconMagnification),
      "Tree.openIcon", new MagnifiedIcon(MetalIconFactory.getTreeFolderIcon, iconMagnification),
      "Tree.closedIcon", new MagnifiedIcon(MetalIconFactory.getTreeFolderIcon, iconMagnification),
      "Tree.leafIcon", new MagnifiedIcon(MetalIconFactory.getTreeLeafIcon, iconMagnification),
      "Tree.expandedIcon", new MagnifiedIcon(MetalIconFactory.getTreeControlIcon(MetalIconFactory.DARK), iconMagnification),
      "Tree.collapsedIcon", new MagnifiedIcon(MetalIconFactory.getTreeControlIcon(MetalIconFactory.LIGHT), iconMagnification),
      "FileChooser.detailsViewIcon", new MagnifiedIcon(MetalIconFactory.getFileChooserDetailViewIcon, iconMagnification),
      "FileChooser.homeFolderIcon", new MagnifiedIcon(MetalIconFactory.getFileChooserHomeFolderIcon, iconMagnification),
      "FileChooser.listViewIcon", new MagnifiedIcon(MetalIconFactory.getFileChooserListViewIcon, iconMagnification),
      "FileChooser.newFolderIcon", new MagnifiedIcon(MetalIconFactory.getFileChooserNewFolderIcon, iconMagnification),
      "FileChooser.upFolderIcon", new MagnifiedIcon(MetalIconFactory.getFileChooserUpFolderIcon, iconMagnification)
    )
    table.putDefaults(defaults)
  }


  /** A class to create a magnified version of an existing icon */
  protected class MagnifiedIcon(icon:Icon, factor:Double) extends Icon {

    def getIconWidth: Int = {
      (icon.getIconWidth * factor).toInt
    }

    def getIconHeight: Int = {
      (icon.getIconHeight * factor).toInt
    }

    def paintIcon(c: Component, g: Graphics, x: Int, y: Int): Unit = {
      val g2d = g.create.asInstanceOf[Graphics2D]
      g2d.translate(x, y)
      g2d.scale(factor, factor)
      icon.paintIcon(c, g2d, 0, 0)
      g2d.dispose
    }
  }
}

protected object HighContrastTheme {
  private def getColor(key: String): ColorUIResource = {
    new ColorUIResource(Integer.parseInt(HighContrastLAF.properties.getProperty(key), 16))
  }
}
/** A color theme which loads the main colors and fonts from an external properties file */
protected class HighContrastTheme extends DefaultMetalTheme {
  import HighContrastLAF._
  import HighContrastTheme._

  private val fontName = properties.getProperty("fontName", "Dialog")
  private var fontSize = 12
  try {
    fontSize = Integer.parseInt(properties.getProperty("fontSize"))
  } catch {case exc:Exception =>}

  private val font = new FontUIResource(fontName, Font.PLAIN, fontSize)

  override protected def getWhite: ColorUIResource = {
    getColor("backgroundColor")
  }

  override protected def getBlack: ColorUIResource = {
    getColor("foregroundColor")
  }

  override protected def getPrimary1: ColorUIResource = {
    getColor("primaryColor1")
  }

  override protected def getPrimary2: ColorUIResource = {
    getColor("primaryColor2")
  }

  override protected def getPrimary3: ColorUIResource = {
    getColor("primaryColor3")
  }

  override protected def getSecondary1: ColorUIResource = {
    getColor("secondaryColor1")
  }

  override protected def getSecondary2: ColorUIResource = {
    getColor("secondaryColor2")
  }

  override protected def getSecondary3: ColorUIResource = {
    getColor("secondaryColor3")
  }

  protected def getSelectionForeground: ColorUIResource = {
    getColor("selectionForeground")
  }

  protected def getSelectionBackground: ColorUIResource = {
    getColor("selectionBackground")
  }

  override def getMenuSelectedBackground: ColorUIResource = {
    getSelectionBackground
  }

  override def getMenuSelectedForeground: ColorUIResource = {
    getSelectionForeground
  }

  override def getTextHighlightColor: ColorUIResource = {
    getSelectionBackground
  }

  override def getHighlightedTextColor: ColorUIResource = {
    getSelectionForeground
  }

  override def getControlTextFont: FontUIResource = {
    font
  }

  override def getMenuTextFont: FontUIResource = {
    font
  }

  override def getSubTextFont: FontUIResource = {
    font
  }

  override def getSystemTextFont: FontUIResource = {
    font
  }

  override def getUserTextFont: FontUIResource = {
    font
  }

  override def getWindowTitleFont: FontUIResource = {
    font
  }
}



