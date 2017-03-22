package com.av.websocket

import spray.json._

/**
  * We could write many case classes or simply use a map for janus messages
  *
  */

object JanusProtocol extends DefaultJsonProtocol {
  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case n: Int => JsNumber(n)
      case s: String => JsString(s)
      case x: Seq[_] => seqFormat[Any].write(x)
      case m: Map[String, _] => mapFormat[String, Any].write(m)
      case b: Boolean if b => JsTrue
      case b: Boolean if !b => JsFalse
      case x => serializationError("Do not understand object of type " + x.getClass.getName)
    }
    def read(value: JsValue) = value match {
      case JsNumber(n) => n.intValue()
      case JsString(s) => s
      case a: JsArray => listFormat[Any].read(value)
      case o: JsObject => mapFormat[String, Any].read(value)
      case JsTrue => true
      case JsFalse => false
      case x => deserializationError("Do not understand how to deserialize " + x)
    }
  }
}