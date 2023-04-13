package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._

object Contents extends Table[Content]
{
   val generalInfo =  "generalInfos_id".BIGINT REFERENCES(GeneralInfos)

   val content = "content" VARCHAR(3000)
}

class Content {
  var generalInfo : GeneralInfo = _

  var content : String = _
}
