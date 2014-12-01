package pilot;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import util.ImageProcessing;
import util.OpenCVUtil;
import net.miginfocom.swing.MigLayout;

public class AdjustPlaneSlider {
	
	JFrame frame;
	DicomSlider parent;
	BufferedImage[] baseImages;
	
	public AdjustPlaneSlider(Image[] images, DicomSlider ds){
		parent = ds;
		baseImages = new BufferedImage[images.length];
		for(int i = 0; i < images.length; i++){
			baseImages[i] = DicomSlider.toBufferedImage(images[i]);
		}
	}
	
	// [vert] [img] [vert]
	// [slider]
	// explanation
	// [+] noise [-]
	// explanation
	// [OK]
	
	JLabel imageLabel;
	JSlider imageSlider;
	
	JSlider lSlider;
	JSlider rSlider;
	int START_L = 100;
	int START_R = 200;
	
	JLabel noise;
	JButton plusNoise;
	JButton minusNoise;
	int noiseAmount;
	
	private JPanel createPanel(){
		JPanel panel = new JPanel();
		MigLayout layout = new MigLayout("", "[]10px[]10px[]", "[][][]4px[][]4px[]");
		panel.setLayout(layout);
		
		imageLabel = new JLabel(new ImageIcon(baseImages[baseImages.length / 2]));
		
		imageSlider = new JSlider(0, baseImages.length - 1, baseImages.length / 2);
		imageSlider.setPreferredSize(new Dimension(300, 30));
		imageSlider.addChangeListener(new ImageSlideListener());
		
		lSlider = new JSlider(0, baseImages[0].getHeight(), baseImages[0].getHeight() - START_L);
		lSlider.setOrientation(JSlider.VERTICAL);
		lSlider.setPreferredSize(new Dimension(25, baseImages[0].getHeight()));
		lSlider.addChangeListener(new LeftSlideListener());
		
		rSlider = new JSlider(0, baseImages[0].getHeight(), START_R);
		rSlider.setOrientation(JSlider.VERTICAL);
		rSlider.setPreferredSize(new Dimension(25, baseImages[0].getHeight()));
		rSlider.addChangeListener(new RightSlideListener());

		panel.add(lSlider, "cell 0 0");
		panel.add(imageLabel, "cell 1 0");
		panel.add(rSlider, "cell 2 0");
		
		panel.add(imageSlider, "cell 0 1 3 1, align center");
		panel.add(new JLabel("<html>a) Adjust left and right sliders so the plane bisects a regular section"
				+"through the brain (above eyes & nape of neck).</html>"), "cell 0 2 3 1");
		
		
		noiseAmount = 0;
		noise = new JLabel("0");
		plusNoise = new JButton("+");
		minusNoise = new JButton("-");
		plusNoise.addActionListener(new NoiseChangeListener(1));
		minusNoise.addActionListener(new NoiseChangeListener(-1));
		
		JPanel noiseAdjustPanel = new JPanel();
		MigLayout noiseLayout = new MigLayout();
		noiseAdjustPanel.setLayout(noiseLayout);
		noiseAdjustPanel.add(minusNoise, "cell 0 0");
		noiseAdjustPanel.add(noise, "cell 1 0");
		noiseAdjustPanel.add(plusNoise, "cell 2 0");
		
		panel.add(noiseAdjustPanel, "cell 1 3");
		panel.add(new JLabel("b) Adjust how many slices from top are considered noise."), "cell 0 4 3 1");
		
		JButton okayButton = new JButton("Done.");
		okayButton.addActionListener(new OkayListener());
		panel.add(okayButton, "cell 2 5");
		
		return panel;
	}
	
	private void redrawImage(){
		BufferedImage sliceImage = baseImages[imageSlider.getValue()];
		for(int i = 0; i < noiseAmount; i++){
			sliceImage = OpenCVUtil.addLine(sliceImage, new Point(0, i), new Point(sliceImage.getWidth(), i), 0, 1);
		}
		final BufferedImage temp = sliceImage;
		SwingUtilities.invokeLater(() -> imageLabel.setIcon(new ImageIcon(OpenCVUtil.addLine(temp, new java.awt.Point(0, getLSliderValue()), new java.awt.Point(temp.getWidth(), temp.getHeight() - START_R), 127, 5))));
	
	}
	
	private int getLSliderValue(){
		return lSlider.getMaximum() - lSlider.getValue();
	}
	
	private void setLSliderValue(int value){
		lSlider.setValue(lSlider.getMaximum() - value);
	}
	
	private class ImageSlideListener implements ChangeListener{

		@Override
		public void stateChanged(ChangeEvent e) {
			redrawImage();
		}
		
	}
	
	private class LeftSlideListener implements ChangeListener{

		@Override
		public void stateChanged(ChangeEvent e) {
			//START_L = lSlider.getValue();
			redrawImage();
		}	
	}
	
	private class RightSlideListener implements ChangeListener{

		@Override
		public void stateChanged(ChangeEvent e) {
			START_R = rSlider.getValue();
			redrawImage();
		}	
	}

	private class OkayListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			parent.setNoiseLevel(noiseAmount);
			parent.setLinePoints(getLSliderValue(), rSlider.getMaximum() - rSlider.getValue());
			frame.setVisible(false);
			frame.dispose();
		}		
	}
	
	private class NoiseChangeListener implements ActionListener{

		int impact;
		
		public NoiseChangeListener(int impact){
			this.impact = impact;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if(noiseAmount + this.impact >= 0){
				noiseAmount = noiseAmount + this.impact;
				noise.setText(noiseAmount + "");
				redrawImage();
			}
		}
		
	}
	
	public void createAndShowGUI() {
        //Create and set up the window.
        frame = new JFrame("Adjust Plane");
 
        //Add content to the window.
        frame.add(createPanel());
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
