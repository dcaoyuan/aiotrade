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
import java.beans.IntrospectionException;
import java.util.HashSet;
import javax.swing.Action;
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.Content;
import org.aiotrade.lib.math.timeseries.descriptor.Descriptor;
import org.aiotrade.lib.util.swing.action.AddAction;
import org.aiotrade.lib.util.swing.action.RefreshAction;
import org.aiotrade.lib.util.swing.action.UpdateAction;
import org.aiotrade.modules.ui.GroupDescriptor
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

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
 *      +- Indicators (DescriptorGroupNode)
 *      |  +- MACD (DescriptorNode)
 *      |  |   +-opt1
 *      |  |   +-opt2
 *      |  +- ROC
 *      |     +-opt1
 *      |     +-opt2
 *      +- Drawings (DescriptorGroupNode)
 *         +- layer1
 *         |  +- line
 *         |  +- parallel
 *         |  +- gann period
 *         +- layer2
 */
object GroupNode {
  /**
   * The children wrap class
   * ------------------------------------------------------------------------
   */
  private class GroupChildren(content: Content, groupClass: Class[Descriptor[_]]) extends Children.Keys[Descriptor[_]] {

    /**
     * since setKeys(childrenKeys) will copy the elements of childrenKeys, it's safe to
     * use a repeatly used bufChildrenKeys here.
     * And, to sort them in letter order, we can use a SortedSet to copy from collection.(TODO)
     */
    private val bufChildrenKeys = new java.util.HashSet[Descriptor[_]]

    override def addNotify {
      val node = getNode.asInstanceOf[GroupNode]
      bufChildrenKeys.clear
      val descriptors = content.lookupDescriptors(groupClass, node.freq).iterator
      while (descriptors.hasNext) {
        bufChildrenKeys.add(descriptors.next)
      }
      setKeys(bufChildrenKeys)
    }

    def createNodes(key: Descriptor[_]): Array[Node] = {
      try {
        Array(new DescriptorNode(key, content))
      } catch {
        case ex: IntrospectionException =>
          ErrorManager.getDefault.notify(ErrorManager.INFORMATIONAL, ex)
          /** Should never happen - no reason for it to fail above */
          Array(new AbstractNode(Children.LEAF) {
              override def getHtmlDisplayName = "<font color='red'>" + ex.getMessage + "</font>"
            }
          )
      }
    }
  }

  private class GroupRefreshAction(node: GroupNode) extends RefreshAction {

    def execute {
      val children = node.getChildren.asInstanceOf[GroupChildren]
      /** if new descriptor is added, this will add it to children */
      children.addNotify
      for (child <- children.getNodes) {
        child.asInstanceOf[DescriptorNode].refreshIcon
        child.asInstanceOf[DescriptorNode].refreshDisplayName
      }
    }
  }

  private class GroupUpdateAction(node: GroupNode) extends UpdateAction {
    def execute {
      val children = node.getChildren.asInstanceOf[GroupChildren]
      /**
       * by calling children.addNotify(), the children will re setKeys()
       * according to the current time unit and nUnits.
       * @see GroupChildren.addNotify()
       */
      children.addNotify
    }
  }
}

@throws(classOf[IntrospectionException])
class GroupNode(group: GroupDescriptor[Descriptor[_]], content: Content, ic: InstanceContent
) extends FilterNode(new BeanNode[GroupDescriptor[_]](group), new GroupNode.GroupChildren(content, group.getBindClass), new AbstractLookup(ic)) {
  import GroupNode._

  private var _freq: TFreq = TFreq.DAILY

  /* add aditional items to the lookup */
  ic.add(content)

  ic.add(new GroupRefreshAction(this))
  ic.add(new GroupUpdateAction(this))

  /**
   * add actions carried with nodeInfo
   */
  for (action <- group.createActions(content)) {
    /**
     * as content only do flat lookup, should add actions one by one,
     * instead of adding an array, otherwise this.getLookup().loopup
     * can only search an array.
     */
    ic.add(action)
  }


  @throws(classOf[IntrospectionException])
  def this(group: GroupDescriptor[Descriptor[_]], content: Content) = {
    this(group, content, new InstanceContent)

    setName(group.getDisplayName)
  }

  /**
   * Providing the Open action on a each descriptor groupClass
   */
  override def getActions(popup: Boolean): Array[Action] = {
    /**
     * Use SystemAction to find instance of those actions registered in layer.xml
     *
     * The following code works for any kind of group node witch implemented
     * AddAction and has been added into the lookup content in construction.
     */
    Array(getLookup.lookup(classOf[AddAction]))

  }

  override def getPreferredAction: Action = {
    getActions(false)(0);
  }

  override def getOpenedIcon(tpe: Int): Image = {
    getIcon(0);
  }

  override def getDisplayName: String = {
    group.getDisplayName
  }

  /**
   * Making a tooltip out of the descriptor's description
   */
  override def getShortDescription: String = {
    group.getTooltip
  }

  override def getIcon(tpe: Int): Image = {
    group.getIcon(tpe)
  }

  def freq: TFreq = _freq
  def freq_=(freq: TFreq) {
    this._freq = freq
    getLookup.lookup(classOf[UpdateAction]).execute
  }


}
