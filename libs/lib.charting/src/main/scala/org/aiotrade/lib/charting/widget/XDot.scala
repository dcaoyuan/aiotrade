package org.aiotrade.lib.charting.widget

class XDot extends PathWidget {
  final class Model extends WidgetModel {
    var x: Double = _
    var y: Double = _
    var width: Double = _

    def set(x: Double, y: Double, width: Double) {
      this.x = x
      this.y = y
      this.width = width
    }
  }

  type M = Model

  override protected def createModel = new Model

  override protected def plotWidget {
    val m = model
    val path = getPath
    path.reset

    val radius = if (m.width > 2) 2 else 1
    if (m.width <= 2) {
      path.moveTo(m.x, m.y)
      path.lineTo(m.x, m.y)
    } else {
      path.moveTo(m.x - radius, m.y - radius)
      path.lineTo(m.x + radius, m.y + radius)
      path.moveTo(m.x + radius, m.y - radius)
      path.lineTo(m.x - radius, m.y + radius)
    }
  }

}
