package com.outr.solr4s.name

import com.outr.solr4s._
import com.outr.solr4s.query._

import scala.collection.mutable.ListBuffer

case class HumanName(parts: List[NamePart]) extends Ordered[HumanName] {
  def firstByType(`type`: NamePartType): Option[NamePart] = parts.find(_.is(`type`))
  def byType(`type`: NamePartType): List[NamePart] = parts.filter(_.is(`type`))

  lazy val primary: List[String] = byType(NamePartType.Primary).map(_.value)
  lazy val first: Option[String] = primary.headOption
  lazy val middle: List[String] = primary.tail
  lazy val nick: Option[String] = firstByType(NamePartType.Nickname).map(_.value)
  lazy val family: Option[String] = firstByType(NamePartType.Family).map(_.value)
  lazy val salutation: Option[String] = firstByType(NamePartType.Salutation).map(_.value)
  lazy val prefix: Option[String] = firstByType(NamePartType.Prefix).map(_.value)
  lazy val suffix: Option[String] = firstByType(NamePartType.Suffix).map(_.value)
  lazy val postnominal: Option[String] = firstByType(NamePartType.Postnominal).map(_.value)

  lazy val full: String = {
    val b = new StringBuilder
    parts.foreach {
      case p if p.is(NamePartType.Comma) => b.append(',')
      case part => {
        if (b.nonEmpty) b.append(' ')
        val value = if (part.is(NamePartType.Nickname)) {
          s"'${part.value}'"
        } else {
          part.value
        }
        b.append(value)
      }
    }
    b.toString()
  }

  lazy val lastFirst: String = {
    val p1 = family.map(s => s"$s, ").getOrElse("")
    s"$p1${primary.mkString(" ")}"
  }

  def words(includeTypes: NamePartType*): List[String] = {
    val types = if (includeTypes.isEmpty) {
      NamePartType.all
    } else {
      includeTypes.toSet
    }
    parts.filter(p => types.contains(p.`type`)).flatMap(_.simple.split(' ').toList).filterNot(_.isEmpty)
  }

  /**
    * Returns true if the supplied HumanName is a probable match to this name
    */
  def isFamilialMatch(that: HumanName): Boolean = family.map(_.toLowerCase) == that.family.map(_.toLowerCase)

  def isPrimaryMatch(that: HumanName, minimumMatches: Int = 1): Boolean = {
    val p1 = this.primary.map(_.toLowerCase)
    val p2 = that.primary.map(_.toLowerCase)
    val matches = p1.count(p2.contains)
    matches >= minimumMatches
  }

  def isMatch(that: HumanName): Boolean = isFamilialMatch(that) && isPrimaryMatch(that)

  override def compare(that: HumanName): Int = this.lastFirst.compare(that.lastFirst)

  override def toString: String = s"HumanName(${parts.mkString(", ")})"
}

object HumanName {
  private val salutations = Set("mr", "master", "mister", "mrs", "miss", "ms", "dr", "prof", "rev", "fr", "judge",
    "honorable", "hon")
  private val prefixes = Set("bar", "ben", "bin", "da", "dal", "de la", "de", "del", "der", "di", "ibn", "la", "le",
    "san", "st", "ste", "van", "van der", "van den", "vel", "von")
  private val suffixes = Set("jr", "sr", "2", "3", "4", "5", "6", "ii", "iii", "iv", "v", "vi", "senior",
    "junior", "2d", "2nd", "3d", "3rd", "4d", "4th", "5d", "5th", "6d", "6th")
  private val postnominals = Set("phd", "esq", "esquire", "apr", "rph", "pe", "md", "ma", "dmd", "cme",
    "dds", "cpa", "dvm", "rdh")

  private lazy val NicknameRegex = """[('"]+(.+?)["')]+""".r
  private lazy val MultiNameRegex = """(.+) (and|&) (\S+) (.+)""".r

  def filter(name: String,
             fullNameField: Field[String],
             primaryNamesField: Field[String],
             familyNameField: Field[String],
             fullNameBoost: Double,
             primaryNameBoost: Double,
             aliasBoost: Double,
             familyNameBoost: Double): Query = {
    val hn = parse(name)
    val exact = fullNameField.filter(name, boost = Some(fullNameBoost))
    val primary = hn.primary.flatMap { pn =>
      val primaryFilter = primaryNamesField.filter(pn, boost = Some(primaryNameBoost))
      primaryFilter :: Aliases(pn, excludeOriginal = true).toList.map { a =>
        primaryNamesField.filter(a, boost = Some(aliasBoost))
      }
    }
    val family = hn.family.toList.map(familyNameField.filter(_, boost = Some(familyNameBoost)))

    or(exact :: primary ::: family: _*)
  }

  def parseMulti(fullName: String): List[HumanName] = fullName match {
    case MultiNameRegex(primary1, _, primary2, extras) => List(
      parse(s"$primary1 $extras"),
      parse(s"$primary2 $extras")
    )
    case _ => List(parse(fullName))
  }

  def parse(fullName: String): HumanName = {
    val parts: List[NamePart] = parseWords(fullName).flatMap { block =>
      if (block == ",") {
        List(NamePart(NamePartType.Comma, ","))
      } else {
        var word = block
        val pre = if (block.startsWith(",")) {
          word = word.substring(1)
          Some(NamePart(NamePartType.Comma, ","))
        } else {
          None
        }
        val post = if (block.endsWith(",")) {
          if (block != ",") {
            word = word.substring(0, word.length - 1)
          }
          Some(NamePart(NamePartType.Comma, ","))
        } else {
          None
        }
        val lc = word.toLowerCase.replaceAllLiterally(".", "")

        val part = word match {
          case _ if salutations.contains(lc) => NamePart(NamePartType.Salutation, word)
          case _ if prefixes.contains(lc) => NamePart(NamePartType.Prefix, word)
          case _ if suffixes.contains(lc) => NamePart(NamePartType.Suffix, word)
          case _ if postnominals.contains(lc) => NamePart(NamePartType.Postnominal, word)
          case NicknameRegex(nick) => NamePart(NamePartType.Nickname, nick)
          case _ => NamePart(NamePartType.Primary, word)
        }

        List(pre, Some(part), post).flatten
      }
    }
    val name = parts.reverse.find(_.is(NamePartType.Primary)) match {
      case Some(lastName) => parts.map { part =>
        if (part eq lastName) {
          part.copy(`type` = NamePartType.Family)
        } else {
          part
        }
      }
      case None => parts
    }
    val hn = HumanName(name)
    if (hn.parts.size >= 3 && hn.parts(1).`type` == NamePartType.Comma) {   // Last Name, First Name correction
      val familyName = hn.parts.head.value
      HumanName(hn.parts.drop(2).flatMap {
        case p if p.`type` == NamePartType.Family => List(p.copy(`type` = NamePartType.Primary), NamePart(NamePartType.Family, familyName))
        case p => List(p)
      })
    } else {
      hn
    }
  }

  private val groupCharacters = Set('(', '[', '{', '"')
  private val groupEndings = Map('(' -> ')', '[' -> ']', '{' -> '}', '"' -> '"')
  private def parseWords(s: String): List[String] = {
    val b = new StringBuilder
    var open = List.empty[Char]
    val words = ListBuffer.empty[String]
    s.toCharArray.foreach {
      case ' ' if open.isEmpty => {
        if (b.nonEmpty) {
          words += b.toString()
          b.clear()
        }
      }
      case ',' if open.isEmpty => {
        if (b.nonEmpty) {
          words += b.toString()
          b.clear()
        }
        words += ","
      }
      case c if open.nonEmpty && groupEndings(open.head) == c => {
        b.append(c)
        open = open.tail
      }
      case c if groupCharacters.contains(c) => {
        b.append(c)
        open = c :: open
      }
      case c => b.append(c)
    }
    if (b.nonEmpty) {
      words += b.toString()
    }
    words.toList
  }
}