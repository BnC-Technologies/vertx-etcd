package com.bnctech.etcd.protocol
/**
  * TODO
  */
class EtcdError {
  private var errorCode:Int = 0
  private var message:String = _
  private var cause:String = _
  private var index = 0

  def getErrorCode: Int = errorCode

  def setErrorCode(errorCode: Int): Unit = this.errorCode = errorCode

  def getMessage: String = message

  def setMessage(message: String): Unit = this.message = message

  def getIndex: Int = index

  def setIndex(index: Int): Unit = this.index = index

  def getCause: String = cause

  def setCause(cause: String): Unit = this.cause = cause

}
