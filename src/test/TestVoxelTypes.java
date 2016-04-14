package test;

import voxelEngine.VoxelTypes;

/**
 *
 * @author francois
 */
public class TestVoxelTypes implements VoxelTypes {
    
    private int[][] faces;

    public TestVoxelTypes(){
        this.faces = new int[][] {
             // VOID
                null
            ,
            { // DIRT
                1,1,1,1,1,1,
            },
            { // GRASS
                1,1,1,1,1,2,
            },
             // WOOD
                null
            ,
            { // STONE
                4,4,4,4,4,4,
            },
        };
    }

    @Override
    public int[][] getFaces() {
        return faces;
    }

}
