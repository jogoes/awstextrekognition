package com.github.jogoes.awstextrekognition

import java.awt.image.BufferedImage
import java.awt.{Color, Font}
import IOHelper._

object ImageUtils {

  def toRad(angleDeg: Int): Double = angleDeg * Math.PI / 180

  def createImage(width: Int, height: Int): BufferedImage = {
    new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
  }

  /**
    * Draw the specified text in the given image rotated with the given angle.
    */
  def addText(image: BufferedImage, text: String, angleDeg: Int): BufferedImage = {
    val angleRad = toRad(angleDeg)

    val width = image.getWidth
    val height = image.getHeight

    withResource(image.createGraphics())(_.dispose()) { g =>
      g.setBackground(Color.white)
      g.setPaint(Color.black)
      g.clearRect(0, 0, width, height)

      val fm = g.getFontMetrics()
      val textWidth = fm.stringWidth(text)
      g.setFont(new Font("Serif", Font.BOLD, 20))

      val transform = g.getTransform
      g.translate(width / 2, height / 2)
      g.rotate(-angleRad)

      val textX = -Math.cos(angleRad) * textWidth
      g.drawString(text, textX.intValue(), 0)

      g.setTransform(transform)
    }

    image
  }

}
