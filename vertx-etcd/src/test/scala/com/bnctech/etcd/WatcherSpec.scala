package com.bnctech.etcd

import java.util

import com.bnctech.etcd.exceptions.EtcdErrorException
import com.bnctech.etcd.protocol._
import io.vertx.core.buffer.Buffer
import io.vertx.core.http._
import io.vertx.core.json.{DecodeException, Json}
import io.vertx.core.{AsyncResult, Handler, Vertx}
import org.mockito.Matchers.any
import org.mockito.Mockito.{doAnswer, doReturn, spy, when}

import scala.concurrent.Promise

/**
  * Created by fjim on 21/02/2017.
  */
class WatcherSpec extends BaseTest {
  private var etcdClient: EtcdClient = _
  private var httpClient: HttpClient = _
  private var httpClientResponse: HttpClientResponse = _
  private var httpClientRequest: HttpClientRequest = _
  private implicit val vertx = spy(Vertx.vertx)

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
    etcdClient = new EtcdClient("127.0.0.1", 2789, vertx)
  }
  "EtcdResponse" should {
    "fail" when {
      "The json retrieved is not valid" in {
        httpClientResponseBodyHandlerError
        recoverToSucceededIf[DecodeException] {
          val promise = Promise[EtcdResponse]
          etcdClient.delete("key1", convertHandlerToFuture(promise))
          promise.future
        }
      }
      "It's not a directory" in {
        when(httpClientResponse.statusCode) thenReturn 400
        httpClientResponseBodyHandlerError
        recoverToSucceededIf[EtcdErrorException] {
          val promise = Promise[EtcdResponse]
          etcdClient.get("key1", convertHandlerToFuture(promise))
          promise.future
        }
      }
    }
  }
  "EtcdListResponse" should {
    "fail" when {
      "The json retrieved is not valid" in {
        httpClientResponseBodyHandlerError
        recoverToSucceededIf[DecodeException] {
          val promise = Promise[EtcdListResponse]
          etcdClient.createDir("key1", convertHandlerToFuture(promise))
          promise.future
        }
      }
      "It's not a directory" in {
        httpClientResponseBodyHandlerError
        when(httpClientResponse.statusCode) thenReturn 400
        recoverToSucceededIf[EtcdErrorException] {
          val promise = Promise[EtcdListResponse]
          etcdClient.createDir("key1", convertHandlerToFuture(promise))
          promise.future
        }
      }
    }
  }
  "get" should {
    "success" in {
      val promise = Promise[EtcdResponse]
      httpClientResponseBodyHandlerKeySuccess
      etcdClient.get("key1", convertHandlerToFuture(promise))
      promise.future map {
        result => result.getNode.getValue shouldBe 12.3
      }
    }
    "fail" when {
      "The status code is different with 200" in {
        httpClientResponseBodyHandlerError
        when(httpClientResponse.statusCode) thenReturn 501
        recoverToSucceededIf[EtcdErrorException] {
          val promise = Promise[EtcdResponse]
          etcdClient.get("key1", convertHandlerToFuture(promise))
          promise.future
        }
      }
    }
  }
  "set" should {
    "success" when {
      "ttl is set" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        etcdClient.set("key1", 12.3, 1, convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getValue shouldBe 12.3
        }
      }
      "ttl is not set" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        etcdClient.set("key1", 12.3, convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getValue shouldBe 12.3
        }
      }
    }
  }
  "delete" should {
    "success" in {
      httpClientResponseBodyHandlerKeySuccess
      val promise = Promise[EtcdResponse]
      etcdClient.delete("key1", convertHandlerToFuture(promise))
      promise.future map {
        result => result.getNode.getKey shouldBe "key1"
      }
    }
    "fail" when {
      "The key does not exist" in {
        when(httpClientResponse.statusCode) thenReturn 404
        httpClientResponseBodyHandlerError
        recoverToSucceededIf[EtcdErrorException] {
          val promise = Promise[EtcdResponse]
          etcdClient.delete("key1", convertHandlerToFuture(promise))
          promise.future
        }
      }
    }
  }
  "listDir" should {
    "success" when {
      "recursive is set" in {
        httpClientResponseBodyHandlerDirSuccess
        val promise = Promise[EtcdListResponse]
        etcdClient.listDir("key1", true, convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getValue shouldBe "value"
        }
      }
      "recursive is not set" in {
        httpClientResponseBodyHandlerDirSuccess
        val promise = Promise[EtcdListResponse]
        etcdClient.listDir("key1", convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getValue shouldBe "value"
        }
      }
    }
  }
  "createDir" should {
    "success" in {
      httpClientResponseBodyHandlerDirSuccess
      val promise = Promise[EtcdListResponse]
      etcdClient.createDir("key1", convertHandlerToFuture(promise))
      promise.future map {
        result => result.getNode.getDir shouldBe true
      }
    }
  }
  "deleteDir" should {
    "success" when {
      "recursive is set" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        etcdClient.deleteDir("key1", true, convertHandlerToFuture[EtcdResponse](promise))
        promise.future map {
          result => result.getNode.getKey shouldBe "key1"
        }
      }
      "recursive is not set" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        etcdClient.deleteDir("key1", convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getKey shouldBe "key1"
        }
      }
    }
  }
  "watch" should {
    "success" when {
      "give all parameters" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        val watcher = etcdClient.watch("key1", 0, false)
        watcher.start(convertHandlerToFuture(promise))
        promise.future map {
          result =>
            watcher.stop()
            result.getNode should not be null
        }
      }
      "give the key and the waitIndex" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        val watcher = etcdClient.watch("key1", 0)
        watcher.start(convertHandlerToFuture(promise))
        promise.future map {
          result =>
            watcher.stop()
            result.getNode should not be null
        }
      }
      "give the key and the recursive parameter" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        val watcher = etcdClient.watch("key1", false)
        watcher.start(convertHandlerToFuture(promise))
        promise.future map {
          result =>
            watcher.stop()
            result.getNode should not be null
        }
      }
      "just give the key" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        val watcher = etcdClient.watch("key1")
        watcher.start(convertHandlerToFuture(promise))
        promise.future map {
          result =>
            watcher.stop()
            result.getNode should not be null
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

  private def httpClientResponseBodyHandlerDirSuccess = {

    val etcdListResponse = new EtcdListResponse
    val nodeListElement = new NodeListElement
    val subNodeListElement = new NodeListElement

    nodeListElement.setKey("key1")
    nodeListElement.setDir(true)
    nodeListElement.setValue("value")
    nodeListElement.setCreatedIndex(31)
    nodeListElement.setModifiedIndex(30)

    subNodeListElement.setKey("subkey1")
    subNodeListElement.setDir(false)
    subNodeListElement.setValue("subvalue1")
    subNodeListElement.setCreatedIndex(23)
    subNodeListElement.setModifiedIndex(23)

    nodeListElement.setNodes(util.Arrays.asList(subNodeListElement))

    etcdListResponse.setAction("get")
    etcdListResponse.setNode(nodeListElement)
    httpClientResponseBodyHandler(etcdListResponse)
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
    }) when httpClientResponse bodyHandler any(classOf[Handler[Buffer]])
  }
}
