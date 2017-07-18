package com.bnctech.etcd.exceptions

import com.bnctech.etcd.protocol.EtcdError

/**
  * Trait for etcd exceptions
  */
trait EtcdException extends RuntimeException

class EtcdErrorException(val etcdError: EtcdError) extends EtcdException(s"cause : ${etcdError.getCause}, ${etcdError.getMessage}")

