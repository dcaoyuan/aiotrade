package org.aiotrade.modules.ui.actions

import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.util.actors.Reactions
import org.openide.util.HelpCtx
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction
import scala.concurrent.SyncVar

class LoadAllQuotesFromRemoteAction extends CallableSystemAction with Publisher {
  val log = Logger.getLogger(this.getClass.getName)
  
  def performAction {
    java.awt.EventQueue.invokeLater(new Runnable {
        def run {
          log.info("Loading quotes of secs in " + Exchange.activeExchanges)
          
          // add symbols to exchange folder
          for (exchange <- Exchange.activeExchanges;
               sec <- Exchange.secsOf(exchange);
               ser <- sec.serOf(TFreq.DAILY); if !ser.isLoaded
          ) {
            log.info("Loading quotes of " + sec.uniSymbol)
            
            val syncVar = new SyncVar[Boolean]
            sec.loadSer(ser)
            var reaction: Reactions.Reaction = null
            reaction = {
              case TSerEvent.Loaded(serx, _, _, _, _, _) if serx eq ser =>
                reactions -= reaction
                deafTo(ser)
                sec.resetSers // release for memory
                log.info("Quotes of " + sec.uniSymbol + " loaded.")
                syncVar.set(true)
            }
            reactions += reaction
            listenTo(ser)
            
            //syncVar.get
          }
        }
      })
  }

  def getName = {
    //"Add Exchange Symbols"
    val name = NbBundle.getMessage(this.getClass,"CTL_LoadAllQuotesFromRemoteAction")
    name
  }
    
  def getHelpCtx: HelpCtx = {
    HelpCtx.DEFAULT_HELP
  }
    
  override protected def iconResource: String = {
    "org/aiotrade/modules/ui/resources/newSymbol.gif"
  }
    
  override protected def asynchronous: Boolean = {
    false
  }
    
    
}
