package pilot;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.Image3DUniverse;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class ImageJConstruction implements PlugIn{

	//private BufferedImage[] images; 
	
	public ImageJConstruction(){
		
	}
	
	public void openAndConstruct(){
		new ij.ImageJ();
		IJ.runPlugIn("pilot.ImageJConstruction", "");
	}

	private final double RESIZE = 0.1;
	
	@Override
	public void run(String arg0) {
		System.out.println("Run ImageJ constructor as plugin");
		BufferedImage[] images = FoolishSingleton.getImages();
		
		ImageStack stack = new ImageStack((int)(images[0].getWidth() * RESIZE), (int)(images[0].getHeight() * RESIZE));
		
		for(int i = 0; i < images.length; i++){
			stack.addSlice(new ColorProcessor(resize(images[i], RESIZE)));
		}
		
	    // Open an image
	    ImagePlus imp = new ImagePlus("stack", stack);
	    //new StackConverter(imp).convertToGray8();
	    System.out.println("Resized.");
	    new StackConverter(imp).convertToGray8();
	    imp.setDimensions(1, stack.getSize(), 1);
	    
	    // Create a universe and show it
	    Image3DUniverse univ = new Image3DUniverse();
	    univ.show();
	    //imp.show();
	    //System.out.println(imp.getBufferedImage());

	    // Add the image as a volume rendering
	    univ.addVoltex(imp);
	}
	
	public static BufferedImage resize(BufferedImage img, double pct) { 
	    Image tmp = img.getScaledInstance((int)(img.getWidth() * pct), (int)(img.getHeight() * pct), Image.SCALE_SMOOTH);
	    BufferedImage dimg = new BufferedImage((int)(img.getWidth() * pct), (int)(img.getHeight() * pct), BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2d = dimg.createGraphics();
	    g2d.drawImage(tmp, 0, 0, null);
	    g2d.dispose();

	    return dimg;
	}  
}
