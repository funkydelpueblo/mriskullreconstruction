package pilot;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.vecmath.Point3d;

import org.opencv.core.Point3;

import util.ImageProcessing;

public class Flooding {
	
	private java.util.ArrayList<Point3d> points;
	
	public Flooding(){
		points = new java.util.ArrayList<Point3d>();
	}
	
	public java.util.ArrayList<Point3d> getPoints(){
		return this.points;
	}
	
	/**
	 * Given a slice image, floods it, and stores the point locations of the flood points for reconstruction later.
	 * @param bi
	 * @param index
	 * @param YL
	 * @param YR
	 * @param noiseEnd
	 * @return
	 */
	public BufferedImage floodToImage(BufferedImage bi, int index, int YL, int YR, int noiseEnd){
		java.util.ArrayList<Point> flood = flood(bi, YL, YR, noiseEnd);
		int[][] pixels = convertTo2DUsingGetRGB(bi);
		
		//a) Prep image to return
		for(Point p : flood){
			pixels[p.y][p.x] = -20000;
		}
		
		// b) Take out pixels on inside of skull, so only shell (removing need for drawing unseen pixels)
		// 	1. Create map of flooded parts
		byte[][] floodLocs = new byte[pixels.length][pixels[0].length];
		for(Point p : flood){
			floodLocs[p.y][p.x] = 1;
		}
		//	2. Only save pixels that are not surrounded on all sides by other pixels
		for(Point p : flood){
			int x = p.x; int y = p.y;
			if((x - 1 >= 0 && floodLocs[y][x - 1] == 0) || (x + 1 < floodLocs[y].length && floodLocs[y][x+1] == 0)
					|| (y - 1 >= 0 && floodLocs[y -1][x] == 0) || (y + 1 < floodLocs.length && floodLocs[y+1][x] == 0)){
				points.add(new Point3d(p.getX(), p.getY(), (double)index)); //start building index of points
			}
		}
		
		return ImageProcessing.getImage(fixBadRotation(pixels));
	}
	
	/**
	 * Actual custom flooding algorithm.
	 * @param bi
	 * @param YL
	 * @param YR
	 * @param noiseEnd
	 * @return
	 */
	public java.util.ArrayList<Point> flood(BufferedImage bi, int YL, int YR, int noiseEnd){
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
	
	//From StackOverflow #2534116
	public static int[] colorIntToValues(int color){
		int r = (color)&0xFF;
		int g = (color>>8)&0xFF;
		int b = (color>>16)&0xFF;
		int a = (color>>24)&0xFF;
		int[] rgba = {r, g, b, a};
		return rgba;
	}
}
