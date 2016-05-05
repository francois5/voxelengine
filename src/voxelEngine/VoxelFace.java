package voxelEngine;

/**
 * 
 * @author francois
 */
public class VoxelFace {
    
    public boolean transparent;
    public int voxelType;
    public int faceType;
    public int light;

    public boolean lightOff() {
        if (light > 0) {
            light = 0;
            return true;
        }
        return false;
    }

    public boolean equals(final VoxelFace face) {
        return face.transparent == this.transparent
                && face.faceType == this.faceType
                && face.light == this.light;
    }
}