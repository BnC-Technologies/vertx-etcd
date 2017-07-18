package com.bnctech.scala.etcd


import com.bnctech.etcd.protocol.{EtcdListResponse, EtcdResponse}
import com.bnctech.etcd.{Watcher, EtcdClient => JEtcdClient}
import io.vertx.core.{AsyncResult, Handler, Vertx => JVertx}
import io.vertx.scala.core.Vertx

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * Etcd client
  *
  * @param host  Etcd hostname
  * @param port  Etcd port
  * @param vertx Vertx instance
  * @param ctx   [[ExecutionContext]] for the futures
  */
class EtcdClient(host: String, port: Int, vertx: Vertx)(implicit ctx: ExecutionContext) {
  private val jVertx = vertx.asJava.asInstanceOf[JVertx]
  private val client = new JEtcdClient(host, port, jVertx)

  /**
    * Resolve an async result to a promise
    *
    * @param promise [[Promise]] contenaing the result
    * @param handler [[AsyncResult]] containing the current result
    * @tparam T Type of the result
    * @return Handler passed to a function
    */
  private def completeHandlerInPromise[T](promise: Promise[T])(handler: AsyncResult[T]) =
    if (handler.failed) promise failure handler.cause else promise success handler.result


  /**
    * Get the value of a key
    *
    * @param key     Key to retrieve
    * @param handler Callback handling the response
    */
  def get(key: String, handler: Handler[AsyncResult[EtcdResponse]]): Unit = client.get(key, handler)

  /**
    * Get the value of a key and return it to a future
    *
    * @param key Key to retrieve
    * @return Future of the result
    */
  def getFuture(key: String): Future[EtcdResponse] = {
    val promise = Promise[EtcdResponse]
    get(key, completeHandlerInPromise(promise))
    promise.future
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
  def set(key: String, value: Any, ttl: Option[Int] = None, handler: Handler[AsyncResult[EtcdResponse]]): Unit =
    if (ttl.isEmpty) client.set(key, value, handler) else client.set(key, value, ttl.get, handler)

  /**
    * Set a value for a key and return the result in a future
    * If the key does not exist then it will be created. Otherwise the value will be updated
    *
    * @param key   Key which will have the value set
    * @param value Value to set
    * @param ttl   Optional time to live for the key
    * @return Future containing the result
    */
  def setFuture(key: String, value: Any, ttl: Option[Int] = None): Future[EtcdResponse] = {
    val promise = Promise[EtcdResponse]
    set(key, value, ttl, completeHandlerInPromise(promise))
    promise.future
  }

  /**
    * Delete a key
    *
    * @param key     Key to delete
    * @param handler Callback handling the response
    */
  def delete(key: String, handler: Handler[AsyncResult[EtcdResponse]]): Unit =
    client.delete(key, handler)

  /**
    * Delete a key and return a future of the response
    *
    * @param key Key to delete
    * @return Future containing the response
    */
  def deleteFuture(key: String): Future[EtcdResponse] = {
    val promise = Promise[EtcdResponse]
    delete(key, completeHandlerInPromise(promise))
    promise.future
  }

  /**
    * Watch every change on a key or a directory
    *
    * @param key       Key or directory to watch
    * @param waitIndex Start the watch from this index
    * @param recursive Recursively watch a directory
    * @return [[Watcher]] object for this key
    */
  def watch(key: String, waitIndex: Option[Long] = None, recursive: Boolean = false): Watcher =
    if (waitIndex.isEmpty) client.watch(key, recursive) else client.watch(key, waitIndex.get, recursive)

  /**
    * Create a directory
    *
    * @param dir     Directory to create
    * @param handler Callback handling the response
    */
  def createDir(dir: String, handler: Handler[AsyncResult[EtcdListResponse]]): Unit =
    client.createDir(dir, handler)

  /**
    * Create a directory
    *
    * @param dir Directory to create
    * @return Future containing the response
    */
  def createDirFuture(dir: String): Future[EtcdListResponse] = {
    val promise = Promise[EtcdListResponse]
    createDir(dir, completeHandlerInPromise(promise))
    promise.future
  }

  /**
    * List a directory
    *
    * @param dir       Directory to list
    * @param recursive List the directory recursively
    * @param handler   Callback handling the response
    */
  def listDir(dir: String, recursive: Option[Boolean] = None, handler: Handler[AsyncResult[EtcdListResponse]]): Unit =
    if (recursive.isEmpty) client.listDir(dir, handler) else client.listDir(dir, recursive.get, handler)

  /**
    * List a directory
    *
    * @param dir       Directory to list
    * @param recursive List the directory recursively
    * @return Future of the response
    */
  def listDirFuture(dir: String, recursive: Option[Boolean] = None): Future[EtcdListResponse] = {
    val promise = Promise[EtcdListResponse]
    listDir(dir, recursive, completeHandlerInPromise(promise))
    promise.future
  }

  /**
    * Delete a directory
    *
    * @param dir       Directory to delete
    * @param recursive Delete the directory recursively
    * @param handler   Callback handling the response
    */
  def deleteDir(dir: String, recursive: Option[Boolean] = None, handler: Handler[AsyncResult[EtcdResponse]]): Unit =
    if (recursive.isEmpty) client.deleteDir(dir, handler) else client.deleteDir(dir, recursive.get, handler)

  /**
    * Delete a directory
    *
    * @param dir       Directory to delete
    * @param recursive Delete the directory recursively
    * @return Future of the response
    */
  def deleteDirFuture(dir: String, recursive: Option[Boolean] = None): Future[EtcdResponse] = {
    val promise = Promise[EtcdResponse]
    deleteDir(dir, recursive, completeHandlerInPromise(promise))
    promise.future
  }

}
