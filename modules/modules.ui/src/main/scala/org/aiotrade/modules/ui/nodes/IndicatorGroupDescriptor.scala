/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.modules.ui.nodes;

import java.awt.Image;
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.aiotrade.lib.charting.laf.LookFeel;
import org.aiotrade.lib.indicator.Indicator
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.math.timeseries.descriptor.Content;
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.util.swing.action.AddAction;
import org.aiotrade.lib.util.swing.action.SaveAction;
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.modules.ui.GroupDescriptor
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent;
import org.aiotrade.modules.ui.dialog.PickIndicatorDialog;
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.windows.WindowManager
import scala.collection.mutable


/**
 *
 *
 *
 * @author Caoyuan Deng
 */
class IndicatorGroupDescriptor extends GroupDescriptor[IndicatorDescriptor] {
  import IndicatorGroupDescriptor._

  private val log = Logger.getLogger(this.getClass.getName)
  
  def getBindClass: Class[IndicatorDescriptor] = {
    classOf[IndicatorDescriptor]
  }
    
  def createActions(content: Content): Array[Action] = {
    Array(new AddIndicatorAction(content))
  }
    
  def getDisplayName = NAME
  def getTooltip = NAME
  def getIcon(tpe: Int): Image = ICON
    
  private class AddIndicatorAction(content: Content) extends AddAction {
//    putValue(Action.NAME, "Add Indicator")
    putValue(Action.NAME,NbBundle.getMessage(this .getClass,"Add_Indicator"))

    def execute {
      val analysisTc = AnalysisChartTopComponent.selected getOrElse {return}
            
      val keyToResult = mutable.Map[String, Object]()
            
      val dialog = new PickIndicatorDialog(
        WindowManager.getDefault.getMainWindow,
        true,
        keyToResult
      )
      dialog.setVisible(true)
            
      if (keyToResult("Option").asInstanceOf[Int] != JOptionPane.OK_OPTION) {
        return
      }

      try {
        val selectedIndicator = keyToResult("selectedIndicator").asInstanceOf[String]
        val multipleEnable    = keyToResult("multipleEnable").asInstanceOf[Boolean]
        val nUnits            = keyToResult("nUnits").asInstanceOf[Int]
        val unit              = keyToResult("unit").asInstanceOf[TUnit]
        
        /**
         * setAllowMultipleIndicatorOnQuoteChartView in OptionManager, let
         * DescriptorNode.IndicatorViewAction or anyone to decide how to treat it.
         */
        LookFeel().setAllowMultipleIndicatorOnQuoteChartView(multipleEnable)

        val indicators = PersistenceManager().lookupAllRegisteredServices(classOf[Indicator], "Indicators")
        val indicator = indicators find (x => x.displayName == selectedIndicator) getOrElse (return)
        val className = indicator.getClass.getName

        (content.lookupDescriptor(classOf[IndicatorDescriptor], className, TFreq(unit, nUnits)) match {
            case None =>
              content.createDescriptor(classOf[IndicatorDescriptor], className, TFreq(unit, nUnits)) map {x =>
                indicator.uniSymbol match {
                  case Some(s) => x.uniSymbol = s; x
                  case None => x
                }
              }
            case some => some
          }
        ) foreach {descriptor =>
          content.lookupAction(classOf[SaveAction]) foreach {_.execute}
          descriptor.lookupAction(classOf[ViewAction]) foreach {_.execute}
        }
      } catch {
        case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
      }
            
    }
        
  }

}

object IndicatorGroupDescriptor {
//  val NAME = "Indicators"
  val NAME = NbBundle.getMessage(this .getClass,"Indicators")
  val ICON = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/indicators.gif")
}
