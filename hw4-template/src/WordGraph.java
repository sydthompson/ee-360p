import scala.Tuple2;

import org.apache.spark.api.java.function.*;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.api.java.JavaSparkContext;

import com.google.common.collect.Lists;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class WordGraph {

	/*
	* The main function needs to create a word graph of the text files provided in arg[0]
	* The output of the word graph should be written to arg[1]
	*/
    public static void main(String[] args) throws Exception {
		// Regex patterns for pre-processing
		final Pattern NEWLINE = Pattern.compile("\n");
		final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");
		final Pattern WHITESPACE = Pattern.compile("\\s+");

		if (args.length < 1) {
			System.err.println("Usage: WordGraph <file>");
			System.exit(1);
		}

		SparkSession spark = SparkSession
			.builder()
			.appName("WordGraph")
			.getOrCreate();

		// Reads file(s) within pointed directory from args[0]
		JavaRDD<String> dataset = spark.read().textFile(args[0]).javaRDD();
	
		// Pre-processing the data	
		JavaRDD<ArrayList<String>> words = dataset.map(s -> NOT_ALPHANUMERIC.matcher(s.toLowerCase())							// Convert to lowercase and change non-alphanumeric characters to spaces
												  							.replaceAll(" ")
												  							.trim())															
												  .map(s -> new ArrayList<String>(Arrays.asList(WHITESPACE.split(s))))			// Split on whitespace to obtain lists of words per row in the RDD
												  .filter(s -> !(s.get(0).equals("")));											// Remove whitespace lines (only element empty string)

		// Use custom in-line flatmapfunction to iterate the list and create Tuple2 objects
		// Once the tuples for a line are created we don't care about keeping sentences together
		JavaRDD<Tuple2<String, String>> cooccurrences = words.flatMap(new FlatMapFunction<ArrayList<String>, Tuple2<String, String>>() {
			public Iterator<Tuple2<String, String>> call(ArrayList<String> s) {
				// List to populate with co-occurrences
				ArrayList<Tuple2<String,String>> pairs = new ArrayList<Tuple2<String, String>>();

				Iterator<String> iter = s.iterator();
				String s1 = iter.next();
				// Tuple is composed of current word and next word to create a co-occurrence
				while (iter.hasNext()) {
					String s2 = iter.next();
					pairs.add(new Tuple2<String, String>(s1, s2));
					s1 = s2;
				}

				// Return the iterator since we're using a flatmap transformation
				return pairs.iterator();
			}
		});

		// We now have a flatmapped RDD containing Tuple2, we should now remap the data FOR the actual MapReduce step(s), aka translate to PairRDDs

		// PairRDD 1: Take the cooccurrences, map the cooccurence s.t. it is the key, and has a value of 1
		// Thus we are able to derive the number of occurrences of the cooccurrence in the reduce step
		
		JavaPairRDD<Tuple2<String, String>, Integer> mapCooccurenceCount = 
			cooccurrences.mapToPair(s -> new Tuple2<>(s, 1));
		

		// counting number of times predecessor p-word appears in map
		JavaPairRDD<String, Integer> numFirst = 
			cooccurrences.mapToPair(s -> new Tuple2<>(s._1(), 1));

		JavaPairRDD<String, Integer> reduceNumFirst =
			numFirst.reduceByKey(new Function2<Integer, Integer, Integer>() {
				public Integer call(Integer i1, Integer i2) {
					return i1+i2;
				}
			});

		Map<String, Integer> firstCount = reduceNumFirst.collectAsMap();
		// each pair maps to number of occurences of word1->word2
		JavaPairRDD<Tuple2<String, String>, Integer> reduceCoccurrenceCount =
			mapCooccurenceCount.reduceByKey(new Function2<Integer, Integer, Integer>() {
				public Integer call(Integer i1, Integer i2) {
					return i1+i2;
				}
			});

		JavaPairRDD<Tuple2<String, String>, Double> mapEdges = reduceCoccurrenceCount.map(
			s -> new Tuple2<>(s._1(), (s._2()* 1.0 / firstCount.get(s._1()._1()))));
			
		// PairRDD 2: Take the cooccurrences, map the cooccurrence s.t. element 1 of the tuple is the key
		// and element 2 is the value. This allows reduction s.t. we obtain the general # of outgoing edges

		JavaPairRDD<String, String> mapOutgoingEdges = 
			cooccurrences.mapToPair(s -> new Tuple2<>(s._1(), s._2()));

		// When reducing, we might need an accumulator to track the number of occurrences of a given co-occurence

		for (Tuple2<String, String> pair:cooccurrences.collect()) {
            System.out.println(String.format("(%s, %s)", pair._1(), pair._2()));
        }

		spark.close();

    }
}
