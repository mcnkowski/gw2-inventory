package com.mcnkowski.gw2

import services.GW2APIService
import json._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import akka.actor.ActorSystem
import scalafx.Includes._
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control.{Alert, Button, Label, TableColumn, TableView, TextField, TextFormatter}
import scalafx.scene.layout.{BorderPane, HBox, VBox}
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.stage.WindowEvent
import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import scalafx.event.ActionEvent
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.TextFormatter.Change
import scalafx.scene.image.{Image, ImageView}


object Main extends JFXApp3{


  implicit val ec:ExecutionContext = ExecutionContext.global
  implicit val actorsys:ActorSystem = ActorSystem.apply("ActorSystem")

  override def start():Unit = {

    val processingRequest:BooleanProperty = BooleanProperty(false) //flag used to avoid queueing up multiple requests

    val characterTable = new TableView[String](ObservableBuffer.empty[String]) {
      columns += new TableColumn[String, String] {
        text = "Characters"
        cellValueFactory = item => StringProperty(item.value)
        prefWidth = 150
      }
      prefWidth = 150
    }

    val itemTable = new TableView[GWDetailedItem](ObservableBuffer.empty[GWDetailedItem]) {
      columns ++= ObservableBuffer(
        new TableColumn[GWDetailedItem,Image] {
          text = ""
          cellValueFactory = item => ObjectProperty(item.value.details.icon)
          cellFactory = (cell,image) => cell.graphic = new ImageView(image)
          prefWidth = 26
          resizable = false
        },
        new TableColumn[GWDetailedItem,String] {
          text = "Name"
          cellValueFactory = item => StringProperty(item.value.details.name)
        },
        new TableColumn[GWDetailedItem,String] {
          text = "Type"
          cellValueFactory = item => StringProperty(item.value.details.itemtype)
        },
        new TableColumn[GWDetailedItem,Int] {
          text = "Amount"
          cellValueFactory = item => ObjectProperty(item.value.amount)
        }
      )
    }

    val tokenField = new TextField {
      promptText = "API key"
      textFormatter = new TextFormatter[String]( //prevent user from inputting special symbols
        (change:Change) => {
          if (change.controlNewText.matches("""[\w\-]*""")) {
            change
          } else {
            null
          }
        }
      )
    }

    val disableButtons = (tokenField.text === "") || processingRequest

    val cancelButt = new Button {text = "Cancel request"}

    val cancelPane = new BorderPane {
      visible = false
      left = new Label("Working on it...")
      right = cancelButt
    }

    val buttonPane = new BorderPane {

      left = new Button {
        text = "Load character list"
        disable <== disableButtons
        onAction = (_:ActionEvent) => {
          processingRequest.value = true

          GW2APIService.getCharacters(tokenField.text.value).onComplete {
            case Success(values) => Platform.runLater {
              characterTable.items = ObservableBuffer.from(values)
              processingRequest.value = false
            }
            case Failure(err) => Platform.runLater {
              new Alert(AlertType.Error) {
                initOwner(stage)
                headerText = "Failed fetching character list."
                contentText = "Error: " + err.getMessage
              }.showAndWait()
              processingRequest.value = false
            }
          }
        }
      }

      right = new HBox {
        children ++= ObservableBuffer(
          new Button {
            text = "Load character inventory"
            disable <== disableButtons
            onAction = (_:ActionEvent) => {
              if(!characterTable.selectionModel.value.isEmpty) {
                processingRequest.value = true

                val (killswitch,request) = GW2APIService.getDetailedInventory(tokenField.text.value,
                  characterTable.selectionModel.value.selectedItem.value)

                cancelPane.visible = true
                cancelButt.onAction = (_:ActionEvent) => {
                  killswitch.abort(new Throwable("Request cancelled."))
                }

                request.onComplete {
                  case Success(values) => Platform.runLater {
                    itemTable.items = ObservableBuffer.from(values)
                    cancelPane.visible = false
                    processingRequest.value = false
                  }
                  case Failure(err) => Platform.runLater {
                    new Alert(AlertType.Error) {
                      initOwner(stage)
                      headerText = "Failed fetching inventory."
                      contentText = "Error: " + err.getMessage
                    }.showAndWait()
                    cancelPane.visible = false
                    processingRequest.value = false
                  }
                }
              }
            }
          },
          new Button {
            text = "Load material storage"
            disable <== disableButtons
            onAction = (_:ActionEvent) => {
              processingRequest.value = true

              val (killswitch,request) = GW2APIService.getDetailedMaterialStorage(tokenField.text.value)

              cancelPane.visible = true
              cancelButt.onAction = (_:ActionEvent) => {
                killswitch.abort(new Throwable("Request cancelled."))
              }

              request.onComplete {
                case Success(values) => Platform.runLater {
                  itemTable.items = ObservableBuffer.from(values)
                  cancelPane.visible = false
                  processingRequest.value = false
                }
                case Failure(err) => Platform.runLater {
                  new Alert(AlertType.Error) {
                    initOwner(stage)
                    headerText = "Failed fetching material storage."
                    contentText = "Error: " + err.getMessage
                  }.showAndWait()
                  cancelPane.visible = false
                  processingRequest.value = false
                }
              }
            }
          },
          new Button {
            text = "Load bank"
            disable <== disableButtons
            onAction = (_:ActionEvent) => {
              processingRequest.value = true

              val (killswitch,request) = GW2APIService.getDetailedBank(tokenField.text.value)

              cancelPane.visible = true
              cancelButt.onAction = (_:ActionEvent) => {
                killswitch.abort(new Throwable("Request cancelled."))
              }

              request.onComplete {
                case Success(values) => Platform.runLater {
                  itemTable.items = ObservableBuffer.from(values)
                  cancelPane.visible = false
                  processingRequest.value = false
                }
                case Failure(err) => Platform.runLater {
                  new Alert(AlertType.Error) {
                    initOwner(stage)
                    headerText = "Failed fetching bank."
                    contentText = "Error: " + err.getMessage
                  }.showAndWait()
                  cancelPane.visible = false
                  processingRequest.value = false
                }
              }
            }
          }
        )
      }
    }


    stage = new JFXApp3.PrimaryStage {
      onCloseRequest = (_:WindowEvent) => {actorsys.terminate(); Platform.exit()}
      scene = new Scene {
        root = new BorderPane {

          center = itemTable
          left = characterTable

          top = new VBox {
            children ++= ObservableBuffer(
              tokenField,
              buttonPane,
              cancelPane
            )
          }
        }
      }
    }
  }
}
