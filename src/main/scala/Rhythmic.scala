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
  val albumCellPadding: Int = 0
  val startingScreenWidth: Int = 500
  val startingScreenHeight: Int = 520

  // initialize the observable list of albums
  var albumList: ObservableList[Album] = FXCollections.observableArrayList()
  albumList.addAll(RhythmicFileUtils.getAlbumsInDirectory(musicDirectory))


  val rectanglePanes: ObservableList[Pane] = rectanglePanesBuilder


  def rectanglePanesBuilder: ObservableList[Pane] = {
    println("creating rectangle panes")
    val panes = albumList.map(f = album => new Pane {
      thisPane =>
      style = "-fx-background-color: gray;"

      prefWidth = (startingScreenWidth / 3) - albumCellPadding
      prefHeight <== prefWidth

      onMouseClicked = (e: Event) => {
        println("mouse click on " + album.name)
        e.consume()

        // this code moves the clicked album to the top of the list
        albumList.remove(album)
        albumList.add(0, album)

        updateFlowPane()
      }

      content = album.content(thisPane)
    })
    panes
  }


  def mouseClickOnAlbumHandlerCreator(album: Album) = {
    // build and return this closure:
    (e: Event) => {
      println("mouse click on " + album.name)
      e.consume()

      // this code moves the clicked album to the top of the list
      albumList.remove(album)
      albumList.add(0, album)

      updateFlowPane()
    }
  }


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


  val flowPane: FlowPane = new FlowPane {
    prefWidth  = startingScreenWidth
    prefHeight = startingScreenHeight
    orientation = Orientation.HORIZONTAL
    style = "-fx-background-color:transparent"
    vgap = albumCellPadding
    hgap = albumCellPadding
    columnHalignment = HPos.LEFT
    content = rectanglePanes
    snapToPixel = true
  }


  // nifty function to scale an individual pane
  def scaleRectangle(pane: Pane, multiplier: Double): Unit = {
    val prefWidth = Math.floor(this.flowPane.getPrefWidth * multiplier) - albumCellPadding
    pane.setPrefWidth(prefWidth)
  }


  // calculate the scaling multiplier of each album art display based
  // on the width of the flow pane
  def getMultiplierFromPaneWidth: Double = {
      flowPane.getWidth match {
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

    this.flowPane.setPrefWidth(stage.width.value)
    this.flowPane.setMaxWidth(stage.width.value)


    val multiplier: Double = getMultiplierFromPaneWidth

    // scale all the rectangles
    rectanglePanes.map(p => scaleRectangle(p, multiplier))
  }


  // allows us to scroll up and down (and technically left/right as well)
  val scrollPane: ScrollPane = new ScrollPane {
    //styleClass.add("noborder-scroll-pane")
    style = "-fx-background-color:transparent"
    prefHeight = startingScreenHeight
    prefWidth = startingScreenWidth

    // keep the UI Cleaner by hiding scrollbars
    vbarPolicy = ScrollBarPolicy.NEVER
    hbarPolicy = ScrollBarPolicy.NEVER

    content = flowPane
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


  stage = new PrimaryStage() {
    title.value = "Rhythmic"
    width = startingScreenWidth
    height = startingScreenHeight

    // these bindings do the heavy lifting for resizing all the children
    width onChange scaleRectangles
    height onChange scaleRectangles

    scene = new Scene {
      root = new BorderPane {
        center = new VBox { // vertically arrange statusbar
          content = Seq(
            statusBar,
            scrollPane
          )
        }
      }
    }
  }
}