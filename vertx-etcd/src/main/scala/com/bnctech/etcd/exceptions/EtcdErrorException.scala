package com.bnctech.etcd.exceptions

import com.bnctech.etcd.protocol.EtcdError

/**
  * Trait for etcd exceptions
  */
class EtcdErrorException(val etcdError: EtcdError) extends RuntimeException(s"cause : ${etcdError.getCause}, ${etcdError.getMessage}")

