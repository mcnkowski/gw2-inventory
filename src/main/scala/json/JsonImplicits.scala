package com.mcnkowski.gw2
package json

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._


object JsonImplicits {

  implicit val itemreads: Reads[GWItem] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "count").read[Int]
    ) (GWItem.apply _)

  implicit val optionalitemreads: Reads[Option[GWItem]] = JsPath.readNullable[GWItem]

  implicit val itemdetailreads: Reads[GWItemDetails] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "type").read[String] and
      (JsPath \ "icon").readNullable[String]

  ) ((n,t,i) => GWItemDetails(n,t,new scalafx.scene.image.Image(i.getOrElse(""),20,20,true,false)))
  //TODO: icon url can't be empty; make a placeholder image in case of null
}
