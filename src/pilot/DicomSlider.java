package pilot;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import util.ImageProcessing;
import external.DicomHeaderReader;
import external.DicomReader;
import net.miginfocom.swing.MigLayout;

public class DicomSlider {

	JFileChooser fileChooser;
	JButton openButton;
	JButton flipButton;
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
		
		MigLayout layout = new MigLayout("", "[320px][320px]", "[][640px][]");
		JPanel panel = new JPanel();
		panel.setLayout(layout);
		panel.add(openButton, "cell 0 0");
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
		
		panel.add(progressPanel, "cell 1 0");
		
		openButton.addActionListener(new OpenDirectoryListener(panel));
		//flipButton.addActionListener(new FlipSlicesListener());
		slider.addChangeListener(new ImageSlideListener());
		
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
				slider.setMaximum(dicomFiles.length - 1);
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
		        imageLabel.setIcon(new ImageIcon(dicomFiles[slice]));
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
			}
			
		};
		loadWorker.execute();
	}
	
	private class FlipSlicesListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			BufferedImage[] temp = new BufferedImage[dicomFiles.length];
			for(int i = 0; i < dicomFiles.length; i++){
				temp[i] = toBufferedImage(dicomFiles[i]);
			}
			dicomFiles = ImageProcessing.flipAxes(temp, progress);
			slider.setMaximum(dicomFiles.length - 1);
			imageLabel.setIcon(new ImageIcon(dicomFiles[0]));
		}
		
	}
	
	private void resetProgress(){
		this.progress.setValue(0);
		this.progressLabel.setText("");
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
            //Turn off metal's use of bold fonts
            UIManager.put("swing.boldMetal", Boolean.FALSE); 
            DicomSlider slider = new DicomSlider();
            slider.createAndShowGUI();
        	}
    	});
	}
}
