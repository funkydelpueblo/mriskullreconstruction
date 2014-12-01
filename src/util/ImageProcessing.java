package util;

import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ImageProcessing {

	final static int SAMPLE_FACTOR = 4;
	final static int STRETCH_FACTOR = 2;
	
	/**
	 * Flip direction of slices from top to bottom to left to right
	 * @param slices
	 * @param progress 
	 * @return
	 */
	public static Image[] flipAxes(BufferedImage[] slices, JProgressBar progress){
		// 1. Turn images into pixel array.
		int[][][] pixels = new int[slices.length][][];
		
		for(int i = 0; i < slices.length; i++){
			pixels[i] = intArrayFromImage(slices[i]);
		}
		
		//Flip image 90 degrees
		//TODO: Account for this underneath
		for(int k = 0; k < pixels.length; k++){
			assert pixels[k].length == pixels[k][0].length;
			int m = pixels[k].length;
			for(int i = 0; i < m; i++) {
				for(int j = i+1; j < m; j++) {
					int temp = pixels[k][i][j];
					pixels[k][i][j] = pixels[k][j][i];
					pixels[k][j][i] = temp;
				}
			}
		}
		
		final int sampleLength = pixels[0][0].length / SAMPLE_FACTOR;
		Image[] flipped = new Image[sampleLength];
		
		SwingUtilities.invokeLater(() -> progress.setMinimum(0));
		SwingUtilities.invokeLater(() -> progress.setMaximum(sampleLength)); //feedback
		
		//2. Flip image pixels
		//int[][][] result = new int[pixels[0][0].length][][];
		System.out.println("Need to process " + sampleLength + " images");
		for(int x = 0; x < pixels[0][0].length; x+=SAMPLE_FACTOR){
			int[][] newslice = new int[pixels.length * STRETCH_FACTOR][pixels[0].length];
			for(int z = 0; z < pixels.length; z++){
				int[] row = new int[pixels[0].length];
				
				for(int y = 0; y < pixels[0].length; y++){
					row[y] = pixels[z][y][x];
				}
				newslice[(z * STRETCH_FACTOR)] = row;
				newslice[(z * STRETCH_FACTOR) + 1] = row;
			}
			//result[x] = newslice;
			
			//3. Turn back into iamge
			flipped[x/4] = getImage(transposeOutOfPlace(newslice));
			
			final int prog = x/4;
			SwingUtilities.invokeLater(() -> progress.setValue(prog));
			if(x % 10 == 0){ System.out.println("Processed " + x + " images"); }
		}
		
		return flipped;
	}
	
	//From StackOverflow: #8422374
	public static int[][] transposeOutOfPlace(int[][] array){
		  int width = array.length;
		  int height = array[0].length;

		  int[][] array_new = new int[height][width];

		  for (int x = 0; x < width; x++) {
		    for (int y = 0; y < height; y++) {
		      array_new[y][width - x - 1] = array[x][y];
		    }
		  }
		  return array_new;
	}
	
	// From StackOverflow #15002706
	public static BufferedImage getImage(int pixels[][]){
	     int w=pixels.length;
	     int h=pixels[0].length;
	     
	    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
	    WritableRaster raster = bi.getRaster();
		for(int i=0;i<w;i++)
	    {
	         for(int j=0;j<h;j++)
	         {
	             raster.setSample(i,j,0,(int)pixels[i][j]);
	         }
	    }

		return bi;
	}
	
	public static BufferedImage getBinaryImage(byte pixels[][]){
	     int w=pixels.length;
	     int h=pixels[0].length;
	     
	    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
	    WritableRaster raster = bi.getRaster();
		for(int i=0;i<w;i++)
	    {
	         for(int j=0;j<h;j++)
	         {
	             raster.setSample(i,j,0,(int)pixels[i][j]);
	         }
	    }

		return bi;
	}
	
	//From StackOverflow: #6524196
	public static int[][] intArrayFromImage(BufferedImage image) {
	      final int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
	      final int width = image.getWidth();
	      final int height = image.getHeight();

	      int[][] result = new int[height][width];
	      for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += 1) {
	            result[row][col] = pixels[pixel];
	            row++;
	            if (row == height) {
	               row = 0;
	               col++;
	            }
	         }
	      return result;
	   }
	
	public static byte[][] byteArrayFromImage(BufferedImage image) {
	      final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	      final int width = image.getWidth();
	      final int height = image.getHeight();

	      byte[][] result = new byte[height][width];
	      for (int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += 1) {
	            result[row][col] = pixels[pixel];
	            row++;
	            if (row == height) {
	               row = 0;
	               col++;
	            }
	        }
	      return result;
	   }
	
	public static BufferedImage threshold(BufferedImage input, int th){
		Mat imageMat = OpenCVUtil.bufferedImageToMat(input);
		Mat result = new Mat(imageMat.rows(), imageMat.cols(), imageMat.type());
		org.opencv.imgproc.Imgproc.threshold(imageMat, result, th, 255, Imgproc.THRESH_BINARY);
		return OpenCVUtil.matToBufferedImage(result, BufferedImage.TYPE_BYTE_GRAY);
	}
	
	final static int elem_size = 1;
	
	public static BufferedImage openThenClose(BufferedImage input, int amt){
		Mat A = OpenCVUtil.bufferedImageToMat(input);
		Mat B = new Mat(A.rows(), A.cols(), A.type());
		
        //Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2*elem_size + 1, 2*elem_size+1));
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(amt, amt));
		//Open
        Imgproc.erode(A, B, element);
        Imgproc.dilate(B, A, element);
        //Close
        Imgproc.dilate(A, B, element);
        Imgproc.erode(B, A, element);
        
        return OpenCVUtil.matToBufferedImage(A, BufferedImage.TYPE_BYTE_GRAY);
	}
}
