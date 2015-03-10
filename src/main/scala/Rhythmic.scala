/**
 * Rhythmic Music Player
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



object RhythmicFileUtils {

  // file extension helpers
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
    audioFiles ++ dirs.foldLeft(List[AudioFile]())( (result, dir) => result ++ getAudioFilesInDirectory(dir) )
  }


  def getAlbumsInDirectory(directory: File): List[Album] = {
    // entirely based off folder structure, rather than metadata

    // get all the files in the folder
    val files: List[File] = directory.listFiles().toList

    // recurse into all the directories, appending whatever is returned
    val dirs: List[File] = files.filter(_.isDirectory)

    if (dirs.size != 0) {
      // recurse if we find any directories at all
      // this will skip any free-floating music files
      List[Album]() ::: dirs.foldLeft(List[Album]()) { (z, f) =>
        val subAlbums = getAlbumsInDirectory(f)
        z ::: subAlbums
      }

    } else {
      // no directories, we're at a bottom level folder, which we assume is an album
      val audioFiles: List[AudioFile] = files.filter(_.isFile)
                                             .filter(isMusicFiletype)
                                             .map(AudioFileIO.read)

      // create the album
      val newAlbum: Album = new Album(audioFiles.head.getTag.getFirst(FieldKey.ALBUM),
                               audioFiles.head.getTag.getFirst(FieldKey.ARTIST),
                               audioFiles)

      // set the album art if we can find any in the local folder
      newAlbum.artworkFile = files.filter(_.isFile).find(isImageFiletype)

      if (!newAlbum.artworkFile.isDefined) {
        newAlbum.artworkFile = getAlbumArtFromLastFm(newAlbum.artist, newAlbum.name)
      }

      println("album: " + newAlbum.name + ", artist: " + newAlbum.artist
        + ", artwork: " + newAlbum.artworkFile)

      List(newAlbum)
    }
  }


  def getAlbumArtFromLastFm(artist: String, albumName: String) : Option[File] = {
    val lastfmKey: String = "c91a3cdf11a3f9ccbc1d861604eac168"
    val album: lastfm.Album = Album.getInfo(artist, albumName, lastfmKey)

    // mega, extralarge, large, medium, small all seem to usually have URLs
    // TODO make a fallback mechanism to pick the largest available art size
    //// (in case MEGA is absent)
    val albumUrl: String = album.getImageURL(ImageSize.MEGA)

    // get the file extension from the url
    val fileExtension: String = albumUrl.substring(albumUrl.lastIndexOf('.'), albumUrl.length)

    // TODO: save album art to the existing file structure and simplify all of this a bit
    // create the file handle
    val coverArtFile: File = new File("coverArt/" + albumName + " art" + fileExtension)

    if (!coverArtFile.exists()) {
      // download the file if we don't have it already
      println("downloading " + coverArtFile.getName)
      FileUtils.copyURLToFile(new URL(albumUrl), coverArtFile)
    }

    Some(coverArtFile)
  }

}


class Album(val name: String, val artist: String, var songs: List[AudioFile]) {
  var artworkFile: Option[File] = None

  // create a view of our album art
  def content (parentPane: Pane) = {
    println(name)
    Seq (
      new ImageView {
        def createAlbumImage(file: Option[File]): Image = {
          file match {
            case Some(f) =>
              val bufferedImage: BufferedImage = ImageIO.read(f)
              SwingFXUtils.toFXImage(bufferedImage, null)

            case None =>
              new Image("defaultAlbumArtwork.jpg")
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
