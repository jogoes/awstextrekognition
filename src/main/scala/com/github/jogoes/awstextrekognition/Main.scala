package com.github.jogoes.awstextrekognition

import java.io.{FileOutputStream, PrintWriter}
import java.nio.file._

import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder
import com.amazonaws.services.rekognition.model.TextDetection

import scala.util.Success

case class FileTextDetection(path: Path, detections: Seq[TextDetection])

case class TextDetectionResult(imageInfo: ImageInfo, textDetections: List[TextDetection], targetPath: Path)

object Main {

  def jsonFile(path: Path): Path = {
    val parent = path.getParent
    parent.resolve(s"${path.getFileName}.json")
  }

  def runDetection(textDetector: TextDetector, images: Seq[ImageInfo], targetPath: Path): Seq[TextDetectionResult] = {
    images
      .map(imageInfo => {
        val textDetection = textDetector.detectText(imageInfo.createStream())
        textDetection match {
          case Success(detections) => Some((imageInfo, detections))
          case _ => None
        }
      })
      .flatMap {
        case Some((imageInfo, textDetections)) =>
          val targetFile = imageInfo.metadata match {
            case FileImageMetadata(file, path) =>
              val targetFilePath = targetPath.resolve(file)
              Files.copy(path, targetFilePath, StandardCopyOption.REPLACE_EXISTING)
              targetFilePath
            case RotatedTextImageMetadata(angle, text) =>
              val is = imageInfo.createStream()
              val targetFilePath = targetPath.resolve(s"rotate_$angle.jpg")
              IOHelper.withResource(new FileOutputStream(targetFilePath.toFile))(_.close()) { os =>
                os.write(IOHelper.toByteArray(is))
                os.flush()
              }
              targetFilePath
          }
          Some(TextDetectionResult(imageInfo, textDetections, targetFile))
        case _ => None
      }
  }

  def detectAndReport(textDetector: TextDetector, images: Seq[ImageInfo], targetPath: Path): Unit = {

    val reportFile = targetPath.resolve("index.html")
    val reportWriter = new PrintWriter(new FileOutputStream(reportFile.toFile))

    reportWriter.println("<html><head></head><body>")
    reportWriter.println("<table><tr>")
    reportWriter.println("<th>Image</th><th>Detected text</th>")
    reportWriter.println("</tr>")

    runDetection(textDetector, images, targetPath).foreach {
      case TextDetectionResult(imageInfo, textDetections, targetFilePath) =>
        reportWriter.print("<tr>")
        val fileName = targetFilePath.getFileName.toString
        reportWriter.print(s"""<td><img src="$fileName" alt="$fileName" width=640></td>""")
        val texts = textDetections.map(detection => {
          val confidence = detection.getConfidence.toInt
          s"${detection.getDetectedText} ($confidence%)"
        }).mkString(", ")
        reportWriter.print(s"<td>$texts</td>")
        reportWriter.print("</tr>")

        reportWriter.println()
    }
    reportWriter.print("</table>")
    reportWriter.print("</body></html>")
    reportWriter.flush()
    reportWriter.close()
    println(s"Completed. Report created in ${reportFile.toString}")
  }

  def runDetection(path: Path): Unit = {
    val rekognitionClient = AmazonRekognitionClientBuilder.standard()
      .withRegion("eu-west-1")
      .build()
    val textDetector = new TextDetector(rekognitionClient)

    val imageRotationSource = new RotatedTextImageSource(0, 90, 10, angle => s"Rotated text at $angle degrees", 600, 600)
    val fsImageSource = new DirectoryImageSource(path)

    val imagesSource = imageRotationSource.images ++ fsImageSource.images

    val targetPath = path.resolve("report")
    if (!targetPath.toFile.exists()) {
      Files.createDirectory(targetPath)
    }

    detectAndReport(textDetector, imagesSource, targetPath)
  }

  def main(args: Array[String]): Unit = {
    args.toList match {
      case dir :: Nil => runDetection(Paths.get(dir))
      case _ => println("Required path argument missing.")
    }
  }
}
