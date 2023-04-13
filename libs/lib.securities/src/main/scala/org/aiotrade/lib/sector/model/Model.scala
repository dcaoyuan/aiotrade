package org.aiotrade.lib.sector.model

import org.aiotrade.lib.securities.model.Exchange
import ru.circumflex.orm.DDLUnit

object Model {
  def schema {
    val tables = List(BullVSBears,Sectors, Portfolios, PortfolioBreakouts)

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => println(msg.body))
    
  }
  
  def testSelectSector {
    Sectors.sectorOf("LONG_TERM") match {
      case Some(sector) =>
        /* val breakouts = (SELECT (PortfolioBreakouts.*) FROM PortfolioBreakouts WHERE (PortfolioBreakouts.portfolio.field EQ Portfolios.idOf(sector.portfolio)) list) */
        for(breakout <- sector.portfolio.breakouts){
          println("long-term:" + breakout.sec.secInfo.uniSymbol)
        }
      case None => Unit
    }
  }
  
  def testSaveSector {
    val sectorS = new Sector()
    sectorS.code = "SHORT_TERM"
    sectorS.name = "short term portfolio"

    Sectors.save(sectorS)

    val porfolioS = new Portfolio()
    porfolioS.name = "technical porfolio"
    porfolioS.sector = sectorS
    Portfolios.save(porfolioS)

    sectorS.portfolio = porfolioS
    Sectors.update(sectorS)

    Exchange.secOf("600000.SS") match {
      case Some(x) =>
        val breakoutS1 = new PortfolioBreakout()
        breakoutS1.portfolio = porfolioS
        breakoutS1.serialNo = 1
        breakoutS1.rank = 1
        breakoutS1.sec = x
        PortfolioBreakouts.save(breakoutS1)
      case None => Unit

    }

    Exchange.secOf("600001.SS") match {
      case Some(x) =>
        val breakoutS2 = new PortfolioBreakout()
        breakoutS2.portfolio = porfolioS
        breakoutS2.serialNo = 2
        breakoutS2.rank = 1
        breakoutS2.sec = x
        PortfolioBreakouts.save(breakoutS2)
      case None => Unit
    }

    Exchange.secOf("600004.SS") match {
      case Some(x) =>
        val breakoutS3 = new PortfolioBreakout()
        breakoutS3.portfolio = porfolioS
        breakoutS3.serialNo = 3
        breakoutS3.rank = 2
        breakoutS3.sec = x
        PortfolioBreakouts.save(breakoutS3)
      case None => Unit
    }

    val sectorL = new Sector()
    sectorL.code = "LONG_TERM"
    sectorL.name = "long term portfolio"

    Sectors.save(sectorL)

    val porfolioL = new Portfolio()
    porfolioL.name = "fundmental porfolio"
    porfolioL.sector = sectorL
    Portfolios.save(porfolioL)

    sectorL.portfolio = porfolioL
    Sectors.update(sectorL)

    Exchange.secOf("600000.SS") match {
      case  Some(x) =>
        val breakoutL1 = new PortfolioBreakout()
        breakoutL1.portfolio = porfolioL
        breakoutL1.serialNo = 1
        breakoutL1.rank = 1
        breakoutL1.sec =x
        PortfolioBreakouts.save(breakoutL1)
      case None => Unit
    }

    Exchange.secOf("600001.SS") match {
      case Some(x) =>
        val breakoutL2 = new PortfolioBreakout()
        breakoutL2.portfolio = porfolioL
        breakoutL2.serialNo = 2
        breakoutL2.rank = 2
        breakoutL2.sec = x
        PortfolioBreakouts.save(breakoutL2)
      case None => Unit
    }

    Exchange.secOf("000001.SZ") match {
      case Some(x) =>    val breakoutL3 = new PortfolioBreakout()
        breakoutL3.portfolio = porfolioL
        breakoutL3.serialNo = 3
        breakoutL3.rank = 3
        breakoutL3.sec = x
        PortfolioBreakouts.save(breakoutL3)
      case None => Unit
      
    }

    val bullbear = new BullVSBear()
    bullbear.ratio = 0.8f
    bullbear.summary = "good macro economic enviroment"
    bullbear.time = 0L
    BullVSBears.save(bullbear)

  }


}
