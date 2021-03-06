package vu.nl.lsde.group01;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector.BuiltInCascade;
import org.openimaj.image.processing.resize.ResizeProcessor;

public class FacesTest{
    private static Logger logger = Logger.getLogger(FacesTest.class);
    
    protected static String input;
    
    protected static String output;
    
    public static void prepare(String[] args){
        if(args.length!=2){
            System.out.println("input and output parameter are missing. call \"java -jar .jar 'input' 'ouput'\" to run it correctly.");
        }else{
            input = args[0];
            output = args[1];
        }
    }
    
    static class FacesMapper extends Mapper<Object, Text, Text, BytesWritable> {
    	protected HaarCascadeDetector fd = null;
    	protected SortedMap<String, ResizeProcessor> resizers = null;
    	protected static Float[] RESIZE_LEVEL = new Float[]{50f,100f};
    	
        public FacesMapper() {
        	BuiltInCascade cascade = BuiltInCascade.frontalface_alt_tree;
            fd = cascade.load();
            fd.setScale(1.2f);
            
            resizers = new TreeMap<String, ResizeProcessor>();
            for(Float size : RESIZE_LEVEL){
            	resizers.put(size.toString(), new ResizeProcessor(size, size));
            }
        }

        @Override
        protected void setup(Mapper<Object, Text, Text, BytesWritable>.Context context) {
        }

        @Override
        protected void map(Object key, Text value, Mapper<Object, Text, Text, BytesWritable>.Context context) throws InterruptedException {
            InputStream in = null;
            String line = value.toString();
            String[] columns = null;
            ArrayList<String> errors = new ArrayList<String>();
            
            columns = line.split("\t");
            
            //if video or not geotagged
            if(!columns[22].equals("0") || columns[10].isEmpty()){
                return;
            }
            
            //get url
            String url = columns[14];
            URL link = null;
            try {
                link = new URL(url);
            } catch (MalformedURLException e1) {
                errors.add("Image at URL: "+url+" not found.");
                return;
            } 
            try {
                in = new BufferedInputStream(link.openStream());
            } catch (IOException e1) {
                errors.add("Could not load Image at URL: "+url+".");
                return;
            }

            try {
                MBFImage img = ImageUtilities.readMBF(in);
                List<DetectedFace> faces = fd.detectFaces(Transforms.calculateIntensity(img));
                
                for(DetectedFace face : faces){
                    System.out.println("Face detected");

                	MBFImage faceImage = img.extractROI(face.getBounds());
                    String lon = columns[10].trim();
                    String lat = columns[11].trim();
                	for(String size : resizers.keySet()){
                		MBFImage faceImageResized = faceImage.process(resizers.get(size));
                		ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageUtilities.write(faceImageResized, "jpg", baos);
                        
                        //uncomment to get image in Base64 encoding
                        context.write(new Text(lon+";"+lat+";"+size), new BytesWritable(Base64.encodeBase64(baos.toByteArray())));                	
                        //context.write(new Text(lon+";"+lat+";"+size), new BytesWritable(baos.toByteArray()));                	
                	}
                	
                }
                in.close();
            } catch (Exception e) {
            	return;
            }

        }

    }
    
    
    static class FacesReducer extends Reducer<Text, BytesWritable, Text, BytesWritable> {
        public FacesReducer() {}

        @Override
        protected void setup(Reducer<Text, BytesWritable, Text, BytesWritable>.Context context) {
        }
        
        @Override
        protected void reduce(Text key, Iterable<BytesWritable> values, Context context) 
                throws IOException, InterruptedException {
            for(BytesWritable value: values) {
                context.write(key, value);
            }
        }
       
    }

    /**
     * The main entry point
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
    	prepare(args);
    	Configuration conf = new Configuration();
        conf.setLong("mapred.max.split.size", 700000);

        Job job = new Job(conf, "world map of faces test");
        job.setJarByClass(FacesTest.class);
        job.setMapperClass(FacesMapper.class);
        job.setReducerClass(FacesReducer.class);
        job.setNumReduceTasks(1);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(BytesWritable.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(input));
        FileOutputFormat.setOutputPath(job, new Path(output));
        
        job.waitForCompletion(true);
    }
}
