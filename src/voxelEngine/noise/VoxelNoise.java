package voxelEngine.noise;

/**
 * Interface that let you define the world generation procedure
 * 
 * @author francois
 */
public interface VoxelNoise {
    /*
     * This method returns the voxel type for a location
     */
    public int noise(int xin, int yin, int zin);
}
