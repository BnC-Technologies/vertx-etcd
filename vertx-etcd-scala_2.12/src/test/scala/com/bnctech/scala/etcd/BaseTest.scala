package com.bnctech.scala.etcd

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncWordSpec, BeforeAndAfter, Matchers, WordSpec}

/**
  * Base test
  */
trait BaseTest extends AsyncWordSpec with BeforeAndAfter with Matchers with MockitoSugar
