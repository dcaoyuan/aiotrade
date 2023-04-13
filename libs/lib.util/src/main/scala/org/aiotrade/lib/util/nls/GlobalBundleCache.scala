/*
 * Copyright (C) 2010, Sasa Zivkov <sasa.zivkov@sap.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.aiotrade.lib.util.nls

import java.util.HashMap
import java.util.Locale
import java.util.Map

/**
 * Global cache of translation bundles.
 * <p>
 * Every translation bundle will be cached here when it gets loaded for the
 * first time from a thread. Another lookup for the same translation bundle
 * (same locale and type) from the same or a different thread will return the
 * cached one.
 * <p>
 * Note that NLS instances maintain per-thread Map of loaded translation
 * bundles. Once a thread accesses a translation bundle it will keep reference
 * to it and will not call {@link #lookupBundle(Locale, Class)} again for the
 * same translation bundle as long as its locale doesn't change.
 */
object GlobalBundleCache {
  private val cachedBundles = new HashMap[Locale, Map[Class[_], TranslationBundle]]()


  /**
   * Looks up for a translation bundle in the global cache. If found returns
   * the cached bundle. If not found creates a new instance puts it into the
   * cache and returns it.
   *
   * @param <T>
   *            required bundle type
   * @param locale
   *            the preferred locale
   * @param type
   *            required bundle type
   * @return an instance of the required bundle type
   * @exception TranslationBundleLoadingException see {@link TranslationBundle#load(Locale)}
   * @exception TranslationStringMissingException see {@link TranslationBundle#load(Locale)}
   */
  def lookupBundle[T <: TranslationBundle](locale: Locale, tpe: Class[T]): T = synchronized {
    try {
      var bundles = cachedBundles.get(locale)
      if (bundles == null) {
        bundles = new HashMap[Class[_], TranslationBundle]();
        cachedBundles.put(locale, bundles)
      }
      var bundle = bundles.get(tpe)
      if (bundle == null) {
        bundle = tpe.newInstance
        bundle.load(locale)
        bundles.put(tpe, bundle)
      }
      bundle.asInstanceOf[T]
    } catch {
      case e: InstantiationException => throw new Error(e)
      case e: IllegalAccessException=> throw new Error(e)
    }
  }
}
