package pilot;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import external.DicomHeaderReader;
import external.DicomReader;
import net.miginfocom.swing.MigLayout;

public class DicomSlider {

	JFileChooser fileChooser;
	JButton openButton;
	JLabel imageLabel;
	JSlider slider;
	
	public JPanel createPanel(){
		fileChooser = new JFileChooser("/Users/aaron/Documents/Dropbox/Schoolwork/BioGeo/");
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		imageLabel = new JLabel();
		openButton = new JButton("Open .dcm directory");
		
		slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		slider.setMajorTickSpacing(10);
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setPreferredSize(new Dimension(300, 40));
		
		MigLayout layout = new MigLayout("", "[640px]", "[][640px][]");
		JPanel panel = new JPanel();
		panel.setLayout(layout);
		panel.add(openButton, "cell 0 0");
		panel.add(imageLabel, "cell 0 1");
		panel.add(slider, "cell 0 2");
		
		openButton.addActionListener(new OpenDirectoryListener(panel));
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
	            File file = fileChooser.getSelectedFile();
	            loadAllDicom(file);
	            imageLabel.setIcon(new ImageIcon(dicomFiles[0]));
	        }
		}
	}
	
	Image[] dicomFiles;
	
	private void loadAllDicom(File folder){
		// 1. Load all the .DCM files in the directory
		File[] files = folder.listFiles(new FileFilter(){
			@Override
			public boolean accept(File pathname) {
				// TODO Auto-generated method stub
				return getFileExtension(pathname).equals(".dcm");
			}
		});
		
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
		
		// 3. Load the image data into an array
		dicomFiles = new Image[files.length];
		for(int i = 0; i < files.length; i++){
			try {
				DicomReader reader = new DicomReader(files[i].toURL());
				dicomFiles[i] = reader.getImage();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//4. Update Slider max...
		slider.setMaximum(dicomFiles.length - 1);
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
