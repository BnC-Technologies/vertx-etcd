package com.bnctech.etcd

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

import com.bnctech.etcd.exceptions.EtcdErrorException
import com.bnctech.etcd.protocol.{EtcdError, EtcdResponse}
import com.bnctech.etcd.utils.Converter.convertStringToObject
import io.vertx.core.http.{HttpClient, HttpClientResponse, HttpMethod}
import io.vertx.core.json.{Json, JsonObject}
import io.vertx.core.{AsyncResult, Future, Handler, Vertx}

import scala.util.{Failure, Success, Try}

/**
  * Long polling watcher class
  *
  * @param httpClient Vertx HTTP client to use
  * @param key        ETCD key to watch
  * @param waitIndex  Optional index to start the waiting
  * @param recursive  Tell etcd to watch recursively a key
  * @param vertx      Vertx instance
  */
class Watcher(private val httpClient: HttpClient,
              private val key: String,
              private val waitIndex: Option[Long] = None,
              private val recursive: Boolean,
              private val vertx: Vertx) {
  private val eventBus = vertx.eventBus()
  private val etcdClientLongPollingEventbusAddress = s"etcdclient-polling-${UUID.randomUUID().toString}"
  private val localConsumer = eventBus.localConsumer[AnyRef](etcdClientLongPollingEventbusAddress)
  private var timer: Long = -1
  private val isRunning = new AtomicBoolean(false)

  private def watch(handler: Handler[AsyncResult[EtcdResponse]]): Unit = {
    localConsumer handler {
      message =>
        isRunning.set(false)
        message.body match {
          case json: JsonObject => handler handle Future.succeededFuture(Json.decodeValue(json.encode(), classOf[EtcdResponse]))
          case exception: Exception =>
            stop()
            handler handle Future.failedFuture(exception)
        }

    }
    timer = vertx.setPeriodic(500, _ => timerHandler())
    //ScalaFuture will create a new thread and it will not block the main thread
  }

  private def timerHandler() = {
    if (!isRunning.get()) {
      isRunning.set(true)
      implicit val list = List("wait" -> Some(true), "waitIndex" -> waitIndex, "recursive" -> Some(recursive))
      httpClient.request(HttpMethod.GET, EtcdClient.prepareUrl(key), new Handler[HttpClientResponse] {
        override def handle(httpClientResponse: HttpClientResponse): Unit = {
          httpClientResponse.bodyHandler(buffer => {
            if (httpClientResponse.statusCode() != 200) {
              eventBus.send(etcdClientLongPollingEventbusAddress, (Try {
                val etcdError = Json.decodeValue(buffer.toString(), classOf[EtcdError])
                new EtcdErrorException(etcdError)
              } recover { case e: Exception => e }).get)
            } else {
              Try {
                val response = Json.decodeValue(buffer.toJsonObject.encode, classOf[EtcdResponse])
                Option(response.getNode.getValue) foreach {
                  case value: String => response.getNode.setValue(convertStringToObject(value))
                }
                response
              } match {
                case Success(response) =>
                  eventBus.send(etcdClientLongPollingEventbusAddress, new JsonObject(Json.encode(response)))
                case Failure(e) => eventBus.send(etcdClientLongPollingEventbusAddress, e)
              }
            }
          })
        }
      }).end()
    }
  }


  /**
    * Start watching the key
    *
    * @param handler Handler use to receive all the events
    */
  def start(handler: Handler[AsyncResult[EtcdResponse]]): Unit = watch(handler)

  /**
    * Stop watching
    */
  def stop(): Unit = {
    isRunning.set(false)
    localConsumer.unregister()
    if (timer > -1) vertx cancelTimer timer
    httpClient.close()
  }
}
