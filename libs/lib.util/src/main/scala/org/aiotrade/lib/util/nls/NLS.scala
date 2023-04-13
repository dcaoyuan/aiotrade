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

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * The purpose of this class is to provide NLS (National Language Support)
 * configurable per thread.
 */
class NLS(locale: Locale) {
  private val map = new ConcurrentHashMap[Class[_], TranslationBundle](8, 0.9f, 1)

  private def get[T <: TranslationBundle](tpe: Class[T]): T = {
    var bundle = map.get(tpe)
    if (bundle == null) {
      bundle = GlobalBundleCache.lookupBundle(locale, tpe);
      // There is a small opportunity for a race, which we may
      // lose. Accept defeat and return the winner's instance.
      val old = map.putIfAbsent(tpe, bundle)
      if (old != null)
        bundle = old
    }
    bundle.asInstanceOf[T]
  }
}

object NLS {
  /** The root locale constant. It is defined here because the Locale.ROOT is not defined in Java 5 */
  def ROOT_LOCALE = new Locale("", "", "")

  private def local = new InheritableThreadLocal[NLS] {
    override protected def initialValue = new NLS(Locale.getDefault)
  }
  
  /**
   * Sets the locale for the calling thread.
   * <p>
   * The {@link #getBundleFor(Class)} method will honor this setting if if it
   * is supported by the provided resource bundle property files. Otherwise,
   * it will use a fall back locale as described in the
   * {@link TranslationBundle}
   *
   * @param locale
   *            the preferred locale
   */
  def setLocale(locale: Locale) {
    local.set(new NLS(locale))
  }

  /**
   * Sets the JVM default locale as the locale for the calling thread.
   * <p>
   * Semantically this is equivalent to <code>NLS.setLocale(Locale.getDefault())</code>.
   */
  def useJVMDefaultLocale {
    local.set(new NLS(Locale.getDefault))
  }

  /**
   * Returns an instance of the translation bundle of the required type. All
   * public String fields of the bundle instance will get their values
   * injected as described in the {@link TranslationBundle}.
   *
   * @param <T>
   *            required bundle type
   * @param type
   *            required bundle type
   * @return an instance of the required bundle type
   * @exception TranslationBundleLoadingException see {@link TranslationBundleLoadingException}
   * @exception TranslationStringMissingException see {@link TranslationStringMissingException}
   */
  def getBundleFor[T <: TranslationBundle](tpe: Class[T]): T = {
    local.get.get(tpe)
  }

}