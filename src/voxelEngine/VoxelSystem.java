package voxelEngine;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 *
 * @author francois
 */
public interface VoxelSystem {
    public Node getNode();
    public int getSystemHeight();
    public void putBlock(Vector3f location, int type);
    public void removeBlock(Vector3f location);
    public int getHeight(int x, int z);
    public void setPlayerLocation(Vector3f location);
    public Integer getBlock(Vector3f location);
    public Vector3f getPointedBlockLocation(Vector3f collisionContactPoint, boolean getNeighborLocation);
    
    
    public void notifyUpdate(Vector3f translation, int side, boolean refreshDisplay);
    public void refreshDisplay(Vector3f translation, int side);
    public void externalPropagation(Vector3f translation, int chunkSide, 
            int x, int y, int z, int pValue, int voxelSide);
    public void updatePropagation(Vector3f translation);
    public int getLightVal(Vector3f translation, int side, int x, int y, int z);
    public VoxelFace getVoxel(Vector3f translation, int side, int x, int y, int z);

    public void addPhysicsSpace(RigidBodyControl rigidBodyControl);
    public void removePhysicsSpace(RigidBodyControl rigidBodyControl);
    public int getVoxelFaces(int voxelType, int side);
    public Material getMaterial(int type, int light);
}
