package edu.purdue.cs.HSPGiST.UserDefinedSection;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.ToolRunner;

import edu.purdue.cs.HSPGiST.AbstractClasses.HSPIndex;
import edu.purdue.cs.HSPGiST.AbstractClasses.Parser;
import edu.purdue.cs.HSPGiST.MapReduceJobs.BinaryReaderTest;
import edu.purdue.cs.HSPGiST.MapReduceJobs.LocalIndexConstructor;
import edu.purdue.cs.HSPGiST.MapReduceJobs.RandomSample;
import edu.purdue.cs.HSPGiST.SupportClasses.CopyWritableLong;
import edu.purdue.cs.HSPGiST.SupportClasses.WritablePoint;
import edu.purdue.cs.HSPGiST.SupportClasses.WritableRectangle;

/**
 * The command interpreter is the main of HSP-GiST
 * It takes user input to run one of the Jobs/Jobsets
 * It requires updating to add user classes to allow expedient
 * command line use 
 * @author Stefan Brinton & Daniel Fortney
 *
 */
public class CommandInterpreter {
	public static final String CONSTRUCTFIRSTOUT = "FirstOutput";
	@SuppressWarnings({ "rawtypes" })
	public static void main(String args[]) throws Exception{
		//Determines the operation to run
		// DO NOT MODIFY
		switch (args[0]) {
		case "build":
			HSPIndex index;
			Parser parse;
			LocalIndexConstructor construct = null;
			RandomSample sampler = null;
			//This switch statement determines the parser
			//Add cases for your parsers to add them
			switch(args[2]){
			case "OSM":
				index = makeIndex(args[1], new LongWritable());
				parse = new OSMParser();
				sampler = new RandomSample<Object, Text, WritablePoint, Text>(parse, index);
				construct = new LocalIndexConstructor<Object,Text,WritablePoint,CopyWritableLong,WritableRectangle>(parse, index);
			}
			if(construct == null){
				System.err.println("Failed to provide a valid parser on build instruction");
				System.exit(-1);
			}
			ToolRunner.run(sampler, args);
			ToolRunner.run(construct, args);
			System.exit(ToolRunner.run(new BinaryReaderTest(), args));
		}
	}
	/**
	 * This method encapsulates index creation so to expedite user
	 * addition of parsers and index types
	 * @param arg This argument is args[1], i.e. the name of the index type
	 * @param infer This argument is used to let the method infer the type of the sp tree
	 * @return An new instance of that index type
	 */
	@SuppressWarnings("rawtypes")
	public static <R> HSPIndex makeIndex(String arg, R infer){
		//This switch statement determines the index type
		//Add cases for your parsers to add them
		switch(arg){
		case "Quad":
			return new QuadTree<R>();
		}
		System.err.println("Failed to provide a valid index tree type");
		System.exit(-1);
		return null;
	}
}