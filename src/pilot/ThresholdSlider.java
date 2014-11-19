package pilot;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import util.ImageProcessing;
import net.miginfocom.swing.MigLayout;

public class ThresholdSlider {

	JSlider slider;
	JLabel imageLabel;
	JLabel thLabel;
	JButton accept;
	BufferedImage sliceImage;
	DicomSlider parent;
	
	public ThresholdSlider(Image image, DicomSlider ds){
		parent = ds;
		sliceImage = DicomSlider.toBufferedImage(image);
	}
	
	private final int INITIAL = 127;
	
	private JPanel createPanel(){
		JPanel panel = new JPanel();
		MigLayout layout = new MigLayout("", "[][][]", "[]20[]");
		panel.setLayout(layout);
		
		sliceImage = ImageProcessing.threshold(sliceImage, INITIAL);
		imageLabel = new JLabel(new ImageIcon(sliceImage));
		slider = new JSlider(JSlider.HORIZONTAL, 0, 255, INITIAL);
		slider.setPreferredSize(new Dimension(250, 30));
		thLabel = new JLabel(INITIAL+"");
		
		accept = new JButton("Set Threshold");
		
		panel.add(imageLabel, "cell 0 0 3 1");
		panel.add(slider, "cell 0 1");
		panel.add(thLabel, "cell 1 1");
		panel.add(accept, "cell 2 1");
		
		return panel;
	}
	
	public void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Select Threshold");
 
        //Add content to the window.
        frame.add(createPanel());
 
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
