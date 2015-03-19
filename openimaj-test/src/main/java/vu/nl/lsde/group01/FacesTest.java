package vu.nl.lsde.group01;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetectorFeatures;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector.BuiltInCascade;

public class FacesTest extends Configured implements Tool {
    private static Logger logger = Logger.getLogger(FacesTest.class);
    
    protected static String input;
    
    protected static String output;
    
    public void prepare(String[] args){
        if(args.length!=2){
            System.out.println("input and output parameter are missing. call \"java -jar .jar 'input' 'ouput'\" to run it correctly.");
        }else{
            input = args[0];
            output = args[1];
        }
    }
    
    static class FacesMapper extends Mapper<Object, Text, DoubleWritable, BytesWritable> {
        public FacesMapper() {}

        @Override
        protected void setup(Mapper<Object, Text, DoubleWritable, BytesWritable>.Context context) {
        }

        @Override
        protected void map(Object key, Text value, Mapper<Object, Text, DoubleWritable, BytesWritable>.Context context) throws InterruptedException {
            InputStream in = null;
            String line = value.toString();
            String[] columns = null;
            ArrayList<String> errors = new ArrayList<String>();
            
            columns = line.split("\t");
            
            //get url
            String url = columns[14];
            
            //if video or not geotagged
            if(!columns[22].equals("0") || columns[10].isEmpty()){
                return;
            }
            
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
                FaceDetectorFeatures mode = FaceDetectorFeatures.BLOBS;
                BuiltInCascade cascade = BuiltInCascade.frontalface_default;

                HaarCascadeDetector fd = cascade.load();
                List<DetectedFace> faces = fd.detectFaces(Transforms.calculateIntensity(img));
                
                if (faces.isEmpty()){
                	return;
                }
                	
                MBFImage faceImage = img.extractROI(faces.get(0).getBounds());
                System.out.println("Face detected");
                //FeatureVector fv = mode.getFeatureVector(fd.detectFaces(Transforms.calculateIntensityNTSC(img)), img);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                
                Double lon = Double.parseDouble(columns[10].trim());
                byte[] lonBytes = new byte[8];
                ByteBuffer.wrap(lonBytes).putDouble(lon);
                baos.write(lonBytes);
                
                Double lat = Double.parseDouble(columns[11].trim());
                byte[] latBytes = new byte[8];
                ByteBuffer.wrap(latBytes).putDouble(lat);
                baos.write(latBytes);
                
                ImageUtilities.write(faceImage,"jpg",baos);

                context.write(new DoubleWritable(lon), new BytesWritable(baos.toByteArray()));
                in.close();
                
            } catch (Exception e) {
            	return;
            }

        }
    }
    
    static class FacesReducer extends Reducer<Double, BytesWritable, Text, BytesWritable> {
        public FacesReducer() {}

        @Override
        protected void setup(Reducer<Double, BytesWritable, Text, BytesWritable>.Context context) {
        }
        
        @Override
        protected void reduce(Double key, Iterable<BytesWritable> values, Context context) 
                throws IOException, InterruptedException {
            for(BytesWritable value: values) {
                context.write(new Text(key.toString()), (BytesWritable) value);
            }
        }
       
    }

    @Override
    public int run(String[] args) throws Exception {
        prepare(args);
        
        Job job = Job.getInstance(getConf(), "world map of faces");
        job.setJarByClass(this.getClass());
        job.setMapperClass(FacesMapper.class);
        job.setReducerClass(FacesReducer.class);
        job.setNumReduceTasks(1);

        FileInputFormat.addInputPath(job, new Path(input));
        FileOutputFormat.setOutputPath(job, new Path(output));
        
        job.waitForCompletion(true);
        
        return 0;
    }

    /**
     * The main entry point
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(new FacesTest(), args);
    }
}
