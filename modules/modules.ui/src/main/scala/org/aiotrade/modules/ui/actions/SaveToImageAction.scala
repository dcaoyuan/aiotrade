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
package org.aiotrade.modules.ui.actions;
import java.io.File;
import java.util.Calendar;
import javax.swing.JOptionPane;
import org.aiotrade.modules.ui.dialog.SaveToImageDialog;
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent;
import org.openide.ErrorManager;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;

/**
 *
 * @author Caoyuan Deng
 */
class SaveToImageAction extends CallableSystemAction {
    
  def performAction {
    try {
      java.awt.EventQueue.invokeLater(new Runnable() {
          def run {
            val analysisTc = AnalysisChartTopComponent.selected getOrElse {return}
                    
            val calendar = Calendar.getInstance
//                    
//                    String fileName;
//                    
//                    JFileChooser fileChooser = new JFileChooser();
//                    
//                    fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
//                    fileChooser.setDialogTitle("Select Export png File");
//                    
//                    FileFilter pngFileFilter = new FileFilter() {
//                        public boolean accept(File f) {
//                            boolean accept = f.isDirectory();
//                            
//                            if (!accept) {
//                                String suffix = suffixOf(f);
//                                
//                                if (suffix != null && suffix.equals("png") ) {
//                                    accept = true;
//                                }
//                            }
//                            
//                            return accept;
//                        }
//                        
//                        public String getDescription() {
//                            return "Image files (*.png)";
//                        }
//                    };
//                    
//                    fileChooser.setFileFilter(pngFileFilter);
//                    
//                    if (fileChooser.showDialog(
//                            WindowManager.getDefault().getMainWindow(),
//                            "Export to .png") != JFileChooser.APPROVE_OPTION) {
//                        return;
//                    }
//                    
//                    File file = fileChooser.getSelectedFile();
//                    
//                    String suffix = suffixOf(file);
//                    if (suffix == null || !suffix.equals("png") ) {
//                        file = new File(file.getPath() + ".png");
//                    }
                    
            val dialog = new SaveToImageDialog(
              WindowManager.getDefault.getMainWindow,
              analysisTc.viewContainer
            );
            dialog.setVisible(true);
                    
            if (!(dialog.getValue() == JOptionPane.OK_OPTION)) {
              return;
            }
                    
            val begTime = dialog.getFromTime();
            val endTime = dialog.getToTime();
            val height = dialog.getImageHeight();
            val file = dialog.getFile();
                    
            dialog.dispose();
                    
            try {
              analysisTc.viewContainer.saveToCustomSizeImage(file, "png", begTime, endTime, height)
            } catch {case ex: Exception => ErrorManager.getDefault().notify(ex)}
          }
        });
    } catch {case ex: Exception =>}
        
  }
    
  private def suffixOf(f: File): String = {
    var suffix: String = null;
        
    val s = f.getPath();
    val i = s.lastIndexOf('.');
        
    if (i > 0 && i < s.length() - 1) {
      suffix = s.substring(i + 1).toLowerCase();
    }
        
    suffix;
  }
    
  def getName: String = {
    //"Save to Image"
    val name = NbBundle.getMessage(this.getClass,"CTL_AddExchangeSymbolsAction")
    name
  }
    
  def getHelpCtx: HelpCtx = {
    HelpCtx.DEFAULT_HELP
  }
    
  override protected def iconResource: String = {
    "org/aiotrade/modules/ui/resources/saveToImage.png"
  }
    
  override protected def asynchronous: Boolean = {
    false
  }
    
    
}





