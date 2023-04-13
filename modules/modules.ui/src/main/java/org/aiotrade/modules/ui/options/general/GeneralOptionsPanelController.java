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
package org.aiotrade.modules.ui.options.general;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.InetSocketAddress;
import java.net.Proxy;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import org.aiotrade.lib.securities.PersistenceManager$;
import org.aiotrade.lib.securities.util.UserOptionsManager;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/**
 * Implementation of one panel in Options Dialog.
 *
 * @author Caoyuan Deng
 */
public final class GeneralOptionsPanelController extends OptionsPanelController {
    
    private PropertyChangeSupport pcs;
    private GeneralOptionsPanel optionsPanel;
    private boolean changed = false;
    private boolean valid = true;
    private String currentPropertyValue = "property value";
    
    
    public GeneralOptionsPanelController() {
        optionsPanel = new GeneralOptionsPanel();
        
    }
    
    public void update() {
        // init values in your panel here
        optionsPanel.proxyTypeRadioGroup.add(optionsPanel.noProxyRadio);
        optionsPanel.proxyTypeRadioGroup.add(optionsPanel.systemProxyRadio);
        optionsPanel.proxyTypeRadioGroup.add(optionsPanel.httpProxyRadio);
        
        Proxy proxy = UserOptionsManager.getProxy();
        if (proxy == null) {
            optionsPanel.proxyTypeRadioGroup.setSelected(optionsPanel.systemProxyRadio.getModel(), true);
        } else{
            switch (proxy.type()) {
                case DIRECT:
                    optionsPanel.proxyTypeRadioGroup.setSelected(optionsPanel.noProxyRadio.getModel(), true);
                    break;
                case HTTP:
                    optionsPanel.proxyTypeRadioGroup.setSelected(optionsPanel.httpProxyRadio.getModel(), true);
                    break;
                default:
                    optionsPanel.proxyTypeRadioGroup.setSelected(optionsPanel.systemProxyRadio.getModel(), true);
            }
            InetSocketAddress addr = (InetSocketAddress)proxy.address();
            String hostName = (addr == null) ? "" : addr.getHostName();
            int port = (addr == null) ? 80 : addr.getPort();
            optionsPanel.proxyHost.setText(hostName);
            optionsPanel.proxyPort.setText(String.valueOf(port));
        }
        
        if (optionsPanel.proxyTypeRadioGroup.getSelection().equals(optionsPanel.httpProxyRadio.getModel())) {
            optionsPanel.proxyHost.setEnabled(true);
            optionsPanel.proxyPort.setEnabled(true);
        } else {
            optionsPanel.proxyHost.setEnabled(false);
            optionsPanel.proxyPort.setEnabled(false);
        }
    }
    
    /**
     * this method is called when Ok button has been pressed
     * 
     * save values here 
     */
    public void applyChanges() {
        valid = true;
        
        String proxyHostStr = optionsPanel.proxyHost.getText();
        String proxyPortStr = optionsPanel.proxyPort.getText();
        
        Proxy proxy = null;
        ButtonModel selectedRadio = optionsPanel.proxyTypeRadioGroup.getSelection();
        if (selectedRadio.equals(optionsPanel.noProxyRadio.getModel())) {
            proxy = Proxy.NO_PROXY;
        } else if (selectedRadio.equals(optionsPanel.httpProxyRadio.getModel())) {
            int port = 80;
            try {
                port = Integer.parseInt(proxyPortStr.trim());
                /** check port valide range: */
                if (port < 0 || port > 0xFFFF) {
                    valid = false;
                } else {
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHostStr, port));
                }
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        } else {
            /** use system proxies */
            proxy = null;
        }
        
        if (isValid()) {
            UserOptionsManager.setProxy(proxy);
            PersistenceManager$.MODULE$.apply().saveProperties();
            
            changed = true;
        }
    }
    
    /**
     * This method is called when Cancel button has been pressed
     * revert any possible changes here
     */
    public void cancel() {
        /** enforce OptionsSystem to reload options from previous properties files */
        UserOptionsManager.setOptionsLoaded(false);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public boolean isChanged() {
        return changed;
    }
    
    public HelpCtx getHelpCtx() {
        return new HelpCtx("netbeans.optionsDialog.editor.example");
    }
    
    public JComponent getComponent(Lookup masterLookup) {
        return optionsPanel;
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }
    
    
    
    
}
