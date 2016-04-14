package voxelEngine;

import voxelEngine.noise.VoxelNoise;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import jme3tools.optimize.GeometryBatchFactory;

/**
 *
 * @author francois
 * @author Mikola Lysenko for the greedy meshing algorithm
 * @author Rob O'Leary for the porting to Java of the greedy meshing algorithm
 */
public class GreedyMeshChunk implements Chunk {
    private Vector3f translation;
    private boolean modified = false;
    
    private int voxelSize;
    private int chunkWidth;
    private int chunkHeight;

    private VoxelFace [][][] voxels;
    
    private VoxelFace neighborVoxel;
    
    private static final int SOUTH      = 0;
    private static final int NORTH      = 1;
    private static final int EAST       = 2;
    private static final int WEST       = 3;
    private static final int BOTTOM     = 4;
    private static final int TOP        = 5;
    private Node node = new Node();
    
    private Node optimizedChunkSpatial;
    private RigidBodyControl rigidBodyControl;
    private final Node parent;
    private final VoxelSystem voxelSystem;
    
    private boolean[] insideOutPropagation = new boolean[6];
    private boolean lightTurnedOff = false;
    private boolean faceCullingEnable;
    private boolean attached = false;
    private boolean voidChunk = true;
    
    public GreedyMeshChunk(VoxelSystem blockSystem, Node parent, 
            Vector3f translation, int voxelSize, int chunkWidth, int chunkHeight, boolean enableFaceCulling) {
        this.parent = parent;
        this.translation = translation;
        this.voxelSize = voxelSize;
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;
        this.voxelSystem = blockSystem;
        this.faceCullingEnable = enableFaceCulling;
        voxels = new VoxelFace [chunkWidth][chunkHeight][chunkWidth];
    }
    
    @Override
    public void init(VoxelNoise noise) {
        for(int side = 0; side < 6; ++side)
            insideOutPropagation[side] = false;
        initVoxels(noise);
        updatePropagation(true);
        rebuildMeshs();
        this.attach();
    }
    
    private void initVoxels(VoxelNoise noise) {
        for(int x = 0; x < chunkWidth; ++x) {
            for(int y = 0; y < chunkHeight; ++y) {
                for(int z = 0; z < chunkWidth; ++z) {
                    internalAddVoxel(x, y, z, noise.noise((int)translation.x+x, 
                            (int)translation.y+y, (int)translation.z+z));
                }
            }
        }
    }

    @Override
    public void setTranslation(Vector3f translation) {
        this.translation = translation;
    }

    @Override
    public boolean lightTurnedOff(boolean reset) {
        if(lightTurnedOff && reset) {
            lightTurnedOff = false;
            return true;
        }
        else
            return lightTurnedOff;
    }
    
    @Override
    public void addBlock(int x, int y, int z, int type) {
        if(!addValidLocation(x,y,z))
            return;
        modified = true;
        addPropagation(x, y, z, type);
    }
    
    @Override
    public void removeBlock(int x, int y, int z) {
        if(!removeValidLocation(x,y,z))
            return;
        modified = true;
        internalRemoveVoxel(x, y, z);
        removePropagation(x, y, z);
        refreshDisplay();
    }
    
    @Override
    public void refreshDisplay() {
        rebuildMeshs();
        detach();
        attach();
    }
    
    @Override
    public void refreshDisplayNoAttach() {
        rebuildMeshs();
    }
    
    private void checkInsideOutPropagation(int sideExclude, boolean removeCulling, boolean refreshNeighborsDisplay) {
        for(int side = 0; side < 6; ++side) {
            if(insideOutPropagation[side] == true) {
                if(side == sideExclude)
                    insideOutPropagation[side] = false;
                else {
                    insideOutPropagation[side] = false;
                    voxelSystem.notifyUpdate(translation, side, refreshNeighborsDisplay);
                }
            }
            else if(refreshNeighborsDisplay && removeCulling)
                voxelSystem.refreshDisplay(translation, side);
        }
    }
    
    @Override
    public void lightOff() {
        for(int x = 0; x < chunkWidth; ++x)
            for(int y = 0; y < chunkHeight; ++y)
                for(int z = 0; z < chunkWidth; ++z)
                    if(voxels[x][y][z].lightOff())
                        lightTurnedOff = true;
    }
    
    private boolean optimizedPropagation(){
        if(voidChunk) {
            boolean fullyEnlightened = false;
            if(translation.y == (voxelSystem.getSystemHeight()-1)*this.chunkHeight) { //top chunk
                fullyEnlightened = true;
            }
            else {
                fullyEnlightened = true;
                for(int x = 0; x < chunkWidth; ++x)
                    for(int z = 0; z < chunkWidth; ++z) {
                        int val = (voxelSystem.getLightVal(translation, 5, x, 0, z));
                        if(val != 15)
                            fullyEnlightened = false;
                    }
            }
            if(fullyEnlightened) {
                for(int x = 0; x < chunkWidth; ++x)
                    for(int z = 0; z < chunkWidth; ++z)
                        for(int y = chunkHeight-1; y >= 0; --y)
                            voxels[x][y][z].light = 15;
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void updatePropagation(boolean refreshDisplayInCheckInsideOutPropagation) {
        if(!optimizedPropagation())
            internalUpdatePropagation();
        if(refreshDisplayInCheckInsideOutPropagation)
            checkInsideOutPropagation(-1, true, false);
        else
            checkInsideOutPropagation(-1, false, false);
    }
    
    private void internalUpdatePropagation() {
        // prepropagation of up->down light
        if(translation.y == (voxelSystem.getSystemHeight()-1)*this.chunkHeight) //top chunk -> sun light
            for(int x = 0; x < chunkWidth; ++x)
                for(int z = 0; z < chunkWidth; ++z) {
                    for(int y = chunkHeight-1; y >= 0 && voxels[x][y][z].voxelType == 0; --y)
                        voxels[x][y][z].light = 14; 
                }
        else
            for(int x = 0; x < chunkWidth; ++x)
                for(int z = 0; z < chunkWidth; ++z) {
                    int val = (voxelSystem.getLightVal(translation, 5, x, 0, z) - 1);
                    if(val > 0 && val < 15) {
                        for(int y = chunkHeight-1; y >= 0 && voxels[x][y][z].voxelType == 0; --y)
                            voxels[x][y][z].light = val;
                    }
                }
        for(int x = 0; x < chunkWidth; ++x)
            for(int z = 0; z < chunkWidth; ++z) {
                for(int y = chunkHeight-1; y >= 0 && voxels[x][y][z].voxelType == 0 && voxels[x][y][z].light > 0; --y) {
                    propagate(x, y, z, voxels[x][y][z].light+1, 5);
                }
            }
            
        // full recurcive propagation of other sides
        for(int x = 0; x < chunkWidth; ++x)
            for(int z = 0; z < chunkWidth; ++z) {
                propagate(x, 0, z, voxelSystem.getLightVal(translation, 4, x, chunkHeight-1, z), 4);
            }
        for(int x = 0; x < chunkWidth; ++x)
            for(int y = 0; y < chunkWidth; ++y) {
                propagate(x, y, 0, voxelSystem.getLightVal(translation, 0, x, y, chunkWidth-1), 0);
                propagate(x, y, chunkWidth-1, voxelSystem.getLightVal(translation, 1, x, y, 0), 1);
            }
        for(int y = 0; y < chunkWidth; ++y)
            for(int z = 0; z < chunkWidth; ++z) {
                propagate(0, y, z, voxelSystem.getLightVal(translation, 3, chunkWidth-1, y, z), 3);
                propagate(chunkWidth-1, y, z, voxelSystem.getLightVal(translation, 2, 0, y, z), 2);
            }
    }
    
    @Override
    public int getLightVal(int x, int y, int z) {
        if(voxels[x][y][z].voxelType != 0)
            return 16;
        return voxels[x][y][z].light;
    }
    
    @Override
    public VoxelFace getVoxel(int x, int y, int z) {
        return voxels[x][y][z];
    }
    
    private void addPropagation(int x, int y, int z, int type) {
        internalAddVoxel(x, y, z, type);
        refreshDisplay();
        voxelSystem.updatePropagation(translation);
    }
    
    private void removePropagation(int x, int y, int z) {
        propagate(x, y, z, -1, -1);
        checkInsideOutPropagation(-1, faceCullingEnable, true);
    }
    
    @Override
    public void externalUpdate(int side, boolean refreshDisplay) {
        checkInsideOutPropagation(side, faceCullingEnable, refreshDisplay);
        if(refreshDisplay)
            refreshDisplay();
    }
    
    @Override
    public void propagate(int x, int y, int z, int pValue, int side) {
        if(pValue >= 16)
            return;
        
        if (y < 0) { // side 4
            voxelSystem.externalPropagation(translation, 4, x, chunkHeight-1, z, pValue, 5);
            insideOutPropagation[4] = true;
            return;
        } else if (y >= chunkHeight) { // side 5
            voxelSystem.externalPropagation(translation, 5, x, 0, z, pValue, 4);
            insideOutPropagation[5] = true;
            return;
        } else if (x < 0) { // side 3
            voxelSystem.externalPropagation(translation, 3, chunkWidth-1, y, z, pValue, 2);
            insideOutPropagation[3] = true;
            return;
        } else if (x >= this.chunkWidth) { // side 2
            voxelSystem.externalPropagation(translation, 2, 0, y, z, pValue, 3);
            insideOutPropagation[2] = true;
            return;
        } else if (z < 0) { // side 0
            voxelSystem.externalPropagation(translation, 0, x, y, chunkWidth-1, pValue, 1);
            insideOutPropagation[0] = true;
            return;
        } else if (z >= chunkWidth) { // side 1
            voxelSystem.externalPropagation(translation, 1, x, y, 0, pValue, 0);
            insideOutPropagation[1] = true;
            return;
        }
        
        if(pValue == -1) { // -1 block removed
            try {
                pValue = callMaxVoidNeighborLightVal(x, y, z)-1;
            } catch(NullPointerException e) { 
                System.out.println("NullPointerException in callMaxVoidNeighborLightVal("+x+", "+y+", "+z+")"); 
            }
            voxels[x][y][z].light = 0;
        }
        if (voxels[x][y][z].voxelType == 0) {
            if (pValue > voxels[x][y][z].light) {
                voxels[x][y][z].light = pValue;
                propagate(x, y - 1, z, pValue, 5);
                propagate(x, y + 1, z, pValue - 1, 4);
                propagate(x - 1, y, z, pValue - 1, 2);
                propagate(x + 1, y, z, pValue - 1, 3);
                propagate(x, y, z - 1, pValue - 1, 1);
                propagate(x, y, z + 1, pValue - 1, 0);
            }
        }
    }
    
    private int callMaxVoidNeighborLightVal(int x, int y, int z) throws NullPointerException {
        int res = 0;
        if(z == 0)
            neighborVoxel = voxelSystem.getVoxel(translation, 0, x, y, chunkWidth-1);
        else
            neighborVoxel = voxels[x][y][z-1];
        if(neighborVoxel.voxelType == 0 && neighborVoxel.light > res)
            res = neighborVoxel.light;
        
        if(z == chunkWidth-1)
            neighborVoxel = voxelSystem.getVoxel(translation, 1, x, y, 0);
        else
            neighborVoxel = voxels[x][y][z+1];
        if(neighborVoxel.voxelType == 0 && neighborVoxel.light > res)
            res = neighborVoxel.light;
        
        if(x == chunkWidth-1)
            neighborVoxel = voxelSystem.getVoxel(translation, 2, 0, y, z);
        else
            neighborVoxel = voxels[x+1][y][z];
        if(neighborVoxel.voxelType == 0 && neighborVoxel.light > res)
            res = neighborVoxel.light;
        
        if(x == 0)
            neighborVoxel = voxelSystem.getVoxel(translation, 3, chunkWidth-1, y, z);
        else
            neighborVoxel = voxels[x-1][y][z];
        if(neighborVoxel.voxelType == 0 && neighborVoxel.light > res)
            res = neighborVoxel.light;
        
        if(y == 0)
            neighborVoxel = voxelSystem.getVoxel(translation, 4, x, chunkHeight-1, z);
        else
            neighborVoxel = voxels[x][y-1][z];
        if(neighborVoxel != null && neighborVoxel.voxelType == 0 && neighborVoxel.light > res)
            res = neighborVoxel.light;
        
        if(y == chunkHeight-1)
            neighborVoxel = voxelSystem.getVoxel(translation, 5, x, 0, z);
        else
            neighborVoxel = voxels[x][y+1][z];
        if(neighborVoxel.voxelType == 0 && neighborVoxel.light+1 > res)
            res = neighborVoxel.light+1;
        
        return res;
    }

    @Override
    public Vector3f getTranslation() {
        return translation;
    }

    private void internalAddVoxel(int x, int y, int z, int type) {
        if(voxels[x][y][z] == null)
            voxels[x][y][z] = new VoxelFace();
        voxels[x][y][z].voxelType = type;
        voxels[x][y][z].light = 0;
        if(type != 0)
            voidChunk = false;
    }

    private void internalRemoveVoxel(int x, int y, int z) {
        voxels[x][y][z].voxelType = 0;
    }
    
    private boolean addValidLocation(int x, int y, int z) {
        if(translation.y == (voxelSystem.getSystemHeight()-1)*this.chunkHeight
           && y == chunkHeight-1)
            return false;
        return !(voxels.length <= x || voxels[x].length <= y || voxels[x][y].length <= z);
    }
    
    private boolean removeValidLocation(int x, int y, int z) {
        if(translation.y == 0 && y == 0)
            return false;
        return !(voxels.length <= x || voxels[x].length <= y || voxels[x][y].length <= z);
    }
    
    private void rebuildMeshs() {
        this.node.detachAllChildren();
        greedy();
        this.optimizedChunkSpatial = (Node) GeometryBatchFactory.optimize(this.node);
    }
    
    public void detach() {
        parent.detachChild(optimizedChunkSpatial);
        voxelSystem.removePhysicsSpace(rigidBodyControl);
        attached = false;
    }
    
    public void attach() {
        parent.attachChild(optimizedChunkSpatial);
        rigidBodyUpdate();
        attached = true;
    }

    public boolean isAttached() {
        return attached;
    }
    
    private void rigidBodyUpdate() {
        if(rigidBodyControl != null)
            voxelSystem.removePhysicsSpace(rigidBodyControl);
        rigidBodyControl = new RigidBodyControl(0);
        rigidBodyControl.setCollisionShape(CollisionShapeFactory.createMeshShape(optimizedChunkSpatial));
        optimizedChunkSpatial.removeControl(RigidBodyControl.class);
        optimizedChunkSpatial.addControl(rigidBodyControl);
        voxelSystem.addPhysicsSpace(rigidBodyControl);
    }
    
    private void greedy() {

        /*
         * These are just working variables for the algorithm - almost all taken 
         * directly from Mikola Lysenko's javascript implementation.
         */
        int i, j, k, l, w, h, u, v, n, side = 0;
        
        final int[] x = new int []{0,0,0};
        final int[] q = new int []{0,0,0};
        final int[] du = new int[]{0,0,0}; 
        final int[] dv = new int[]{0,0,0};         
        
        /*
         * We create a mask - this will contain the groups of matching voxel faces 
         * as we proceed through the chunk in 6 directions - once for each face.
         */
        final VoxelFace[] mask = new VoxelFace [chunkWidth * chunkHeight];
        
        /*
         * These are just working variables to hold two faces during comparison.
         */
        VoxelFace voxelFace, voxelFace1;

        /**
         * We start with the lesser-spotted boolean for-loop (also known as the old flippy floppy). 
         * 
         * The variable backFace will be TRUE on the first iteration and FALSE on the second - this allows 
         * us to track which direction the indices should run during creation of the quad.
         * 
         * This loop runs twice, and the inner loop 3 times - totally 6 iterations - one for each 
         * voxel face.
         */
        for (boolean backFace = true, b = false; b != backFace; backFace = backFace && b, b = !b) { 

            /*
             * We sweep over the 3 dimensions - most of what follows is well described by Mikola Lysenko 
             * in his post - and is ported from his Javascript implementation.  Where this implementation 
             * diverges, I've added commentary.
             */
            for(int d = 0; d < 3; d++) {

                u = (d + 1) % 3; 
                v = (d + 2) % 3;

                x[0] = 0;
                x[1] = 0;
                x[2] = 0;

                q[0] = 0;
                q[1] = 0;
                q[2] = 0;
                q[d] = 1;

                /*
                 * Here we're keeping track of the side that we're meshing.
                 */
                if (d == 0)      { side = backFace ? WEST   : EAST;  }
                else if (d == 1) { side = backFace ? BOTTOM : TOP;   }
                else if (d == 2) { side = backFace ? SOUTH  : NORTH; }                

                /*
                 * We move through the dimension from front to back
                 */            
                for(x[d] = -1; x[d] < chunkWidth;) {

                    /*
                     * -------------------------------------------------------------------
                     *   We compute the mask
                     * -------------------------------------------------------------------
                     */
                    n = 0;

                    for(x[v] = 0; x[v] < chunkHeight; x[v]++) {

                        for(x[u] = 0; x[u] < chunkWidth; x[u]++) {

                            /*
                             * Here we retrieve two voxel faces for comparison.
                             */
                            voxelFace  = (x[d] >= 0 )             ? getVoxelFace(x[0], x[1], x[2], side)                      : null;
                            voxelFace1 = (x[d] < chunkWidth - 1) ? getVoxelFace(x[0] + q[0], x[1] + q[1], x[2] + q[2], side) : null;

                            /*
                             * Note that we're using the equals function in the voxel face class here, which lets the faces 
                             * be compared based on any number of attributes.
                             * 
                             * Also, we choose the face to add to the mask depending on whether we're moving through on a backface or not.
                             */
                            mask[n++] = ((voxelFace != null && voxelFace1 != null && voxelFace.equals(voxelFace1))) 
                                        ? null 
                                        : backFace ? voxelFace1 : voxelFace;
                        }
                    }

                    x[d]++;

                    /*
                     * Now we generate the mesh for the mask
                     */
                    n = 0;

                    for(j = 0; j < chunkHeight; j++) {

                        for(i = 0; i < chunkWidth;) {

                            if(mask[n] != null) {

                                /*
                                 * We compute the width
                                 */
                                for(w = 1; i + w < chunkWidth && mask[n + w] != null && mask[n + w].equals(mask[n]); w++) {}

                                /*
                                 * Then we compute height
                                 */
                                boolean done = false;

                                for(h = 1; j + h < chunkHeight; h++) {

                                    for(k = 0; k < w; k++) {

                                        if(mask[n + k + h * chunkWidth] == null || !mask[n + k + h * chunkWidth].equals(mask[n])) { done = true; break; }
                                    }

                                    if(done) { break; }
                                }

                                /*
                                 * Here we check the "transparent" attribute in the VoxelFace class to ensure that we don't mesh 
                                 * any culled faces.
                                 */
                                if (!mask[n].transparent) {
                                    /*
                                     * Add quad
                                     */
                                    x[u] = i;  
                                    x[v] = j;

                                    du[0] = 0;
                                    du[1] = 0;
                                    du[2] = 0;
                                    du[u] = w;

                                    dv[0] = 0;
                                    dv[1] = 0;
                                    dv[2] = 0;
                                    dv[v] = h;

                                    /*
                                     * And here we call the quad function in order to render a merged quad in the scene.
                                     * 
                                     * We pass mask[n] to the function, which is an instance of the VoxelFace class containing 
                                     * all the attributes of the face - which allows for variables to be passed to shaders - for 
                                     * example lighting values used to create ambient occlusion.
                                     */
                                    quad(new Vector3f(x[0],                 x[1],                   x[2]), 
                                         new Vector3f(x[0] + du[0],         x[1] + du[1],           x[2] + du[2]), 
                                         new Vector3f(x[0] + du[0] + dv[0], x[1] + du[1] + dv[1],   x[2] + du[2] + dv[2]), 
                                         new Vector3f(x[0] + dv[0],         x[1] + dv[1],           x[2] + dv[2]), 
                                         w,
                                         h,
                                         mask[n],
                                         backFace);
                                }

                                /*
                                 * We zero out the mask
                                 */
                                for(l = 0; l < h; ++l) {

                                    for(k = 0; k < w; ++k) { mask[n + k + l * chunkWidth] = null; }
                                }

                                /*
                                 * And then finally increment the counters and continue
                                 */
                                i += w; 
                                n += w;

                            } else {

                              i++;
                              n++;
                            }
                        }
                    } 
                }
            }        
        }
    }
    
    private VoxelFace getVoxelFace(final int x, final int y, final int z, final int side) {
        if(voxels[x][y][z].voxelType != 0 && faceCullingEnable) {
            if(side == 0) {
                if(z == 0)
                    neighborVoxel = voxelSystem.getVoxel(translation, 0, x, y, chunkWidth-1);
                else
                    neighborVoxel = voxels[x][y][z-1];
                if(neighborVoxel != null && neighborVoxel.voxelType == 0) {
                    voxels[x][y][z].transparent = false;
                    voxels[x][y][z].light = neighborVoxel.light;
                }
                else
                    voxels[x][y][z].transparent = true;
            } else if(side == 1) {
                if(z == chunkWidth-1)
                    neighborVoxel = voxelSystem.getVoxel(translation, 1, x, y, 0);
                else
                    neighborVoxel = voxels[x][y][z+1];
                if(neighborVoxel != null && neighborVoxel.voxelType == 0) {
                    voxels[x][y][z].transparent = false;
                    voxels[x][y][z].light = neighborVoxel.light;
                }
                else
                    voxels[x][y][z].transparent = true;
            } else if(side == 2) {
                if(x == chunkWidth-1)
                    neighborVoxel = voxelSystem.getVoxel(translation, 2, 0, y, z);
                else
                    neighborVoxel = voxels[x+1][y][z];
                if(neighborVoxel != null && neighborVoxel.voxelType == 0) {
                    voxels[x][y][z].transparent = false;
                    voxels[x][y][z].light = neighborVoxel.light;
                }
                else
                    voxels[x][y][z].transparent = true;
            } else if(side == 3) {
                if(x == 0)
                    neighborVoxel = voxelSystem.getVoxel(translation, 3, chunkWidth-1, y, z);
                else
                    neighborVoxel = voxels[x-1][y][z];
                if(neighborVoxel != null && neighborVoxel.voxelType == 0) {
                    voxels[x][y][z].transparent = false;
                    voxels[x][y][z].light = neighborVoxel.light;
                }
                else
                    voxels[x][y][z].transparent = true;
            } else if(side == 4) {
                if(y == 0)
                    neighborVoxel = voxelSystem.getVoxel(translation, 4, x, chunkHeight-1, z);
                else
                    neighborVoxel = voxels[x][y-1][z];
                if(neighborVoxel != null && neighborVoxel.voxelType == 0) {
                    voxels[x][y][z].transparent = false;
                    voxels[x][y][z].light = neighborVoxel.light;
                }
                else
                    voxels[x][y][z].transparent = true;
            } else if(side == 5) {
                if(y == chunkHeight-1)
                    neighborVoxel = voxelSystem.getVoxel(translation, 5, x, 0, z);
                else
                    neighborVoxel = voxels[x][y+1][z];
                if(neighborVoxel != null && neighborVoxel.voxelType == 0) {
                    voxels[x][y][z].transparent = false;
                    voxels[x][y][z].light = neighborVoxel.light;
                }
                else
                    voxels[x][y][z].transparent = true;
            }
            voxels[x][y][z].type = voxelSystem.getVoxelFaces(voxels[x][y][z].voxelType, side);
            return voxels[x][y][z];
        }
        return null;
    }
    
    /**
     * This function renders a single quad in the scene. This quad may represent many adjacent voxel 
     * faces - so in order to create the illusion of many faces, you might consider using a tiling 
     * function in your voxel shader. For this reason I've included the quad width and height as parameters.
     * 
     * For example, if your texture coordinates for a single voxel face were 0 - 1 on a given axis, they should now 
     * be 0 - width or 0 - height. Then you can calculate the correct texture coordinate in your fragement 
     * shader using coord.xy = fract(coord.xy). 
     * 
     * 
     * @param bottomLeft
     * @param topLeft
     * @param topRight
     * @param bottomRight
     * @param width
     * @param height
     * @param face
     * @param backFace 
     */
    private void quad(final Vector3f bottomLeft, 
              final Vector3f topLeft, 
              final Vector3f topRight, 
              final Vector3f bottomRight,
              final int width,
              final int height,
              final VoxelFace face, 
              final boolean backFace) {
        
        if (!(face.type == 0)) {
 
        final Vector3f [] localVertices = new Vector3f[4];

        localVertices[2] = topLeft.multLocal(voxelSize).addLocal(translation);
        localVertices[3] = topRight.multLocal(voxelSize).addLocal(translation);
        localVertices[0] = bottomLeft.multLocal(voxelSize).addLocal(translation);
        localVertices[1] = bottomRight.multLocal(voxelSize).addLocal(translation);
        
        final int [] localIndexes = backFace ? new int[] { 2,0,1, 1,3,2 } : new int[]{ 2,3,1, 1,0,2 };
        Vector2f [] texCoord = new Vector2f[4];
        texCoord[0] = new Vector2f(0,0);
        texCoord[1] = new Vector2f(1,0);
        texCoord[2] = new Vector2f(0,1);
        texCoord[3] = new Vector2f(1,1);
        
        Mesh mesh = new Mesh();
        
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(localVertices));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
        mesh.setBuffer(VertexBuffer.Type.Index,    3, BufferUtils.createIntBuffer(localIndexes));
        mesh.updateBound();
        Geometry geo = new Geometry("mesh", mesh);
        Material mat = getMaterial(face.type, face.light);
        geo.setMaterial(mat);
        mesh.scaleTextureCoordinates(new Vector2f(height,width));
        this.node.attachChild(geo);
    }}
    
    private Material getMaterial(int type, int light) {
        if(light >= 16)
            light = 0;
        return voxelSystem.getMaterial(type, light);
    }
    
    @Override
    public int getHeight(int x, int z) {
        
        for(int height = 0; height < voxels[x].length; ++height)
            if((voxels[x][height][z]).voxelType == 0)
                return height;
        return this.chunkHeight;
    }

    @Override
    public int getBlock(int x, int y, int z) {
        try {
            return voxels[x][y][z].voxelType;
        } catch(ArrayIndexOutOfBoundsException e) { return 0; }
    }

    @Override
    public boolean modified() {
        return modified;
    }
    
}
