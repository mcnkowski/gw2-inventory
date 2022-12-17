package com.mcnkowski.gw2

package object json {

  case class GWItem(ID:Int, amount:Int)
  case class GWItemDetails(name: String, itemtype:String, icon:scalafx.scene.image.Image)
  case class GWDetailedItem(ID:Int, amount:Int, details:GWItemDetails)
}
