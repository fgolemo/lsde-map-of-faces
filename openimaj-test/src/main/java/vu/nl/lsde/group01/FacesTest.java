package vu.nl.lsde.group01;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configured;
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

/**
 * Hadoop version of the LocalFeaturesTool. Capable of extracting features from
 * images in sequencefiles.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 */
public class FacesTest extends Configured implements Tool {
    private static final String ARGS_KEY = "facestest.args";
    private static Logger logger = Logger.getLogger(FacesTest.class);
    

    @Option(name = "-input", aliases="-i", required=true, usage="Set the input path(s) or uri(s)")
    protected static List<String> input;
    
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
            try {
                MBFImage img = ImageUtilities.readMBF(new ByteArrayInputStream(value.getBytes()));
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
            } catch (Exception e) {
                logger.warn("Problem processing image " + key + " (" + e + ")");
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
        job.getConfiguration().setStrings(ARGS_KEY, args);
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
