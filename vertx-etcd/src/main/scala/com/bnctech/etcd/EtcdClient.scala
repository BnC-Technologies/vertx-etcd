package com.bnctech.etcd

import com.bnctech.etcd.protocol.{EtcdListResponse, EtcdResponse}
import com.bnctech.etcd.utils.Converter._
import io.vertx.core.http.{HttpClient, HttpClientOptions, HttpClientResponse, HttpMethod}
import io.vertx.core.json.Json
import io.vertx.core.{AsyncResult, Future, Handler, Vertx}

import scala.util.{Failure, Success, Try}

/**
  * Etcd client
  */
object EtcdClient {
  private val SLASH = "/"
  private val BASE_URL: String = "/v2/keys"
  private val HEADER = "application/x-www-form-urlencoded"

  def prepareUrl[A](key: String = "")(implicit list: List[(String, Option[A])] = Nil): String = {
    val appUrl = list filter {
      _._2.isDefined
    } map { case (param, value) => s"$param=${value.get}" } match {
      case Nil => ""
      case parametersList => "?" concat {
        parametersList mkString "&"
      }
    }
    s"$BASE_URL$SLASH$key$appUrl"
  }

}

/**
  * Etcd client
  *
  * @param host  Etcd hostname
  * @param port  Etcd port
  * @param vertx Vertx instance
  */
class EtcdClient(host: String, port: Int, vertx: Vertx) {

  import EtcdClient._

  private val options: HttpClientOptions = new HttpClientOptions()
    .setDefaultHost(host)
    .setDefaultPort(port)
  private val httpClient: HttpClient = vertx.createHttpClient(options)

  /**
    * Get the value of a key
    *
    * @param key     Key to retrieve
    * @param handler Callback handling the response
    */
  def get(key: String, handler: Handler[AsyncResult[EtcdResponse]]): Unit = {
    httpClient.request(HttpMethod.GET, prepareUrl(key), executeKey(handler)).end()
  }

  /**
    * Set a value for a key
    * If the key does not exist then it will be created. Otherwise the value will be updated
    *
    * @param key     Key which will have the value set
    * @param value   Value to set
    * @param ttl     Optional time to live for the key
    * @param handler Callback handling the response
    */
  def set(key: String, value: Any, ttl: Int, handler: Handler[AsyncResult[EtcdResponse]]): Unit =
    set(key, value, Option(ttl), handler)

  /**
    * Set a value for a key
    * If the key does not exist then it will be created. Otherwise the value will be updated
    *
    * @param key     Key which will have the value set
    * @param value   Value to set
    * @param handler Callback handling the response
    */
  def set(key: String, value: Any, handler: Handler[AsyncResult[EtcdResponse]]): Unit =
    set(key, value, None, handler)

  /**
    * Set a value for a key
    * If the key does not exist then it will be created. Otherwise the value will be updated
    *
    * @param key     Key which will have the value set
    * @param value   Value to set
    * @param ttl     Optional time to live for the key
    * @param handler Callback handling the response
    */
  private def set(key: String, value: Any, ttl: Option[Int], handler: Handler[AsyncResult[EtcdResponse]]): Unit =
    httpClient.request(HttpMethod.PUT, prepareUrl(key), executeKey(handler))
      .putHeader("Content-Type", HEADER)
      .end(s"value=$value" concat (ttl map { ttl => s"&ttl=$ttl" } getOrElse ""))

  /**
    * Delete a key
    *
    * @param key     Key to delete
    * @param handler Callback handling the response
    */
  def delete(key: String, handler: Handler[AsyncResult[EtcdResponse]]): Unit =
    httpClient.request(HttpMethod.DELETE, prepareUrl(key), executeKey(handler)).end()

  /**
    * Watch every change on a key or a directory
    *
    * @param key       Key or directory to watch
    * @param waitIndex Start the watch from this index
    * @param recursive Recursively watch a directory
    * @return [[Watcher]] object for this key
    */
  def watch(key: String, waitIndex: Long, recursive: Boolean): Watcher = watch(key, Some(waitIndex), recursive)

  /**
    * Watch every change on a key or a directory without recursive watch
    *
    * @param key       Key or directory to watch
    * @param waitIndex Start the watch from this index
    * @return [[Watcher]] object for this key
    */
  def watch(key: String, waitIndex: Long): Watcher = watch(key, waitIndex, false)

  /**
    * Watch every change on a key or a directory
    *
    * @param key       Key or directory to watch
    * @param recursive Recursively watch a directory
    * @return [[Watcher]] object for this key
    */
  def watch(key: String, recursive: Boolean): Watcher = watch(key, None, recursive)

  /**
    * Watch every change on a key or a directory without recursive watch
    *
    * @param key Key or directory to watch
    * @return [[Watcher]] object for this key
    */
  def watch(key: String): Watcher = watch(key, false)

  /**
    * Watch every change on a key or a directory
    *
    * @param key       Key or directory to watch
    * @param waitIndex Start the watch from this index
    * @param recursive Recursively watch a directory
    * @return [[Watcher]] object for this key
    */
  private def watch(key: String, waitIndex: Option[Long] = None, recursive: Boolean): Watcher =
    new Watcher(httpClient, key, waitIndex, recursive, vertx)

  /**
    * Create a directory
    *
    * @param dir     Directory to create
    * @param handler Callback handling the response
    */
  def createDir(dir: String, handler: Handler[AsyncResult[EtcdListResponse]]): Unit =
    httpClient.request(HttpMethod.PUT, prepareUrl(dir), executeList(handler))
      .putHeader("Content-Type", HEADER)
      .end("dir=true")

  /**
    * List a directory
    *
    * @param dir       Directory to list
    * @param recursive List the directory recursively
    * @param handler   Callback handling the response
    */
  def listDir(dir: String, recursive: Boolean, handler: Handler[AsyncResult[EtcdListResponse]]): Unit =
    listDir(dir, Option(recursive), handler)

  /**
    * List a directory not recursively
    *
    * @param dir     Directory to list
    * @param handler Callback handling the response
    */
  def listDir(dir: String, handler: Handler[AsyncResult[EtcdListResponse]]): Unit =
    listDir(dir, None, handler)

  /**
    * List a directory
    *
    * @param dir       Directory to list
    * @param recursive List the directory recursively
    * @param handler   Callback handling the response
    */
  private def listDir(dir: String, recursive: Option[Boolean], handler: Handler[AsyncResult[EtcdListResponse]]): Unit = {
    implicit val list = List(("recursive", recursive))
    httpClient.request(HttpMethod.GET, prepareUrl(dir), executeList(handler)).end()
  }

  /**
    * Delete a directory
    *
    * @param dir       Directory to delete
    * @param recursive Delete the directory recursively
    * @param handler   Callback handling the response
    */
  def deleteDir(dir: String, recursive: Boolean, handler: Handler[AsyncResult[EtcdResponse]]): Unit =
    deleteDir(dir, Option(recursive), handler)

  /**
    * Delete a directory not recursively
    *
    * @param dir     Directory to delete
    * @param handler Callback handling the response
    */
  def deleteDir(dir: String, handler: Handler[AsyncResult[EtcdResponse]]): Unit =
    deleteDir(dir, None, handler)

  /**
    * Delete a directory
    *
    * @param dir       Directory to delete
    * @param recursive Delete the directory recursively
    * @param handler   Callback handling the response
    */
  private def deleteDir(dir: String, recursive: Option[Boolean], handler: Handler[AsyncResult[EtcdResponse]]): Unit = {
    implicit val list = List(("recursive", recursive))
    httpClient.request(HttpMethod.DELETE, prepareUrl(dir), executeKey(handler)).end()
  }

  private def executeKey(handler: Handler[AsyncResult[EtcdResponse]]): Handler[HttpClientResponse] = {
    (response: HttpClientResponse) => {
      response.bodyHandler(buffer =>
        response.statusCode() match {
          case 200 | 201 =>
            Try {
              Json.decodeValue(buffer.toString(), classOf[EtcdResponse])
            } match {
              case Success(etcdResponse) =>
                Option(etcdResponse.getNode.getValue) foreach {
                  case value: String => etcdResponse.getNode.setValue(convertStringToObject(value))
                }
                handler handle Future.succeededFuture(etcdResponse)
              case Failure(e) => handler handle Future.failedFuture(e)
            }
          case _ =>
            handleError(handler, buffer)
        })
    }
  }

  private def executeList(handler: Handler[AsyncResult[EtcdListResponse]]): Handler[HttpClientResponse] = {
    (response: HttpClientResponse) => {
      response.bodyHandler(buffer =>
        response.statusCode() match {
          case 200 | 201 =>
            Try {
              Json.decodeValue(buffer.toString(), classOf[EtcdListResponse])
            } match {
              case Success(etcdListResponse) =>
                convertListElement(etcdListResponse.getNode)
                handler handle Future.succeededFuture[EtcdListResponse](etcdListResponse)
              case Failure(e) => handler handle Future.failedFuture(e)
            }
          case _ =>
            handleError(handler, buffer)
        })
    }
  }
}
