package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Secs
import org.aiotrade.lib.securities.model.Sec

object InfoSecs extends Table[InfoSec]{
  val sec = "secs_id".BIGINT REFERENCES(Secs)
  val generalInfo = "generalInfos_id".BIGINT REFERENCES(GeneralInfos)
}


class InfoSec {
    var sec : Sec = _
    var generalInfo : GeneralInfo = _
}


