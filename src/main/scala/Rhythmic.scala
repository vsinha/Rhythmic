/**
 * Created by viraj on 2/25/15.
 */

import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import java.util.logging.{Level, Logger}
import javafx.collections.{FXCollections, ObservableList}
import javafx.embed.swing.SwingFXUtils
import javax.imageio.ImageIO

import de.umass.lastfm
import de.umass.lastfm._
import org.apache.commons.io.FileUtils
import org.jaudiotagger.audio.{AudioFile, AudioFileIO}
import org.jaudiotagger.tag.FieldKey

import collection.JavaConversions._

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.event.Event
import scalafx.geometry._
import scalafx.scene.Scene
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.control._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout._
import scalafx.scene.paint.Color
import scalafx.scene.text.TextAlignment
import scalafx.stage.StageStyle


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
  }

  this.flowPane.setSnapToPixel(true)



  def scaleRectangles(): Unit = {
    // gets called whenever the main window size changes
    this.flowPane.setPrefWidth(this.scrollPane.getBoundsInLocal.getWidth)
    this.flowPane.setMaxWidth(this.scrollPane.getBoundsInLocal.getWidth)
    println("scrollpane width: " + this.scrollPane.getBoundsInLocal.getWidth)

    // nifty function to scale an individual pane
    def scaleRectangle(pane: Pane, multiplier: Double): Unit = {
      val prefWidth = Math.floor(this.flowPane.getPrefWidth * multiplier) - albumCellPadding
      pane.setPrefWidth(prefWidth)
    }

    // calculate the scaling multiplier of each album art display based
    // on the width of the flow panel
    var multiplier: Double = 1.0
    flowPane.getWidth match {
      case n if   0 until 200 contains n => multiplier = 1.0
      case n if 200 until 400 contains n => multiplier = 0.5
      case n if 400 until 600 contains n => multiplier = 1.0/3.0
      case n if 600 until 800 contains n => multiplier = 0.25
      case _                             => multiplier = 0.2
    }

    // scale all the rectangles
    rectanglePanes.map(p => scaleRectangle(p, multiplier))
  }

  stage = new PrimaryStage() {
    title.value = "Hello Stage"
    height = startingScreenHeight
    width = startingScreenWidth

    width onChange scaleRectangles
    height onChange scaleRectangles
  }

  val statusBar: HBox = new HBox { thisBox =>
    content = Seq (
      new Label {
        prefWidth <== thisBox.width / 2
        text = "This text is a test."
        style = "-fx-font-size: 8pt"
        //fill = BLACK
      },
      new Label {
        prefWidth <== thisBox.width / 2
        text = "This is another test"
        style = "-fx-font-size: 8pt"
        textAlignment = TextAlignment.RIGHT
        alignment = Pos.TOP_RIGHT
        //fill = BLACK
      }
    )
  }

  val scrollPane: ScrollPane = new ScrollPane {
    //styleClass.add("noborder-scroll-pane")
    style = "-fx-background-color:transparent"
    prefHeight <== stage.height
    prefWidth <== stage.width
    content = flowPane
  }

  // keep the UI Cleaner by hiding scrollbars
  this.scrollPane.setVbarPolicy(ScrollBarPolicy.NEVER)
  this.scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER)

  stage.scene = new Scene {
    root = new BorderPane {
      center = new VBox {
        content = Seq (
          statusBar,
          scrollPane
        )
      }
    }
  }
}

object RhythmicFileUtils {

  def getListOfSubDirectories(directoryName: String): List[File] =
    new File(directoryName).listFiles().filter(_.isDirectory).toList

  val musicFileExtensions = List(".mp3", ".mp4", ".flac", ".ogg", ".wav")
  val imageFileExtensions = List(".jpg", ".png", ".gif")
  def isMusicFiletype(file: File): Boolean = musicFileExtensions.exists(file.getName.endsWith)
  def isImageFiletype(file: File): Boolean = imageFileExtensions.exists(file.getName.endsWith)

  // recurse all the way down to find all MP3s
  def getAudioFilesInDirectory(directory: File): List[AudioFile] = {
    println("dir: " + directory.getName)

    val files: List[File] = directory.listFiles().toList

    // get all the songs in the directory
    val audioFiles: List[AudioFile] = files.filter(_.isFile)
                                 .filter(f => isMusicFiletype(f))
                                 .map(f => AudioFileIO.read(f))

    // get all the directories
    val dirs: List[File] = files.filter(_.isDirectory)

    // recurse into directories
    audioFiles ++ dirs.foldLeft(List[AudioFile]())((result, dir) => result ++ getAudioFilesInDirectory(dir))
  }

  def getAlbumsInDirectory(directory: File): List[Album] = {
    // entirely based off folder structure rather than metadata

    // initialize our list
    val albums: List[Album] = List[Album]()

    // get all the files in the folder
    val files: List[File] = directory.listFiles().toList

    // recurse into all the directories, appending whatever is returned
    val dirs = files.filter(_.isDirectory)

    if (dirs.size != 0) {
      // recurse if we find any directories at all
      // this will skip any free-floating music files
      albums ::: dirs.foldLeft(List[Album]()) { (z, f) =>
        val subAlbums = getAlbumsInDirectory(f)
        z ::: subAlbums
      }
    } else {
      // no directories, we're at a bottom level folder, which we assume is an album
      val audioFiles: List[AudioFile] = files.filter(_.isFile)
                                             .filter(isMusicFiletype)
                                             .map(AudioFileIO.read)

      // create the album!
      val newAlbum = new Album(audioFiles.head.getTag.getFirst(FieldKey.ALBUM),
                               audioFiles.head.getTag.getFirst(FieldKey.ARTIST),
                               audioFiles)

      // set the album art if we can find any
      newAlbum.artworkFile = files.filter(_.isFile).find(isImageFiletype)

      if (!newAlbum.artworkFile.isDefined) {
        val lastfmKey: String = "c91a3cdf11a3f9ccbc1d861604eac168"
        val album: lastfm.Album = Album.getInfo(newAlbum.artist, newAlbum.name, lastfmKey)

        // mega, extralarge, large, medium, small all seem to usually have URLs
        // TODO make a fallback mechanism to pick the largest available art size
        //// (in case MEGA is absent)
        val albumUrl = album.getImageURL(ImageSize.MEGA)

        // get the file extension from the url
        val fileExtension = albumUrl.substring(albumUrl.lastIndexOf('.'), albumUrl.length)

        // create the file handle
        val coverArtFile = new File("coverArt/" + newAlbum.name + " art" + fileExtension)

        if (!coverArtFile.exists()) {
          // download the file if we don't have it already
          println("downloading " + coverArtFile.getName)
          FileUtils.copyURLToFile(new URL(albumUrl), coverArtFile)
        }

        newAlbum.artworkFile = Some(coverArtFile)
      }


      println("album: " + newAlbum.name + ", artist: " + newAlbum.artist
        + ", artwork: " + newAlbum.artworkFile)

      List(newAlbum)
    }
  }
}

class Album(val name: String, val artist: String, var songs: List[AudioFile]) {
  var artworkFile: Option[File] = None

  // create the panel to view this album
  def content (parentPane: Pane) = {
    println(name)
    Seq (
      new ImageView {
        def createAlbumImage(file: Option[File]): Image = {
          file match {
            case Some(f) => {
              //println(f.getCanonicalPath)
              val bufferedImage: BufferedImage = ImageIO.read(f)
              SwingFXUtils.toFXImage(bufferedImage, null)
            }
            case None => {
              new Image("defaultAlbumArtwork.jpg")
            }
          }
        }

        image = createAlbumImage(artworkFile)
        fitWidth <== parentPane.width
        preserveRatio = true
        smooth = true
        cache = true
        opacity <== when (hover) choose 0.5 otherwise 1.0
      }
    )
  }
}


//
//    panes.map(p => p.setOnDragDetected((event: MouseEvent) => {
//      println("click drag detected")
//      //event.consume()
//    }))
//
//    panes.map(p => p.setOnDragOver((event: Event) => {
//      println("drag over detected")
//      //event.consume()
//    }))
//
//    panes.map(p => p.setOnDragDropped((event: Event) => {
//      println("drop detected")
//      //event.setDropCompleted(true)
//      //event.consume()
//    }))


//        ,
