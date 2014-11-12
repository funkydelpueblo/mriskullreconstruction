package pilot;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import external.DicomReader;
import net.miginfocom.swt.MigLayout;

public class DicomLoad extends JPanel implements ActionListener{

	JFileChooser fileChooser;
	JButton openButton;
	JLabel imageLabel;
	
	public DicomLoad(){
		fileChooser = new JFileChooser();
		imageLabel = new JLabel();
		openButton = new JButton("Open .dcm file");
		openButton.addActionListener(this);
		
		JPanel panel = new JPanel();
		add(openButton);
		add(imageLabel);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		int returnVal = fileChooser.showOpenDialog(DicomLoad.this);
		 
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            //This is where a real application would open the file.
            try {
				//BufferedImage myPicture = ImageIO.read(file);
				DicomReader reader = new DicomReader(file.toURL());
				Image myPicture = reader.getImage();
            	imageLabel.setIcon(new ImageIcon(myPicture));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
	}
	
	 private static void createAndShowGUI() {
	        //Create and set up the window.
	        JFrame frame = new JFrame("Load DICOM");
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	 
	        //Add content to the window.
	        frame.add(new DicomLoad());
	 
	        //Display the window.
	        frame.pack();
	        frame.setVisible(true);
	    }
	
	public static void main(String[] args){
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 
                createAndShowGUI();
            }
        });
	}

}
