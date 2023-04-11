import scala.Tuple2;

import org.apache.spark.api.java.function.*;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.api.java.JavaSparkContext;

import com.google.common.collect.Lists;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

		// PairRDD 1: Take the cooccurrences, map the cooccurrence s.t. it is the key, and has a value of 1
		// Thus we are able to derive the number of occurrences of the cooccurrence in the reduce step
		JavaPairRDD<Tuple2<String, String>, Integer> mapCooccurrenceCount = 
			cooccurrences.mapToPair(s -> new Tuple2<>(s, 1));

		JavaPairRDD<Tuple2<String, String>, Integer> reduceCooccurrenceCount =
			mapCooccurrenceCount.reduceByKey(new Function2<Integer, Integer, Integer>() {
				public Integer call(Integer i1, Integer i2) {
					return i1+i2;
				}
			});

		// PairRDD 2: Take the cooccurrences, map the cooccurrence s.t. element 1 of the tuple is the key
		// and 1 is the value. This allows reduction s.t. we obtain the general # of outgoing edges corresponding to
		// the predecessor
		JavaPairRDD<String, Integer> mapOutgoingEdges = 
			cooccurrences.mapToPair(s -> new Tuple2<>(s._1(), 1));

		JavaPairRDD<String, Integer> reduceOutgoingEdges =
			mapOutgoingEdges.reduceByKey(new Function2<Integer, Integer, Integer>() {
				public Integer call(Integer i1, Integer i2) {
					return i1+i2;
				}
			});

		// Collect as Map for reduceOutgoingEdges
		// Index hashmap for the p-word
		// Map to pair with weight for reduceCooccurrenceCount

		HashMap<String, Integer> totalCountPredecessor = new HashMap<String, Integer>(reduceOutgoingEdges.collectAsMap());

		JavaPairRDD<Tuple2<String, String>, Double> edgeWeights = reduceCooccurrenceCount.mapToPair(
			pair -> new Tuple2<>(pair._1(), (Double) (Double.valueOf(pair._2()) / totalCountPredecessor.get(pair._1()._1())))
		);
		
		//Print out a PairRDD
		Map<Tuple2<String, String>, Double> counts = edgeWeights.collectAsMap();

		for (Map.Entry entry:counts.entrySet()) {
			System.out.println(String.format("(%s, %f)", entry.getKey(), entry.getValue()));
        }
		

		HashMap<String, HashMap<String, Double>> edges = new HashMap<>();
		for(Tuple2<String, String> tuple: counts.keySet()) {
			Map<String, Double> myMap = edges.get(tuple._1());
			if(myMap == null) {
				edges.put(tuple._1(),new HashMap<>());
				myMap=edges.get(tuple._1());
			}
			myMap.put(tuple._2(), counts.get(tuple));
		}

		File file = new File(args[1]);
		PrintWriter writer = new PrintWriter(file);

		for (String entry:edges.keySet()) {
			HashMap<String, Double> currentEdgeMap = edges.get(entry);
			String output = String.format("(%s, %d)", entry, currentEdgeMap.size());
			writer.println(output);
			System.out.println(output);

			for(String currentEdge: currentEdgeMap.keySet()) {
				String output2 = String.format("<%s, %f.2>", currentEdge, currentEdgeMap.get(currentEdge));
				writer.println(output2);
				System.out.println(output2);
			}
		}

		writer.flush();
		writer.close();

		spark.close();

    }
}
