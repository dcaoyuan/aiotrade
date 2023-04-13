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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES LOSS OF USE, DATA, OR PROFITS 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.modules.ui.dialogs

import java.awt.Dimension
import java.awt.Frame
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.text.DecimalFormat
import java.text.ParseException
import java.util.ResourceBundle
import javax.swing.BorderFactory
import javax.swing.JOptionPane
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.EventListenerList
import javax.swing.text.NumberFormatter
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.math.indicator.Factor

/**
 * 
 * 
 * @author Caoyuan Deng
 * @NOTICE: IndicatorDescriptore IndicatorDescriptor's factors instead of indicator's factors
 */
class ChangeIndicatorFactorsPane(owner: Frame, descriptor: IndicatorDescriptor) extends JComponent {
    
  private val factors = descriptor.factors
  private val length = factors.length
  private val oldFactors = new Array[Factor](length)
  for (i <- 0 until length) {
    oldFactors(i) = factors(i).clone
  }
  private val factorNameLables = new Array[JLabel](length)
  private val factorValueSpinners = new Array[JSpinner](length)
  private val factorSpinnerNumberModels = new Array[SpinnerNumberModel](length)
    
  private val previewCheckBox = new JCheckBox()
  private val saveAsDefaultCheckBox = new JCheckBox()
  private val applyToAllCheckBox = new JCheckBox()
    
  private var saveAsDefault: Boolean = _
  private var applyToAll: Boolean = _
    
  private val spinnerChangelistenerList = new EventListenerList
  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.modules.ui.dialog.Bundle")
    
  initComponents()
    
  private def initComponents() {
        
    val gbl = new GridBagLayout()
    val gbc = new GridBagConstraints()
    setLayout(gbl)
        
        
    /** @TODO use this NumberFormatter ? */
    val df = new DecimalFormat("#####")
    val nf = new NumberFormatter(df) {
      @throws(classOf[ParseException])
      override def valueToString(iv: AnyRef): String = {
        if ((iv == null) || (iv.asInstanceOf[Double].doubleValue == -1)) {
          ""
        } else {
          super.valueToString(iv)
        }
      }
      @throws(classOf[ParseException])
      override def stringToValue(text: String): AnyRef = {
        if ("".equals(text)) {
          null
        } else super.stringToValue(text)
      }
    }
    nf.setMinimum(0.0.asInstanceOf[Comparable[_]])
    nf.setMaximum(65534.0.asInstanceOf[Comparable[_]])
    nf.setValueClass(classOf[Double])
        
        
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.fill = GridBagConstraints.HORIZONTAL
    val factorsPanel = new JPanel()
    //optsPanel.setBorder(BorderFactory.createTitledBorder(" Indicator Options "))
    factorsPanel.setBorder(BorderFactory.createTitledBorder(BUNDLE.getString("Indicator_Options")))
    add(factorsPanel, gbc)
        
    factorsPanel.setLayout(new GridBagLayout())
        
    gbc.gridx = 0
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    gbc.fill = GridBagConstraints.HORIZONTAL
    val previewLabel = new JLabel(BUNDLE.getString("Preview"))
    previewLabel.setHorizontalAlignment(SwingConstants.RIGHT)
    factorsPanel.add(previewLabel, gbc)
        
    gbc.gridx = 1
    gbc.gridy = 0
    gbc.gridwidth = 1
    gbc.gridheight = 1
    previewCheckBox.setSelected(true)
    factorsPanel.add(previewCheckBox, gbc)
        
    val spinnerChangeListener = new ChangeListener() {
      def stateChanged(e: ChangeEvent) {
        if (previewCheckBox.isSelected) {
          transferValues()
                    
          /** forward the event to whom care about it */
          fireSpinnerChangeEvent(e)
        }
      }
    }
        
    for (i <- 0 until factors.length) {
      val factor = factors(i)
            
      gbc.gridx = 0
      gbc.gridy = i + 1
      gbc.gridwidth = 2
      gbc.gridheight = 1
      gbc.fill = GridBagConstraints.HORIZONTAL
      gbc.ipadx = 5
      factorNameLables(i) = new JLabel()
      factorNameLables(i).setText(factor.name)
      factorsPanel.add(factorNameLables(i), gbc)
            
      factorSpinnerNumberModels(i) = new SpinnerNumberModel()
      factorSpinnerNumberModels(i).setValue(factor.value)
      factorSpinnerNumberModels(i).setStepSize(factor.step)
      factorSpinnerNumberModels(i).setMinimum(factor.minValue.asInstanceOf[Comparable[_]])
      factorSpinnerNumberModels(i).setMaximum(factor.maxValue.asInstanceOf[Comparable[_]])
            
      gbc.gridx = 2
      gbc.gridy = i + 1
      gbc.gridwidth = 1
      gbc.gridheight = 1
      factorValueSpinners(i) = new JSpinner()
      factorValueSpinners(i).setPreferredSize(new Dimension(80, 20))
      factorValueSpinners(i).setModel(factorSpinnerNumberModels(i))
      factorsPanel.add(factorValueSpinners(i), gbc)
            
      factorValueSpinners(i).addChangeListener(spinnerChangeListener)
      factorValueSpinners(i).setEnabled(true)
    }
        
    factorsPanel.setFocusable(true)
        
    gbc.gridx = 0
    gbc.gridy = GridBagConstraints.RELATIVE
    gbc.gridwidth = 1
    gbc.gridheight = 1
    val additionalDecisionsPanel = new JPanel()
    additionalDecisionsPanel.setBorder(BorderFactory.createTitledBorder(BUNDLE.getString("Additional")))
    add(additionalDecisionsPanel, gbc)
        
    additionalDecisionsPanel.setLayout(new GridBagLayout())
        
    gbc.gridx = 0
    gbc.gridy = GridBagConstraints.RELATIVE
    gbc.gridwidth = 1
    gbc.gridheight = 1
    val saveAsDefaultLabel = new JLabel(BUNDLE.getString("Save_As_Default"))
    saveAsDefaultLabel.setHorizontalAlignment(SwingConstants.RIGHT)
    additionalDecisionsPanel.add(saveAsDefaultLabel, gbc)
        
    gbc.gridx = 1
    gbc.gridy = GridBagConstraints.RELATIVE
    gbc.gridwidth = 1
    gbc.gridheight = 1
    saveAsDefaultCheckBox.setSelected(false)
    additionalDecisionsPanel.add(saveAsDefaultCheckBox, gbc)
        
    gbc.gridx = 0
    gbc.gridy = GridBagConstraints.RELATIVE
    gbc.gridwidth = 1
    gbc.gridheight = 1
    val applyToAllLabel = new JLabel(BUNDLE.getString("Apply_to_All"))
    applyToAllLabel.setHorizontalAlignment(SwingConstants.RIGHT)
    additionalDecisionsPanel.add(applyToAllLabel, gbc)
    
    gbc.gridx = 1
    gbc.gridy = GridBagConstraints.RELATIVE
    gbc.gridwidth = 1
    gbc.gridheight = 1
    applyToAllCheckBox.setSelected(false)
    additionalDecisionsPanel.add(applyToAllCheckBox, gbc)
        
    additionalDecisionsPanel.setFocusable(true)
  }
    
  def showDialog: Int = {
    val message = Array(BUNDLE.getString("Please_input_new_options") + descriptor.displayName + ":",
                        this)
        
    val retValue = JOptionPane.showConfirmDialog(
      owner,
      message,
      // "Change options",
      BUNDLE.getString("Change_Options"),
      JOptionPane.OK_CANCEL_OPTION
    )
        
    if (retValue == JOptionPane.OK_OPTION) {
      try {
        for (optValueSpinner <- factorValueSpinners) {
          optValueSpinner.commitEdit
        }
      } catch {
        case ex: Exception => ex.printStackTrace
      }
      transferValues
    } else {
      descriptor.factors = oldFactors
    }
        
    retValue
  }
    
  private def transferValues() {
    for (i <- 0 until length) {
      val factorValue = factorValueSpinners(i).getValue().asInstanceOf[Number].doubleValue
      factors(i).value = factorValue
    }
        
    saveAsDefault = saveAsDefaultCheckBox.isSelected
    applyToAll = applyToAllCheckBox.isSelected
  }
    
  def getOpts = factors
    
  def isSaveAsDefault = saveAsDefault
  def isApplyToAll = applyToAll
    
  def addSpinnerChangeListener(listener: ChangeListener) {
    spinnerChangelistenerList.add(classOf[ChangeListener], listener)
  }
    
  def removeSpinnerChangeListener(listener: ChangeListener) {
    spinnerChangelistenerList.remove(classOf[ChangeListener], listener)
  }
    
  private def fireSpinnerChangeEvent(evt: ChangeEvent) {
    val listeners = spinnerChangelistenerList.getListenerList()
    /** Each listener occupies two elements - the first is the listener class */
    var i = 0
    while (i < listeners.length) {
      if (listeners(i) == classOf[ChangeListener]) {
        listeners(i + 1).asInstanceOf[ChangeListener].stateChanged(evt)
      }
      i += 2
    }
  }
    
}




