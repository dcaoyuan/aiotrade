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
package org.aiotrade.modules.ui.dialog;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.aiotrade.lib.math.timeseries.TFreq;
import org.aiotrade.lib.math.timeseries.TUnit;
import org.aiotrade.lib.securities.PersistenceManager$;
import org.aiotrade.lib.securities.dataserver.QuoteContract;
import org.aiotrade.lib.securities.dataserver.QuoteServer;
import org.aiotrade.lib.securities.util.UserOptionsManager;
import scala.Option;

/**
 *
 * @author  Caoyuan Deng
 */
public class ImportSymbolDialog extends javax.swing.JPanel {

    Component parent;
    QuoteContract quoteContract;
    Date sampleDate = Calendar.getInstance().getTime();
    private ResourceBundle bundle = ResourceBundle.getBundle("org.aiotrade.modules.ui.dialog.Bundle");
    /**
     * Creates new form ImportSymbolDialog
     */
    public ImportSymbolDialog(Component parent, QuoteContract quoteContract, boolean isNewSymbol) {
        this.parent = parent;
        this.quoteContract = quoteContract;
        initComponents();

        scala.collection.Iterator<QuoteServer> quoteServers = PersistenceManager$.MODULE$.apply().lookupAllRegisteredServices(QuoteServer.class, "QuoteServers").iterator();
        List<QuoteServer> servers = new ArrayList<QuoteServer>();
        while (quoteServers.hasNext()) {
            servers.add(quoteServers.next());
        }
        dataSourceComboBox.setModel(new DefaultComboBoxModel(servers.toArray()));

        QuoteContract quoteContractTemplate = isNewSymbol
                ? UserOptionsManager.currentPreferredQuoteContract()
                : quoteContract;
        if (quoteContractTemplate == null) {
            /** no currentPreferredQuoteContract */
            quoteContractTemplate = quoteContract;
        }
        QuoteServer quoteServerTemplate = quoteContractTemplate.lookupServiceTemplate(QuoteServer.class, "DataServers").get();

        dataSourceComboBox.setSelectedItem(quoteServerTemplate);

        timeUnitField.setModel(new DefaultComboBoxModel(TUnit.values()));
        timeUnitField.setSelectedItem(quoteContractTemplate.freq().unit());
        unitTimesField.setValue(quoteContractTemplate.freq().nUnits());

        refreshable.setSelected(quoteContractTemplate.isRefreshable());
        refreshInterval.setValue(quoteContractTemplate.refreshInterval());

        pathField.setText(quoteContractTemplate.urlString());
        stockSymbolsField.setText(quoteContractTemplate.srcSymbol());
        
        Calendar cal = Calendar.getInstance();
        toDateField.setValue(cal.getTime());
        cal.setTimeInMillis(quoteContractTemplate.fromTime());
        fromDateField.setValue(cal.getTime());
        DateFormat format = DateFormat.getDateInstance(DateFormat.DEFAULT);
        if (format instanceof SimpleDateFormat) {
            String pattern = new StringBuffer("(").append(((SimpleDateFormat) format).toPattern()).append(")").toString();

            jLabel6.setText(pattern);
            jLabel7.setText(pattern);
        }

        String dfPattern = "";
        if (quoteContractTemplate.datePattern().isDefined()) {
            dfPattern = quoteContractTemplate.datePattern().get();
        }
        formatStringField.setText(dfPattern);
        SimpleDateFormat sdf = new SimpleDateFormat(dfPattern, Locale.US);
        dateFormatSample.setText(sdf.format(sampleDate));

        stockSymbolsField.grabFocus();
    }

    public int showDialog() {
        Object[] message = {this};

        int retValue = JOptionPane.showConfirmDialog(
                parent,
                message,
//                "Security Data Source",
                bundle.getString("Security_Data_Source"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null);

        if (retValue == JOptionPane.OK_OPTION) {
            try {
                unitTimesField.commitEdit();
                refreshInterval.commitEdit();
                fromDateField.commitEdit();
                toDateField.commitEdit();
            } catch (Exception e) {
                e.printStackTrace();
            }
            applyChanges();
        }

        return retValue;
    }

    private void applyChanges() {
        QuoteServer selectedServer = (QuoteServer) dataSourceComboBox.getSelectedItem();
        quoteContract.active_$eq(true);
        quoteContract.serviceClassName_$eq(selectedServer.getClass().getName());
        quoteContract.srcSymbol_$eq(stockSymbolsField.getText().trim().toUpperCase());
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date) fromDateField.getValue());
        quoteContract.fromTime_$eq(cal.getTimeInMillis());
        cal.setTime((Date) toDateField.getValue());
        quoteContract.toTime_$eq(cal.getTimeInMillis());
        quoteContract.urlString_$eq(pathField.getText().trim());

        UserOptionsManager.currentPreferredQuoteContract_$eq(quoteContract);
        TFreq freq = new TFreq(
                (TUnit) timeUnitField.getSelectedItem(),
                (Integer) unitTimesField.getValue());
        quoteContract.freq_$eq(freq);

        quoteContract.isRefreshable_$eq(refreshable.isSelected());
        quoteContract.refreshInterval_$eq((Integer) refreshInterval.getValue());

        String str = formatStringField.getText().trim();
        quoteContract.datePattern_$eq(Option.apply(propDateFormatString(str)));
    }

    private String propDateFormatString(String str) {
        str = str.trim();
        str = str.replace('Y', 'y');
        str = str.replace('D', 'd');
        return str;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser1 = new javax.swing.JFileChooser();
        jLabel1 = new javax.swing.JLabel();
        dataSourceComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        pathField = new javax.swing.JTextField();
        chooseButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        fromDateField = new javax.swing.JFormattedTextField();
        toDateField = new javax.swing.JFormattedTextField();
        jLabel5 = new javax.swing.JLabel();
        stockSymbolsField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        formatStringField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        dateFormatSample = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        unitTimesField = new javax.swing.JSpinner();
        timeUnitField = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        refreshable = new javax.swing.JCheckBox();
        refreshInterval = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        iconLabel = new javax.swing.JLabel();

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel1.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel1.text_2")); // NOI18N

        dataSourceComboBox.setFont(new java.awt.Font("Dialog", 0, 11));
        dataSourceComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                dataSourceComboBoxItemStateChanged(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel2.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel2.text_2")); // NOI18N

        pathField.setFont(new java.awt.Font("Dialog", 0, 11));

        chooseButton.setFont(new java.awt.Font("Dialog", 0, 11));
        chooseButton.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.chooseButton.text_2")); // NOI18N
        chooseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooseButtonActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel3.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel3.text_2")); // NOI18N

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel4.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel4.text_2")); // NOI18N

        fromDateField.setFont(new java.awt.Font("DialogInput", 0, 11));

        toDateField.setFont(new java.awt.Font("DialogInput", 0, 11));

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel5.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel5.text_2")); // NOI18N

        stockSymbolsField.setFont(new java.awt.Font("Dialog", 0, 11));

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel8.text_2")); // NOI18N

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel6.text_2")); // NOI18N

        jLabel7.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel7.text_2")); // NOI18N

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel9.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel9.text_2")); // NOI18N

        formatStringField.setFont(new java.awt.Font("DialogInput", 0, 11));
        formatStringField.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.formatStringField.text_2")); // NOI18N
        formatStringField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                formatStringFieldFocusLost(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel10.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel10.text_2")); // NOI18N

        dateFormatSample.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        dateFormatSample.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.dateFormatSample.text_2")); // NOI18N

        jLabel11.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel11.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel11.text_2")); // NOI18N

        unitTimesField.setFont(new java.awt.Font("Dialog", 0, 11));

        timeUnitField.setFont(new java.awt.Font("Dialog", 0, 11));
        timeUnitField.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        timeUnitField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timeUnitFieldActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jPanel1.border.title_2"))); // NOI18N
        jPanel1.setFont(new java.awt.Font("Dialog", 0, 12));

        refreshable.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        refreshable.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.refreshable.text_2")); // NOI18N
        refreshable.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        refreshable.setMargin(new java.awt.Insets(0, 0, 0, 0));
        refreshable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshableActionPerformed(evt);
            }
        });

        refreshInterval.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N

        jLabel13.setFont(new java.awt.Font("Dialog", 0, 11)); // NOI18N
        jLabel13.setText(org.openide.util.NbBundle.getMessage(ImportSymbolDialog.class, "ImportSymbolDialog.jLabel13.text_2")); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(refreshable, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(refreshInterval, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 48, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel13)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(refreshable)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel13)
                    .add(refreshInterval, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                            .add(jLabel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                            .add(jLabel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                            .add(jLabel11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                            .add(jLabel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                            .add(jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(stockSymbolsField)
                            .add(layout.createSequentialGroup()
                                .add(1, 1, 1)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(formatStringField)
                                    .add(layout.createSequentialGroup()
                                        .add(unitTimesField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(timeUnitField, 0, 83, Short.MAX_VALUE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED))
                                    .add(toDateField)
                                    .add(fromDateField))
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(layout.createSequentialGroup()
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .add(jLabel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 47, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(dateFormatSample, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 93, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(layout.createSequentialGroup()
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jLabel6)
                                            .add(jLabel7)))))
                            .add(pathField)
                            .add(dataSourceComboBox, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(chooseButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(iconLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(stockSymbolsField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel8)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(dataSourceComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1)
                    .add(iconLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(pathField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2)
                    .add(chooseButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(dateFormatSample)
                    .add(formatStringField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel9))
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, timeUnitField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, unitTimesField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(32, 32, 32)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(fromDateField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel4))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(toDateField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel7)
                            .add(jLabel5))
                        .addContainerGap(32, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(23, 23, 23))
                    .add(layout.createSequentialGroup()
                        .add(9, 9, 9)
                        .add(jLabel11)
                        .add(114, 114, 114))))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void refreshableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshableActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_refreshableActionPerformed

    private void timeUnitFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_timeUnitFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_timeUnitFieldActionPerformed

    private void formatStringFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_formatStringFieldFocusLost
        String str = formatStringField.getText().trim();
        formatStringField.setText(str);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(propDateFormatString(str), Locale.US);
            dateFormatSample.setText(sdf.format(sampleDate));
        } catch (Exception e) {
//            dateFormatSample.setText("Ilegal Date Format!");
            dateFormatSample.setText(bundle.getString("Ilegal_Date_Format"));
            formatStringField.grabFocus();
        }

    }//GEN-LAST:event_formatStringFieldFocusLost

    private void dataSourceComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_dataSourceComboBoxItemStateChanged
        /**
         * change a item may cause two times itemStateChanged, the old one
         * will get the ItemEvent.DESELECTED and the new item will get the
         * ItemEvent.SELECTED. so, should check the affected item first:
         */
        if (evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }

        QuoteServer selectedServer = (QuoteServer) evt.getItem();
        Image icon = (Image) selectedServer.icon().get();
        if (icon != null) {
            iconLabel.setIcon(new ImageIcon(icon));
        }

        if (selectedServer.displayName().toUpperCase().contains("INTERNET") == false) {
            chooseButton.setEnabled(true);
            pathField.setEnabled(true);
            formatStringField.setEnabled(true);
        } else {
            chooseButton.setEnabled(false);
            pathField.setEnabled(false);
            formatStringField.setEnabled(false);
        }
        String selectedDfStr = selectedServer.defaultDatePattern();
        SimpleDateFormat sdf = new SimpleDateFormat(selectedDfStr, Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(quoteContract.fromTime());
        dateFormatSample.setText(sdf.format(cal.getTime()));
        formatStringField.setText(selectedDfStr);
    }//GEN-LAST:event_dataSourceComboBoxItemStateChanged

    private void chooseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooseButtonActionPerformed
        if (pathField.getText().toUpperCase().startsWith("FILE:")) {
            try {
                File dir = new File(pathField.getText().substring(5));
                jFileChooser1.setCurrentDirectory(dir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int option = jFileChooser1.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            pathField.setText("file:" + jFileChooser1.getSelectedFile().getPath());
        }
    }//GEN-LAST:event_chooseButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JButton chooseButton;
    public javax.swing.JComboBox dataSourceComboBox;
    public javax.swing.JLabel dateFormatSample;
    public javax.swing.JTextField formatStringField;
    public javax.swing.JFormattedTextField fromDateField;
    public javax.swing.JLabel iconLabel;
    public javax.swing.JFileChooser jFileChooser1;
    public javax.swing.JLabel jLabel1;
    public javax.swing.JLabel jLabel10;
    public javax.swing.JLabel jLabel11;
    public javax.swing.JLabel jLabel13;
    public javax.swing.JLabel jLabel2;
    public javax.swing.JLabel jLabel3;
    public javax.swing.JLabel jLabel4;
    public javax.swing.JLabel jLabel5;
    public javax.swing.JLabel jLabel6;
    public javax.swing.JLabel jLabel7;
    public javax.swing.JLabel jLabel8;
    public javax.swing.JLabel jLabel9;
    public javax.swing.JPanel jPanel1;
    public javax.swing.JTextField pathField;
    public javax.swing.JSpinner refreshInterval;
    public javax.swing.JCheckBox refreshable;
    public javax.swing.JTextField stockSymbolsField;
    public javax.swing.JComboBox timeUnitField;
    public javax.swing.JFormattedTextField toDateField;
    public javax.swing.JSpinner unitTimesField;
    // End of variables declaration//GEN-END:variables
}
