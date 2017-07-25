package com.bnctech.etcd.exceptions

import com.bnctech.etcd.protocol.EtcdError

/**
  * Etcd error exception
  */
class EtcdErrorException(val etcdError: EtcdError) extends RuntimeException(s"cause : ${etcdError.getCause}, ${etcdError.getMessage}")
