package com.bnctech.etcd.protocol
import java.util

/**
  * TODO
  */
class NodeListElement {
  private var key:String = _
  private var dir:Boolean = false
  private var value:AnyRef = _
  private var createdIndex:Int = 0
  private var modifiedIndex:Int = 0
  private var nodes:util.List[NodeListElement] = _

  def getKey: String = key

  def setKey(key: String): Unit = this.key = key

  def getDir: Boolean = dir

  def setDir(dir: Boolean): Unit = this.dir = dir

  def getValue: AnyRef = value

  def setValue(value: AnyRef): Unit = this.value = value

  def getCreatedIndex: Integer = createdIndex

  def setCreatedIndex(createdIndex: Integer): Unit = this.createdIndex = createdIndex

  def getModifiedIndex: Integer = modifiedIndex

  def setModifiedIndex(modifiedIndex: Integer): Unit = this.modifiedIndex = modifiedIndex

  def getNodes: util.List[NodeListElement] = nodes

  def setNodes(nodes: util.List[NodeListElement]): Unit = this.nodes = nodes
}
