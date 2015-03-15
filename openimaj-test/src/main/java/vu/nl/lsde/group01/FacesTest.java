package vu.nl.lsde.group01;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.openimaj.feature.FeatureVector;
import org.openimaj.hadoop.mapreduce.TextBytesJobUtil;
import org.openimaj.hadoop.sequencefile.MetadataConfiguration;
import org.openimaj.hadoop.sequencefile.SequenceFileUtility;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.FaceDetectorFeatures;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector.BuiltInCascade;
import org.openimaj.io.IOUtils;

public class FacesTest extends Configured implements Tool {
    private static Logger logger = Logger.getLogger(FacesTest.class);
    

    @Option(name = "-input", aliases="-i", required=true, usage="Set the input path(s) or uri(s)")
    protected static Collection<String> input;
    
    @Option(name = "-output", aliases="-o", required=true, usage="Set the output location")
    protected static String output;
    
    @Option(name = "--binary", aliases="-b", required=false, usage="Set output mode to binary")
    protected static boolean binary = false;
    
    public void prepare(String[] args){
        final CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (final CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: hadoop jar test.jar [options...] [files...]");
            parser.printUsage(System.err);

            System.exit(1);
        }
    }
    
    static class FacesMapper extends Mapper<Text, BytesWritable, Text, BytesWritable> {
        public FacesMapper() {}

        @Override
        protected void setup(Mapper<Text, BytesWritable, Text, BytesWritable>.Context context) {
        }

        @Override
        protected void map(Text key, BytesWritable value, Mapper<Text, BytesWritable, Text, BytesWritable>.Context context) throws InterruptedException {
            BufferedInputStream bis = null;
            
            bis = new BufferedInputStream(new ByteArrayInputStream(value.getBytes()));
            
            CompressorInputStream input = null;
            try {
                input = new CompressorStreamFactory().createCompressorInputStream(bis);
            } catch (CompressorException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
            BufferedReader br2 = new BufferedReader(new InputStreamReader(input));

            boolean end = false;
            InputStream in = null;
            String line = null;
            String[] columns = null;
            ArrayList<String> errors = new ArrayList<String>();
            int i = 0;
            while(!end){
                try {
                    line = br2.readLine();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                columns = line.split("\t");
                
                //get url
                String url = columns[14];
                
                //if video or not geotagged
                if(!columns[22].equals("0") || columns[10].isEmpty()){
                    continue;
                }
                
                URL link = null;
                try {
                    link = new URL(url);
                } catch (MalformedURLException e1) {
                    errors.add("Image at URL: "+url+" not found.");
                    continue;
                } 
                try {
                    in = new BufferedInputStream(link.openStream());
                } catch (IOException e1) {
                    errors.add("Could not load Image at URL: "+url+".");
                    continue;
                }

                try {
                    MBFImage img = ImageUtilities.readMBF(in);
                    FaceDetectorFeatures mode = FaceDetectorFeatures.BLOBS;
                    BuiltInCascade cascade = BuiltInCascade.frontalface_default;

                    HaarCascadeDetector fd = cascade.load();
                    FeatureVector fv = mode.getFeatureVector(fd.detectFaces(Transforms.calculateIntensityNTSC(img)), img);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if (FacesTest.binary)
                        IOUtils.writeBinary(baos, fv);
                    else
                        IOUtils.writeASCII(baos, fv);

                    context.write(key, new BytesWritable(baos.toByteArray()));
                    in.close();
                } catch (Exception e) {
                    continue;
                }
                
                i++;
                if(i>1000) end = true;
            } 
        }
    }
    
    static class FacesReducer extends Reducer<Text, BytesWritable, Text, BytesWritable> {
        public FacesReducer() {}

        @Override
        protected void setup(Reducer<Text, BytesWritable, Text, BytesWritable>.Context context) {
        }

        protected void reduce(Text key, Iterable<BytesWritable> values, Context context) 
                throws IOException, InterruptedException {
            for(BytesWritable value: values) {
                context.write((Text) key, (BytesWritable) value);
            }
        }
       
    }

    @Override
    public int run(String[] args) throws Exception {
        Map<String,String> metadata = new HashMap<String,String>();
        metadata.put(MetadataConfiguration.CONTENT_TYPE_KEY, "application/globalfeature-HAAR_FACES-" + (binary? "bin" : "ascii" ));
        metadata.put("clusterquantiser.filetype", (binary ? "bin" : "ascii" ));
        
        List<Path> allPaths = new ArrayList<Path>();
        for (String p : input) {
            allPaths.addAll(Arrays.asList(SequenceFileUtility.getFilePaths(p, "part")));
        }
        
        Job job = TextBytesJobUtil.createJob(allPaths, new Path(output), metadata, this.getConf());
        job.setJarByClass(this.getClass());
        job.setMapperClass(FacesMapper.class);
        job.setReducerClass(FacesReducer.class);
        job.setNumReduceTasks(1);

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
