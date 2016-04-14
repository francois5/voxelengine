package test;

import voxelEngine.noise.VoxelNoise;

/**
 *
 * @author francois
 */
public class TestVoxelNoise implements VoxelNoise {
    
    private TestSimplexNoise noise2D;
    
    public TestVoxelNoise(int systemHeight, int chunkHeight, int maxRelief, int resolution) {
        noise2D = new TestSimplexNoise((systemHeight * chunkHeight) / 2, maxRelief, resolution);
    }
    
    @Override
    public int noise(int xin, int yin, int zin) {
        int groundLevel = noise2D.noise(xin + 0.5, zin + 0.5);
        if(groundLevel < yin)
            return 0;
        else if(groundLevel == yin)
            return 2;
        else
            return 1;
    }
    
}
