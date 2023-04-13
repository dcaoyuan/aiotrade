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
package org.aiotrade.lib.securities.data.git

import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.net.MalformedURLException
import java.net.URL
import java.text.MessageFormat
import java.util.logging.Level
import java.util.logging.Logger
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.PullResult
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.transport.URIish

/**
 *
 * @author Caoyuan Deng
 */
object Git {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private var _monitor: ProgressMonitor = _
  private def monitor = {
    if (_monitor == null) {
      _monitor = new TextProgressMonitor
    }
    _monitor
  }
  def monitor_=(monitor: ProgressMonitor) {
    _monitor = monitor
  }
  
  def clone(gitPath: String, sourceUri: String, 
            localName: String = null, 
            branch: String = null,
            remote: String = Constants.DEFAULT_REMOTE_NAME
  ): Option[org.eclipse.jgit.api.Git] = {
  
    try {
      val gitDir = guessGitDir(gitPath, localName, sourceUri)    
      if (gitDir.exists) {
        log.info(gitDir.getAbsolutePath + " existed, will delete it first.")
        try {
          deleteDir(gitDir)
          log.info(gitDir.getAbsolutePath + " deleted: " + !gitDir.exists)
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex) 
        }
      }
    
      gitDir.mkdirs
      
      val cmd = new CloneCommand
      cmd.setDirectory(gitDir)
      cmd.setURI(sourceUri)
      if (branch != null) {
        cmd.setBranch(branch)
        cmd.setBranchesToClone(java.util.Collections.singletonList(branch))
      }
      cmd.setRemote(remote)
      cmd.setProgressMonitor(monitor)
    
      val t0 = System.currentTimeMillis
      val git = cmd.call
      log.info("Cloned in " + (System.currentTimeMillis - t0) / 1000.0 + "s")
      Option(git)
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); None
    }
  }
  
  def pull(gitPath: String): Option[PullResult] = getGit(gitPath) flatMap {pull(_)}
  
  def pull(git: org.eclipse.jgit.api.Git): Option[PullResult] = {
    val cmd = git.pull
    cmd.setProgressMonitor(monitor)

    val t0 = System.currentTimeMillis
    val pullResult = try {
      Option(cmd.call)
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); None
    }
    log.info("Pulled in " + (System.currentTimeMillis - t0) / 1000.0 + "s")
    
    pullResult
  }
  
  def addAll(gitPath: String) {getGit(gitPath) foreach {addAll(_)}}
  def addAll(git: org.eclipse.jgit.api.Git) {
    val cmd = git.add
    cmd.addFilepattern(".")
    
    val t0 = System.currentTimeMillis
    try {
      cmd.call
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex) 
    }
    log.info("Added all in " + (System.currentTimeMillis - t0) / 1000.0 + "s")
  }

  def commitAll(gitPath: String, msg: String) {getGit(gitPath) foreach {commitAll(_, msg)}}
  def commitAll(git: org.eclipse.jgit.api.Git, msg: String) {
    val cmd = git.commit
    cmd.setAll(true)
    cmd.setMessage(msg)
    
    val t0 = System.currentTimeMillis
    try {
      cmd.call
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex) 
    }
    log.info("Committed in " + (System.currentTimeMillis - t0) / 1000.0 + "s")
  }
  
  def pushAll(gitPath: String) {getGit(gitPath) foreach {pushAll(_)}}
  def pushAll(git: org.eclipse.jgit.api.Git, remote: String = Constants.DEFAULT_REMOTE_NAME) {
    val cmd = git.push
    cmd.setRemote(remote).setPushAll
    cmd.setProgressMonitor(monitor)

    val t0 = System.currentTimeMillis
    try {
      cmd.call
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex) 
    }
    log.info("Pushed in " + (System.currentTimeMillis - t0) / 1000.0 + "s")
  }
  
  // --- helper
  
  def getGit(gitPath: String, localName: String = null): Option[org.eclipse.jgit.api.Git] = {
    try {
      val gitDir = guessGitDir(gitPath, localName)   
      val repo = openGitRepository(gitDir)
      Option(new org.eclipse.jgit.api.Git(repo))
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); None
    }
  }
  
  @throws(classOf[Exception])
  private def guessGitDir(gitPath: String, aLocalName: String = null, sourceUri: String = null): File = {
    if (aLocalName != null && gitPath != null) {
      throw new RuntimeException(CLIText().conflictingUsageOf_git_dir_andArguments)
    }
    
    val gitDir = if (gitPath == null) {
      val localName = if (aLocalName == null && sourceUri != null) {
        try {
          new URIish(sourceUri).getHumanishName
        } catch {
          case e: IllegalArgumentException => throw new Exception(MessageFormat.format(CLIText().cannotGuessLocalNameFrom, sourceUri))
        }
      } else aLocalName
      
      new File(localName).getAbsolutePath
    } else gitPath
    
    new File(gitDir)
  }
  
  private def deleteDir(dir: File) {
    try {
      val files = dir.listFiles
      var i = 0
      while (i < files.length) {
        val file = files(i)
        if (file.isDirectory) {
          deleteDir(file)
        } else {
          file.delete
        }
        i += 1
      }
      dir.delete
    } catch {
      case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
    }
  }
  
  @throws(classOf[IOException])
  private def openGitRepository(gitPath: File): Repository = {
    val rb = (new RepositoryBuilder).setGitDir(gitPath).readEnvironment.findGitDir
    if (rb.getGitDir == null) throw new RuntimeException(CLIText().cantFindGitDirectory)
    
    rb.build
  }
  
  @throws(classOf[MalformedURLException])
  private def configureHttpProxy {
    val s = System.getenv("http_proxy")
    if (s == null || s.equals(""))
      return

    val u = new URL(if (s.indexOf("://") == -1) "http://" + s else s)
    if (!"http".equals(u.getProtocol))
      throw new MalformedURLException(MessageFormat.format(CLIText().invalidHttpProxyOnlyHttpSupported, s))

    val proxyHost = u.getHost
    val proxyPort = u.getPort

    System.setProperty("http.proxyHost", proxyHost)
    if (proxyPort > 0)
      System.setProperty("http.proxyPort", String.valueOf(proxyPort))

    val userpass = u.getUserInfo
    if (userpass != null && userpass.contains(":")) {
      val c = userpass.indexOf(':')
      val user = userpass.substring(0, c)
      val pass = userpass.substring(c + 1)
      CachedAuthenticator.add(CachedAuthenticator.CachedAuthentication(proxyHost, proxyPort, user, pass))
    }
  }
  
  // @Note: 'val dst = new FileRepository(gitDir)' in this method cause NetBeans run out of stack space 
//  @throws(classOf[Exception])
//  private def createFileRepository(gitDir: String) = {
//    val dst = new org.eclipse.jgit.storage.file.FileRepository(gitDir)
//    dst.create()
//    val dstcfg = dst.getConfig
//    dstcfg.setBoolean("core", null, "bare", false)
//    dstcfg.save
//
//    log.info("Initialized empty git repository in " + gitDir)
//    dst
//  }

  // --- simple test
  def main(args: Array[String]) {
    val userHome = System.getProperty("user.home")
    val tmpPath = userHome + File.separator + "gittest" + File.separator
    val workPath = tmpPath + "clone_test"
    
    if ({val file = new File(workPath); !file.exists}) {
      clone(workPath, "file://" + tmpPath + "origin_test.git")
    }
    
    val git = getGit(workPath + "/.git").get
    pull(git)
    
    // --- change some contents
    val file = new File(git.getRepository.getWorkTree, "a.txt")
    if (!file.exists) {
      file.createNewFile
    }
    val writer = new PrintWriter(file)
    writer.print("Content at " + new java.util.Date + "\n")
    writer.close
    // --- end change some contents

    addAll(git)
    commitAll(git, "from " + this.getClass.getName)
    pushAll(git)
  }
}
