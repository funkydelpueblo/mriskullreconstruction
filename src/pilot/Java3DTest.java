package pilot;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import net.miginfocom.swing.MigLayout;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class Java3DTest {

	public static void helloWorld(){
		SimpleUniverse su = new SimpleUniverse();
		BranchGroup branch = new BranchGroup();
		Sphere sphere = new Sphere(0.5f);
		branch.addChild(sphere);
		
		Color3f light1Color = new Color3f(java.awt.Color.WHITE);
		BoundingSphere bounds = new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);
		Vector3f light1Direction = new Vector3f(4.0f, -7.0f, -12.0f);
		DirectionalLight light1 = new DirectionalLight(light1Color, light1Direction);
		light1.setInfluencingBounds(bounds);
		
		AmbientLight light2 = new AmbientLight(new Color3f(java.awt.Color.CYAN));
		light2.setInfluencingBounds(bounds);
		
		branch.addChild(light1);
		branch.addChild(light2);
		
		su.getViewingPlatform().setNominalViewingTransform();
		su.addBranchGraph(branch);
	}
	
	//Based on StackOverflow #12313917
	public static JPanel buildPanel(){
		JPanel panel = new JPanel();
		MigLayout layout = new MigLayout();
		panel.setLayout(new BorderLayout());
		GraphicsConfiguration gc=SimpleUniverse.getPreferredConfiguration();
		Canvas3D canvas = new Canvas3D(gc);
		panel.add("Center", canvas);
		
		//Setup Branch group
		BranchGroup group = new BranchGroup();
		Appearance app = new Appearance();
	    ColoringAttributes ca = new ColoringAttributes(new Color3f(180.0f, 204.0f,180.0f), ColoringAttributes.SHADE_GOURAUD);
	    app.setColoringAttributes(ca);
		
	    //Add points to array
	    Point3f[] plaPts = new Point3f[4];

	    int c = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j <2; j++) {
                plaPts[c] = new Point3f(i/10.0f,j/10.0f,0);
                c++;
            }
        }
        
        PointArray pla = new PointArray(4, GeometryArray.COORDINATES);
        pla.setCoordinates(0, plaPts);
        
        //Increase point size
        PointAttributes pointAtt=new PointAttributes();
        pointAtt.setPointSize(10.0f);
        pointAtt.setPointAntialiasingEnable(true);
        
        app.setPointAttributes(pointAtt);
        
        //Turn points into geometry object
        Shape3D plShape = new Shape3D(pla, app);
        TransformGroup objRotate = new TransformGroup();
        objRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objRotate.addChild(plShape);
        group.addChild(objRotate);
	    
        //Mouse interactions
        MouseRotate mr=new MouseRotate();
        mr.setTransformGroup(objRotate);
        mr.setSchedulingBounds(new BoundingSphere());
        group.addChild(mr);
        MouseZoom mz=new MouseZoom();
        mz.setTransformGroup(objRotate);
        mz.setSchedulingBounds(new BoundingSphere());
        group.addChild(mz);
        MouseTranslate msl=new MouseTranslate();
        msl.setTransformGroup(objRotate);
        msl.setSchedulingBounds(new BoundingSphere());
        group.addChild(msl);
        
        //Lighting
        Color3f light1Color = new Color3f(java.awt.Color.WHITE);
		BoundingSphere bounds = new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);
		Vector3f light1Direction = new Vector3f(4.0f, -7.0f, -12.0f);
		DirectionalLight light1 = new DirectionalLight(light1Color, light1Direction);
		light1.setInfluencingBounds(bounds);
		
		AmbientLight light2 = new AmbientLight(new Color3f(java.awt.Color.CYAN));
		light2.setInfluencingBounds(bounds);
        
		group.addChild(light1);
		group.addChild(light2);
		
		//Finish up
		group.compile();	
		SimpleUniverse universe = new SimpleUniverse(canvas);
		universe.getViewingPlatform().setNominalViewingTransform();
		universe.addBranchGraph(group);
		return panel;
	}
	
	public static void main(String[] args){
		JFrame frame = new JFrame();
        frame.add(new JScrollPane(buildPanel()));
        frame.setSize(300, 300);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//helloWorld();
	}
	
}
