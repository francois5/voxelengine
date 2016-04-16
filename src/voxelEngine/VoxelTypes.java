package voxelEngine;

/**
 * Interface that let you define your voxel types
 * 
 * @author francois
 */
public interface VoxelTypes {
    /*
     * This method returns an array with for all type an array with 6 face types 
     * (the face types must be defined in a VoxelFace implementation)
     */
    public int[][] getFaces();
}
