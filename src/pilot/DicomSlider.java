package pilot;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point3d;

import org.opencv.core.Core;

import testdev.IJViewerTest;
import util.ImageProcessing;
import util.OpenCVUtil;
import external.DicomHeaderReader;
import external.DicomReader;
import net.miginfocom.swing.MigLayout;
import non_com.media.jai.codec.ImageCodec;
import non_com.media.jai.codec.ImageDecoder;
import non_com.media.jai.codec.TIFFCodec;
import non_com.media.jai.codec.TIFFEncodeParam;
import non_com.media.jai.codec.TIFFImageEncoder;

public class DicomSlider {

	JFileChooser fileChooser;
	JButton openButton;
	JButton flipButton;
	JButton buildButton;
	JButton floodButton;
	JButton exportButton;
	JLabel imageLabel;
	JSlider slider;
	
	//Progress
	JProgressBar progress;
	JLabel progressLabel;
	
	public JPanel createPanel(){
		fileChooser = new JFileChooser("/Users/aaron/Documents/Dropbox/Schoolwork/BioGeo/MRIReconstructData");
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		imageLabel = new JLabel();
		openButton = new JButton("Open .dcm directory");
		flipButton = new JButton("Flip slices");
		
		slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		slider.setMajorTickSpacing(10);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setPreferredSize(new Dimension(500, 40));
		
		MigLayout layout = new MigLayout("", "20px[320px][320px]", "12px[][400px][40px]12px");
		JPanel panel = new JPanel();
		panel.setLayout(layout);
		//panel.add(openButton, "cell 0 0");
		//panel.add(flipButton, "cell 1 0");
		panel.add(imageLabel, "cell 0 1 2 1");
		panel.add(slider, "cell 0 2 2 1");
		
		//Progress
		progress = new JProgressBar();
		progressLabel = new JLabel();
		MigLayout progressLayout = new MigLayout("", "[]", "[][]");
		JPanel progressPanel = new JPanel();
		progressPanel.setLayout(progressLayout);
		progressPanel.add(progress, "cell 0 0");
		progressPanel.add(progressLabel, "cell 0 1");
		
		panel.add(progressPanel, "cell 0 0");
		
		floodButton = new JButton("Isolate skull");
		floodButton.addActionListener(new TryFlood());
		floodButton.setEnabled(false);
		
		exportButton= new JButton(".tiff save (test)");
		exportButton.addActionListener(new TryTiff());
		exportButton.setEnabled(false);
		
		// Steps Panel
		JPanel stepsPanel = new JPanel();
		MigLayout stepsLayout = new MigLayout();
		stepsPanel.setLayout(stepsLayout);
		stepsPanel.add(new JLabel("<html><big><b>Directions<b></big></html>"), "wrap");
		stepsPanel.add(new JLabel("<html>1. Open DICOM directory</html>"), "wrap");
		stepsPanel.add(openButton, "wrap");
		stepsPanel.add(new JLabel("<html>2. Threshold image (opens automatically)</html>"), "wrap");
		stepsPanel.add(new JLabel("<html>3. Adjust cutoff plane (opens automatically)</html>"), "wrap");
		stepsPanel.add(new JLabel("<html>4. Isolate skull</html>"), "wrap");
		stepsPanel.add(floodButton, "wrap");
		stepsPanel.add(new JLabel("<html>5. Export for viewing</html"), "wrap");
		stepsPanel.add(exportButton, "wrap");
		stepsPanel.add(new JLabel("<html>6. Open in ImageJ (external)</html>"), "wrap");
		
		panel.add(stepsPanel, "east");
		
		openButton.addActionListener(new OpenDirectoryListener(panel));
		//flipButton.addActionListener(new FlipSlicesListener());
		slider.addChangeListener(new ImageSlideListener());
		slider.setEnabled(false);
		
		return panel;
	}
	
	private class OpenDirectoryListener implements ActionListener{
		JPanel panel;
		
		public OpenDirectoryListener(JPanel panel){
			this.panel = panel;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			int returnVal = fileChooser.showOpenDialog(this.panel);
			 
	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	        	// 1) Load all DICOM files
	            File file = fileChooser.getSelectedFile();
	            loadAllDicom(file);
	        }
		}
	}
	
	Image[] dicomFiles;
	
	private void loadAllDicom(File folder){
		SwingWorker<Image[], Image[]> loadWorker = new SwingWorker<Image[], Image[]>(){
			
			@Override
			protected Image[] doInBackground() throws Exception {
				SwingUtilities.invokeLater(() -> openButton.setEnabled(false));
				SwingUtilities.invokeLater(() -> progressLabel.setText("Loading DICOM files..."));
				
				Image[] result;
				
				// 1. Load all the .DCM files in the directory
				File[] files = folder.listFiles(new FileFilter(){
					@Override
					public boolean accept(File pathname) {
						// TODO Auto-generated method stub
						return getFileExtension(pathname).equals(".dcm");
					}
				});
				
				SwingUtilities.invokeLater(() -> progress.setMaximum(0));
				SwingUtilities.invokeLater(() -> progress.setMaximum(files.length + 20)); //feedback
				
				// 2. Sort based on sliceLocation
				
				Arrays.sort(files, new Comparator<File>(){
					@SuppressWarnings("deprecation")
					@Override
					public int compare(File o1, File o2) {
						try {
							DicomReader readerA = new DicomReader(o1.toURL());
							DicomReader readerB = new DicomReader(o2.toURL());
							double aLoc = Double.parseDouble(readerA.getDicomHeaderReader().getaString(0x0020, 0x1041));
							double bLoc = Double.parseDouble(readerB.getDicomHeaderReader().getaString(0x0020, 0x1041));
							return (int)(aLoc - bLoc);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return 0;
					}
				});
				
				SwingUtilities.invokeLater(() -> progress.setValue(20));
				
				// 3. Load the image data into an array
				result = new Image[files.length];
				for(int i = 0; i < files.length; i++){
					try {
						DicomReader reader = new DicomReader(files[i].toURL());
						result[i] = reader.getImage();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					final int percent = i;
					SwingUtilities.invokeLater(() -> progress.setValue(percent + 20)); //feedback
				}
				
				//4. Update Slider max...
				SwingUtilities.invokeLater(() -> resetProgress());
				return result;
			}
			
			public void done(){
				Image[] results = null;
				try {
					results = this.get();
				} catch (InterruptedException | ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("DONE! " + results);
				dicomFiles = results;
				//slider.setMaximum(dicomFiles.length - 1);
				System.out.println(dicomFiles.length);
				flipAllSlices();
			}
			
		};

		loadWorker.execute();
		
	}
	
	private String getFileExtension(File file) {
	    String name = file.getName();
	    int lastIndexOf = name.lastIndexOf(".");
	    if (lastIndexOf == -1) {
	        return ""; // empty extension
	    }
	    return name.substring(lastIndexOf);
	}
	
	private class ImageSlideListener implements ChangeListener{

		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider)e.getSource();
			//if (!source.getValueIsAdjusting()) {
		        int slice = (int)source.getValue();
		        BufferedImage bi = toBufferedImage(dicomFiles[slice]);
		        imageLabel.setIcon(new ImageIcon(OpenCVUtil.addLine(bi, new java.awt.Point(0, X_LINE), new java.awt.Point(bi.getWidth(), Y_LINE))));
		    //}
		}
		
	}
	
	private void flipAllSlices(){
		SwingWorker<Image[], Image[]> loadWorker = new SwingWorker<Image[], Image[]>(){

			private Image[] results;

			@Override
			protected Image[] doInBackground() throws Exception {
				SwingUtilities.invokeLater(() -> progressLabel.setText("Aligning axes..."));
				SwingUtilities.invokeLater(() -> progress.setValue(0));
				
				BufferedImage[] temp = new BufferedImage[dicomFiles.length];
				for(int i = 0; i < dicomFiles.length; i++){
					temp[i] = toBufferedImage(dicomFiles[i]);
				}
				
				results = ImageProcessing.flipAxes(temp, progress);
				return null;
			}
			
			public void done(){
				dicomFiles = results;
				System.out.println(dicomFiles.length);
				slider.setMaximum(dicomFiles.length - 1);
				imageLabel.setIcon(new ImageIcon(dicomFiles[0]));
				resetProgress();
				
				//Enable proper components
				SwingUtilities.invokeLater(() -> slider.setEnabled(true));
				
				//Open threshold window
				ThresholdSlider thresholdSlider = new ThresholdSlider(dicomFiles, DicomSlider.this);
				thresholdSlider.createAndShowGUI();
			}
			
		};
		loadWorker.execute();
	}
	
	AdjustPlaneSlider adjustPlane;
	
	public void thresholdImages(int threshold, boolean openclose){
		for(int i = 0; i < dicomFiles.length; i++){
			dicomFiles[i] = ImageProcessing.threshold(toBufferedImage(dicomFiles[i]), threshold);
			if(openclose){
				dicomFiles[i] = ImageProcessing.openThenClose(toBufferedImage(dicomFiles[i]));
			}
			imageLabel.setIcon(new ImageIcon(dicomFiles[slider.getValue()]));
		}
		System.out.println("Done thresholding.");
		adjustPlane = new AdjustPlaneSlider(dicomFiles, this);
		adjustPlane.createAndShowGUI();
	}
	
	public void setLinePoints(int left, int right){
		System.out.println("Left: " + left + " Right: " + right);
		this.X_LINE = left;
		this.Y_LINE = right;
		SwingUtilities.invokeLater(() -> floodButton.setEnabled(true));
	}
	
	private void resetProgress(){
		this.progress.setValue(0);
		this.progressLabel.setText("");
	}
	
	//
	// ACTUAL INTERESTING PROCESSING STUFF INITIATED HERE
	//
	
	int X_LINE = 100;
	int Y_LINE = 200;
	final int NOISE_END = 10;
	
	private Flooding flooding;
	//private java.util.ArrayList<java.util.ArrayList<Point3d>> floodPoints;
	
	public class TryFlood implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			SwingUtilities.invokeLater(() -> floodButton.setEnabled(false));
			SwingUtilities.invokeLater(() -> exportButton.setEnabled(true));

			flooding = new Flooding(dicomFiles.length);
			for(int i = 0; i < dicomFiles.length; i++){
				dicomFiles[i] = flooding.floodToImage(toBufferedImage(dicomFiles[i]), i, X_LINE, Y_LINE, NOISE_END);
				imageLabel.setIcon(new ImageIcon(dicomFiles[slider.getValue()]));
			}
			System.out.println("Done flooding.");
		}
	}
	
	public class TryConstruct implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			java.util.ArrayList<Point3d> floodPoints = flooding.getPoints();
			SkullBuilder.constructSkullShowWindow(floodPoints);
		}
	}
	
	public class TryImageJ implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			/*BufferedImage[] bis = new BufferedImage[10];
			for(int i = 75; i < 85; i++){
				bis[i-75] = toBufferedImage(dicomFiles[i]);
			}
			FoolishSingleton.setImages(bis);
			ImageJConstruction ijc = new ImageJConstruction();
			ijc.openAndConstruct();*/
			IJViewerTest.main(null);
		}
	}
	
	final double RESIZE = .1;
	
	public class TryTiff implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {	
			File tiffF = new File("/Users/aaron/Desktop/test.tif");
	        BufferedOutputStream out;
	        BufferedImage[] floodSlices = flooding.getFloodSlices();
			try {
				out = new BufferedOutputStream(new FileOutputStream(tiffF));
				BufferedImage bi = ImageJConstruction.resize(toBufferedImage(floodSlices[0]), RESIZE);
				BufferedImage convertedImg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			    convertedImg.getGraphics().drawImage(bi, 0, 0, null);
				bi = convertedImg;
			    
				TIFFEncodeParam param = new TIFFEncodeParam();
		        param.setTileSize(bi.getWidth(), bi.getHeight());
		       
		        Vector<BufferedImage> extra = new Vector<BufferedImage>();
		        BufferedImage temp;
		        BufferedImage start;
		        for(int i = 2; i < floodSlices.length; i+=2){
		        	start = ImageJConstruction.resize(toBufferedImage(floodSlices[i]), RESIZE);
					temp = new BufferedImage(start.getWidth(), start.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
				    temp.getGraphics().drawImage(start, 0, 0, null);
		        	extra.add(temp);
		        }
		        param.setExtraImages(extra.iterator());
		        TIFFImageEncoder encoder = (TIFFImageEncoder) TIFFCodec.createImageEncoder("tiff", out, param); 
		        encoder.encode(bi);
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Converts a given Image into a BufferedImage
	 * From StackOverflow #13605248
	 *
	 * @param img The Image to be converted
	 * @return The converted BufferedImage
	 */
	public static BufferedImage toBufferedImage(Image img)
	{
	    if (img instanceof BufferedImage)
	    {
	        return (BufferedImage) img;
	    }

	    // Create a buffered image with transparency
	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    // Draw the image on to the buffered image
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();

	    // Return the buffered image
	    return bimage;
	}
	
	private void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Dicom Slider");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
        //Add content to the window.
        frame.add(createPanel());
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

	public static void main(String[] args){
	SwingUtilities.invokeLater(new Runnable() {
        public void run() {
        	//Make sure we access proper OpenCV libraries before anything else!
        	System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        	
            //Turn off metal's use of bold fonts
            UIManager.put("swing.boldMetal", Boolean.FALSE); 
            DicomSlider slider = new DicomSlider();
            slider.createAndShowGUI();
        	}
    	});
	}
}
