package org.aiotrade.lib.json

import scala.collection.mutable.HashMap

object MainApp {

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]) {
    testDecode
    testVmap
  }
  
  def testDecode {
    var text = """{"L":{"s":"600001.SS","t":1265737049852,"v":[202.04144,61.16,51.15,31.13,41.14,740.1218,660.10156,21.12]}}"""
    var json = JsonBuilder.readJson(text)
    println(json)

    json = JsonBuilder.readJson("""{"key1": "val1"}""")
    println(json)

    json = JsonBuilder.readJson("""{"key\"1" : "val1"}""")
    println(json)

    json = JsonBuilder.readJson("""{"key\"1" : ["val1", "val2"]}""")
    println(json)

    json = JsonBuilder.readJson("""["val1", "val2"]""")
    println(json)
        
    json = JsonBuilder.readJson("""[{"key1" : "val1"}, 123, -123.1, "ab\"c"]""")
    println(json)

    text = """
  {"businesses": [{"address1": "650 Mission Street",
                   "address2": "",
                   "avg_rating": 4.5,
                   "categories": [{"category_filter": "localflavor",
                                   "name": "Local Flavor",
                                   "search_url": "http://lightpole.net/search"}],
                   "city": "San Francisco, 旧金山",
                   "distance": 0.085253790020942688,
                   "id": "4kMBvIEWPxWkWKFN__8SxQ",
                   "latitude": 37.787185668945298,
                   "longitude": -122.40093994140599},
                  {"address1": "25 Maiden Lane",
                   "address2": "",
                   "avg_rating": 5.0,
                   "categories": [{"category_filter": "localflavor",
                                   "name": "Local Flavor",
                                   "search_url": "http://lightpole.net/search"}],
                   "city": "San Francisco",
                   "distance": 0.23186808824539185,
                   "id": "O1zPF_b7RyEY_NNsizX7Yw",
                   "latitude": 37.788387,
                   "longitude": -122.40401}]} \n\"""
    json = JsonBuilder.readJson(text)
    println(json)
  }
  
  
  def testVmap {
    val vmap = new HashMap[String, Array[_]]
    vmap += ("." -> Array(1, 2, 3, 4))
    vmap += ("a" -> Array(1.1, 2.1, 3.1, 4.1))
    vmap += ("b" -> Array(Array("up", 10.0, "White"), Array("dn", 11.0, "White"), Array("up", 12.0, "White"), Array("up", 13.0, "White")))
    
    val bytes = Json.encode(vmap)
    val json = new String(bytes)
    println("\n========= encode ==============")
    println(json)

    println("\n========= decode ==============")
    val jsonObj = Json.decode(json)
    println(jsonObj)

  }
}
