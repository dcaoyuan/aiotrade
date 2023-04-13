package org.aiotrade.modules.ui.actions;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.modules.ui.MarketNewsConfig
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent;
import org.aiotrade.modules.ui.windows.RealTimeChartTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;

/**
 *
 * @author Caoyuan Deng
 */
object MarketNewsAction {


}
class MarketNewsAction extends CallableSystemAction {
  import MarketNewsAction._

  def performAction {
    java.awt.EventQueue.invokeLater(new Runnable() {
        def run {
//          JOptionPane.showMessageDialog(null, "已到此了！")

          val tc = WindowManager.getDefault.getRegistry.getActivated
          val viewContainer = tc match {
            case x: AnalysisChartTopComponent => x.viewContainer
            case x: RealTimeChartTopComponent => x.viewContainer
            case null => null
            case _ => null
          }

          println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@  "+viewContainer.controller.serProvider.uniSymbol)
          
          MarketNewsConfig.symbol = viewContainer.controller.serProvider.uniSymbol
          
          val tComponent = WindowManager.getDefault.findTopComponent("marketnewsTopComponent")
          if(tComponent != null){
             tComponent.close
             tComponent.open
             tComponent.requestActive
          }else{
            JOptionPane.showMessageDialog(null, "marketnewsTopComponent为空值！")
          }
        }
      })
  }
    
  def getName: String = {
    val name = NbBundle.getMessage(this.getClass,"CTL_MarketNewsAction")
    //"Calendar/Trading date View"
    name
  }
    
    
  def getHelpCtx: HelpCtx = {
    HelpCtx.DEFAULT_HELP
  }
    
  override protected def iconResource: String = {
    "org/aiotrade/modules/ui/resources/naturalTrading.gif"
  }

}

