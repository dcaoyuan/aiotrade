/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of AIOTrade Computing Co. nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.view.securities

import java.text.DecimalFormat
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.JPanel
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.securities.api
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.util.awt.StatusLineElementProvider
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.util.actors.Reactor

class MarketStatus extends StatusLineElementProvider {
  private val status = new JPanel
  status.setOpaque(true)
  //status.setBackground(LookFeel().backgroundColor)
  val box = Box.createHorizontalBox
  status.add(box)

  private val SSName = new JLabel("上证")
  SSName.setOpaque(true)
  private val SZName = new JLabel("深证")
  SZName.setOpaque(true)
  private val SSValue = new JLabel("-")
  SSValue.setOpaque(true)
  private val SZValue = new JLabel("-")
  SZValue.setOpaque(true)
  private val SSChange = new JLabel("-")
  SSChange.setOpaque(true)
  private val SZChange = new JLabel("-")
  SZChange.setOpaque(true)
  private val SSPercent = new JLabel("-")
  SSPercent.setOpaque(true)
  private val SZPercent = new JLabel("-")
  SZPercent.setOpaque(true)

  private val priceDf = new DecimalFormat("0.000")
  private val neuColor = LookFeel().getNeutralColor
  private val posColor = LookFeel().getPositiveColor
  private val negColor = LookFeel().getNegativeColor

  private val SSReactor = new Reactor {
    reactions += {
      case api.TickerEvt(ticker) => setSSValue(ticker)
    }
  }
  private val SZReactor = new Reactor {
    reactions += {
      case api.TickerEvt(ticker) => setSZValue(ticker)
    }
  }

  for (indexSS <- Exchange.secOf("000001.SS");
       indexSZ <- Exchange.secOf("399001.SZ")
  ) {
    Exchange.SS.uniSymbolToLastTradingDayTicker.get(indexSS.uniSymbol) foreach {lastTicker =>
      setSSValue(lastTicker)
    }
    Exchange.SZ.uniSymbolToLastTradingDayTicker.get(indexSZ.uniSymbol) foreach {lastTicker =>
      setSZValue(lastTicker)
    }

    for (label <- List(SSName, SSValue, SSChange, SSPercent, SZName, SZValue, SZChange, SZPercent)) {
      box.add(label)
      box.add(Box.createHorizontalStrut(5))
    }

    SSReactor.listenTo(indexSS)
    SZReactor.listenTo(indexSZ)
  }

  private def setSSValue(ticker: Ticker) {
    if (ticker.changeInPercent > 0) {
      //SSName.setForeground(posColor)
      SSValue.setBackground(posColor)
      SSChange.setBackground(posColor)
      SSPercent.setBackground(posColor)
    } else if (ticker.changeInPercent == 0) {
      //SSName.setForeground(neuColor)
      SSValue.setBackground(neuColor)
      SSChange.setBackground(neuColor)
      SSPercent.setBackground(neuColor)
    } else {
      //SSName.setForeground(negColor)
      SSValue.setBackground(negColor)
      SSChange.setBackground(negColor)
      SSPercent.setBackground(negColor)
    }

    SSValue.setText(priceDf.format(ticker.lastPrice))
    SSChange.setText(priceDf.format(ticker.dayChange))
    SSPercent.setText("%+3.2f%%" format ticker.changeInPercent)
  }

  private def setSZValue(ticker: Ticker) {
    if (ticker.changeInPercent > 0) {
      //SZName.setForeground(posColor)
      SZValue.setBackground(posColor)
      SZChange.setBackground(posColor)
      SZPercent.setBackground(posColor)
    } else if (ticker.changeInPercent == 0) {
      //SZName.setForeground(neuColor)
      SZValue.setBackground(neuColor)
      SZChange.setBackground(neuColor)
      SZPercent.setBackground(neuColor)
    } else {
      //SZName.setForeground(negColor)
      SZValue.setBackground(negColor)
      SZChange.setBackground(negColor)
      SZPercent.setBackground(negColor)
    }

    SZValue.setText(priceDf.format(ticker.lastPrice))
    SZChange.setText(priceDf.format(ticker.dayChange))
    SZPercent.setText("%+3.2f%%" format ticker.changeInPercent)
  }

  def getStatusLineElement = status
}
