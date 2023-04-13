package org.aiotrade.lib.charting.chart

import org.aiotrade.lib.charting.widget.XDot
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import java.awt.Color
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.widget.PathsWidget

class InfoPointChart extends AbstractChart {
  final class Model extends WidgetModel {
    var v: TVar[_] = _
    var infos : TVar[_] = _
    def set(v: TVar[_], infos : TVar[_]) {
      this.v = v
      this.infos = infos
    }
  }

  type M = Model

  protected def createModel = new Model

  protected def plotChart {
    val m = model
    val color = Color.YELLOW
    setForeground(color)

    val pathsWidget = addChild(new PathsWidget)
    val tp = new XDot

    val y = datumPlane.yChartUpper + 2

    var bar = 1
    while (bar <= nBars) {

      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
        if (ser.exists(time)) {
          val value = model.v.double(time)

          if (Null.not(value)) {
            tp.model.set(xb(bar), y, wBar)
            tp.setForeground(color)
            tp.plot
            pathsWidget.appendFrom(tp)
          }
        }

        i += 1
      }

      bar += nBarsCompressed
    }

  }


}
