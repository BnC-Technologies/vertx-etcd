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
  * Static uril methods for converting stuffs
  */
private[etcd] object Converter {
  /**
    * Methods that convert the buffer to [[EtcdErrorException]]
    *
    * @param handler Handler handling the answer
    * @param buffer  Buffer containing the error
    * @tparam T Type of the [[AsyncResult]]
    */
  def handleError[T](handler: Handler[AsyncResult[T]], buffer: Buffer): Unit = {
    handler handle Future.failedFuture((Try {
      val etcdError = Json.decodeValue(buffer.toString(), classOf[EtcdError])
      new EtcdErrorException(etcdError)
    } recover { case e: Exception => e }).get)
  }

  /**
    * Convert a String to an object.
    * Try successively to convert to Long, Double, Boolean.
    * If any of those fail return the string
    *
    * @param str String to convert
    * @return The converted value
    */
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

  /**
    * Convert recursively the value from [[NodeListElement]]
    *
    * @param nodeListElement Node list to convert from
    */
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
