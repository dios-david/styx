/**
 * Copyright (C) 2013-2017 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.styx.proxy

import com.hotels.styx.api.HttpRequest
import com.hotels.styx.support.backends.FakeHttpServer
import com.hotels.styx.support.configuration.{HttpBackend, Origins}
import com.hotels.styx.{DefaultStyxConfiguration, StyxClientSupplier, StyxProxySpec}
import io.netty.handler.codec.http.HttpResponseStatus.{METHOD_NOT_ALLOWED, OK}
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec}

import scala.concurrent.duration._

class OriginsReloadSpec extends FunSpec
  with StyxProxySpec
  with DefaultStyxConfiguration
  with StyxClientSupplier
  with Eventually {

  val backend1 = FakeHttpServer.HttpStartupConfig(appId = "appOne", originId = "appOne-01").start()
  val backend2 = FakeHttpServer.HttpStartupConfig(appId = "appOne", originId = "appOne-02").start()
  val backend3 = FakeHttpServer.HttpStartupConfig(appId = "appOne", originId = "appOne-03").start()

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    styxServer.setBackends(
      "/foobar" -> HttpBackend("appOne", Origins(backend1, backend2))
    )
  }

  override protected def afterAll(): Unit = {
    backend1.stop()
    backend2.stop()
    backend3.stop()
    super.afterAll()
  }

  describe("Reload Origins Endpoint") {
    it("should return 200 for POST /admin/tasks/origins/reload") {
      val response = post(styxServer.adminURL("/admin/tasks/origins/reload"))

      assert(response.status() == OK)
    }

    it("should return 405 for GET /admin/tasks/origins/reload") {
      val response = get(styxServer.adminURL("/admin/tasks/origins/reload"))

      assert(response.status() == METHOD_NOT_ALLOWED)
    }

    it("should reflect origin addition in origins status page") {
      styxServer.setBackends(
        "/foobar" -> HttpBackend("appOne", Origins(backend1, backend2, backend3))
      )

      eventually(timeout(1.seconds)) {
        val response = get(styxServer.adminURL("/admin/origins/status"))

        response.body should include("localhost:" + backend1.port())
        response.body should include("localhost:" + backend2.port())
        response.body should include("localhost:" + backend3.port())
      }
    }

    it("should reflect origin removal in origins status page") {
      styxServer.setBackends(
        "/foobar" -> HttpBackend("appOne", Origins(backend1))
      )

      eventually(timeout(1.seconds)) {
        val response = get(styxServer.adminURL("/admin/origins/status"))

        response.body should include("localhost:" + backend1.port())
        response.body should not include ("localhost:" + backend2.port())
        response.body should not include ("localhost:" + backend3.port())
      }
    }
  }

  private def post(url: String, content: String = "") = {
    decodedRequest(HttpRequest.Builder.post(url).body(content).build())
  }

  private def get(url: String) = {
    decodedRequest(HttpRequest.Builder.get(url).build())
  }
}
