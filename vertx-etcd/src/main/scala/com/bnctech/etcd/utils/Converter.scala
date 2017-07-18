package com.bnctech.etcd.utils

import java.util.{Collections, Optional}

import com.bnctech.etcd.protocol.NodeListElement

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Created by fjim on 06/03/2017.
  */
private[etcd] object Converter {

  def convertStringToObject(str: String): AnyRef =
    Try(str.toLong)
      .getOrElse(
        Try(str.toDouble)
          .getOrElse(
            Try(str.toBoolean)
              .getOrElse(str)
          )
      )
      .asInstanceOf[AnyRef]

  def convertListElement(nodeListElement: NodeListElement): Unit = {
    @tailrec
    def recursive(list: Iterable[NodeListElement]): Unit = list match {
      case Nil =>
      case head :: tail =>
        head.setValue(
          Optional.of(
            convertStringToObject(Option(head.getValue).getOrElse("").asInstanceOf[String])
          ))
        recursive(tail ++ (Option(head.getNodes) getOrElse Collections.emptyList()).asScala)
    }

    recursive(List(nodeListElement))
  }

}
