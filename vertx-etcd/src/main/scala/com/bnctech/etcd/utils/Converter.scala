package com.bnctech.etcd.utils

import java.util.Collections

import com.bnctech.etcd.exceptions.EtcdErrorException
import com.bnctech.etcd.protocol.{EtcdError, NodeListElement}
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.core.{AsyncResult, Future, Handler}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Created by fjim on 06/03/2017.
  */
private[etcd] object Converter {
  def handleError[T](handler: Handler[AsyncResult[T]], buffer: Buffer): Unit = {
    handler handle Future.failedFuture((Try {
      val etcdError = Json.decodeValue(buffer.toString(), classOf[EtcdError])
      new EtcdErrorException(etcdError)
    } recover { case e: Exception => e }).get)
  }

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
        Option(head.getValue) foreach {
          case value: String => head.setValue(convertStringToObject(value))
        }
        recursive(tail ++ (Option(head.getNodes) getOrElse Collections.emptyList()).asScala)
    }

    recursive(List(nodeListElement))
  }

}
