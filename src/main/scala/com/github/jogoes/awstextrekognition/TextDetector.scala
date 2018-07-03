package com.github.jogoes.awstextrekognition

import java.io.{File, InputStream}
import java.nio.ByteBuffer

import com.amazonaws.services.rekognition.AmazonRekognition
import com.amazonaws.services.rekognition.model.{DetectTextRequest, Image, S3Object, TextDetection}

import scala.util.{Failure, Try}
import scala.collection.JavaConverters._

class TextDetector(rekognition: AmazonRekognition) {

  // the maximum size AWS allows for uploading image data
  // images with bigger sizes have to be taken from S3
  private val MAX_IMAGE_SIZE = 5 * 1024 * 1024

  def detectText(bucketName: String, imageName: String): Try[List[TextDetection]] = {
    val image = new Image()
      .withS3Object(new S3Object()
        .withBucket(bucketName)
        .withName(imageName))

    detectText(image)
  }

  def detectText(is: InputStream): Try[List[TextDetection]] = {
    val bytes = IOHelper.toByteArray(is)
    detectText(ByteBuffer.wrap(bytes))
  }

  def detectText(bytes: ByteBuffer): Try[List[TextDetection]] = {
    val imageBytes = bytes.remaining()
    if (bytes.remaining() > MAX_IMAGE_SIZE) {
      Failure(new IllegalArgumentException(s"Image data is too large: maximum: $MAX_IMAGE_SIZE, actual: $imageBytes"))
    }
    val image = new Image().withBytes(bytes)
    detectText(image)
  }

  def detectText(image: Image): Try[List[TextDetection]] = Try {
    val request = new DetectTextRequest()
      .withImage(image)
    rekognition.detectText(request).getTextDetections.asScala.toList
  }

  def detectText(file: File): Try[List[TextDetection]] = {
    IOHelper.readFile(file, MAX_IMAGE_SIZE).flatMap {
      case Some(buffer) => detectText(buffer)
      case _ => Failure(throw new UnsupportedOperationException(s"Images < $MAX_IMAGE_SIZE currently not supported"))
    }
  }

}
