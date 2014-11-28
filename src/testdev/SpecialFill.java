package testdev;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import util.ImageProcessing;

public class SpecialFill {
	public static BufferedImage noteBoundaries(BufferedImage bi, int x0, int y0, int x1, int y1){
		int[][] pixels = convertTo2DUsingGetRGB(bi);
		for(int c = 0; c < 10; c++){
			pixels[y0][x0+c] = -20000; 
			pixels[y1][x1+c] = -20000;
		}

		return ImageProcessing.getImage(pixels);
	}
	
	public static BufferedImage specialFlood(BufferedImage bi, int colwidth, int Xh, int Yv){
		/*for(int i = 0 ; i < pixels.length && i < pixels[0].length; i++){
			pixels[i][i] = -2000;	
		}*/
		/*byte[] pixels = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, pixels);
		
		return OpenCVUtil.matToBufferedImage(mat, BufferedImage.TYPE_3BYTE_BGR);*/
		int[][] pixels = convertTo2DUsingGetRGB(bi);
		
		int t = pixels.length - Yv;
		int u = pixels[0].length - Xh;
		double m = ((pixels.length - Yv) + 1 ) / (pixels[0].length - Xh - 0.0);
		
		int state = 0; //0: black; 1: white; 2: black; 3: white; 4: black; 5: white; 6: black
		
		int start = Xh;
		int Cx = Xh;
		double Cy = 0.0;
		while(start < pixels[0].length){
			java.util.ArrayList<Point> fill = new ArrayList<Point>();
			while(Cy < pixels.length && Cx < pixels[0].length){
				int px = pixels[(int) Math.round(Cy)][Cx];

				//Update state
				if(state == 0 && px > 200){
					state = 1;
				}else if(state == 1 && px < 200){
					state = 2;
				}else if(state == 2 && px > 200){
					state = 3;
				}else if(state == 3 && px < 200){
					state = 4;
				}else if(state == 4 && px > 200){
					state = 5;
				}else if(state == 5 && px < 200){
					state = 6;
				}
				
				if(state == 2 || state == 4){
					fill.add(new Point(Cx,(int) Math.round(Cy)));
				}
				//System.out.print("(" + Cx + "," + Cy + ")");
				Cx ++;
				Cy += m;
			}
			
			if(state == 3){
				for(Point p : fill){
					pixels[p.y][p.x] = -20000;
				}
			}
			//

			start++;
			Cx = start;
			state = 0;
			Cy = 0.0;
		}
		
		return ImageProcessing.getImage(pixels);
	}
	
	//From StackOverflow #6524196
	private static int[][] convertTo2DUsingGetRGB(BufferedImage image) {
	      int width = image.getWidth();
	      int height = image.getHeight();
	      int[][] result = new int[height][width];

	      for (int row = height -1; row >= 0; row--) {
	         for (int col = width-1; col >= width; col--) {
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
