package edu.purdue.cs.HSPGiST;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;


/**
 * This MapReduce Job will construct all local indexes for the given file 
 * with the specified HSPIndex type and parses based on the user supplied parser
 * @author Stefan Brinton
 *
 */
public class LocalIndexConstructor<MKIn, MVIn, MKOut, MVOut, Pred> extends Configured implements Tool {
	@SuppressWarnings("rawtypes")
	static Parser parser = null;
	@SuppressWarnings("rawtypes")
	static HSPIndex index;
	@SuppressWarnings("rawtypes")
	public LocalIndexConstructor(Parser parser, HSPIndex index){
		super();
		LocalIndexConstructor.parser = parser;
		LocalIndexConstructor.index = index;
	}
	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "Local-Construction");
		job.setJarByClass(LocalIndexConstructor.class);
		job.setMapOutputKeyClass(parser.keyout);
		job.setMapOutputValueClass(parser.valout);
		job.setOutputKeyClass(HSPIndexNode.class);
		job.setOutputValueClass(HSPLeafNode.class);
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		FileInputFormat.addInputPath(job, new Path(args[3]));
		FileOutputFormat.setOutputPath(job, new Path("FirstOutput"));
		//Need to pass parameterization at runtime
		job.setMapperClass(LocalMapper.class);
		job.setReducerClass(LocalReducer.class);
		//TODO:Change this after demo
		job.setNumReduceTasks(4);
		boolean succ = job.waitForCompletion(true);
		return succ ? 0: 1;
	}
	private static class LocalMapper<MKIn,MVIn,MKOut,MVOut> extends Mapper<MKIn,MVIn,MKOut,MVOut>{
		@SuppressWarnings("rawtypes")
		Parser local;
		public void setup(Context context){
			local = parser.clone();
		}
		@SuppressWarnings("unchecked")
		public void map(MKIn key, MVIn value, Context context) throws IOException, InterruptedException{
			if(local.isArrayParse){
				ArrayList<Pair<MKOut, MVOut>> list = local.arrayParse(key, value);
				for(Pair<MKOut, MVOut> pair : list){
					context.write(pair.getFirst(), pair.getSecond());
				}
			}
			else{
				Pair<MKOut, MVOut> pair = local.parse(key, value);
				
				context.write(pair.getFirst(), pair.getSecond());
			}
		}
	}
	private static class LocalReducer<MKOut, MVOut, RKOut, RVOut, Pred> extends Reducer<MKOut, MVOut, RKOut, RVOut>{
		HSPNode<Pred,MKOut,MVOut> root = null;
		HSPIndex<Pred,MKOut,MVOut> local;
		@SuppressWarnings("unchecked")
		public void setup(Context context){
			local = index;
		}
		@SuppressWarnings("unchecked")
		public void reduce(MKOut key, Iterable<MVOut> values, Context context) throws IOException, InterruptedException {
			//Unsure if keys can have more than one value, if they do this becomes more complicated
			
			MVOut val = null;
			for(MVOut value : values){
				val = value;
			}
			root = local.insert(root, ((Copyable<MKOut>)key).copy(), ((Copyable<MVOut>)val).copy(),1);
		}
		@SuppressWarnings("unchecked")
		public void cleanup(Context context) throws IOException, InterruptedException{
			ArrayList<HSPNode<Pred,MKOut,MVOut>> stack = new ArrayList<HSPNode<Pred,MKOut,MVOut>>();
			HSPNode<Pred,MKOut,MVOut> node = root;
			while(!(stack.size() == 0 && node == null)){
				if(node != null){
					if(node instanceof HSPIndexNode<?,?,?>){
						HSPIndexNode<Pred,MKOut,MVOut> temp = (HSPIndexNode<Pred,MKOut,MVOut>)node;
						context.write((RKOut)node,  (RVOut) new HSPLeafNode<Pred,MKOut,MVOut>(null));
						for(int i = 1;i < temp.children.size();i++){
							stack.add(temp.children.get(i));
						}
						node = temp.children.get(0);
					}
					else{
						context.write((RKOut)new HSPIndexNode<Pred,MKOut,MVOut>((ArrayList<HSPNode<Pred,MKOut,MVOut>>)null, null),  (RVOut) node);
						node = null;
					}
				}
				else
					node = stack.remove(stack.size()-1);
			}
		}
	}
}
