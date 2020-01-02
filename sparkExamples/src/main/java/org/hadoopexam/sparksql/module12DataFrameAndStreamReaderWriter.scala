package org.hadoopexam.sparksql

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Encoder
import org.apache.spark.sql.Encoders
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.Column
import org.apache.spark.sql.Column
import org.apache.spark.sql.types.DoubleType
import org.apache.log4j.Level
import org.apache.log4j.Logger

/*
 * Usage - We are going indepth example of Spark SQL schema 
 * 
 * args(0) - local[1]
 * args(1) - testdata\hadoopexam\sparkSql\input\module12DataFrameAndStreamReaderWriter\
 * args(2) - testdata\hadoopexam\sparkSql\output\module12DataFrameAndStreamReaderWriter\
 *
 */


object module12DataFrameAndStreamReaderWriter {
 Logger.getLogger("org").setLevel(Level.ERROR)  
  case class course(id: Int, name:String, fee: Int ,venue:String , duration:Int )
  
  def main(args : Array[String]) : Unit = {
    
    if(args.length < 3) {
      
      println("USAGE MASTER INPUTLOC OUTPUTLOC")
      System.exit(1)
    }
    
    
    val master = args(0)
    val inputLoc = args(1)
    val outputLoc = args(2)
    
    val sparkConf = new SparkConf().setMaster(master).setAppName("module12DataFrameAndStreamReaderWriter")
    
    
    val sparkSession = SparkSession.builder().config(sparkConf).getOrCreate()
    
    
    /*
     * Format agnostic load method 
     * Loading csv file 
     *
     * inferschema  it can derive the schema 
     */
    println("Reading data from csv file by inferSchema ")
    sparkSession.read.format("csv").option("header", true).option("inferSchema", true).load(inputLoc+"Training.csv").show()
    
    
      println("Reading data from csv file by  not inderSchema ")
    sparkSession.read.format("csv").option("header", true).load(inputLoc+"Training.csv").show()
    /*
     * Loading Json file  also we can load from csv, rdbms, orc, parquet, jdbc, 
     * 
     * inferschema  it can derive the schema 
     */    
    
    println("Reading data from JSON file by inderSchema ")
    sparkSession.read.format("json").option("header", true).option("inferSchema", true).load(inputLoc+"data_1.json").show()
    
    println("Reading data from JSON file by  not inderSchema ")
    sparkSession.read.format("json").option("header", true).load(inputLoc+"data_1.json").show()
    
    /*
     * Default format is parquet format    
     */
     println("Defualt format of data is parqut")
//    sparkSession.read.load(inputLoc+"data_1.json").show()
    
    /*
     * Using json, csv, txt method from DataFrameReader
     */
     println("Reading data using json method using DataFrame reader directly")
     sparkSession.read.option("inferSchema", true).json(inputLoc+"data_1.json").show()
     sparkSession.read.json(inputLoc+"data_1.json").printSchema()
     
     println("Reading data using csv method using DataFrame reader directly")
     sparkSession.read.option("inferSchema", true).csv(inputLoc+"Training.csv").show()
     sparkSession.read.csv(inputLoc+"Training.csv").printSchema()
     
     
     println("Reading data of csv file using text method of DataFrame reader directly")
     import sparkSession.sqlContext.implicits._ 
    /*
     *  With out above statement you will get error of
     *  could not find implicit value for evidence parameter of type org.apache.spark.sql.Encoder[Array[String]] 
     */
     sparkSession.read.textFile(inputLoc+"Training.csv").map(x => x.split(",")).show()
     
    /*
     * Reading data using schema 
     */
     
     val schame = new StructType().add($"id".int).add($"name".string).add($"fee".long.copy(nullable = false)).add($"venue".string).add($"date".string)
     
     sparkSession.read.schema(schame).format("csv").load(inputLoc+"Training.csv").show()
     
     
     /*
      * Example of reading with schema and writing data with compression 
      * 
      */
     sparkSession.read.schema(schame).format("csv").load(inputLoc+"Training.csv").write.option("compression", "gzip").save(outputLoc+"Training")
   
     println("Reading data from parquet file right away writing it ")
     sparkSession.read.schema(schame).load(outputLoc+"Training").show()
      
     
     /*
      * Reading data from Table using DataFrame reader 
      */

     val jsonData = sparkSession.read.format("json").load(inputLoc+"data_1.json")
     
     jsonData.createOrReplaceTempView("COURSE_TP_DATA")
     
     //Checking to see if catalog exist 
     println("COURSE_TP_DATA catalog exist "+sparkSession.catalog.tableExists("COURSE_TP_DATA"))
    
     println("Reading data from Temporary view")
     sparkSession.read.table("COURSE_TP_DATA").show()
     
     /*
      * Example of saving DataFrame/DataSets using DataFrame writer 
      */
     //reading data from csv file and writing data to parquet file 
     println("Reading data as CSV file and writing data as parquet format with default compression snappy ")
     sparkSession.read.schema(schame).format("csv").load(inputLoc+"Training.csv").write.save(outputLoc+"Training1")
     
     //Reading data from csv file and writing data to json format 
     println("Reading data as CSV file and writing data as parquet format with no compression as we are providing format ")     
     sparkSession.read.format("csv").load(inputLoc+"Training.csv").write.format("json").save(outputLoc+"Trianing_JSON")
     
     sparkSession.read.textFile(inputLoc+"Training.csv").write.json(outputLoc+"Trianing_JSON1")
     
     //Writing data as a table 
     sparkSession.read.textFile(inputLoc+"Training.csv").write.saveAsTable("TRAINING_TABLE")
     print("After writing the table we check from sql statment ")
     sparkSession.sql("select * from TRAINING_TABLE").show()

     println("List out all table in catalog "+sparkSession.catalog.listTables())

     //Saving data by partitioning by type

     println("Reading csv file and saving the data with fee partition ")
     sparkSession.read.format("csv").option("header", true).load(inputLoc+"Training.csv").write.partitionBy("fee").save(outputLoc+"Part_Training")

     //Inserting data into existing table 
     println("Number of records in TRAINING_TABLE "+sparkSession.sql("select * from TRAINING_TABLE").count())
     sparkSession.read.format("csv").option("header", true).load(inputLoc+"Training.csv").write.insertInto("TRAINING_TABLE")
    println("Number of records in TRAINING_TABLE AFTER ADDING DATA "+sparkSession.sql("select * from TRAINING_TABLE").count())     
  
    
    /*
     * Bound and unbound columns 
     */
    //A way of creating unbounded column using '
    val nameCol : Column = 'name
    
    //Another way of creating unbounded column with $ symbol
    val feeCol : Column = $"fee"
    
    //Another way of creating unbounded column using Column functions 
    
    import org.apache.spark.sql.functions._
    val durationCol = col("duration")
    val venueCol = new Column("venue")
    
    val heDS = sparkSession.sparkContext.parallelize(Seq(course(1, "Hadoop", 6000, "Mumbai", 5),course(2, "Spark", 5000, "Pune", 4),course(3, "Python", 4000, "Hyderabad", 3))).toDS() 
    //Created bounded column 
    
    val boundedNameCol = heDS.col("name")
    val boundedFeeCol = heDS.col("fee")
    val boundedVenueCol = heDS.col("venue")
    
    //Now select the data using unbounded and bounded columns 
    println("Selecting data from unbounded and bounded columns")
    heDS.select(nameCol, feeCol, venueCol, boundedNameCol, boundedFeeCol, boundedVenueCol).show()
    
    //Creating typed column 
    
    val feeColumType = $"fee".as[String]
    println("Changing the unbounded feeColumn type to String type")
    heDS.select(nameCol, feeColumType, venueCol, boundedNameCol, boundedFeeCol, boundedVenueCol).show()
    
    //Casting column type 
    println("Casting the feeColumn type so it can be casted to Double type")
    heDS.select(nameCol, feeColumType.cast(DoubleType), venueCol, boundedNameCol, boundedFeeCol, boundedVenueCol).show()
    
    //Using column isin operator to check whether expected data is there or not in dataset //filter all the records which has 5000 and 6000 as fee in the given course, it helps to check more than one values in one shot
    heDS.filter(heDS("fee").isin(5000,6000)).show() 
 
  //Select all the records which does not have fee as 5000 and 6000 
    heDS.filter(!heDS("fee").isin(5000,6000)).show() 
 
  //Select record which has fee as 6000 
    heDS.filter(heDS("fee")===6000).show()
    
    
  
  }
  
  
}