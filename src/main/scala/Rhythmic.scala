/**
 * Rhythmic Music Player
 * Created by viraj on 2/25/15.
 */

import java.io.File
import java.util.logging.{Level, Logger}
import javafx.collections.{FXCollections, ObservableList}

import scala.collection.JavaConversions._
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.event.Event
import scalafx.geometry._
import scalafx.scene.Scene
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.text.TextAlignment


object Rhythmic extends JFXApp {

  // Log level for jaudiotagger is overly verbose by default
  Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING)

  val musicDirectory = new File("/Users/viraj/Music/testMusicFolder")
  val startingScreenWidth  = 500
  val startingScreenHeight = 520

  // initialize the observable list of albums
  var albumList: ObservableList[Album] = FXCollections.observableArrayList()
  albumList.addAll(RhythmicFileUtils.getAlbumsInDirectory(musicDirectory))

  // each rectangle pane should hold an album and play controls
  val rectanglePanes: ObservableList[Pane] = rectanglePanesBuilder


  def rectanglePanesBuilder: ObservableList[Pane] = {
    println("creating rectangle panes")
    val panes = albumList.map(f = album => new Pane { thisPane =>
      style = "-fx-background-color: gray;"

      // bind width to height to automagically maintain squares
      // start up with a nice 3x3 grid of albums
      prefWidth = startingScreenWidth / 3
      prefHeight <== prefWidth

      // set up the click handler
      onMouseClicked = mouseClickOnAlbumHandlerCreator(album)

      content = album.content(thisPane)
    })
    panes
  }


  def mouseClickOnAlbumHandlerCreator(album: Album) = {
    (e: Event) => {
      // build and return this closure:
      println("mouse click on " + album.name)
      e.consume()

      // this code moves the clicked album to the top of the list
      albumList.remove(album)
      albumList.add(0, album)

      updateFlowPane()
    }
  }


  // this is the only function that seems to need this flowPane field
  // TODO maybe refactor this so flowpane doesn't have to be explicitly exposed at all?
  var flowPane: FlowPane = null
  def updateFlowPane(): Unit = {
    // we zip the refreshed album list into the existing set of panes
    // to avoid having to reload and resize every pane
    (rectanglePanes, albumList).zipped map ((p, a) => {
      // update the content
      p.content = a.content(p)

      // and update the onClick handler to point at the new album sitting in this pane
      p.onMouseClicked = mouseClickOnAlbumHandlerCreator(a)
    })

    flowPane.content = rectanglePanes
  }


  // nifty function to scale an individual pane
  private def _scaleRectangle(pane: Pane, multiplier: Double): Unit = {
    val prefWidth = Math.floor(this.stage.getWidth * multiplier)
    pane.setPrefWidth(prefWidth)
  }


  // calculate the scaling multiplier of each album art display based
  // on the width of the flow pane
  private def _getMultiplierFromPaneWidth: Double = {
      stage.getWidth match {
      case n if   0 until 200 contains n => 1.0
      case n if 200 until 400 contains n => 0.5
      case n if 400 until 600 contains n => 1.0/3.0
      case n if 600 until 800 contains n => 0.25
      case _                             => 0.2
    }
  }


  def scaleRectangles(): Unit = {
    // gets called whenever the main window size changes
    println("stage width: " + stage.width.value)

    // scale all the rectangles
    rectanglePanes.map(p => _scaleRectangle(p, _getMultiplierFromPaneWidth))
  }


  // should display some "now playing" info, maybe have some play/pause buttons?
  val statusBar: HBox = new HBox { thisBox =>
    content = Seq (
      new Label {
        prefWidth <== thisBox.width / 2
        text = "This text is a test."
        style = "-fx-font-size: 8pt"
      },
      new Label {
        prefWidth <== thisBox.width / 2
        text = "This is another test"
        style = "-fx-font-size: 8pt"
        textAlignment = TextAlignment.RIGHT
        alignment = Pos.TOP_RIGHT
      }
    )
  }


  stage = new PrimaryStage() { thisStage =>
    title.value = "Rhythmic"
    width  = startingScreenWidth
    height = startingScreenHeight

    // these bindings do the heavy lifting for resizing all the children
    width  onChange scaleRectangles
    height onChange scaleRectangles

    scene = new Scene {

      root = new BorderPane {

        center = new VBox { // vertically arrange statusbar
          content = Seq(
          
            statusBar,

            new ScrollPane {
              style = "-fx-background-color:transparent"

              // keep the UI Cleaner by hiding scrollbars
              vbarPolicy = ScrollBarPolicy.NEVER
              hbarPolicy = ScrollBarPolicy.NEVER

              content = new FlowPane { thisFlowPane =>
                flowPane = thisFlowPane // set the class-wide handle to this
                prefWidth  <== thisStage.width
                prefHeight <== thisStage.height
                orientation = Orientation.HORIZONTAL
                style = "-fx-background-color:transparent"
                columnHalignment = HPos.CENTER
                content = rectanglePanes
                snapToPixel = true
              } // end FlowPane
            } // end ScrollPane
          ) // end VBox Seq
        } // end VBox
      } // end BorderPane
    } // end Scene
  } // end Stage
}