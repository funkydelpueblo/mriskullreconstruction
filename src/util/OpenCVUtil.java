package util;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class OpenCVUtil {
	
	// From http://enfanote.blogspot.com/2013/06/converting-java-bufferedimage-to-opencv.html
	public static Mat bufferedImageToMat(BufferedImage myBufferedImage){
		BufferedImage image = myBufferedImage;
		byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);
		mat.put(0, 0, data);
		return mat;
	}

	// From http://enfanote.blogspot.com/2013/06/converting-java-bufferedimage-to-opencv.html
	public static BufferedImage matToBufferedImage(Mat myMat){
		Mat mat = myMat;
		byte[] data = new byte[mat.rows()*mat.cols()*(int)(mat.elemSize())];
		mat.get(0, 0, data);
		if (mat.channels() == 3) {
		 for (int i = 0; i < data.length; i += 3) {
		  byte temp = data[i];
		  data[i] = data[i + 2];
		  data[i + 2] = temp;
		 }
		}
		BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_BYTE_GRAY);
		image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
		return image;
	}
}
