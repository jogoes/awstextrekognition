package com.github.jogoes.awstextrekognition

import java.io._
import java.nio.file.Path

import javax.imageio.ImageIO

sealed trait ImageMetadata

final case class FileImageMetadata(filename: String, path: Path) extends ImageMetadata

final case class RotatedTextImageMetadata(angle: Int, text: String) extends ImageMetadata

case class ImageInfo(metadata: ImageMetadata, createStream: () => InputStream)

trait ImageSource {
  def images: Seq[ImageInfo]
}

class DirectoryImageSource(path: Path) extends ImageSource {
  override def images: Seq[ImageInfo] = {

    path.toFile.listFiles(t => {
      val name = t.getName.toLowerCase()
      name.endsWith("jpg") || name.endsWith("gif") || name.endsWith("png")
    })
      .map(file => ImageInfo(FileImageMetadata(file.getName, file.toPath), () => new FileInputStream(file)))
  }
}

class RotatedTextImageSource(angleFrom: Int, angleTo: Int, angleStep: Int, textFromAngle: Int => String, width: Int, height: Int) extends ImageSource {
  override def images: Seq[ImageInfo] = {
    (angleFrom to angleTo by angleStep).map(angle => {
      val image = ImageUtils.createImage(width, height)
      val text = s"${textFromAngle(angle)}"
      ImageUtils.addText(image, text, angle)

      val metadata = RotatedTextImageMetadata(angle, text)

      val bos = new ByteArrayOutputStream()
      ImageIO.write(image, "jpg", bos)
      ImageInfo(metadata, () => new ByteArrayInputStream(bos.toByteArray))
    })
  }
}
