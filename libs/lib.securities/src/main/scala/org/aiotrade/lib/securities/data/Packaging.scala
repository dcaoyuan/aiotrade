/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.securities.data

import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.securities.data.git.Git

/**
 * @see org.apache.maven.archetype.common.util.ListScanner#DEFAULTEXCLUDES, which contains 
 * the default exclude list taht excludes all '.git' files. We have to rename '.git' to
 * 'dotgit' to package it to jar. And further, we want to git clone  base data to
 * src/main/resource/data during mvn install, so we define this object here and execute it
 * during generate-resources phase.
 * 
 * @see pom.xml, maven-scala-plugin -> launcher 'git-clone-data'
 * 
 * @author Caoyuan Deng
 */
object Packaging {
  private val log = Logger.getLogger(this.getClass.getName)
  
  def main(args: Array[String]) {
    val destPath = args(0)
    val sourceUri = args(1)
    val branch = if (args.length > 2) args(2) else null
    log.info("Re-cloning basis data to " + destPath + " from " + sourceUri + ", branch=" + (if (branch == null) "refs/heads/master" else branch))
    try {
      Git.clone(destPath, sourceUri, branch = branch)
    } catch {
      case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
    }
    
    val dotGit = new File(destPath, ".git")
    if (dotGit.exists) {
      dotGit.renameTo(new File(destPath, "dotgit"))
    }
    else{
      log.info("There is no file of " + dotGit.getPath)
    }
  }

}
