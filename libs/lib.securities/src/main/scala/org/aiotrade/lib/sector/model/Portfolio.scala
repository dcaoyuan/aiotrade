package org.aiotrade.lib.sector.model

import ru.circumflex.orm.Table

object  Portfolios extends Table[Portfolio]{
  val sector = "sectors_id".BIGINT REFERENCES(Sectors)

  val validFrom = "validFrom" BIGINT
  val validTo = "validTo" BIGINT
  val name = "name" VARCHAR(30)
  def breakouts = inverse(PortfolioBreakouts.portfolio)
}

class Portfolio {
  var sector : Sector = _

  var name : String = ""
  var validFrom: Long = _
  var validTo: Long = _

  def breakouts :Seq[PortfolioBreakout] = Portfolios.breakouts(this)
}
