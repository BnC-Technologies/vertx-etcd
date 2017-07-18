package com.bnctech.etcd.protocol

/**
  * TODO
  */
class NodeResponse {
  private var key: String = _
  private var value: AnyRef = _
  private var modifiedIndex: Int = 0
  private var createdIndex: Int = 0
  private var ttl: Int = 0
  private var expiration: String = _
  private var dir:Boolean = false

  def getKey: String = key

  def setKey(key: String): Unit = this.key = key

  def getValue: AnyRef = value

  def setValue(value: AnyRef): Unit = this.value = value

  def getModifiedIndex: Int = modifiedIndex

  def setModifiedIndex(modifiedIndex: Int): Unit = this.modifiedIndex = modifiedIndex

  def getCreatedIndex: Int = createdIndex

  def setCreatedIndex(createdIndex: Int): Unit = this.createdIndex = createdIndex

  def getTtl: Int = ttl

  def setTtl(ttl: Int): Unit = this.ttl = ttl

  def getExpiration: String = expiration

  def setExpiration(expiration: String): Unit = this.expiration = expiration

  def getDir: Boolean = dir

  def setDir(dir: Boolean): Unit = this.dir = dir
}
