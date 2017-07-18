package com.bnctech.etcd.protocol

/**
  * TODO
  */
class EtcdResponse {
  private var action: String = _
  private var node: NodeResponse = _
  private var prevNode: NodeResponse = _

  def getAction: String = action

  def setAction(action: String): Unit = this.action = action

  def getNode: NodeResponse = node

  def setNode(node: NodeResponse): Unit = this.node = node

  def getPrevNode: NodeResponse = prevNode

  def setPrevNode(prevNode: NodeResponse): Unit = this.prevNode = prevNode
}
