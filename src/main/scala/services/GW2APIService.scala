package com.mcnkowski.gw2
package services

import json._
import json.JsonImplicits._

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.{KillSwitches, UniqueKillSwitch}
import akka.stream.scaladsl.{Keep, Sink, Source}
import play.api.libs.json.{JsValue, Json}

import java.net.URLEncoder

object GW2APIService {

  implicit val jsonUnmarshaller:Unmarshaller[HttpEntity,JsValue] = Unmarshaller.stringUnmarshaller.map {
    response => Json.parse(response)
  }

  val baseURL:String = """https://api.guildwars2.com/v2"""

  def getCharacters(auth_token:String)(implicit ec:ExecutionContext, system:ClassicActorSystemProvider):Future[Seq[String]] = {
    val query:String = s"access_token=$auth_token"

    GET(s"$baseURL/characters?$query")
      .flatMap(response => Unmarshal(response.entity).to[JsValue])
      .map(_.as[Seq[String]])
  }

  def getCharacterInventory(auth_token:String,name:String)(implicit ec:ExecutionContext, system:ClassicActorSystemProvider):Future[Seq[GWItem]] = {
    val query: String = s"access_token=$auth_token"
    val encodedname = URLEncoder.encode(name, "UTF-8").replace("+", "%20")

    GET(s"$baseURL/characters/$encodedname/inventory?$query")
      .flatMap(response => Unmarshal(response.entity).to[JsValue])
      .map { json =>
        (json \\ "inventory").flatMap(_.as[Seq[Option[GWItem]]])
        .collect{case Some(item) => item}.toSeq //collect to filter out nulls
      }
  }

  def getBank(auth_token:String)(implicit ec:ExecutionContext, system:ClassicActorSystemProvider):Future[Seq[GWItem]] = {
    val query: String = s"access_token=$auth_token"

    GET(s"$baseURL/account/bank?$query")
      .flatMap(response => Unmarshal(response.entity).to[JsValue])
      .map (
        _.as[Seq[Option[GWItem]]]
        .collect{case Some(item) => item}
      )
  }

  def getMaterialStorage(auth_token:String)(implicit ec:ExecutionContext, system:ClassicActorSystemProvider):Future[Seq[GWItem]] = {
    val query: String = s"access_token=$auth_token"

    GET(s"$baseURL/account/materials?$query")
      .flatMap(response => Unmarshal(response.entity).to[JsValue])
      .map ( _.as[Seq[GWItem]].filter(_.amount != 0) )
  }

  def getDetailedItem(item:GWItem)(implicit ec:ExecutionContext, system:ClassicActorSystemProvider):Future[GWDetailedItem] = {
    GET(s"$baseURL/items/${item.ID}")
      .flatMap(response => Unmarshal(response.entity).to[JsValue])
      .map(details => GWDetailedItem(item.ID,item.amount,details.as[GWItemDetails]))
  }

/* OLD
  def getDetailedInventory(items:collection.immutable.Iterable[GWItem], parallelism:Int=4)
                          (implicit ec:ExecutionContext, system:ClassicActorSystemProvider):Future[Seq[GWDetailedItem]] = {

     Source(items)
       .mapAsyncUnordered(parallelism)(getDetailedItem)
       .runWith(Sink.seq)
  }*/

  def getDetailedInventory(auth_token:String, name:String, parallelism:Int=4)
                          (implicit ec:ExecutionContext, system:ClassicActorSystemProvider):(UniqueKillSwitch,Future[Seq[GWDetailedItem]]) = {

    detailed(getCharacterInventory(auth_token,name),parallelism)
  }

  def getDetailedMaterialStorage(auth_token:String,parallelism:Int=4)
                                (implicit ec:ExecutionContext, system:ClassicActorSystemProvider):(UniqueKillSwitch,Future[Seq[GWDetailedItem]]) = {

    detailed(getMaterialStorage(auth_token),parallelism)
  }

  def getDetailedBank(auth_token:String,parallelism:Int=4)
                     (implicit ec:ExecutionContext, system:ClassicActorSystemProvider):(UniqueKillSwitch,Future[Seq[GWDetailedItem]]) = {

    detailed(getBank(auth_token),parallelism)
  }

  private def detailed(future:Future[Seq[GWItem]],parallelism:Int)
                      (implicit ec:ExecutionContext, system:ClassicActorSystemProvider):(UniqueKillSwitch,Future[Seq[GWDetailedItem]]) = {

    Source.futureSource(future.map(Source(_)))
      .viaMat(KillSwitches.single)(Keep.right)
      .mapAsyncUnordered(parallelism)(getDetailedItem)
      .toMat(Sink.seq)(Keep.both)
      .run()
  }

}
