package com.bnctech.etcd.exceptions

/**
  * Created by fjim on 15/03/2017.
  */
case class TestFailedException() extends RuntimeException{
  override def toString: String = "Test failed exception"
}
