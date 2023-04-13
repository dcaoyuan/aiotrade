package org.aiotrade.lib.sector.model

import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Secs
import ru.circumflex.orm.Table

object PortfolioBreakouts extends Table[PortfolioBreakout] {
  val portfolio = "portfolios_id".BIGINT REFERENCES(Portfolios)
  val serialNo = "serialNo" INTEGER
  val rank = "rank"  INTEGER
  val sec = "secs_id".BIGINT REFERENCES(Secs)

}

class PortfolioBreakout {
  var portfolio : Portfolio = _
  var serialNo : Int = _
  var rank : Int = _
  var sec : Sec = _
  
}
