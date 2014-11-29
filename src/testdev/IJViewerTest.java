package testdev;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.Image3DUniverse;

public class IJViewerTest implements PlugIn{
	public static void main(String[] args){
		new ij.ImageJ();
		IJ.runPlugIn("testdev.IJViewerTest", "");
	}

	@Override
	public void run(String arg) {
		System.out.println("RUN...");
	    // Open an image
	    String path = "/Users/aaron/Downloads/t1-head.tif";
	    ImagePlus imp = IJ.openImage(path);
	    new StackConverter(imp).convertToGray8();

	    // Create a universe and show it
	    Image3DUniverse univ = new Image3DUniverse();
	    univ.show();

	    // Add the image as a volume rendering
	    Content c = univ.addMesh(imp);

	    // Display the image as orthslices
	    //c.displayAs(Content.ORTHO);

	    // Remove the content
	    //univ.removeContent(c.getName());

	    // Add an isosurface
	    //Content c = univ.addMesh(imp);

	    // display slice 10 as a surface plot
	    /*univ.removeContent(c.getName());
	    imp.setSlice(10);
	    c = univ.addSurfacePlot(imp);

	    // remove all contents
	    univ.removeAllContents();

	    // close
	    univ.close();*/
  }
}
