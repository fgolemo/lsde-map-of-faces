package vu.nl.lsde.group01;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.Size;



/**
 * OpenIMAJ Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws IOException, FlickrException, SAXException, ParserConfigurationException {
        String apiKey = "19e4b91e258067acc16b6d4425edc417";
        String sharedSecret = "14fa85fc31f3907a";
        Flickr f = new Flickr(apiKey, sharedSecret, new REST());
    	//Create an image
        Photo photo = f.getPhotosInterface().getPhoto("4571314348");
        
        MBFImage image = ImageUtilities.readMBF(f.getPhotosInterface().getImageAsStream(photo, Size.ORIGINAL));


        FaceDetector<DetectedFace,FImage> fd = new HaarCascadeDetector(40);
        List<DetectedFace> faces = fd.detectFaces(Transforms.calculateIntensity(image));
        
        for( DetectedFace face : faces ) {
            image.drawShape(face.getBounds(), RGBColour.RED);
        }
        
        DisplayUtilities.display(image);
    }
}
