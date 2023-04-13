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

import java.awt.Color;
import java.awt.Frame;
import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.ResourceBundle;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.math.timeseries.BaseTSer;

/**
 *
 * @author  mememe
 */
public class SaveToImageDialog extends javax.swing.JDialog {
    private Frame parent;
    private ChartViewContainer viewContainer;
    private BaseTSer baseSer;
    private long fromTime, toTime;
    private int imageHeight, imageWidth;
    private File file;
    
    private int value = JOptionPane.CANCEL_OPTION;
    
    private Calendar calendar = Calendar.getInstance();
    private DateFormat dateFormat = DateFormat.getInstance();
    private ResourceBundle bundle = ResourceBundle.getBundle("org.aiotrade.modules.ui.dialog.Bundle");
    /**
     * Creates new form SaveToImageDialog
     */
    public SaveToImageDialog(Frame parent, ChartViewContainer viewContainer) {
        super(parent, true);
        this.viewContainer = viewContainer;
        this.baseSer = viewContainer.controller().baseSer();
        initComponents();
        
//        setTitle("Save image to ...");
        setTitle(bundle.getString("Save_Image_to"));
        
        buttonGroup.add(currentRadio);
        buttonGroup.add(customRadio);
        buttonGroup.setSelected(currentRadio.getModel(), true);
        
        this.fromTime = viewContainer.controller().leftSideTime();
        this.toTime = viewContainer.controller().rightSideTime();
        calendar.setTimeInMillis(fromTime);
        fromDateField.setText(dateFormat.format(calendar.getTime()));
        calendar.setTimeInMillis(toTime);
        toDateField.setText(dateFormat.format(calendar.getTime()));
        fromDateField.setEnabled(false);
        toDateField.setEnabled(false);
        
        imageHeight = viewContainer.getHeight();
        imageWidth = viewContainer.getWidth();
        heightSpinner.setValue(imageHeight);
        widthLable.setText(String.valueOf(imageWidth));
        heightSpinner.setEnabled(false);
        
        
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
//        fileChooser.setDialogTitle("Select Export png File");
        fileChooser.setDialogTitle(bundle.getString("Select_Export_Png_File"));
        
        FileFilter pngFileFilter = new FileFilter() {
            public boolean accept(File f) {
                boolean accept = f.isDirectory();
                if (!accept) {
                    String suffix = suffixOf(f);
                    
                    if (suffix != null && suffix.equals("png") ) {
                        accept = true;
                    }
                }
                
                return accept;
            }
            
            public String getDescription() {
//                return "Image files (*.png)";
                return bundle.getString("Image_File");
            }
        };
        
        fileChooser.setFileFilter(pngFileFilter);
        
        
        pack();
        setLocationRelativeTo(parent);
    }
    
    private String suffixOf(File f) {
        String suffix = null;
        
        String s = f.getPath();
        int i = s.lastIndexOf('.');
        
        if (i > 0 && i < s.length() - 1) {
            suffix = s.substring(i + 1).toLowerCase();
        }
        
        return suffix;
    }
    
    private boolean variableFieldChanged() {
        int begPosition = baseSer.rowOfTime(fromTime);
        int endPosition = baseSer.rowOfTime(toTime);
        imageWidth = (int)((endPosition - begPosition + 1) * viewContainer.controller().wBar());
        widthLable.setText(String.valueOf(imageWidth));
        if (imageWidth * imageHeight > 1024 * 768 * 20) {
            warningLable.setForeground(Color.RED);
//            warningLable.setText("Too big time scope that may exceed memory!");
            warningLable.setText(bundle.getString("Too_Big_Time_Scope"));
            return false;
        } else {
            warningLable.setForeground(Color.BLACK);
//            warningLable.setText("Image size is Ok.");
            warningLable.setText(bundle.getString("Image_Size_Is_Ok"));
            
            return true;
        }
        
    }
    
    private boolean applyChanges() {
        try {
            heightSpinner.commitEdit();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        if (variableFieldChanged()) {
            file = fileChooser.getSelectedFile();
            if (file == null) {
                warningLable.setForeground(Color.RED);
//                warningLable.setText("Should choose a file!");
                warningLable.setText(bundle.getString("Should_Choose_A_File"));
                return false;
            } else {
                String suffix = suffixOf(file);
                if (suffix == null || !suffix.equals("png") ) {
                    file = new File(file.getPath() + ".png");
                }
                return true;
            }
        } else {
            return false;
        }
    }
    
    public long getFromTime() {
        return fromTime;
    }
    
    public long getToTime() {
        return toTime;
    }
    
    public int getImageHeight() {
        return imageHeight;
    }
    
    public File getFile() {
        return file;
    }
    
    public int getValue() {
        return value;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup = new javax.swing.ButtonGroup();
        currentRadio = new javax.swing.JRadioButton();
        customRadio = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        fromDateField = new javax.swing.JTextField();
        toDateField = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        fileChooser = new javax.swing.JFileChooser();
        warningLable = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        heightSpinner = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        widthLable = new javax.swing.JLabel();

        currentRadio.setFont(new java.awt.Font("Dialog", 0, 11));
        currentRadio.setText(org.openide.util.NbBundle.getMessage(SaveToImageDialog.class, "SaveToImagePane.currentRadio.text")); // NOI18N
        currentRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        currentRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        currentRadio.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                currentRadioStateChanged(evt);
            }
        });

        customRadio.setFont(new java.awt.Font("Dialog", 0, 11));
        customRadio.setText(org.openide.util.NbBundle.getMessage(SaveToImageDialog.class, "SaveToImagePane.customRadio.text")); // NOI18N
        customRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        customRadio.setMargin(new java.awt.Insets(0, 0, 0, 0));
        customRadio.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                customRadioStateChanged(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel1.setText(org.openide.util.NbBundle.getMessage(SaveToImageDialog.class, "SaveToImagePane.jLabel1.text")); // NOI18N

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel2.setText(org.openide.util.NbBundle.getMessage(SaveToImageDialog.class, "SaveToImagePane.jLabel2.text")); // NOI18N

        fromDateField.setFont(new java.awt.Font("Dialog", 0, 11));
        fromDateField.setText("jTextField1");
        fromDateField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                fromDateFieldFocusLost(evt);
            }
        });

        toDateField.setFont(new java.awt.Font("Dialog", 0, 11));
        toDateField.setText("jTextField2");
        toDateField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                toDateFieldFocusLost(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(SaveToImageDialog.class, "SaveToImagePane.jPanel1.border.title"))); // NOI18N
        jPanel1.setFont(new java.awt.Font("Dialog", 0, 11));

        fileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        fileChooser.setFont(new java.awt.Font("Dialog", 0, 11));
        fileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileChooserActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(fileChooser, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 559, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(fileChooser, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        warningLable.setFont(new java.awt.Font("Dialog", 1, 11));
        warningLable.setForeground(new java.awt.Color(255, 51, 51));
        warningLable.setText(org.openide.util.NbBundle.getMessage(SaveToImageDialog.class, "SaveToImagePane.warningLable.text")); // NOI18N

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel3.setText(org.openide.util.NbBundle.getMessage(SaveToImageDialog.class, "SaveToImagePane.jLabel3.text")); // NOI18N

        heightSpinner.setFont(new java.awt.Font("Dialog", 0, 11));
        heightSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                heightSpinnerStateChanged(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 11));
        jLabel4.setText(org.openide.util.NbBundle.getMessage(SaveToImageDialog.class, "SaveToImagePane.jLabel4.text")); // NOI18N

        widthLable.setFont(new java.awt.Font("Dialog", 0, 11));
        widthLable.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        widthLable.setText("jLabel5");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(warningLable, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, currentRadio)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                                .add(customRadio)
                                .add(14, 14, 14)
                                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel2)
                                    .add(jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(toDateField)
                            .add(fromDateField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE))
                        .add(16, 16, 16)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 49, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel4))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(heightSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 76, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(widthLable, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 56, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(20, 20, 20))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(10, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(currentRadio)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(customRadio)
                    .add(jLabel1)
                    .add(fromDateField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3)
                    .add(heightSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(toDateField)
                    .add(jLabel4)
                    .add(widthLable))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(warningLable)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    
    private void fileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileChooserActionPerformed
        if (evt.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
            if (applyChanges()) {
                value = JOptionPane.OK_OPTION;
                setVisible(false);
            }
        } else if (evt.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
            value = JOptionPane.CANCEL_OPTION;
            setVisible(false);        
        }        
    }//GEN-LAST:event_fileChooserActionPerformed
    
    private void heightSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_heightSpinnerStateChanged
        imageHeight = (Integer)heightSpinner.getValue();
        variableFieldChanged();
    }//GEN-LAST:event_heightSpinnerStateChanged
    
    private void toDateFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_toDateFieldFocusLost
        try {
            calendar.setTime(dateFormat.parse(toDateField.getText()));
            toTime = calendar.getTimeInMillis();
        } catch (Exception ex) {
//            warningLable.setText("Invalid 'to date' format!");
            warningLable.setText(bundle.getString("Invalid_To_Date_Format"));
            return;
        }
        variableFieldChanged();
    }//GEN-LAST:event_toDateFieldFocusLost
    
    private void fromDateFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fromDateFieldFocusLost
        try {
            calendar.setTime(dateFormat.parse(fromDateField.getText()));
            fromTime = calendar.getTimeInMillis();
        } catch (Exception ex) {
//            warningLable.setText("Invalid 'from date' format!");
            warningLable.setText(bundle.getString("Invalid_From_Date_Format"));
            return;
        }
        variableFieldChanged();
    }//GEN-LAST:event_fromDateFieldFocusLost
    
    private void customRadioStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_customRadioStateChanged
        if (customRadio.isSelected()) {
            fromDateField.setEnabled(true);
            toDateField.setEnabled(true);
            
            heightSpinner.setEnabled(true);
        }
    }//GEN-LAST:event_customRadioStateChanged
    
    private void currentRadioStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_currentRadioStateChanged
        if (currentRadio.isSelected()) {
            this.fromTime = viewContainer.controller().leftSideTime();
            this.toTime = viewContainer.controller().rightSideTime();
            calendar.setTimeInMillis(fromTime);
            fromDateField.setText(dateFormat.format(calendar.getTime()));
            calendar.setTimeInMillis(toTime);
            toDateField.setText(dateFormat.format(calendar.getTime()));
            fromDateField.setEnabled(false);
            toDateField.setEnabled(false);
            
            imageHeight = viewContainer.getHeight();
            imageWidth = viewContainer.getWidth();
            heightSpinner.setValue(imageHeight);
            widthLable.setText(String.valueOf(imageWidth));
            heightSpinner.setEnabled(false);
        }
    }//GEN-LAST:event_currentRadioStateChanged
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup;
    private javax.swing.JRadioButton currentRadio;
    private javax.swing.JRadioButton customRadio;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JTextField fromDateField;
    private javax.swing.JSpinner heightSpinner;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField toDateField;
    private javax.swing.JLabel warningLable;
    private javax.swing.JLabel widthLable;
    // End of variables declaration//GEN-END:variables
    
}
