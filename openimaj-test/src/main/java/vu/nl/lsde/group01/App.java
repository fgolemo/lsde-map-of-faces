package vu.nl.lsde.group01;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.poi.ss.util.ImageUtils;
import org.apache.sanselan.ImageReadException;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.CCDetectedFace;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.FaceDetectorFeatures;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector.BuiltInCascade;
import org.openimaj.image.processing.face.detection.IdentityFaceDetector;
import org.openimaj.image.processing.face.detection.SandeepFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.xml.sax.SAXException;

import cern.colt.Arrays;

import com.flickr4java.flickr.FlickrException;



/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws CompressorException, FileNotFoundException {
        readImages();
        
        //FaceDetector<DetectedFace,FImage> fd = new HaarCascadeDetector(80);
        //FaceDetector<KEDetectedFace, FImage> fdSandeep = new FKEFaceDetector();
        ResizeProcessor resize = new ResizeProcessor(100, 100);
        BuiltInCascade cascade = BuiltInCascade.frontalface_alt_tree;
        HaarCascadeDetector fd = cascade.load();
        fd.setScale(1.15f);
        
        int faceCounter = 0;
        MBFImage image = null;
        
        for(int i = 0; i< 1000; i++){
            try{
                image = ImageUtilities.readMBF(new File("resources/"+i+".jpg"));
            }catch(IOException e){
                continue;
            }
            
            
            List<DetectedFace> faces = fd.detectFaces(Transforms.calculateIntensity(image));
            //FaceDetector<KEDetectedFace, FImage> fdSandeep = new FKEFaceDetector();

            int j = 0;
            for( DetectedFace face : faces ) {
                //image.drawShape(face.getBounds(), RGBColour.RED);
                //faceCounter++;
                MBFImage faceImage = image.extractROI(face.getBounds());
                faceImage = faceImage.process(resize);
                try {
                    ImageUtilities.write(faceImage, new File("resources/new_run/face"+i+"_"+j+".jpg"));
                } catch (IOException e) {
                    continue;
                }
                j++;
                
            }
            if( faces.size() > 0 ){
                System.out.println("Detected Faces: "+(faceCounter++)+" (current url: "+i+")");
            }
        }
        
    }
    
    public static void readImages() throws FileNotFoundException, CompressorException{
        FileInputStream fin = new FileInputStream(new File("resources/yfcc100m_dataset-0.bz2"));
        BufferedInputStream bis = new BufferedInputStream(fin);
        CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
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
            String url = columns[14];

            if(!columns[22].equals("0")){
                continue;
            }
            URL link = null;
            try {
                link = new URL(url);
            } catch (MalformedURLException e1) {
                errors.add("Image at URL: "+url+" not found.");
                continue;
            } //The file that you want to download
            try {
                in = new BufferedInputStream(link.openStream());
            } catch (IOException e1) {
                errors.add("Could not load Image at URL: "+url+".");
                continue;
            }

            try {
                OutputStream out = new FileOutputStream(new File("resources/"+i+"_b.jpg"));
                byte[] buf = new byte[1024];
                int len;
                while((len=in.read(buf))>0){
                    out.write(buf,0,len);
                }
                out.close();
                in.close();
            } catch (Exception e) {
                continue;
            }
            
            i++;
            if(i>1000) end = true;
        }
    }
}
