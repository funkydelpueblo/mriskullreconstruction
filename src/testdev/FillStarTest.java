package testdev;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberInputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;

import util.ImageProcessing;
import util.OpenCVUtil;

public class FillStarTest {

	BufferedImage image;
	JLabel label;
	
	public void createImage(){
		try {
		    image = ImageIO.read(new File("./src/head2.jpg"));
		    //clean
		    int[][] pixels = convertTo2DUsingGetRGB(image);
		    image = floodToImage(image, 50, 100, 0);
		    //image = ImageProcessing.getImage(pixels);
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
	            result[( row * width ) + col] = image.getRGB(col, row);//colorIntToValues(image.getRGB(col, row))[1];
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
	
	public static BufferedImage specialFlood(BufferedImage bi, int Xh, int Yv){
		int[][] pixels = convertTo2DUsingGetRGB(bi);
		
		
		double m = ((pixels.length - Yv) + 1 ) / (pixels[0].length - Xh - 0.0);
		
		int state = 0; 	//0: black (background)
						//1: white (skin)
						//2*: black (bone)
						//3: white (brain)
						//4: black (bone)
						//5: white (skin)
						//6: black (background)
		
		int start = Xh;
		int Cx = Xh;
		double Cy = 0.0;
		while(start < pixels[0].length){
			java.util.ArrayList<Point> fillTwo = new ArrayList<Point>();
			java.util.ArrayList<Point> fillFour = new ArrayList<Point>();
			
			while(Cy < pixels.length && Cx < pixels[0].length){
				int px = pixels[(int) Math.round(Cy)][Cx];
				
				//UPDATE STATE
				if(state == 0 && px > 200){			//On BG, see skin
					state = 1;
				}else if(state == 1 && px < 200){	//On skin, see bone
					state = 2;
				}else if(state == 2 && px > 200){	//On bone, see brain
					state = 3;
				}else if(state == 3 && px < 200){	//On brain, see bone
					state = 4;
				}else if(state == 4 && px > 200){	//On bone, see skin
					state = 5;
				}else if(state == 5 && px < 200){	//On skin, see BG
					state = 6;
				}
				
				if(state == 2){
					fillTwo.add(new Point(Cx,(int) Math.round(Cy)));
				}else if(state == 4){
					fillFour.add(new Point(Cx,(int) Math.round(Cy)));
				}
				Cx ++;
				Cy += m;
			}
			if(state == 6 || state == 4){
				if(state == 6){
					fillTwo.addAll(fillFour);
				}
				for(Point p : fillTwo){
					pixels[p.y][p.x] = -20000;
				}
			}
			//

			start++;
			Cx = start;
			state = 0;
			Cy = 0.0;
		}
		
		return ImageProcessing.getImage(fixBadRotation(pixels));
	}
	
	public static BufferedImage specialFloodB(BufferedImage bi, int YL, int YR, int noiseEnd){
		int[][] pixels = convertTo2DUsingGetRGB(bi);
		
		
		double m = (YR - YL) / (pixels[0].length - 0.0);
		double b = YL;
		int imaginaryStart = (int) ((b * -1) / m);	// y = mx + b for y = 0
		int realStart = (int)Math.round(m * (pixels[0].length) + b); //y = mx + b for x = width
		
		int state = 0; 	//0: black (background)
						//1: white before black (skin)
						//2*: black after white (bone)
						//3: white after black(brain)
						//X4: black (bone)
						//X5: white (skin)
						//X6: black (background)
		
		int Cx = pixels[0].length;
		double Cy = 0.0;
		int K = 0;
		
		while(realStart > 0){
			Cy = realStart;
			Cx = pixels[0].length - 1;
			
			java.util.ArrayList<Point> fillAll = new ArrayList<Point>();
			java.util.ArrayList<Point> fillTemp = new ArrayList<Point>();
			
			state = 0;
			while(Cy >= noiseEnd && Cx >= 0){
				int px = pixels[(int) Math.round(Cy)][Cx];
				
				//UPDATE STATE
				if(state == 0 && px > 200){			//Black, haven't seen any white, now we see white
					state = 1;
				}else if(state == 1 && px < 200){	//On white, after first black, now we see more black
					state = 2;
				}else if(state == 2 && px > 200){	//On black, after white, now we see next white
					state = 3;
					fillAll.addAll(fillTemp);
					fillTemp.clear();
				}else if(state == 3 && px < 200){	//On white, after non-first black, we see another black
					state = 2;
				}
				
				if(state == 2){
					fillTemp.add(new Point(Cx, (int) Math.round(Cy)));
				}
				
				Cx --;
				Cy -= m;
			}
			
			/*if(state == 6 || state == 4){
				if(state == 6){
					fillTwo.addAll(fillFour);
				}
				for(Point p : fillTwo){
					pixels[p.y][p.x] = -20000;
				}
			}*/
			for(Point p : fillAll){
				pixels[p.y][p.x] = -20000;
			}
			//

			K++;
			realStart = (int)Math.round(m * (pixels[0].length) + b) - K; //y = mx + b for x = width
			System.out.print(realStart);
		}
		
		return ImageProcessing.getImage(fixBadRotation(pixels));
	}
	
	public static java.util.ArrayList<Point> specialFloodC(BufferedImage bi, int YL, int YR, int noiseEnd){
		int[][] pixels = convertTo2DUsingGetRGB(bi);
		java.util.ArrayList<Point> result = new ArrayList<>();
		
		double m = (YR - YL) / (pixels[0].length - 0.0);
		double b = YL;
		
		int yStart = (int)Math.ceil(m * (pixels[0].length) + b);
		
		int state = 0; 	//0: black (background)
						//1: white before black (skin)
						//2*: black after white (bone)
						//3: white after black(brain)
						//X4: black (bone)
						//X5: white (skin)
						//X6: black (background)
		
		int Cx = pixels[0].length - 1;
		double Cy = yStart;
		
		while(yStart > 0){
			Cy = yStart;
			Cx = pixels[0].length - 1;
			
			java.util.ArrayList<Point> fillAll = new ArrayList<Point>();
			java.util.ArrayList<Point> fillTemp = new ArrayList<Point>();
			
			//y = mx + b --> y - b = mx --> (y - b) / m = x
			
			state = 0;
			while(Cy >= noiseEnd && Cx >= 0 && Cx > ((yStart - b) / m)){
				int px = pixels[(int) Math.round(Cy)][Cx];
				
				//UPDATE STATE
				if(state == 0 && px > 200){			//Black, haven't seen any white, now we see white
					state = 1;
				}else if(state == 1 && px < 200){	//On white, after first black, now we see more black
					state = 2;
				}else if(state == 2 && px > 200){	//On black, after white, now we see next white
					state = 3;
					fillAll.addAll(fillTemp);
					fillTemp.clear();
				}else if(state == 3 && px < 200){	//On white, after non-first black, we see another black
					state = 2;
				}
				
				if(state == 2){
					fillTemp.add(new Point(Cx, (int) Math.round(Cy)));
				}
				
				Cx --;
			}
			
			for(Point p : fillAll){
				//pixels[p.y][p.x] = -20000;
				result.add(p);
			}
			yStart--;
			System.out.print(yStart);
		}
		
		return result;
	}
	
	public static BufferedImage floodToImage(BufferedImage bi, int YL, int YR, int noiseEnd){
		java.util.ArrayList<Point> flood = specialFloodC(bi, YL, YR, noiseEnd);
		int[][] pixels = convertTo2DUsingGetRGB(bi);
		for(Point p : flood){
			pixels[p.y][p.x] = -20000;
		}
		return ImageProcessing.getImage(fixBadRotation(pixels));
	}
	
	public static int[][] fixBadRotation(int[][] pixels){
		pixels = ImageProcessing.transposeOutOfPlace(pixels);
		int[][] result = new int[pixels.length][pixels[0].length];
		for(int i = 0; i < pixels.length; i++){
			for(int j = 0; j < pixels[0].length; j++){
				result[i][pixels[i].length - 1 - j]  = pixels[i][j];
			}
		}
		
		return result;
	}
}