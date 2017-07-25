package com.bnctech.scala.etcd

import java.util

import com.bnctech.etcd.exceptions.EtcdErrorException
import com.bnctech.etcd.protocol._
import io.vertx.core.buffer.Buffer
import io.vertx.core.http._
import io.vertx.core.json.Json
import io.vertx.core.{AsyncResult, Handler, Vertx => JVertx}
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.scala.core.Vertx
import org.mockito.Matchers.any
import org.mockito.Mockito.{doAnswer, doReturn, spy, when}

import scala.concurrent.Promise

/**
  * Etcd client tests
  */
class EtcdClientSpec extends BaseTest {
  private var etcdClient: EtcdClient = _
  private var httpClient: HttpClient = _
  private var httpClientResponse: HttpClientResponse = _
  private var httpClientRequest: HttpClientRequest = _
  private implicit val vertx = spy(Vertx.vertx)
  private implicit val ctx = VertxExecutionContext(vertx.getOrCreateContext())

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
    val jVertx = spy(JVertx.vertx)
    doReturn(httpClient) when jVertx createHttpClient any[HttpClientOptions]
    doReturn(jVertx).when(vertx).asJava
    etcdClient = new EtcdClient("127.0.0.1", 2789, vertx)
  }

  private val etcdKey = "key1"
  "get" should {
    "success" when {
      "use callback style" in {
        val promise = Promise[EtcdResponse]
        httpClientResponseBodyHandlerKeySuccess
        etcdClient.get(etcdKey, convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getValue shouldBe 12.3
        }
      }
      "use future" in {
        httpClientResponseBodyHandlerKeySuccess
        etcdClient.getFuture(etcdKey) map {
          result => result.getNode.getValue shouldBe 12.3
        }
      }
    }
    "fail" when {
      "The status code is different with 200" in {
        httpClientResponseBodyHandlerError
        when(httpClientResponse.statusCode) thenReturn 501
        recoverToSucceededIf[EtcdErrorException] {
          val promise = Promise[EtcdResponse]
          etcdClient.get(etcdKey, convertHandlerToFuture(promise))
          promise.future
        }
      }
      "use future" in {
        httpClientResponseBodyHandlerError
        when(httpClientResponse.statusCode) thenReturn 501
        recoverToSucceededIf[EtcdErrorException] {
          etcdClient.getFuture(etcdKey)
        }
      }
    }
  }
  "set" should {
    "success" when {
      "use future" in {
        httpClientResponseBodyHandlerKeySuccess
        etcdClient.setFuture(etcdKey, 12.3) map {
          result => result.getNode.getValue shouldBe 12.3
        }
      }
      "ttl is set" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        etcdClient.set(etcdKey, 12.3, Option(1), convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getValue shouldBe 12.3
        }
      }
      "ttl is not set" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        etcdClient.set(etcdKey, 12.3, handler = convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getValue shouldBe 12.3
        }
      }
    }
  }
  "delete" should {
    "success" when {
      "use callback style" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        etcdClient.delete(etcdKey, convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getKey shouldBe etcdKey
        }
      }
      "use future" in {
        httpClientResponseBodyHandlerKeySuccess
        etcdClient.deleteFuture(etcdKey) map {
          result => result.getNode.getKey shouldBe etcdKey
        }
      }
    }
    "fail" when {
      "The key does not exist" in {
        when(httpClientResponse.statusCode) thenReturn 404
        httpClientResponseBodyHandlerError
        recoverToSucceededIf[EtcdErrorException] {
          val promise = Promise[EtcdResponse]
          etcdClient.delete(etcdKey, convertHandlerToFuture(promise))
          promise.future
        }
      }
    }
  }
  "listDir" should {
    "success" when {
      "use future" in {
        httpClientResponseBodyHandlerDirSuccess
        etcdClient.listDirFuture(etcdKey) map {
          result => result.getNode.getValue shouldBe "value"
        }
      }
      "recursive is set" in {
        httpClientResponseBodyHandlerDirSuccess
        val promise = Promise[EtcdListResponse]
        etcdClient.listDir(etcdKey, Some(true), convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getValue shouldBe "value"
        }
      }
      "recursive is not set" in {
        httpClientResponseBodyHandlerDirSuccess
        val promise = Promise[EtcdListResponse]
        etcdClient.listDir(etcdKey, handler = convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getValue shouldBe "value"
        }
      }
    }
  }
  "createDir" should {
    "success" when {
      "use callback style" in {
        httpClientResponseBodyHandlerDirSuccess
        val promise = Promise[EtcdListResponse]
        etcdClient.createDir(etcdKey, convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getDir shouldBe true
        }
      }
      "use future" in {
        httpClientResponseBodyHandlerDirSuccess
        etcdClient.createDirFuture(etcdKey) map {
          result => result.getNode.getDir shouldBe true
        }
      }
    }
  }
  "deleteDir" should {
    "success" when {
      "use future" in {
        httpClientResponseBodyHandlerKeySuccess
        etcdClient.deleteDirFuture(etcdKey) map {
          result => result.getNode.getKey shouldBe etcdKey
        }
      }
      "recursive is set" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        etcdClient.deleteDir(etcdKey, Some(true), convertHandlerToFuture[EtcdResponse](promise))
        promise.future map {
          result => result.getNode.getKey shouldBe etcdKey
        }
      }
      "recursive is not set" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        etcdClient.deleteDir(etcdKey, handler = convertHandlerToFuture(promise))
        promise.future map {
          result => result.getNode.getKey shouldBe etcdKey
        }
      }
    }
  }
  "watch" should {
    "success" when {
      "give the key and the waitIndex" in {
        httpClientResponseBodyHandlerKeySuccess
        val promise = Promise[EtcdResponse]
        val watcher = etcdClient.watch(etcdKey, Some(0))
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
        val watcher = etcdClient.watch(etcdKey)
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
    nodeResponse.setKey(etcdKey)
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

    nodeListElement.setKey(etcdKey)
    nodeListElement.setDir(true)
    nodeListElement.setValue("value")
    nodeListElement.setCreatedIndex(31)
    nodeListElement.setModifiedIndex(30)

    subNodeListElement.setKey("sub" + etcdKey)
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
    etcdResponse.setCause("/" + etcdKey)
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
