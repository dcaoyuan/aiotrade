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
package org.aiotrade.modules.ui.nodes

import java.awt.Image
import java.beans.IntrospectionException
import java.beans.PropertyChangeEvent
import javax.swing.Action
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.math.timeseries.descriptor.Content
import org.aiotrade.lib.math.timeseries.descriptor.Descriptor
import org.aiotrade.lib.util.swing.action.DeleteAction
import org.aiotrade.lib.util.swing.action.EditAction
import org.aiotrade.lib.util.swing.action.HideAction
import org.aiotrade.lib.util.swing.action.SaveAction
import org.aiotrade.lib.util.swing.action.ViewAction
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent
import org.openide.nodes.BeanNode
import org.openide.nodes.Children
import org.openide.nodes.FilterNode
import org.openide.nodes.Node
import org.openide.nodes.NodeEvent
import org.openide.nodes.NodeListener
import org.openide.nodes.NodeMemberEvent
import org.openide.nodes.NodeReorderEvent
import org.openide.util.ImageUtilities
import org.openide.util.lookup.AbstractLookup
import org.openide.util.lookup.InstanceContent

/**
 *
 *
 *
 * @author Caoyuan Deng
 *
 * This node is just a virtul node without any physical object in file system
 *
 * The tree view of Stock and others
 * + Stocks (config/Stocks)
 *   +- sunw (sunw.ser)
 *      +- Indicators DescriptorGroupNodee)
 *      |  +- MACD (DescriptorNode)
 *      |  |   +-opt1
 *      |  |   +-opt2
 *      |  +- ROC
 *      |     +-opt1
 *      |     +-opt2
 *      +- Drawings DescriptorGroupNodee)
 *         +- layer1
 *         |  +- line
 *         |  +- parallel
 *         |  +- gann period
 *         +- layer2
 */
object DescriptorNode {
  private val ACTIVE_ICON   = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/showingTrue.gif")
  private val NOACTIVE_ICON = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/showingFalse.gif")
}

import DescriptorNode._
@throws(classOf[IntrospectionException])
class DescriptorNode(descriptorInfo: Descriptor[_], content: Content, ic: InstanceContent
) extends FilterNode(new BeanNode[Descriptor[_]](descriptorInfo), Children.LEAF, new AbstractLookup(ic)) {

  /**
   * the descriptor param may be a clone of descritor in content, so we
   * should lookup that one in content instead put it in lookup content.
   * Otherwise, the descriptor stored in viewContainer (which is from content)
   * may not the one stored here.
   * @TODO The better solution is give a NodeInfo param
   */
  private val descriptor = content.lookupDescriptor(
    descriptorInfo.getClass.asInstanceOf[Class[Descriptor[_]]],
    descriptorInfo.serviceClassName,
    descriptorInfo.freq
  ).get

  /* adds additional items to the lookup */
  ic.add(content)
    
  def this(descriptor: Descriptor[_], content: Content) = {
    this(descriptor, content, new InstanceContent)
    /**
     * @TODO
     * this method seems that will call descriptor.setName(String) automatically,
     * so should avoid define setName(String) method in desctiptor to avoid
     * unexpected behave
     *
     * Should use another super class to replace FilterNode? (because FilterNode
     * wraps descriptor as a BeanNode)
     */
    setName(descriptor.displayName)
  }
    
  override def getDisplayName = {
    descriptor.displayName
  }
    
  /**
   * Making a tooltip out of the descriptorInfo's description
   */
  override def getShortDescription = {
    descriptor.displayName
  }
    
  override def canRename: Boolean = {
    descriptor.isInstanceOf[DrawingDescriptor]
  }
    
    
  /**
   * Providing the Open action on a stock descriptorInfo
   */
  override def getActions(popup: Boolean): Array[Action] = {        
    /** Use SystemAction to find instance of those actions registered in layer.xml */
    Array(
      descriptor.lookupAction(classOf[ViewAction]).getOrElse(null),
      descriptor.lookupAction(classOf[HideAction]).getOrElse(null),
      descriptor.lookupAction(classOf[EditAction]).getOrElse(null),
      null,
      descriptor.lookupAction(classOf[DeleteAction]).getOrElse(null)
    )
  }
    
  override def getPreferredAction: Action = {
    descriptor.lookupAction(classOf[ViewAction]).getOrElse(null)
  }
    
  override def getIcon(tpe: Int): Image = {
    if (descriptor.active) ACTIVE_ICON else NOACTIVE_ICON
  }
    
  override def getOpenedIcon(tpe: Int): Image = {
    getIcon(0)
  }
    
  def refreshIcon {
    fireIconChange
  }
    
  def refreshDisplayName {
    fireDisplayNameChange(getDisplayName, getDisplayName)
  }
    
  override protected def createNodeListener: NodeListener = {
    val delegate = super.createNodeListener
    val newListener = new NodeListener {
      def childrenAdded(nodeMemberEvent: NodeMemberEvent) {
        delegate.childrenAdded(nodeMemberEvent)
      }
            
      def childrenRemoved(nodeMemberEvent: NodeMemberEvent) {
        delegate.childrenRemoved(nodeMemberEvent)
      }
            
      def childrenReordered(nodeReorderEvent: NodeReorderEvent) {
        delegate.childrenReordered(nodeReorderEvent)
      }
            
      def nodeDestroyed(nodeEvent: NodeEvent) {
        delegate.nodeDestroyed(nodeEvent)
      }
            
      def propertyChange(evt: PropertyChangeEvent) {
        delegate.propertyChange(evt)
        if (evt.getPropertyName == Node.PROP_NAME) {
          val newName = evt.getNewValue.toString
                    
          if (descriptor.isInstanceOf[DrawingDescriptor]) {
            for (analysisWin <- AnalysisChartTopComponent.selected;
                 drawing <- analysisWin.lookupDrawing(descriptor.asInstanceOf[DrawingDescriptor])
            ) {
              drawing.layerName = newName
            }
                        
            descriptor.serviceClassName = evt.getNewValue.toString
            refreshDisplayName
          }
          descriptor.containerContent.lookupAction(classOf[SaveAction]) foreach {_ execute}
        }
      }
    }

    newListener
  }
    
}

