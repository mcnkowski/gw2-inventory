package com.mcnkowski.gw2
package services

import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest, HttpResponse, Uri}

import scala.concurrent.{ExecutionContext, Future}


object GET {
  def apply(URI:Uri,headers:Seq[HttpHeader] = Nil )
           (implicit ec:ExecutionContext, system:ClassicActorSystemProvider):Future[HttpResponse] = {


    Http().singleRequest(HttpRequest(HttpMethods.GET,uri = URI,headers = headers))
  }
}
