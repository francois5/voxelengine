package test;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Line;

/**
 *
 * @author francois
 */
public class CubeSelector {
    
    private Node node = new Node();
    private Geometry[] edges = new Geometry[12];
    public CubeSelector(Node parent) {
        parent.attachChild(node);
        for(int i = 0; i < 12; ++i) {
            edges[i] = new Geometry("line", new Line(Vector3f.ZERO, Vector3f.ZERO));
            edges[i].setMaterial(new Material(TestGreedyMesh.getApp().getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md"));
        }
    }
    
    public void drawCubeEdges(Vector3f location, float width, ColorRGBA color) {
        node.detachAllChildren();
        
        drawLine(location, location.add(0,1,0), width, color, 0);
        drawLine(location, location.add(0,0,1), width, color, 1);
        drawLine(location, location.add(1,0,0), width, color, 2);
        
        drawLine(location.add(1,0,1), location.add(1,1,1), width, color, 3);
        drawLine(location.add(1,0,1), location.add(0,0,1), width, color, 4);
        drawLine(location.add(1,0,1), location.add(1,0,0), width, color, 5);
        
        drawLine(location.add(0,1,1), location.add(1,1,1), width, color, 6);
        drawLine(location.add(0,1,1), location.add(0,1,0), width, color, 7);
        drawLine(location.add(0,1,1), location.add(0,0,1), width, color, 8);
        
        drawLine(location.add(1,1,0), location.add(1,1,1), width, color, 9);
        drawLine(location.add(1,1,0), location.add(0,1,0), width, color, 10);
        drawLine(location.add(1,1,0), location.add(1,0,0), width, color, 11);
    }
    
    private void drawLine(Vector3f start, Vector3f stop, float width, ColorRGBA color, int edgeIndex) {
        ((Line)(edges[edgeIndex].getMesh())).updatePoints(start, stop);
        edges[edgeIndex].getMaterial().setColor("Color", color);
        ((Line)(edges[edgeIndex].getMesh())).setLineWidth(width);
        node.attachChild(edges[edgeIndex]);
    }
}
