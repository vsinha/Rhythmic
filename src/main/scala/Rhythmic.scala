/**
 * Created by viraj on 2/25/15.
 */

import java.awt.Button
import java.awt.image.BufferedImage
import java.io.{IOException, File}
import java.util
import java.util.ArrayList
import java.util.logging.{Level, Logger}
import javafx.beans.InvalidationListener
import javafx.collections.{ListChangeListener, FXCollections, ObservableList}
import javax.imageio.ImageIO

import org.jaudiotagger.audio.{AudioFile, AudioFileIO}
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.FieldKey._

import scala.collection.JavaConversions._

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.geometry._
import scalafx.scene.control.ScrollPane.ScrollBarPolicy
import scalafx.scene.image.{ImageView, Image}
import scalafx.scene.{layout, Scene}
import scalafx.scene.control._
import scalafx.scene.input.{DragEvent, MouseEvent}
import scalafx.scene.layout._
import scalafx.scene.paint.Color._
import scalafx.scene.paint.{LinearGradient, Stops, Color}
import scalafx.scene.shape.{Circle, Rectangle}
import scalafx.scene.text.Text
import scalafx.stage.Stage
import scalafxml.core.{NoDependencyResolver, FXMLView}


object Rhythmic extends JFXApp {

  val listView = new ListView[String]()

  val topDir = new File("/Users/viraj/Music/testMusicFolder")
  val songs: List[AudioFile] = FileUtils.getAudioFilesInDirectory(topDir)
  val songNames = songs.map(x => x.getTag.getFirst(FieldKey.TITLE))
  val songNamesArrayList: util.ArrayList[String] = new util.ArrayList[String](songNames)
//  val songNamesBuffer: ObservableBuffer[String] =
  val observableList: ObservableList[String] = FXCollections.observableArrayList(new util.ArrayList[String] (songNamesArrayList))
  listView.setItems(observableList)

  songNames.map(s => println(s))

  //println(songNames size)

  val albumArt = songs.map(s => {
    s.getTag.getArtworkList
  })

  //println(albumArt)

  val albums: List[Album] = FileUtils.getAlbumsInDirectory(topDir)

  val textList = (0 to 20).map(_ => new Text {
    text = "Hello3 "
    style = "-fx-font-size: 24pt"
    fill = new LinearGradient(
      endX = 0,
      stops = Stops(PALEGREEN, SEAGREEN))
  })

  val albumCellPadding: Int = 2
  val startingScreenWidth: Int = 640
  val startingScreenHeight: Int = 480

  val rectanglePanes = albums.map(album => new Pane { thisPane =>
    style = "-fx-background-color: gray;"
    padding = Insets(0, 0, albumCellPadding, albumCellPadding)
    prefWidth = (startingScreenWidth / 4) - albumCellPadding
    prefHeight <== prefWidth

    content = new VBox { thisVbox =>
      style = "-fx-background-color: gray;"

      onMouseClicked = (e: Event) => {
        println("mouse click!")
        e.consume()
      }

      content = Seq (
//        new Rectangle {
//          width <== thisPane.width
//          height <== width * 0.7
//          fill <== when(hover) choose Color.DARKCYAN otherwise TEAL
//        },
        new ImageView {

          def createAlbumImage(file: Option[File]): Image = {
            file match {
              case Some(f) => {
                println(f.getCanonicalPath)
                val bufferedImage: BufferedImage = ImageIO.read(f)
                SwingFXUtils.toFXImage(bufferedImage, null)
              }
              case None => {
                println("no image file found")
                new Image("defaultAlbumArtwork.jpg")
              }
            }
          }

          image = createAlbumImage(album.artworkFile)
          fitWidth <== thisPane.width
          preserveRatio = true
          smooth = true
          cache = true
          opacity <== when (hover) choose 0.5 otherwise 1.0
        }
//        ,
//        new Label {
//          maxWidth <== thisPane.width
//          //maxHeight <== thisPane.height * 0.1
//          text = album.name
//          style = "-fx-font-size: 8pt"
//          //fill = BLACK
//          alignment = Pos.BASELINE_CENTER
//          visible <== when(hover) choose true otherwise false
//        }
// ,
//        new Label {
//          maxWidth <== thisPane.width
//          //maxHeight <== thisPane.height * 0.1
//          text = album.artist
//          style = "-fx-font-size: 8pt"
//          //fill = BLACK
//          alignment = Pos.BASELINE_CENTER
//        }
      )
    }
  })

//  rectanglePanes.map(p => p.setOnDragDetected((event: MouseEvent) => {
//    println("click drag detected")
//    event.consume()
//  }))
//
//  rectanglePanes.map(p => p.setOnDragOver((event: DragEvent) => {
//    println("drag over detected")
//    event.consume()
//  }))
//
//  rectanglePanes.map(p => p.setOnDragDropped((event: DragEvent) => {
//    println("drop detected")
//    event.setDropCompleted(true)
//    event.consume()
//  }))

  val flowPane: FlowPane = new FlowPane {
    prefWidth  = 640 // leave room for scroll bar
    prefHeight = 100
    orientation = Orientation.HORIZONTAL
    vgap = albumCellPadding
    hgap = albumCellPadding
    columnHalignment = HPos.LEFT
    content = rectanglePanes
  }
  this.flowPane.setSnapToPixel(true)


  def scaleRectangles(): Unit = {
    this.flowPane.setPrefWidth(this.scrollPane.getBoundsInLocal.getWidth)
    this.flowPane.setMaxWidth(this.scrollPane.getBoundsInLocal.getWidth)
    println("scrollpane width: " + this.scrollPane.getBoundsInLocal.getWidth)

    var multiplier: Double = 1.0

    // func to scale an individual pane
    def scaleRectangle(pane: Pane, multiplier: Double): Unit = {
      val prefWidth = Math.floor(this.flowPane.getPrefWidth * multiplier) - albumCellPadding
      pane.setPrefWidth(prefWidth)
    }

    this.rectanglePanes.map(p => {
      flowPane.getWidth match {
        case n if   0 until 200 contains n => multiplier = 1.0
        case n if 200 until 400 contains n => multiplier = 0.5
        case n if 400 until 600 contains n => multiplier = 1.0/3.0
        case n if 600 until 800 contains n => multiplier = 0.25
        case _                             => multiplier = 0.2
      }

      scaleRectangle(p, multiplier)
    })
  }

  stage = new PrimaryStage() {
    title.value = "Hello Stage"
    height = startingScreenHeight
    width = startingScreenWidth

    width onChange scaleRectangles
    height onChange scaleRectangles
  }

  val scrollPane: ScrollPane = new ScrollPane {
    styleClass.add("noborder-scroll-pane")
    prefHeight <== stage.height
    prefWidth <== stage.width
    content = flowPane
  }

  this.scrollPane.setVbarPolicy(ScrollBarPolicy.NEVER)

  stage.scene = new Scene {
    root = new BorderPane {
      center = scrollPane
    }
  }
}

object FileUtils {

  def getListOfSubDirectories(directoryName: String): List[File] =
    new File(directoryName).listFiles().filter(_.isDirectory).toList

  val musicFileExtensions = List(".mp3", ".mp4", ".flac", ".ogg", ".wav")
  val imageFileExtensions = List(".jpg", ".png", ".gif")
  def isMusicFiletype(file: File): Boolean = musicFileExtensions.exists(file.getName.endsWith)
  def isImageFiletype(file: File): Boolean = imageFileExtensions.exists(file.getName.endsWith)

  // recurse all the way down to find all MP3s
  def getAudioFilesInDirectory(directory: File): List[AudioFile] = {
    Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING)

    println("dir: " + directory.getName)

    val files: List[File] = directory.listFiles().toList

    // get all the songs in the directory
    val audioFiles: List[AudioFile] = files.filter(_.isFile)
                                 .filter(f => isMusicFiletype(f))
                                 .map(f => AudioFileIO.read(f))


    val dirs = files.filter(_.isDirectory)

    audioFiles ++ dirs.foldLeft(List[AudioFile]())((result, dir) => result ++ getAudioFilesInDirectory(dir))
  }

  def getAlbumsInDirectory(directory: File): List[Album] = {
    // get all the files in the folder
    val files: List[File] = directory.listFiles().toList
    val albums: List[Album] = List[Album]()

    // recurse into all the directories
    val dirs = files.filter(_.isDirectory)

    if (dirs.size != 0) {
      albums ::: dirs.foldLeft(List[Album]()) { (z, f) =>
        val subAlbums = getAlbumsInDirectory(f)
        z ::: subAlbums
      }
    } else { // no directories, we're at a bottom level folder, which we assume
             // is an album
      val audioFiles: List[AudioFile] = files.filter(_.isFile)
        .filter(isMusicFiletype)
        .map(AudioFileIO.read)

      val albumName: String = audioFiles.head.getTag.getFirst(FieldKey.ALBUM)
      val albumArtist: String = audioFiles.head.getTag.getFirst(FieldKey.ARTIST)

      println("album: " + albumName + ", artist: " + albumArtist)

      val newAlbum = new Album(albumName, albumArtist)

      // replaced filter(isImageFiletype).headOption with find(isImageFiletype)
      newAlbum.artworkFile = files.filter(_.isFile).find(isImageFiletype)

      println(newAlbum.artworkFile)

      newAlbum.songs = audioFiles

      List(newAlbum)
    }

    // if there are any directories, recurse

    // else:
    // add mp3/flac files as Audio objects
    // add any image files as the album art
  }

  /*
  def main (args: Array[String]): Unit = {
    // turn down the jaudiotagger default log level
    Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING)

    val topDir = new File("/Users/viraj/Music/testMusicFolder")
    val songs: List[AudioFile] = getAudioFilesInDirectory(topDir)
    songs.map(x => println("album: " + x.getTag.getFirst(ALBUM) + ". filename: " + x.getFile.getName))
  }
  */
}

class Album(val name: String, val artist: String) {
  var songs: List[AudioFile] = List()
  var artworkFile: Option[File] = None
}