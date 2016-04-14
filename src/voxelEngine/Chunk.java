package voxelEngine;

import voxelEngine.noise.VoxelNoise;
import com.jme3.math.Vector3f;

/**
 *
 * @author francois
 */
public interface Chunk {
    
    public void addBlock(int x, int y, int z, int type);
    public void removeBlock(int x, int y, int z);
    public int getBlock(int x, int y, int z);
    public void init(VoxelNoise noise);
    public void detach();
    public void attach();
    public boolean modified();
    public Vector3f getTranslation();
    public void setTranslation(Vector3f translation);
    public void externalUpdate(int side, boolean refreshDisplay);
    public void updatePropagation(boolean refreshDisplayInCheckInsideOutPropagation);
    public void refreshDisplay();
    public void refreshDisplayNoAttach();
    public void propagate(int x, int y, int z, int pValue, int side);
    public void lightOff();
    public int getLightVal(int x, int y, int z);
    public VoxelFace getVoxel(int x, int y, int z);
    public boolean lightTurnedOff(boolean reset);
    public boolean isAttached();
    public int getHeight(int x, int z);

}
