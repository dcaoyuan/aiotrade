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
import java.util.MissingResourceException
import java.util.ResourceBundle
import org.aiotrade.lib.util.ClassVar

/**
 * Base class for all translation bundles that provides injection of translated
 * texts into public String fields.
 */
abstract class TranslationBundle {

  private var _effectiveLocale: Locale = _
  private var _resourceBundle: ResourceBundle = _

  /**
   * @return the locale locale used for loading the resource bundle from which
   *         the field values were taken
   */
  def effectiveLocale = _effectiveLocale

  /**
   * @return the resource bundle on which this translation bundle is based
   */
  def resourceBundle = _resourceBundle

  /**
   * Injects locale specific text in all instance fields of this instance.
   * Only public instance fields of type <code>String</code> are considered.
   * <p>
   * The name of this (sub)class plus the given <code>locale</code> parameter
   * define the resource bundle to be loaded. In other words the
   * <code>this.getClass().getName()</code> is used as the
   * <code>baseName</code> parameter in the
   * {@link ResourceBundle#getBundle(String, Locale)} parameter to load the
   * resource bundle.
   * <p>
   *
   * @param locale
   *            defines the locale to be used when loading the resource bundle
   * @exception TranslationBundleLoadingException see {@link TranslationBundleLoadingException}
   * @exception TranslationStringMissingException see {@link TranslationStringMissingException}
   */
  @throws(classOf[TranslationBundleLoadingException])
  def load(locale: Locale) {
    val bundleClass = getClass
    try {
      _resourceBundle = ResourceBundle.getBundle(bundleClass.getName, locale)
    } catch {
      case e: MissingResourceException => throw new TranslationBundleLoadingException(bundleClass, locale, e)
    }
    _effectiveLocale = resourceBundle.getLocale

    for (field@ClassVar(name, getter, setter) <- ClassVar.getPublicVars(bundleClass) if field.getter.getReturnType == classOf[String]) {
        try {
          val translatedText = resourceBundle.getString(name)
          field.asInstanceOf[ClassVar[TranslationBundle, String]].set(this, translatedText)
        } catch {
          case e: MissingResourceException =>
            throw new TranslationStringMissingException(bundleClass, locale, name, e)
          case e: IllegalArgumentException =>
            throw new Error(e)
          case e: IllegalAccessException =>
            throw new Error(e)
        }
    }
  }
}