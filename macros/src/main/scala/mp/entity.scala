package mp

import scala.annotation.compileTimeOnly
import scala.annotation.StaticAnnotation
import scala.collection.immutable.Seq
import scala.meta._

@compileTimeOnly("@mp.entity not expanded")
class entity extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    val q"..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends $template" = defn

    val paramssFlat: Seq[Term.Param] = paramss.flatten
    val toMapContents: Seq[Term] = paramssFlat.map { param =>
      val memberName = Term.Name(param.name.value)
      q"${param.name.value} -> $memberName"
    }

    // TODO low prio: support multiple constructor params lists
    val ctorParamsFirst: Seq[Term.Param] = paramss.headOption.getOrElse(Nil)

    def ctorArgs(valuesName: Term.Name): Seq[Term] = ctorParamsFirst.map { param =>
      val nameTerm = Term.Name(param.name.value)
      val tpe: Type = param.decltpe.get.asInstanceOf[Type.Name] // TODO: don't do option.get, don't cast
      q""" $nameTerm = $valuesName(${param.name.value}).asInstanceOf[$tpe] """
    }

    val typeTermName = Term.Name(tname.value)
    val fromMapCtorValuesName: Term.Name = q"values"
    val res = q"""
      ..$mods class $tname[..$tparams](...$paramss) {
        def toMap(): Map[String, Any] = Map[String, Any](..$toMapContents)
      }

      object $typeTermName {
        def fromMap(values: Map[String, Any]): $tname = {
          ${typeTermName}(..${ctorArgs(fromMapCtorValuesName)})
        }
      }
    """

    println(res)
    res
  }

  // def keyValues(it: Int): Int = ???//paramssFlat.map { param =>
                                   //   val memberName = Term.Name(param.name.value)
                                   //   q"${param.name.value} -> $memberName"
                                   // }

  // def bla(i: Int): Term = q"1 + $i"
}
