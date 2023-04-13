/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.aiotrade.modules.quicksearch;

import java.util.Collections
import java.util.logging.Level;
import java.util.logging.Logger;
import org.aiotrade.spi.quicksearch.SearchProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.Lookups;
import scala.collection.mutable.ArrayBuffer


/**
 *
 * @author Dafe Simonek
 */
object ProviderModel {
  /** folder in layer file system where provider of fast access content are searched for */
  private val SEARCH_PROVIDERS_FOLDER = "/QuickSearch" //NOI18N
  private val COMMAND_PREFIX = "command" //NOI18N

  lazy val categories = loadCategories

  private lazy val knownCommands: Set[String] = {
    categories map (_.commandPrefix) toSet
  }

  def isKnownCommand(command: String): Boolean = {
    knownCommands.contains(command)
  }

  private def loadCategories: List[Category] = {
    val categoryFOs = FileUtil.getConfigFile(SEARCH_PROVIDERS_FOLDER).getChildren

    // respect ordering defined in layers
    val jList = new java.util.ArrayList[FileObject](categoryFOs.length)
    categoryFOs foreach (jList.add(_))
    val sortedCats = FileUtil.getOrder(jList, false) // allowing the NetBeans Platform to use the 'position' attribute

    val categories = new ArrayBuffer[Category](sortedCats.size)
    val itr = sortedCats.iterator
    while (itr.hasNext) {
      val curFO = itr.next
      val displayName = try {
        curFO.getFileSystem.getStatus.annotateName(curFO.getNameExt, Collections.singleton(curFO))
      } catch {case ex: FileStateInvalidException =>
          Logger.getLogger(getClass.getName).log(Level.WARNING,
                                                 "Obtaining display name for " + curFO + " failed.", ex)
          null
      }

      val commandPrefix = curFO.getAttribute(COMMAND_PREFIX) match {
        case cpAttr: String => cpAttr
        case _ => null
      }

      categories += Category(curFO, displayName, commandPrefix)
    }

    categories.toList
  }

  final case class Category(private val fo: FileObject, displayName: String, commandPrefix: String) {

    lazy val providers = loadProviders

    private def loadProviders = {
      val catProviders = Lookups.forPath(fo.getPath).lookupAll(classOf[SearchProvider]).iterator
      println(catProviders)
      var result = List[SearchProvider]()
      while (catProviders.hasNext) result ::= catProviders.next
      result
    }

    def name: String = {
      fo.getNameExt
    }
  } // end of Category

}