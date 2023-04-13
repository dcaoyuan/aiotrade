package org.aiotrade.lib.util.config

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale
import java.util.Properties
import java.util.ResourceBundle
import net.lag.configgy.Configgy
import net.lag.configgy.ParseException
import net.lag.logging.Logger

final case class ConfigurationException(message: String) extends RuntimeException(message)

/**
 * Loads up the configuration (from the app.conf file).
 */
object Config {
  private val log = Logger.get(this.getClass.getName)

  val mode = System.getProperty("run.mode", "development")
  val version = "0.10"

  lazy val configDir: Option[String] = List("../conf", "../etc") find {x =>
    val f = new File(x)
    f.exists && f.isDirectory
  }

  private var _config: net.lag.configgy.Config = _

  def apply(fileName: String = null): net.lag.configgy.Config = {
    if (_config == null) {
      val classLoader = Thread.currentThread.getContextClassLoader

      _config = if (fileName != null) {
        try {
          val nbUserPath = System.getProperty("netbeans.user")
          Configgy.configure(fileName)
          log.info("Config loaded directly from [%s].", fileName)
          log.info("netbeans.user=" + nbUserPath)
        } catch {
          case e: ParseException => throw new ConfigurationException(
              "The '" + fileName + " config file can not be found" +
              "\n\tdue to: " + e.toString)
        }
        Configgy.config
      } else if (System.getProperty("run.config", "") != "") {
        val configFile = System.getProperty("run.config", "")
        try {
          Configgy.configure(configFile)
          log.info("Config loaded from -D" + "run.config=%s", configFile)
        } catch {
          case e: ParseException => throw new ConfigurationException(
              "Config could not be loaded from -D" + "run.config=" + configFile +
              "\n\tdue to: " + e.toString)
        }
        Configgy.config
      } else if (configDir.isDefined) {
        try {
          val configFile = configDir.get + "/" + mode + ".conf"
          Configgy.configure(configFile)
          log.info("configDir is defined as [%s], config loaded from [%s].", configDir.get, configFile)
        } catch {
          case e: ParseException => throw new ConfigurationException(
              "configDir is defined as [" + configDir.get + "] " +
              "\n\tbut the '" + mode + ".conf' config file can not be found at [" + configDir.get + "/" + mode + ".conf]," +
              "\n\tdue to: " + e.toString)
        }
        Configgy.config
      } else if (classLoader.getResource(mode + ".conf") != null) {
        try {
          Configgy.configureFromResource(mode + ".conf", classLoader)
          log.info("Config loaded from the application classpath [%s].", mode + ".conf")
        } catch {
          case e: ParseException => throw new ConfigurationException(
              "Can't load '" + mode + ".conf' config file from application classpath," +
              "\n\tdue to: " + e.toString)
        }
        Configgy.config
      } else {
        log.warning(
          "\nCan't load '" + mode + ".conf'." +
          "\nOne of the three ways of locating the '" + mode + ".conf' file needs to be defined:" +
          "\n\t1. Define the '-D" + mode + ".config=...' system property option." +
          "\n\t2. Define './conf' directory." +
          "\n\t3. Put the '" + mode + ".conf' file on the classpath." +
          "\nI have no way of finding the '" + mode + ".conf' configuration file." +
          "\nUsing default values everywhere.")
        net.lag.configgy.Config.fromString("<" + mode + "></" + mode + ">") // default empty config
      }
    }

    val configVersion = _config.getString(mode + ".version", version)
    if (version != configVersion)
      throw new ConfigurationException(
        mode + " version [" + version + "] is different than the provided config ('" + mode + ".conf') version [" + configVersion + "]")

    _config
  }

  val startTime = System.currentTimeMillis
  def uptime = (System.currentTimeMillis - startTime) / 1000


  // --- todo for properties
  def loadProperties(fileName: String) {
    val props = new Properties
    val file = new File(fileName)
    if (file.exists) {
      try {
        val is = new FileInputStream(file)
        if (is != null) props.load(is)
        is.close
      } catch {case _: Throwable =>}
    }
  }

  private val SUFFIX = ".properties"
  def loadProperties($name: String, LOAD_AS_RESOURCE_BUNDLE: Boolean = false): Properties = {
    var name = $name
    if ($name.startsWith("/"))  name = $name.substring(1)
    
    if ($name.endsWith(SUFFIX)) name = $name.substring(0, $name.length - SUFFIX.length)
    
    val props = new Properties

    val loader = classLoader
    var in: InputStream = null
    try {
      if (LOAD_AS_RESOURCE_BUNDLE) {
        name = name.replace('/', '.')
        val rb = ResourceBundle.getBundle(name, Locale.getDefault, loader)
        val keys = rb.getKeys
        while (keys.hasMoreElements) {
          props.put(keys.nextElement.asInstanceOf[String], rb.getString(keys.nextElement.asInstanceOf[String]))
        }
      } else {
        name = name.replace('.', '/')
        if (!name.endsWith(SUFFIX)) name = name.concat(SUFFIX)
        in = loader.getResourceAsStream(name)
        if (in != null) {
          props.load(in) // can throw IOException
        }
      }
      in.close
    } catch {case _: Throwable =>}
    props
  }

  // ### Classloading

  def classLoader: ClassLoader =
    if (_config != null ) {
      _config.getString(mode + ".classLoader") match {
        //case Some(cld: ClassLoader) => cld
        case _ => Thread.currentThread.getContextClassLoader
      }
    } else Thread.currentThread.getContextClassLoader
  def loadClass[C](name: String): Class[C] =
    Class.forName(name, true, classLoader).asInstanceOf[Class[C]]
  def newObject[C](name: String, default: => C): C =
    if (_config != null) {
      _config.getString(name) match {
        case Some(s: String) => loadClass[C](s).newInstance
        case _ => default
      }
    } else default
}
