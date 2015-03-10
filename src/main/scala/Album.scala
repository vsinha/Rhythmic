/**
 * Created by viraj on 3/9/15.
 *
 */

import java.awt.image.BufferedImage
import java.io.File
import javafx.embed.swing.SwingFXUtils
import javax.imageio.ImageIO

import org.jaudiotagger.audio.AudioFile

import scalafx.Includes._
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.Pane


class Album(val name: String, val artist: String, var songs: List[AudioFile]) {
  var artworkFile: Option[File] = None

  // TODO not sure if this lazy val deal is the right way to do this
  // and realistically we want album art to be able to come in on a separate thread
  lazy val artworkImage = createAlbumImage(artworkFile)

  def createAlbumImage(file: Option[File]): Image = {
    file match {
      case Some(f) =>
        val bufferedImage: BufferedImage = ImageIO.read(f)
        SwingFXUtils.toFXImage(bufferedImage, null)

      case None =>
        new Image("defaultAlbumArtwork.jpg")
    }
  }

  // create a view of our album art
  def content (parentPane: Pane) = {
    new ImageView {
      image = artworkImage
      fitWidth <== parentPane.width
      preserveRatio = true
      smooth = true
      cache = true
      opacity <== when (hover) choose 0.5 otherwise 1.0
    }
  }
}
