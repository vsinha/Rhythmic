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


object Rhythmic extends JFXApp {

  // Log level for jaudiotagger is overly verbose by default
  Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING)

  val topDir = new File("/Users/viraj/Music/testMusicFolder")

  val albumCellPadding: Int = 0
  val startingScreenWidth: Int = 640
  val startingScreenHeight: Int = 480

  def moveToFront[A](y: A, xs: List[A]): List[A] = {
    xs.span(_ != y) match {
      case (as, h :: bs) => h :: as ++ bs
      case _ => xs
    }
  }

  // initialize the observable list of albums
  var albums: List[Album] = RhythmicFileUtils.getAlbumsInDirectory(topDir)
  var albumList: ObservableList[Album] = FXCollections.observableArrayList()
  albums.foreach { albumList.add }

  var rectanglePanes: ObservableList[Pane] = createRectanglePanes

  def createRectanglePanes: ObservableList[Pane] = {
    val panes = albumList.map(f = album => new Pane {
      thisPane =>
      style = "-fx-background-color: gray;"
      padding = Insets(0, 0, albumCellPadding/2, albumCellPadding)
      prefWidth = (startingScreenWidth / 4) - albumCellPadding
      prefHeight <== prefWidth

      content = new VBox {
        thisVbox =>
        style = "-fx-background-color: gray;"

        onMouseClicked = (e: Event) => {
          println("mouse click!")
          e.consume()

          // this code moves the clicked album to the top of the list
//          albumList.remove(album)
//          albumList.add(0, album)
//
//          albumList.map(a => println(a.name))
        }

        content = album.content(thisPane)
      }
    })
    panes
  }

  val flowPane: FlowPane = new FlowPane {
    prefWidth  = 640
    prefHeight = 100
    orientation = Orientation.HORIZONTAL
    vgap = albumCellPadding
    hgap = albumCellPadding
    columnHalignment = HPos.LEFT
    content = rectanglePanes
  }

  this.flowPane.setSnapToPixel(true)


  def updateFlowPane(): Unit = {
    rectanglePanes = createRectanglePanes
    flowPane.content = rectanglePanes
  }

  def scaleRectangles(): Unit = {
    // gets called whenever the main window size changes
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

  // keep the UI Cleaner by hiding scrollbars
  this.scrollPane.setVbarPolicy(ScrollBarPolicy.NEVER)
  this.scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER)

  stage.scene = new Scene {
    root = new BorderPane {
      center = scrollPane
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
        println("album art url: " + albumUrl)

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
    Seq (
      new ImageView {
        def createAlbumImage(file: Option[File]): Image = {
          file match {
            case Some(f) => {
              println(f.getCanonicalPath)
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