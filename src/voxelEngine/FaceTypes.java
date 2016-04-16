package voxelEngine;

/**
 * Interface that let you define your voxel face types
 * 
 * @author francois
 */
public interface FaceTypes {
    /*
     * This method returns an array with for all type an array with 16 texture paths
     * This is not the definitive texture management system
     */
    public String[][] getTextures();
}
