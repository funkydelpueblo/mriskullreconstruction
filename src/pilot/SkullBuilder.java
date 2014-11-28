package pilot;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.util.ArrayList;
import java.util.Iterator;

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
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TriangleArray;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import net.miginfocom.swing.MigLayout;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;

import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;

public class SkullBuilder {
	public static void constructSkullShowWindow(java.util.ArrayList<Point3d> points){
		JFrame frame = new JFrame();
        frame.add(new JScrollPane(buildPanel(points)));
        frame.setSize(600, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	//Based on StackOverflow #12313917
	public static JPanel buildPanel(java.util.ArrayList<Point3d> points){
		JPanel panel = new JPanel();
		MigLayout layout = new MigLayout();
		panel.setLayout(new BorderLayout());
		GraphicsConfiguration gc=SimpleUniverse.getPreferredConfiguration();
		Canvas3D canvas = new Canvas3D(gc);
		panel.add("Center", canvas);
		
		//Setup Branch group
		BranchGroup group = pointsToBranchGroup(points);
        
        //Lighting
        Color3f light1Color = new Color3f(java.awt.Color.WHITE);
		BoundingSphere bounds = new BoundingSphere(new Point3d(0.0,0.0,0.0), 1000.0);
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
		
		//Position camera better?
       // above pyramid
     	/*Vector3d viewTranslation = new Vector3d();
     	viewTranslation.z = points.get(0).z;
     	viewTranslation.x = points.get(0).x;
     	viewTranslation.y = points.get(0).y;
     	Transform3D viewTransform = new Transform3D();
     	viewTransform.setTranslation(viewTranslation);*/
		Transform3D lookAt = new Transform3D();
		lookAt.lookAt(new Point3d(0,0,0), points.get(points.size()/2), new Vector3d(0.0, 1.0, 0.0));
		lookAt.invert();
		universe.getViewingPlatform().getViewPlatformTransform().setTransform(lookAt);
     	//Transform3D rotation = new Transform3D();
     	//rotation.rotX(-Math.PI / 12.0d);
     	//rotation.mul(viewTransform);
     	//universe.getViewingPlatform().getViewPlatformTransform().setTransform(rotation);
     	//universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewTransform);
		
		return panel;
	}
	
	private static BranchGroup pointsToBranchGroup(java.util.ArrayList<Point3d> points){
		//Setup Branch group
		BranchGroup group = new BranchGroup();
		Appearance app = new Appearance();
	    ColoringAttributes ca = new ColoringAttributes(new Color3f(180.0f, 204.0f,180.0f), ColoringAttributes.SHADE_GOURAUD);
	    app.setColoringAttributes(ca);
		/*
	    //Add points to array
	    Point3d[] plaPts = new Point3d[points.size()];

	    int c = 0;
	    for(Point3d p : points){
	    	plaPts[c] = new Point3d(p.x / 10, p.y / 10, p.z / 10);
	    	c++;
	    }
        
        PointArray pla = new PointArray(points.size(), GeometryArray.COORDINATES);
        pla.setCoordinates(0, plaPts);
        
        //Increase point size
        PointAttributes pointAtt=new PointAttributes();
        pointAtt.setPointSize(10.0f);
        pointAtt.setPointAntialiasingEnable(true);
        
        app.setPointAttributes(pointAtt);
        
        //Turn points into geometry object
        Shape3D plShape = new Shape3D(pla, app);*/
        TransformGroup objRotate = new TransformGroup();
        objRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        /*int c = 0;
        System.out.println(points.size() + " primitives");
        for(Point3d p : points){
        	if(c > 10000){
        		break;
        	}
        	c++;
        	Transform3D transform = new Transform3D();
        	TransformGroup tg = new TransformGroup();
        	//Sphere s = new Sphere(0.5f);
        	Box b = new Box(0.1f, 0.1f, 0.1f, null);
        	Vector3d v = new Vector3d(p.x / 10, p.y / 10, p.z / 10);
        	transform.setTranslation(v);
        	tg.setTransform(transform);
        	tg.addChild(b);
        	objRotate.addChild(tg);
        }*/
        Point_dt[] dtPoints = new Point_dt[points.size()];
        for(int i = 0; i < points.size(); i++){
        	Point3d p = points.get(i);
        	dtPoints[i] = new Point_dt(p.x, p.y, p.z);
        }
        Delaunay_Triangulation triangles = new Delaunay_Triangulation(dtPoints);
        
        TriangleArray tris = new TriangleArray(triangles.trianglesSize()*3, TriangleArray.COORDINATES);
        Iterator<Triangle_dt> iter = triangles.trianglesIterator();
        int c = 0;
        Point3d pointOne = null;
        while(iter.hasNext()){
        	Triangle_dt t = iter.next();
        	
        	//if(c == 0){
        		Point3d temp = dtPointConv(t.p1());
        	//}
        	
        	tris.setCoordinate(c, dtPointConv(t.p1()));
        	tris.setCoordinate(c+1, dtPointConv(t.p2()));
        	
        	if(t.p3() != null){
            	tris.setCoordinate(c+2, dtPointConv(t.p3()));
        	//}else{
        	//	System.out.println("End?");
        	//	tris.setCoordinate(c+2, pointOne);
        	}
        	pointOne = temp;
        	c+=3;
        }
        GeometryInfo geometryInfo = new GeometryInfo(tris);
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(geometryInfo);
		GeometryArray result = geometryInfo.getGeometryArray();
		Shape3D shape = new Shape3D(result, app);
		objRotate.addChild(shape);
		
        //objRotate.addChild(plShape);
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
        
        return group;
	}
	
	public static Point3d dtPointConv(Point_dt point){
		return new Point3d(point.x(), point.y(), point.z());
	}
	
	public static void main(String[] args){
		java.util.ArrayList<Point3d> test = new ArrayList<Point3d>();
		/*test.add(new Point3d(353.0, 148.0, 55.0));
		test.add(new Point3d(352.0, 138.0, 35.0));
		test.add(new Point3d(351.0, 148.0, 35.0));
		test.add(new Point3d(351.0, 150.0, 35.0));*/
		/*test.add(new Point3d(3.0, 2.0, 5.0));
		test.add(new Point3d(2.0, 2.0, 2.0));
		test.add(new Point3d(1.0, 2.0, 5.0));
		test.add(new Point3d(1.0, 0.0, 3.0));
		test.add(new Point3d(8.0, 2.0, 5.0));
		test.add(new Point3d(2.0, 3.0, 2.0));
		test.add(new Point3d(5.0, 1.0, 6.0));
		test.add(new Point3d(1.0, 0.0, 0.0));*/
		int COUNT = 25;
		for(int i = 0; i < COUNT; i++){
			test.add(new Point3d(Math.random() * 100, Math.random() * 100, Math.random() * 100));
		}
		constructSkullShowWindow(test);
		//(353.0, 148.0, 35.0), (352.0, 148.0, 35.0), (351.0, 148.0, 35.0)
	}
}
