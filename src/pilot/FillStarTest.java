package pilot;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;

import util.ImageProcessing;
import util.OpenCVUtil;

public class FillStarTest {

	BufferedImage image;
	JLabel label;
	
	public void createImage(){
		try {
		    image = ImageIO.read(new File("./src/star.jpg"));
		    //clean
		    int[][] pixels = convertTo2DUsingGetRGB(image);
		    pixels = specialFlood(pixels, 100, 50);
		    image = ImageProcessing.getImage(pixels);
		    //byte[][] temp = ImageProcessing.byteArrayFromImage(image);
		    //image = ImageProcessing.getBinaryImage(temp);
		} catch (IOException e) {
			//Pass
		}
		//image = ImageProcessing.threshold(image, 100);
		//image = OpenCVUtil.addLine(image, new Point(image.getWidth()/2, 0),  new Point(image.getWidth(), image.getHeight()/2));
	}
	
	public JPanel createGUI(){
		createImage();
		JPanel panel = new JPanel();
		label = new JLabel(new ImageIcon(image));
		panel.add(label);
		return panel;
	}
	
	public void createAndShowGUI() {
        JFrame frame = new JFrame("Star fill test demo etc");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(createGUI());
        frame.pack();
        frame.setVisible(true);
    }
	
	public static void main(String[] args){
	SwingUtilities.invokeLater(new Runnable() {
        public void run() {
        	System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            FillStarTest gui = new FillStarTest();
            gui.createAndShowGUI();
        	}
    	});
	}
	
	//From StackOverflow #6524196
	private static int[][] convertTo2DUsingGetRGB(BufferedImage image) {
	      int width = image.getWidth();
	      int height = image.getHeight();
	      int[][] result = new int[height][width];

	      for (int row = 0; row < height; row++) {
	         for (int col = 0; col < width; col++) {
	            result[row][col] = colorIntToValues(image.getRGB(col, row))[1];
	         }
	      }

	      return result;
	}
	
	private static int[] convertTo2Din1DArray(BufferedImage image) {
	      int width = image.getWidth();
	      int height = image.getHeight();
	      int[] result = new int[height * width];

	      for (int row = 0; row < height; row++) {
	         for (int col = 0; col < width; col++) {
	            result[( row * width ) + col] = colorIntToValues(image.getRGB(col, row))[1];
	         }
	      }

	      return result;
	}
	
	
	
	//From StackOverflow #2534116
	public static int[] colorIntToValues(int color){
		int r = (color)&0xFF;
		int g = (color>>8)&0xFF;
		int b = (color>>16)&0xFF;
		int a = (color>>24)&0xFF;
		int[] rgba = {r, g, b, a};
		return rgba;
	}
	
	public static int[][] specialFlood(int[][] pixels, int Xh, int Yv){
		/*for(int i = 0 ; i < pixels.length && i < pixels[0].length; i++){
			pixels[i][i] = -2000;	
		}*/
		//org.opencv.core.MatOfInt mat = new MatOfInt(pixels);
		
		int t = pixels.length - Yv;
		int u = pixels[0].length - Xh;
		double m = ((pixels.length - Yv) + 1 ) / (pixels[0].length - Xh - 0.0);
		
		int start = Xh;
		int Cx = Xh;
		double Cy = 0.0;
		while(start < pixels[0].length){
			while(Cy < pixels.length && Cx < pixels[0].length){
				pixels[(int) Math.round(Cy)][Cx] = -2000;
				//System.out.print("(" + Cx + "," + Cy + ")");
				Cx ++;
				Cy += m;
			}

			start++;
			Cx = start;
			Cy = 0.0;
		}
		
		return pixels;
	}
}
