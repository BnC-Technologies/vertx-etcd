package com.bnctech.etcd.protocol
/**
  * TODO
  */
class EtcdListResponse {
  private var action:String = _
  private var node:NodeListElement= _

  def getAction: String = action

  def setAction(String: String): Unit = this.action = action

  def getNode: NodeListElement = node

  def setNode(node: NodeListElement): Unit = this.node = node
}
