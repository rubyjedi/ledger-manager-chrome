package co.ledger.manager.web.services

import java.util.Date

import biz.enef.angulate.Module.RichModule
import biz.enef.angulate.Service
import co.ledger.manager.web.Application
import co.ledger.manager.web.core.event.JsEventEmitter
import co.ledger.manager.web.core.utils.UrlEncoder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  *
  * ApiService
  * ledger-manager-chrome
  *
  * Created by Pierre Pollastri on 10/10/2016.
  *
  * The MIT License (MIT)
  *
  * Copyright (c) 2016 Ledger
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  *
  */
class ApiService extends Service {
  import ApiService._

  def applications: Future[js.Array[App]] = {
    if (_applications.isEmpty) {
      _applications = Some(Application.httpClient.get("/applications" + queryString).json map {
        case (json, _) =>
          _lastUpdateDate = Some(new Date())
          if (json.has("nanos")) {
            val apps = json.getJSONArray("nanos")
            JSON.parse(apps.toString).asInstanceOf[js.Array[App]]
          } else {
            js.Array()
          }
      })
      _applications.get foreach {(_) =>
        eventEmitter.emit(UpdateDoneEvent())
      }
    }
   _applications.get
  }

  def firmwares: Future[js.Array[Firmware]] = {
    if (_firmwares.isEmpty) {
      _firmwares = Some(Application.httpClient.get("/firmwares" + queryString).json map {
        case (json, _) =>
          _lastUpdateDate = Some(new Date())
          if (json.has("nanos")) {
            val firms = json.getJSONArray("nanos")
            JSON.parse(firms.toString).asInstanceOf[js.Array[Firmware]]
          } else {
            js.Array()
          }
      })
      _firmwares.get foreach {(_) =>
        eventEmitter.emit(UpdateDoneEvent())
      }
    }
    _firmwares.get
  }

  def devices: Future[js.Dictionary[Device]] = {
    Application.httpClient.get("/devices" + queryString).json map {
      case (json, _) =>
       json.asInstanceOf[js.Dictionary[Device]]
    }
  }

  private def queryString = {
    if (!js.isUndefined(js.Dynamic.global.LEDGER) && js.Dynamic.global.LEDGER.asInstanceOf[Boolean] == true)
      "?provider=ledger"
    else if (!js.isUndefined(js.Dynamic.global.CUSTOM_PROVIDER) && js.Dynamic.global.CUSTOM_PROVIDER.toString.nonEmpty)
      s"?provider=${UrlEncoder.encode(js.Dynamic.global.CUSTOM_PROVIDER.toString)}"
    else
      ""
  }

  def refresh(): Future[Unit] = {
    _applications = None
    _firmwares = None
    applications.flatMap({(_) => firmwares}).map({(_) => ()})
  }

  def lastUpdateDate = _lastUpdateDate

  val eventEmitter = new JsEventEmitter

  private var _applications: Option[Future[js.Array[App]]] = None
  private var _firmwares: Option[Future[js.Array[Firmware]]] = None
  private var _lastUpdateDate: Option[Date] = None
}

object ApiService {

  case class UpdateDoneEvent()

  @ScalaJSDefined
  trait App extends js.Object {

  }

  @ScalaJSDefined
  trait Firmware extends js.Object {

  }

  @js.native
  trait Device extends js.Object {
    val targetId: Int
  }

  def init(module: RichModule) = module.serviceOf[ApiService]("apiService")
}
