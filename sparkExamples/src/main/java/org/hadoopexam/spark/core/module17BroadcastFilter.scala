package org.hadoopexam.spark.core

import org.apache.spark.SparkContext
import org.apache.log4j.Logger
import org.apache.log4j.Level

object module17BroadcastFilter {

  /*
   * Usage Explanation
   * args(0) - local[1]
   * args(1) - testdata\hadoopexam\input\module17BroadcastFIlter\content.txt
   * args(2) - testdata\hadoopexam\input\module17BroadcastFIlter\remove.txt
   * args(3) - testdata\hadoopexam\output\module17BroadcastFIlter\result.txt
   * 
   */
  
  def main(args: Array[String] ) :Unit = {
    
    if(args.length < 4) {
      println("USAGE [master] [inputFilePath] [removeMapDataFilePath] [outPutDataFilePath]")   
//      System.exit(1)
    }

    val master = args.length match {
      case x:Int if x > 0 => {
        args(0)
      }
      case _ => "local[1]"
    }
//      args(0)
    val dataFile = args.length match {
      case x:Int if x > 1 => {
         args(1)  
      }
      case _ => "testdata\\hadoopexam\\input\\module17BroadcastFIlter\\content.txt"
    } 
    
    val filteredFile = args.length match {
      case x : Int if x > 2 => {
        args(2)  
      }
      case _ => "testdata\\hadoopexam\\input\\module17BroadcastFIlter\\remove.txt"
    } 
    val outFIlePath = args.length match {
      case x: Int if x > 3 => {
        args(3)
      }
      case _ => "testdata\\hadoopexam\\output\\module17BroadcastFIlter\\result.txt"
    } 

    val sc =new SparkContext(master, "module17BroadcastFilter", System.getenv("SPARK_HOME"))
    
    Logger.getLogger("org").setLevel(Level.ERROR)
    
    //Create a dataRdd which point to inputFile
    val dataRdd = sc.textFile(dataFile)
    
    //Use flatMap to separate each word with space in the file 
    val dataMap = dataRdd.flatMap(x => x.split(" "))
    
    //create filterRdd from filteredFile input content
    val filterRdd = sc.textFile(filteredFile)
    
    //use flatMap to separate each with comma separated and then use map to trim each word
    val filterMap = filterRdd.flatMap(x => x.split(",")).map(word => word.trim())
    
    //Now browdCast filteredMap by converting into List
    val broadcastRdd = sc.broadcast(filterMap.collect().toList)
    
    //Now finally filter the dataMap to make sure no word is broadcastRdd
    val resultData = dataMap.filter{case(word) => !broadcastRdd.value.contains(word) }
    
    println("ResultData after removing filter data is")
    resultData.foreach(println)
    
    val resultPair = resultData.map(x => (x, 1))
    
    val wordCount = resultPair.reduceByKey(_+_)
    println("wordCount of filterData is")
    wordCount.foreach(println)
    
    wordCount.saveAsTextFile(outFIlePath)
  
  }
  
  
}
