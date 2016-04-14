package test;

import voxelEngine.FaceTypes;

/**
 *
 * @author francois
 */
public class TestFaceTypes implements FaceTypes {
    
    private String[][] textures;
    
    public TestFaceTypes() {
        this.textures = new String[][] {
             // VOID
                null
            ,
            { // DIRT
                "Textures/voxelFaces/myDirt_15.png", "Textures/voxelFaces/myDirt_14.png", 
                "Textures/voxelFaces/myDirt_13.png", "Textures/voxelFaces/myDirt_12.png", 
                "Textures/voxelFaces/myDirt_11.png", "Textures/voxelFaces/myDirt_10.png", 
                "Textures/voxelFaces/myDirt_9.png", "Textures/voxelFaces/myDirt_8.png", 
                "Textures/voxelFaces/myDirt_7.png", "Textures/voxelFaces/myDirt_6.png", 
                "Textures/voxelFaces/myDirt_5.png", "Textures/voxelFaces/myDirt_4.png", 
                "Textures/voxelFaces/myDirt_3.png", "Textures/voxelFaces/myDirt_2.png", 
                "Textures/voxelFaces/myDirt_1.png", "Textures/voxelFaces/myDirt_0.png", 
            },
            { // GRASS
                "Textures/voxelFaces/myGrass_15.png", "Textures/voxelFaces/myGrass_14.png", 
                "Textures/voxelFaces/myGrass_13.png", "Textures/voxelFaces/myGrass_12.png", 
                "Textures/voxelFaces/myGrass_11.png", "Textures/voxelFaces/myGrass_10.png", 
                "Textures/voxelFaces/myGrass_9.png", "Textures/voxelFaces/myGrass_8.png", 
                "Textures/voxelFaces/myGrass_7.png", "Textures/voxelFaces/myGrass_6.png", 
                "Textures/voxelFaces/myGrass_5.png", "Textures/voxelFaces/myGrass_4.png", 
                "Textures/voxelFaces/myGrass_3.png", "Textures/voxelFaces/myGrass_2.png", 
                "Textures/voxelFaces/myGrass_1.png", "Textures/voxelFaces/myGrass_0.png", 
            },
             // WOOD
                null
            ,
            { // STONE
                "Textures/voxelFaces/myStone_15.png", "Textures/voxelFaces/myStone_14.png", 
                "Textures/voxelFaces/myStone_13.png", "Textures/voxelFaces/myStone_12.png", 
                "Textures/voxelFaces/myStone_11.png", "Textures/voxelFaces/myStone_10.png", 
                "Textures/voxelFaces/myStone_9.png", "Textures/voxelFaces/myStone_8.png", 
                "Textures/voxelFaces/myStone_7.png", "Textures/voxelFaces/myStone_6.png", 
                "Textures/voxelFaces/myStone_5.png", "Textures/voxelFaces/myStone_4.png", 
                "Textures/voxelFaces/myStone_3.png", "Textures/voxelFaces/myStone_2.png", 
                "Textures/voxelFaces/myStone_1.png", "Textures/voxelFaces/myStone_0.png", 
            },
        };
    }
    
    @Override
    public String[][] getTextures() {
        return textures;
    }

}
