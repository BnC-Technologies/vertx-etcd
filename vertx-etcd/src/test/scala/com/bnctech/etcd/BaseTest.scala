package com.bnctech.etcd

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncWordSpec, BeforeAndAfter, Matchers, WordSpec}

/**
  * Created by fjim on 01/03/2017.
  */
trait BaseTest extends AsyncWordSpec with BeforeAndAfter with Matchers with MockitoSugar
