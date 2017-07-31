package com.bnctech.etcd

import com.bnctech.etcd.protocol._
import io.vertx.core.buffer.Buffer
import io.vertx.core.http._
import io.vertx.core.json.Json
import io.vertx.core.{AsyncResult, Handler, Vertx}
import org.mockito.Matchers.any
import org.mockito.Mockito.{doAnswer, doReturn, spy, when}

import scala.concurrent.Promise

/**
  * Watcher class tests
  */
class WatcherSpec extends BaseTest {
  private val keyEtcd = "test"
  private var httpClient: HttpClient = _
  private var httpClientResponse: HttpClientResponse = _
  private var httpClientRequest: HttpClientRequest = _
  private val vertx = spy(Vertx.vertx)

  private def convertHandlerToFuture[T](promise: Promise[T])(result: AsyncResult[T]): Unit = {
    if (result.failed)
      promise failure result.cause
    else promise success result.result
  }

  before {
    httpClient = mock[HttpClient]
    httpClientResponse = mock[HttpClientResponse]
    httpClientRequest = mock[HttpClientRequest]
    when(httpClientResponse.statusCode) thenReturn 200
    doAnswer(invocationOnMock => {
      val handler = invocationOnMock.getArguments()(2).asInstanceOf[Handler[HttpClientResponse]]
      handler.handle(httpClientResponse)
      httpClientRequest
    }) when httpClient request(any[HttpMethod], any[String], any(classOf[Handler[HttpClientResponse]]))
    doReturn(httpClientRequest) when httpClientRequest putHeader("Content-Type", "application/x-www-form-urlencoded")
    doReturn(httpClient) when vertx createHttpClient any[HttpClientOptions]
  }
  "Watcher" should {
    "stop" when {
      "command after stop" in {
        val watcher = new Watcher(httpClient, keyEtcd, None, false, vertx)
        val promise = Promise[EtcdResponse]
        watcher.start(convertHandlerToFuture(promise))
        Thread.sleep(500)
        watcher.stop()
        httpClientResponseBodyHandlerKeySuccess
        promise.isCompleted shouldBe false
      }
    }
    "fail" when {
      "The json is not valid" in {
        val watcher = new Watcher(httpClient, keyEtcd, None, false, vertx)
        httpClientResponseBodyHandlerWithInvalidJsonError
        recoverToSucceededIf[Exception] {
          val promise = Promise[EtcdResponse]
          watcher.start(convertHandlerToFuture(promise))
          promise.future
        }
      }
      "The response code is not 200" in {
        val watcher = new Watcher(httpClient, keyEtcd, None, false, vertx)
        when(httpClientResponse.statusCode) thenReturn 400
        httpClientResponseBodyHandlerError
        recoverToSucceededIf[Exception] {
          val promise = Promise[EtcdResponse]
          watcher.start(convertHandlerToFuture(promise))
          promise.future
        }
      }
    }
  }

  private def httpClientResponseBodyHandlerKeySuccess = {
    val etcdResponse = new EtcdResponse()
    etcdResponse.setAction("delete")
    val nodeResponse = new NodeResponse
    nodeResponse.setKey("key1")
    nodeResponse.setValue("12.3")
    nodeResponse.setCreatedIndex(0)
    nodeResponse.setModifiedIndex(0)
    etcdResponse.setNode(nodeResponse)
    httpClientResponseBodyHandler(etcdResponse)
  }

  private def httpClientResponseBodyHandlerError = {
    val etcdResponse = new EtcdError
    etcdResponse.setErrorCode(100)
    etcdResponse.setMessage("subNodeListElement")
    etcdResponse.setCause("/key1")
    etcdResponse.setIndex(749)
    httpClientResponseBodyHandler(etcdResponse)
  }

  private def httpClientResponseBodyHandlerWithInvalidJsonError = {

    val etcdResponse = new EtcdError
    etcdResponse.setErrorCode(100)

    doAnswer(invocation => {
      val handler = invocation.getArguments()(0).asInstanceOf[Handler[Buffer]]
      handler.handle(Buffer.buffer("""{"notEtcdResponse":true}"""))
      httpClientResponse
    }) when httpClientResponse bodyHandler any(classOf[Handler[Buffer]])
  }

  private def httpClientResponseBodyHandler(response: Any) = {
    doAnswer(invocation => {
      val handler = invocation.getArguments()(0).asInstanceOf[Handler[Buffer]]
      handler.handle(Buffer.buffer(Json.encode(response)))
      httpClientResponse
    }).when(httpClientResponse).bodyHandler(any(classOf[Handler[Buffer]]))
  }
}
