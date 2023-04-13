/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.modules.ui

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.ResourceBundle
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.SwingUtilities
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Exchanges
import org.aiotrade.lib.securities.data.SyncUtil
import org.aiotrade.lib.securities.util.UserOptionsManager
import org.aiotrade.modules.ui.nodes.SymbolNodes
import org.netbeans.api.progress.ProgressHandle
import org.netbeans.api.progress.ProgressHandleFactory
import org.netbeans.api.progress.ProgressUtils
import org.openide.filesystems.FileLock
import org.openide.filesystems.FileUtil
import org.openide.loaders.DataFolder
import org.openide.modules.ModuleInstall
import org.openide.util.RequestProcessor
import org.openide.windows.WindowManager
import scala.collection.mutable

/**
 * How do i change the closing action of the MainWindow?
 * http://platform.netbeans.org/faqs/actions-how-to-add-actions-to-fi.html#DevFaqMainwindowClosingAction
 *
 * When you click the close button in the top right corner the application closes. 
 * If you want to do something before the application closes (eg. a dialog with 
 * ok and cancel options) or prevent it from closing , you'll have to override 
 * the org.openide.modules.ModuleInstall class.
 *
 * Make a class in your module that extends the org.openide.modules.ModuleInstall
 * class and override the closing() method. Keep in mind that setting the return
 * value of this method to "true" means the application will not be able to close
 * at all.
 * 
 * Next you have to tell your module to load this new class by adding a reference
 * to your Manifest file : e.g. 
 * "OpenIDE-Module-Install: org/netbeans/modules/java/JavaModule.class" 
 * where "JavaModule.class" should be the name of your class.
 *
 *
 * @author Caoyuan Deng
 */

class Installer extends ModuleInstall {
  private val log = Logger.getLogger(this.getClass.getName)

  private val Bundle = ResourceBundle.getBundle("org.aiotrade.modules.ui.windows.Bundle")

  override 
  def restored {
    doRealCheck
    //doCheckWhenUIReady
  }

  /**
   * @Todo, won't work
   */
  private def doCheckWhenUIReady {
    WindowManager.getDefault.invokeWhenUIReady(new Runnable {
        def run {
          RequestProcessor.getDefault.post(doCheck, 100)
        }
      })
  }

  private def doCheck: Runnable = new Runnable  {
    def run {
      if (SwingUtilities.isEventDispatchThread) {
        RequestProcessor.getDefault.post(doCheck)
        return
      }

      doRealCheck
    }
  }

  def doRealCheck {
    // load config as soon as possible
    val configFo = FileUtil.getConfigFile("aiotrade.conf")
    var configFile = FileUtil.toFile(configFo)
    if (configFile == null) {
      // extract it from archive to disk
      var out: OutputStream = null
      var lock: FileLock = null
      try {
        lock = configFo.lock
        val body = configFo.asBytes
        out = configFo.getOutputStream(lock)
        out.write(body)
      } catch {
        case ex: IOException => log.log(Level.WARNING, ex.getMessage, ex)
      } finally {
        /** should remember to do out.close() here */
        if (out  != null) out.close
        if (lock != null) lock.releaseLock
      }
      configFile = FileUtil.toFile(configFo)
    }

    // load config file
    val configFilePath = configFile.getCanonicalPath
    log.info("Config file is " + configFilePath)
    org.aiotrade.lib.util.config.Config(configFilePath)

    val dataPath = System.getProperty("netbeans.user") + File.separator + "data"
    val dataDir = new File(dataPath)
    if (dataDir != null && !dataDir.exists) {
      SyncUtil.extractDataTo(dataPath)      
    } else {
      SyncUtil.initLocalDataGit(dataPath)
    }
    
    // create database if does not exist
    if (!Exchanges.exists) {
      log.info("Database does not exist yet, will create it ...")
      SyncUtil.importDataFrom(dataPath)
    }

    UserOptionsManager.assertLoaded

    /* if (!isSymbolNodesAdded) {
     val handle = ProgressHandleFactory.createHandle(Bundle.getString("MSG_CreateSymbolNodes"))
     ProgressUtils.showProgressDialogAndRun(new Runnable {
     def run {
     addSymbolsFromDB(handle)
     }
     }, handle, true)
     } */

    // run some task in background
//    SwingUtilities.invokeLater(new Runnable {
//        /**
//         * Wrap in EDT to avoid:
//         java.lang.IllegalStateException: Known problem in JDK occurred. If you are interested, vote and report at:
//         http://bugs.sun.com/view_bug.do?bug_id=6424157, http://bugs.sun.com/view_bug.do?bug_id=6553239
//         Also see related discussion at http://www.netbeans.org/issues/show_bug.cgi?id=90590
//         at org.netbeans.core.windows.WindowManagerImpl.warnIfNotInEDT(WindowManagerImpl.java:1523)
//         at org.netbeans.core.windows.WindowManagerImpl.getMainWindow(WindowManagerImpl.java:157)
//         at org.netbeans.core.TimableEventQueue.tick(TimableEventQueue.java:174)
//         */
//        def run {
//          UIManager.put("ScrollBar.width", 12)
//
//          UIManager.put("ScrollBar.foreground", LookFeel().backgroundColor)
//          UIManager.put("ScrollBar.background", LookFeel().backgroundColor)
//          UIManager.put("ScrollBar.track", LookFeel().getTrackColor)
//          UIManager.put("ScrollBar.trackDarkShadow", LookFeel().borderColor)
//          UIManager.put("ScrollBar.trackHighlight", LookFeel().getTrackColor)
//          UIManager.put("ScrollBar.trackShadow", LookFeel().borderColor)
//          UIManager.put("ScrollBar.thumb", LookFeel().getThumbColor)
//          UIManager.put("ScrollBar.thumbDarkShadow", LookFeel().getThumbColor)
//          UIManager.put("ScrollBar.thumbHighlight", LookFeel().getThumbColor)
//          UIManager.put("ScrollBar.thumbShadow", LookFeel().getThumbColor)
//        }
//      })
  }



  private def isSymbolNodesAdded = {
    val rootNode = SymbolNodes.RootSymbolsNode
    rootNode.getChildren.getNodesCount > 0
  }

  private def addSymbolsFromDB(handle: ProgressHandle) {
    val rootNode = SymbolNodes.RootSymbolsNode
    val rootFolder = rootNode.getLookup.lookup(classOf[DataFolder])

    DataFolder.create(rootFolder, SymbolNodes.favoriteFolderName)

    val start = System.currentTimeMillis
    log.info("Create symbols node files from db ...")

    val activeExchanges = Exchange.activeExchanges

    // add symbols to exchange folder
    val symbolsToFolder = mutable.Map[String, DataFolder]()
    for {
      exchange <- activeExchanges
      exchangeFolder = DataFolder.create(rootFolder, exchange.code)
      symbol <- Exchange.symbolsOf(exchange)
    } {
      symbolsToFolder.put(symbol, exchangeFolder)
    }

    handle.switchToDeterminate(symbolsToFolder.size)
    var i = 0
    val allSymbols = symbolsToFolder.iterator
    while (allSymbols.hasNext) {
      handle.progress(i)
      val (symbol, folder) = allSymbols.next
      SymbolNodes.createSymbolXmlFile(folder, symbol)
      i += 1
    }

    handle.finish
    log.info("Created symbols node files from db in " + ((System.currentTimeMillis - start) / 1000.0) + "s")
  }

  override 
  def closing: Boolean = {
    PersistenceManager().shutdown
        
    super.closing
  }

}
