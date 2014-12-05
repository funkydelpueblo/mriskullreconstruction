package pilot;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import util.AdapterForBuild;
import util.ImageProcessing;
import net.miginfocom.swing.MigLayout;

public class ThresholdSlider {

	JSlider slider;
	BufferedImage[] baseImages;
	JLabel imageLabel;
	JLabel thLabel;
	JButton accept;
	JCheckBox openclose;
	JTextField ocamt;
	
	BufferedImage sliceImage;
	AdapterForBuild parent;
	
	JSlider imageSlider;
	
	
	public ThresholdSlider(Image[] images, AdapterForBuild ds){
		parent = ds;
		baseImages = new BufferedImage[images.length];
		for(int i = 0; i < images.length; i++){
			baseImages[i] = ImageProcessing.toBufferedImage(images[i]);
		}
		START_SLICE = (int)(images.length / 2);
	}
	
	private final int START_SLICE;
	private final int INITIAL = 127;
	
	private JPanel createPanel(){
		JPanel panel = new JPanel();
		MigLayout layout = new MigLayout("", "[][][][][]", "[]20[]10[]");
		panel.setLayout(layout);
		
		sliceImage = ImageProcessing.threshold(baseImages[START_SLICE], INITIAL);
		imageLabel = new JLabel(new ImageIcon(sliceImage));
		slider = new JSlider(JSlider.HORIZONTAL, 0, 255, INITIAL);
		slider.setPreferredSize(new Dimension(250, 30));
		slider.addChangeListener(new ThresholdSlideListener());
		thLabel = new JLabel(INITIAL+"");
		
		accept = new JButton("Set Threshold");
		accept.addActionListener(new SetThresholdListener());
		openclose = new JCheckBox();
		
		imageSlider = new JSlider(0, baseImages.length, START_SLICE);
		imageSlider.addChangeListener(new ImageSlideListener());
		
		ocamt = new JTextField("0");
		
		panel.add(imageLabel, "cell 0 0 5 1");
		panel.add(slider, "cell 0 1");
		panel.add(thLabel, "cell 1 1");
		panel.add(accept, "cell 2 1");
		panel.add(new JLabel("Open & Close"), "cell 3 1");
		//panel.add(openclose, "cell 4 1");
		panel.add(ocamt, "cell 4 1");
		
		panel.add(imageSlider, "cell 0 2 5 1");
		
		return panel;
	}
	
	private class ThresholdSlideListener implements ChangeListener{

		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider)e.getSource();
			final int th = (int)source.getValue();
			int slice = imageSlider.getValue();
			if (source.getValueIsAdjusting()) {
		       
		        sliceImage = ImageProcessing.threshold(baseImages[slice], th);
		       
		        int oc = Integer.parseInt(ocamt.getText());
		        if(oc > 0){
			        sliceImage = ImageProcessing.openThenClose(sliceImage, oc); //Open/Close to clean?
		        }
		        
		        SwingUtilities.invokeLater(() -> imageLabel.setIcon(new ImageIcon(sliceImage)));
		        SwingUtilities.invokeLater(() -> thLabel.setText(th+""));
		    }
		}
	}
	
	private class ImageSlideListener implements ChangeListener{

		@Override
		public void stateChanged(ChangeEvent e) {
			sliceImage = ImageProcessing.threshold(baseImages[imageSlider.getValue()], slider.getValue());
			int oc = Integer.parseInt(ocamt.getText());
	        if(oc > 0){
		        sliceImage = ImageProcessing.openThenClose(sliceImage, oc); //Open/Close to clean?
	        }
			SwingUtilities.invokeLater(() -> imageLabel.setIcon(new ImageIcon(sliceImage)));
		}
		
	}
	
	private class SetThresholdListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			parent.thresholdImages(slider.getValue(), Integer.parseInt(ocamt.getText()));
			frame.setVisible(false);
			frame.dispose();
		}
		
	}
	
	JFrame frame;
	
	public void createAndShowGUI() {
        //Create and set up the window.
        frame = new JFrame("Select Threshold");
 
        //Add content to the window.
        frame.add(createPanel());
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
