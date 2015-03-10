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
