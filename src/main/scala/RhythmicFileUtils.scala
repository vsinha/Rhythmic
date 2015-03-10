/**
 * Created by viraj on 3/9/15.
 *
 */

import java.io.File
import java.net.URL

import de.umass.lastfm
import de.umass.lastfm.{Album, ImageSize}
import org.apache.commons.io.FileUtils
import org.jaudiotagger.audio.{AudioFile, AudioFileIO}
import org.jaudiotagger.tag.FieldKey


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
    val lastfmKey = "c91a3cdf11a3f9ccbc1d861604eac168"
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