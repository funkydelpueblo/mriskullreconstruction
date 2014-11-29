package pilot;

import java.awt.image.BufferedImage;

public class FoolishSingleton {
	private static BufferedImage[] images;
	
	public static void setImages(BufferedImage[] im){
		images = im;
	}
	
	public static BufferedImage[] getImages(){
		return images;
	}
}
