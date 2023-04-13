package org.aiotrade.lib.amqp

import java.util.regex.Pattern
import scala.collection.mutable.Map

object ContentType {
  private val TOKEN = "[\\p{ASCII}&&[^\\p{Cntrl} ;/=\\[\\]\\(\\)\\<\\>\\@\\,\\:\\\"\\?\\=]]+"
  private val TOKEN_PATTERN = Pattern.compile("^" + TOKEN + "$")
  private val TYPE_PATTERN = Pattern.compile(
    "(" + TOKEN + ")" +         // type  (G1)
    "/" +                       // separator
    "(" + TOKEN + ")" +         // subtype (G2)
    "\\s*(.*)\\s*", Pattern.DOTALL
  )
  private val ATTR_PATTERN = Pattern.compile(
    "\\s*;\\s*" +
    "(" + TOKEN + ")" +         // attr name  (G1)
    "\\s*=\\s*" +
    "(?:" +
    "\"([^\"]*)\"" +            // value as quoted string (G3)
    "|" +
    "(" + TOKEN + ")?" +        // value as token (G2)
    ")"
  )

  val ATTR_CHARSET = "charset"
  val STAR = "*"
  private val DEFAULT_CHARSET = ATTR_CHARSET + "=UTF-8"

  case object ANY                    extends CaseContentType("*", "*", null)
  case object APPLICATION_XML        extends CaseContentType("application", "xml", null)
  case object ATOM                   extends CaseContentType("application", "atom+xml", DEFAULT_CHARSET)
  case object ATOM_ENTRY             extends CaseContentType("application", "atom+xml;type=entry", DEFAULT_CHARSET)
  case object ATOM_FEED              extends CaseContentType("application", "atom+xml;type=feed", DEFAULT_CHARSET)
  case object ATOM_SERVICE           extends CaseContentType("application", "atomsvc+xml", DEFAULT_CHARSET)
  case object GDATA_ERROR            extends CaseContentType("application", "vnd.google.gdata.error+xml", null)
  case object JSON                   extends CaseContentType("application", "json", DEFAULT_CHARSET)
  case object JAVA_SERIALIZED_OBJECT extends CaseContentType("application", "java-serialized-object", null)
  case object JAVASCRIPT             extends CaseContentType("text", "javascript", DEFAULT_CHARSET)
  case object MESSAGE_RFC822         extends CaseContentType("message", "rfc822", null)
  case object MULTIPART_RELATED      extends CaseContentType("multipart", "related", null)
  case object OPENSEARCH             extends CaseContentType("application", "opensearchdescription+xml", null)
  case object OCTET_STREAM           extends CaseContentType("application", "octet-stream", null) // Non-interpreted binary files
  case object RSS                    extends CaseContentType("application", "rss+xml", DEFAULT_CHARSET)
  case object TEXT_XML               extends CaseContentType("text", "xml", DEFAULT_CHARSET)
  case object TEXT_HTML              extends CaseContentType("text", "html", DEFAULT_CHARSET)
  case object TEXT_PLAIN             extends CaseContentType("text", "plain", DEFAULT_CHARSET)
  case object AVRO                   extends CaseContentType("application", "avro", DEFAULT_CHARSET)
  case object AVRO_BINARY            extends CaseContentType("avro", "binary", DEFAULT_CHARSET)
  
  /**
   * Determines the best "Content-Type" header to use in a servlet response
   * based on the "Accept" header from a servlet request.
   *
   * @param acceptHeader       "Accept" header value from a servlet request (not
   *                           <code>null</code>)
   * @param actualContentTypes actual content types in descending order of
   *                           preference (non-empty, and each entry is of the
   *                           form "type/subtype" without the wildcard char
   *                           '*') or <code>null</code> if no "Accept" header
   *                           was specified
   * @return the best content type to use (or <code>None</code> on no match).
   */
  def getBestContentType(acceptHeader: String, actualContentTypes: List[ContentType]): Option[ContentType] = {

    // If not accept header is specified, return the first actual type
    if (acceptHeader == null) {
      return Some(actualContentTypes.head)
    }

    // iterate over all of the accepted content types to find the best match
    var bestQ = 0f
    var bestContentType: ContentType = null
    val acceptedTypes = acceptHeader.split(",")

    def loopAcceptedTypes(i: Int): Unit = {
      if (i >= acceptedTypes.length) {
        return
      }

      val acceptedTypeString = acceptedTypes(i)

      // create the content type object
      var acceptedContentType: ContentType = null
      try {
        acceptedContentType = apply(acceptedTypeString.trim)
      } catch {case ex: IllegalArgumentException => loopAcceptedTypes(i + 1)}

      // parse the "q" value (default of 1)
      var curQ = 1f
      try {
        val qAttr = acceptedContentType.attribute("q").getOrElse(null)
        if (qAttr != null) {
          val qValue = qAttr.toFloat
          if (qValue <= 0 || qValue > 1) {
            loopAcceptedTypes(i + 1)
          }
          curQ = qValue
        }
      } catch {case ex: NumberFormatException => loopAcceptedTypes(i + 1)}

      // only check it if it's at least as good ("q") as the best one so far
      if (curQ < bestQ) {
        loopAcceptedTypes(i + 1)
      }

      // iterate over the actual content types in order to find the best match
      // to the current accepted content type
      def loopActualContentTypes(types: List[ContentType]): Unit = {
        types match {
          case Nil =>
          case actualContentType :: tail =>
            // if the "q" value is the same as the current best, only check for
            // better content types
            if (curQ == bestQ && bestContentType == actualContentType) {
              return
            }

            // check if the accepted content type matches the current actual
            // content type
            if (actualContentType.matches(acceptedContentType)) {
              bestContentType = actualContentType
              bestQ = curQ
              return
            }

            loopActualContentTypes(tail)
        }
      }

      loopActualContentTypes(actualContentTypes)
    }

    loopAcceptedTypes(0)

    // if found an acceptable content type, return the best one
    if (bestQ != 0) {
      Some(bestContentType)
    } else None
  }

  /**
   * Constructs a new instance from a content-type header value
   * parsing the MIME content type (RFC2045) format.  If the type
   * is {@code null}, then media type and charset will be
   * initialized to default values.
   *
   * @param typeHeader content type value in RFC2045 header format.
   */
  def apply(typeHeader: String) = {
    val attributes = Map[String, String]()
    var inferredCharset = false

    // parse type and subtype
    val typeMatch = TYPE_PATTERN.matcher(typeHeader)
    if (!typeMatch.matches) {
      throw new IllegalArgumentException("Invalid content type:" + typeHeader)
    }

    val tpe    = typeMatch.group(1).toLowerCase
    val subTpe = typeMatch.group(2).toLowerCase
    if (typeMatch.groupCount >= 3) {
      // Get attributes (if any)
      val attrMatch = ATTR_PATTERN.matcher(typeMatch.group(3))
      while (attrMatch.find) {
        var value = attrMatch.group(2)
        if (value == null) {
          value = attrMatch.group(3)
          if (value == null) {
            value = ""
          }
        }

        attributes(attrMatch.group(1).toLowerCase) = value
      }

      // Infer a default charset encoding if unspecified.
      if (!attributes.contains(ATTR_CHARSET)) {
        inferredCharset = true
        (tpe, subTpe) match {
          case ("application", _) if subTpe.endsWith("xml") =>
            // BUGBUG: Actually have need to look at the raw stream here, but
            // if client omitted the charset for "application/xml", they are
            // ignoring the STRONGLY RECOMMEND language in RFC 3023, sec 3.2.
            // I have little sympathy.
            attributes(ATTR_CHARSET) = "utf-8"      // best guess
          case ("application", _) =>
            attributes(ATTR_CHARSET) = "us-ascii"   // RFC3023, sec 3.1
          case (_, "json") =>
            attributes(ATTR_CHARSET) = "utf-8"      // RFC4627, sec 3
          case _ =>
            attributes(ATTR_CHARSET) = "iso-8859-1" // http default
        }
      }
    }

    new ContentType(tpe, subTpe, attributes, inferredCharset, false)
  }

  class JsonContentType(clz: String) extends ContentType(JSON.tpe, JSON.subTpe, Map("class" -> clz))

  abstract class CaseContentType(tpe: String, subTpe: String, charset: String) extends ContentType(tpe, subTpe, charset, true)
}

/**
 * Simple class for parsing and generating Content-Type header values, per
 * RFC 2045 (MIME) and 2616 (HTTP 1.1).
 */
import ContentType._
@serializable
class ContentType(val tpe: String,
                  val subTpe: String,
                  private val attributes: Map[String, String],
                  private val inferredCharset: Boolean,
                  private val immutable: Boolean) {

  def this(tpe: String, subTpe: String, attributes: Map[String, String]) = this(tpe, subTpe, attributes, false, false)
  def this(tpe: String, subTpe: String, charset: String, immutable: Boolean) = this(tpe, subTpe, if (charset != null) Map(ATTR_CHARSET -> charset) else Map[String, String](), false, true)
  def this(tpe: String, subTpe: String, charset: String) = this(tpe, subTpe, charset, false)
  def this(tpe: String, subTpe: String) = this(tpe, subTpe, Map[String, String]())
  def this() = this("application", "octet-stream", Map(ATTR_CHARSET -> "iso-8859-1")) // iso-8859-1 is http default

  /** Returns the full mime type */
  val mimeType = {
    val sb = new StringBuilder
    sb.append(tpe)
    sb.append("/")
    sb.append(subTpe)
    if (attributes.contains("type")) {
      sb.append(";type=").append(attributes.get("type"))
    }
    sb.toString
  }

  private def assertNotLocked {
    if (immutable) {
      throw new IllegalStateException("Unmodifiable instance")
    }
  }

  /**
   * Returns the additional attribute by name of the content type.
   *
   * @param name attribute name
   */
  def attribute(name: String): Option[String] = {
    attributes.get(name)
  }

  def attribute(name: String, value: String) {
    attributes(name) = value
  }

  /*
   * Returns the charset attribute of the content type or null if the
   * attribute has not been set.
   */
  def charset: Option[String] = {
    attributes.get(ATTR_CHARSET)
  }


  /**
   * Returns whether this content type is match by the content type found in the
   * "Accept" header field of an HTTP request.
   *
   * <p>For atom content type, this method will check the optional attribute
   * 'type'. If the type attribute is set in both this and {@code
   * acceptedContentType}, then they must be the same. That is, {@code
   * application/atom+xml} will match both {@code
   * application/atom+xml;type=feed} and {@code
   * application/atom+xml;type=entry}, but {@code
   * application/atom+xml;type=entry} will not match {@code
   * application/atom+xml;type=feed}.a
   *
   * @param acceptedContentType content type found in the "Accept" header field
   *                            of an HTTP request
   */
  def matches(acceptedContentType: ContentType): Boolean = {
    val acceptedType = acceptedContentType.tpe
    val acceptedSubType = acceptedContentType.subTpe
    
    acceptedType == STAR ||
    acceptedType == tpe &&
    (acceptedSubType == STAR || acceptedSubType == subTpe) &&
    (!isAtom || matchsAtom(acceptedContentType))
  }

  /** Returns true if this is an atom content type. */
  private def isAtom: Boolean = {
    tpe == "application" && subTpe == "atom+xml"
  }

  /**
   * Compares the optional 'type' attribute of two content types.
   *
   * <p>This method accepts atom content type without the 'type' attribute
   * but if the types are specified, they must match.
   */
  private def matchsAtom(acceptedContentType: ContentType): Boolean = {
    (attribute("type"), acceptedContentType.attribute("type")) match {
      case (None, _) => true
      case (_, None) => true
      case (Some(atomType), Some(acceptedAtomType)) if atomType == acceptedAtomType => true
      case _ => false
    }
  }

  /**
   * Generates the Content-Type value
   */
  override def toString = {
    val sb = new StringBuffer
    sb.append(tpe)
    sb.append("/")
    sb.append(subTpe)
    for ((name, value) <- attributes) {

      // Don't include any inferred charset attribute in output.
      if (inferredCharset && name == ATTR_CHARSET) {
        // continue
      } else {
        sb.append(";")
        sb.append(name)
        sb.append("=")
        val tokenMatcher = TOKEN_PATTERN.matcher(value)
        if (tokenMatcher.matches) {
          sb.append(value)
        } else {
          sb.append("\"" + value + "\"")
        }
      }
    }
    sb.toString
  }

  override def equals(other: Any) = other match {
    case that: ContentType if that eq this => true
    case that: ContentType =>
      tpe == that.tpe && subTpe == that.subTpe && attributes == that.attributes
    case _ => false
  }

  override def hashCode = {
    (tpe.hashCode * 31 + subTpe.hashCode) * 31 + attributes.hashCode
  }
}
