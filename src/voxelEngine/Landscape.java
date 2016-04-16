package voxelEngine;

import voxelEngine.noise.VoxelNoise;
import voxelEngine.worldMovement.Enlargement;
import voxelEngine.worldMovement.MoveCenter;
import voxelEngine.worldMovement.Narrowing;
import voxelEngine.worldMovement.WorldMovement;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author francois
 */
public class Landscape implements VoxelSystem {
    SimpleApplication app;
    BulletAppState bulletAppState;
    
    int targetedDisplayRadius; // in chunk
    static int systemHeight; // in chunk
    static int voxelSize;    // in world unit
    static int chunkWidth;   // in voxel
    static int chunkHeight;  // in voxel
    private Vector3f center = new Vector3f(0,0,0);
    private Vector3f playerLocation = new Vector3f(0,0,0);
    private Node node = new Node();
    private final Vector3f north;
    private final Vector3f south;
    private final Vector3f east;
    private final Vector3f west;
    
    private final List<List<Chunk[]>> quadrantI;
    private final List<List<Chunk[]>> quadrantII;
    private final List<List<Chunk[]>> quadrantIII;
    private final List<List<Chunk[]>> quadrantIV;
    
    VoxelMaterial[][] MATERIALS;
    VoxelTypes voxelTypes;
    private boolean faceCullingEnable;
    private Queue<PropagationUpdate> updates = new ConcurrentLinkedQueue<>();
    
    private Vector3f translationCp = new Vector3f();
    
    private int detachX = 0;
    private int detachZ = 0;
    private int attachX = 0;
    private int attachZ = 0;
    private int precColumnIndex = -1;
    private Queue<WorldMovement> worldMoves = new LinkedList<>();
    private int displayRadius;
    private final int chunkLoadByFrame;
    private VoxelNoise noise;
    
    public Landscape(SimpleApplication app, BulletAppState bulletAppState, int targetDisplayRadius, int initBeforeStartDisplayRadius, 
            int chunkLoadByFrame, int systemHeight, int voxelSize, int chunkWidth, int chunkHeight, 
            boolean enableFaceCulling, VoxelNoise noise, FaceTypes fTypes, VoxelTypes vTypes) {
        this.app = app;
        this.bulletAppState = bulletAppState;
        this.targetedDisplayRadius = targetDisplayRadius;
        this.chunkLoadByFrame = chunkLoadByFrame;
        this.systemHeight = systemHeight;
        this.voxelSize = voxelSize;
        this.chunkWidth = chunkWidth;
        this.north = new Vector3f(0, -chunkWidth, 0);
        this.south = new Vector3f(0, chunkWidth, 0);
        this.east = new Vector3f(-chunkWidth, 0, 0);
        this.west = new Vector3f(chunkWidth, 0, 0);
        this.chunkHeight = chunkHeight;
        this.faceCullingEnable = enableFaceCulling;
        quadrantI = new LinkedList<List<Chunk[]>>();
        quadrantII = new LinkedList<List<Chunk[]>>();
        quadrantIII = new LinkedList<List<Chunk[]>>();
        quadrantIV = new LinkedList<List<Chunk[]>>();
        this.noise = noise;
        this.voxelTypes = vTypes;
        
        // instanciation of the materials
        String[][] textures = fTypes.getTextures();
        MATERIALS = new VoxelMaterial[textures.length][16];
        for(int face = 0; face < MATERIALS.length; ++face)
            if(textures[face] != null)
                for(int light = 0; light < 16; ++light)
                    MATERIALS[face][light] = new VoxelMaterial(app.getAssetManager(), textures[face][light]);
        
        app.getRootNode().attachChild(this.node);
        app.getStateManager().attach(this);
        initBeforeStart(initBeforeStartDisplayRadius);
    }
    
    @Override
    public Node getNode() {
        return node;
    }
    
    @Override
    public int getSystemHeight() {
        return systemHeight;
    }
    
    @Override
    public void putBlock(Vector3f location, int type) {
        Vector3f locationInChunk = getBlockLocationInChunk(location);
        Chunk chunk = getChunk(location);
        if(chunk == null)
            return;
        chunk.addBlock((int)locationInChunk.x, 
                (int)locationInChunk.z, (int)locationInChunk.y, type);
    }

    @Override
    public void removeBlock(Vector3f location) {
        Vector3f locationInChunk = getBlockLocationInChunk(location);
        getChunk(location).removeBlock((int)locationInChunk.x, 
                (int)locationInChunk.z, (int)locationInChunk.y);
    }
    
    @Override
    public void addPhysicsSpace(RigidBodyControl rigidBodyControl) {
        this.bulletAppState.getPhysicsSpace().add(rigidBodyControl);
    }

    @Override
    public void removePhysicsSpace(RigidBodyControl rigidBodyControl) {
        this.bulletAppState.getPhysicsSpace().remove(rigidBodyControl);
    }

    @Override
    public int getVoxelFaces(int voxelType, int side) {
        return this.voxelTypes.getFaces()[voxelType][side];
    }

    @Override
    public Material getMaterial(int type, int light) {
        return this.MATERIALS[type][light];
    }

    @Override
    public int getHeight(int x, int z) {
        int blockX = Math.abs(x%chunkWidth);
        int blockZ = Math.abs(z%chunkWidth);
        if(x < 0) {
            x-=chunkWidth;
            blockX = invertChunkWidth(blockX);
        }
        if(z < 0) {
            z-=chunkWidth;
            blockZ = invertChunkWidth(blockZ);
        }
        for(int iChunk = systemHeight-1; iChunk >= 0; --iChunk) {
            int height = getChunk(x/chunkWidth, z/chunkWidth, iChunk).getHeight(blockX, blockZ);
            if(height != 0)
                return height+(chunkHeight*iChunk); 
        }
        return 0;
    }
    
    @Override
    public void setPlayerLocation(Vector3f location) {
        playerLocation.x = location.x/16;
        playerLocation.y = location.y/16;
        playerLocation.z = location.z/16;
    }
    
    @Override
    public Integer getBlock(Vector3f location) {
        Vector3f locationInChunk = getBlockLocationInChunk(location);
        Chunk chunk = getChunk(location);
        if(chunk == null)
            return null;
        return chunk.getBlock((int)locationInChunk.x, 
                (int)locationInChunk.z, (int)locationInChunk.y);
    }
    
    @Override
    public Vector3f getPointedBlockLocation(Vector3f collisionContactPoint, boolean getNeighborLocation) {
        incrementNegativeLocation(collisionContactPoint);
        Vector3f collisionLocation = Util.compensateFloatRoundingErrors(collisionContactPoint);
        Vector3f blockLocation = new Vector3f(
                (int) (collisionLocation.getX() / 1),
                (int) (collisionLocation.getY() / 1),
                (int) (collisionLocation.getZ() / 1));
        Integer blockType = getBlock(blockLocation);
        if(blockType == null) return null;
        if((blockType != 0) == getNeighborLocation) {
            if((collisionLocation.getX() % 1) == 0) {
                if(collisionLocation.getX() >=0)
                    blockLocation.subtractLocal(1, 0, 0);
                else
                    blockLocation.addLocal(1, 0, 0);
            }
            else if((collisionLocation.getZ() % 1) == 0) {
                if(collisionLocation.getZ() >=0)
                    blockLocation.subtractLocal(0, 0, 1);
                else
                    blockLocation.addLocal(0, 0, 1);
            }
            else if((collisionLocation.getY() % 1) == 0) {
                if(collisionLocation.getY() >=0)
                    blockLocation.subtractLocal(0, 1, 0);
                else
                    blockLocation.addLocal(0, 1, 0);
            }
        }
        return blockLocation;
    }
    
    @Override
    public void update(float tpf) {
        int i;
        for(i = 0; i < 18; ++i) // 2 levels at a time (18 == 2 * 9)
            if(!internalUpdatePropagation())
                break;
        if(i == 0) {
            // if there is no propagation to do we can do the worldMovement things
            // the 2 at the same time is to hard
            if(worldMoves.peek() != null) {
                if(worldMoves.peek() instanceof MoveCenter)
                    handleMoveCenter((MoveCenter) worldMoves.peek());
                else if(worldMoves.peek() instanceof Enlargement)
                    handleEnlargement((Enlargement) worldMoves.peek());
                else if(worldMoves.peek() instanceof Narrowing)
                    handleNarrowing((Narrowing) worldMoves.peek());
            }
            addEnlargementIfNecessary();
            addMoveCenterIfNecessary();
        }
    }

    public void destroy() {

    }
    
    private void initBeforeStart(int radius) {
        for (int x = -radius; x < radius; ++x) {
            for (int z = -radius; z < radius; ++z) {
                for (int y = systemHeight-1; y >=0; --y) {
                    addChunk(x, z, y, null);
                }
            }
        }
        this.displayRadius = radius;
    }
    
    private boolean enlarge(Enlargement move, int columnIndex) {
        int side = columnIndex / (((displayRadius+1)*2)-1);
        int coIndexInSide = columnIndex % (((displayRadius+1)*2)-1);
        int x = 0;
        int z = 0;
        switch(side) {
            case 0:
                z = displayRadius+(int)center.y;
                x = coIndexInSide-displayRadius+(int)center.x;
                break; 
            case 1:
                x = displayRadius+(int)center.x;
                z = coIndexInSide-(displayRadius+1)+(int)center.y;
                break;
            case 2:
                z = -(displayRadius+1)+(int)center.y;
                x = coIndexInSide-(displayRadius+1)+(int)center.x;
                break;
            case 3:
                x = -(displayRadius+1)+(int)center.x;
                z = coIndexInSide-displayRadius+(int)center.y;
                break;
        }
        int y;
        for(y = move.iy; y >= 0 && move.iy != y+chunkLoadByFrame; --y)
            addChunk(x, z, y, null);
        if(y < 0) {
            ++move.iColumn;
            move.iy = systemHeight-1;;
        }
        else
            move.iy = y;
        return(x == targetedDisplayRadius*2 && z == targetedDisplayRadius*2);
    }
    
    private void addChunk(int x, int y, int z, Chunk recycledChunk) {
        List<List<Chunk[]>> quadrant = getQuadrant(x, y);
        int xAbs = Math.abs(x);
        int yAbs = Math.abs(y);
        while(xAbs >= quadrant.size())        quadrant.add(new LinkedList<Chunk[]>());
        while(yAbs >= quadrant.get(xAbs).size()) quadrant.get(xAbs).add(new Chunk[systemHeight]);
        if(z < systemHeight) {
            Vector3f location = new Vector3f(x*chunkWidth, z*chunkHeight, y*chunkWidth);
            if(recycledChunk == null)
                recycledChunk = new GreedyMeshChunk(this, node, location, 
                        voxelSize, chunkWidth, chunkHeight, faceCullingEnable);
            else {
                recycledChunk.setTranslation(location);
                recycledChunk.lightOff();
            }
            recycledChunk.init(noise);
            quadrant.get(xAbs).get(yAbs)[z] = recycledChunk;
            for(Chunk neighbor : getNeighbors(location))
                if(neighbor != null && neighbor.isAttached())
                    neighbor.refreshDisplay();
        }
    }
    
    private List<Chunk> getNeighbors(Vector3f location) {
        List<Chunk> neighbors = new ArrayList<>();
        Vector3f neighborLocation = new Vector3f(location.x, location.y, location.z);
        //side == 0
        neighborLocation.z = location.z - chunkWidth;
        neighbors.add(getChunk(neighborLocation));
        //side == 1
        neighborLocation.z = location.z + chunkWidth;
        neighbors.add(getChunk(neighborLocation));
        //side == 2
        neighborLocation.z = location.z;
        neighborLocation.x = location.x + chunkWidth;
        neighbors.add(getChunk(neighborLocation));
        //side == 3
        neighborLocation.x = location.x - chunkWidth;
        neighbors.add(getChunk(neighborLocation));
        //side == 4
        neighborLocation.x = location.x;
        neighborLocation.y = location.y - chunkHeight;
        neighbors.add(getChunk(neighborLocation));
        //side == 5
        neighborLocation.y = location.y + chunkHeight;
        neighbors.add(getChunk(neighborLocation));
        return neighbors;
    }
    
    private Vector3f getNeighborLocation(Vector3f location, int side) {
        Vector3f neighborLocation = new Vector3f(location.x, location.y, location.z);
        if(side == 0)
            neighborLocation.z = location.z - chunkWidth;
        else if(side == 1)
            neighborLocation.z = location.z + chunkWidth;
        else if(side == 2)
            neighborLocation.x = location.x + chunkWidth;
        else if(side == 3)
            neighborLocation.x = location.x - chunkWidth;
        else if(side == 4)
            neighborLocation.y = location.y - chunkHeight;
        else if(side == 5)
            neighborLocation.y = location.y + chunkHeight;
        return neighborLocation;
    }
    
    private int oppositeSide(int side) {
        if(side % 2 == 0)
            return side+1;
        else
            return side-1;
    }
   
    
    @Override
    public void notifyUpdate(Vector3f translation, int side, boolean refreshDisplay) {
        Vector3f neighborLocation = getNeighborLocation(translation, side);
        Chunk chunk = getChunk(neighborLocation);
        if(chunk != null)
            chunk.externalUpdate(oppositeSide(side), refreshDisplay);
    }
    
    @Override
    public void refreshDisplay(Vector3f translation, int side) {
        Vector3f neighborLocation = getNeighborLocation(translation, side);
        Chunk chunk = getChunk(neighborLocation);
        if(chunk != null && chunk.isAttached())
            chunk.refreshDisplay();
    }
    
    private Vector3f getNeighborLocation(Vector3f location, int x, int z) {
        Vector3f neighborLocation;
        neighborLocation = new Vector3f(location.x, location.y, location.z);
        
        if(x == 0)
            neighborLocation.x = location.x - chunkWidth;
        else if(x == 2)
            neighborLocation.x = location.x + chunkWidth;
        if(z == 0)
            neighborLocation.z = location.z - chunkWidth;
        else if(z == 2)
            neighborLocation.z = location.z + chunkWidth;
        return neighborLocation;
    }
    
    private void lightOff(Vector3f translation) {
        translationCp.x = translation.x;
        translationCp.z = translation.z;
        int startHeight = (((int)translation.y)/16)+1;
        if(startHeight == systemHeight)
            --startHeight;
        for(int y = startHeight; y >= 0; --y) {
            translationCp.y = y*chunkHeight;
            for(int x = 0; x < 3; ++x) {
                for(int z = 0; z < 3; ++z) {
                    Vector3f neighborLocation = getNeighborLocation(translationCp, x, z);
                    Chunk chunk = getChunk(neighborLocation);
                    if(chunk != null) {
                        chunk.lightOff();
                    }
                }
            }
        }
    }
    
    @Override
    public void externalPropagation(Vector3f translation, int chunkSide, int x, int y, int z, 
            int pValue, int voxelSide) {
        Vector3f neighborLocation = getNeighborLocation(translation, chunkSide);
        Chunk chunk = getChunk(neighborLocation);
        if(chunk != null)
            chunk.propagate(x, y, z, pValue, voxelSide);
    }
    
    @Override
    public void updatePropagation(Vector3f translation) {
        for(PropagationUpdate update : updates)
            if(update.originTranslation.equals(translation) && !update.isBegan()) {
                updates.remove(update);
            }
        updates.add(new PropagationUpdate(new Vector3f(translation.x, translation.y, translation.z)));
    }
    
    // return false if no update to do
    private boolean internalUpdatePropagation() {
        Vector3f location = null;
        if(updates.peek() != null) {
            if(!updates.peek().isBegan()) {
                updates.peek().began();
                lightOff(updates.peek().originTranslation);
            }
            location = updates.peek().nextTranslation();
            if (location == null) {
                updates.poll();
                if (updates.peek() != null) {
                    updates.peek().began();
                    lightOff(updates.peek().originTranslation);
                    location = updates.peek().nextTranslation();
                }
            }
        }
        else
            return false;
        if (location != null) {
            Chunk chunk = getChunk(location);
            if (chunk != null && chunk.lightTurnedOff(false))
                chunk.updatePropagation(false);
            if (updates.peek().lastLocation(location))
                refreshDisplay();
        }
        return true;
    }
    
    private void refreshDisplay() {
        translationCp.x = updates.peek().originTranslation.x;
        translationCp.z = updates.peek().originTranslation.z;
        int startHeight = (((int) updates.peek().originTranslation.y) / 16) + 1;
        if (startHeight == systemHeight) {
            --startHeight;
        }
        for (int y = startHeight; y >= 0; --y) {
            translationCp.y = y * chunkHeight;
            for (int x = 0; x < 3; ++x) {
                for (int z = 0; z < 3; ++z) {
                    Vector3f neighborLocation = getNeighborLocation(translationCp, x, z);
                    Chunk neighbor = getChunk(neighborLocation);
                    if (neighbor != null && neighbor.isAttached() && neighbor.lightTurnedOff(true))
                        neighbor.refreshDisplay();
                }
            }
        }
    }
    
    private void handleMoveCenter(MoveCenter moveCenter) {
        boolean stop = false;
        for(int i = 0; i < chunkLoadByFrame && !stop; ++i) {
            moveCenter(moveCenter.translation, moveCenter.iColumn, moveCenter.iy);
            if(moveCenter.iy == 0) {
                ++moveCenter.iColumn;
                moveCenter.iy = systemHeight-1;
            }
            else
                --moveCenter.iy;
            if (moveCenter.iColumn == (displayRadius)*2) {
                worldMoves.poll();
                stop = true;
            }
        }
    }
    
    private void handleEnlargement(Enlargement enlargement) {
        enlarge((Enlargement) worldMoves.peek(), enlargement.iColumn);
        if(enlargement.iColumn >= ((displayRadius+1)*8)-4) {
            worldMoves.poll();
            ++displayRadius;
        }
    }
    
    private void handleNarrowing(Narrowing narrowing) {
        
    }
    
    private void addMoveCenterIfNecessary() {
        if(!containsMoveCenter((LinkedList)worldMoves)) {
            Vector3f translation = getMoveCenter(center, playerLocation);
            if(translation != null) {
                WorldMovement move = new MoveCenter(translation);
                ((MoveCenter)move).iy = systemHeight-1;
                worldMoves.add(move);
            }
        }
    }
    
    private void addEnlargementIfNecessary() {
        if(!containsEnlargement((LinkedList)worldMoves)) {
            if(this.displayRadius < this.targetedDisplayRadius) {
                WorldMovement move = new Enlargement();
                ((Enlargement)move).iy = systemHeight-1;
                worldMoves.add(move);
            }
        }
    }
    
    private Vector3f getMoveCenter(Vector3f worldCenter, Vector3f playerLocation) {
        int eastDistance = 0;
        int westDistance = 0;
        int northDistance = 0;
        int southDistance = 0;
        if(worldCenter.x > playerLocation.x + 1) {
            eastDistance = (int)(worldCenter.x-(playerLocation.x + 1));
        } if(worldCenter.x < playerLocation.x - 1) {
            westDistance = (int)((playerLocation.x - 1) - worldCenter.x);
        } if(worldCenter.y > playerLocation.z + 1) {
            northDistance = (int)(worldCenter.y - (playerLocation.z + 1));
        } if(worldCenter.y < playerLocation.z - 1) {
            southDistance = (int)((playerLocation.z - 1) - worldCenter.y);
        }
        
        if(eastDistance > westDistance && eastDistance > northDistance && eastDistance > southDistance) {
            return east;
        } else if(westDistance > eastDistance && westDistance > northDistance && westDistance > southDistance) {
            return west;
        } else if(northDistance > eastDistance && northDistance > westDistance && northDistance > southDistance) {
            return north;
        } else if(southDistance > eastDistance && southDistance > westDistance && southDistance > northDistance) {
            return south;
        } else if(eastDistance > 0) {
            return east;
        } else if(westDistance > 0) {
            return west;
        } else if(northDistance > 0) {
            return north;
        } else if(southDistance > 0) {
            return south;
        }
        return null;
    }
    
    
    @Override
    public int getLightVal(Vector3f translation, int side, int x, int y, int z) {
        Vector3f neighborLocation = getNeighborLocation(translation, side);
        Chunk chunk = getChunk(neighborLocation);
        if(chunk == null)
            return 16;
        return chunk.getLightVal(x, y, z);
    }
    
    @Override
    public VoxelFace getVoxel(Vector3f translation, int side, int x, int y, int z) {
        Vector3f neighborLocation = getNeighborLocation(translation, side);
        Chunk chunk = getChunk(neighborLocation);
        if(chunk == null)
            return null;
        return chunk.getVoxel(x, y, z);
    }
    
    private Chunk getChunk(int x, int y, int z) {
        List<List<Chunk[]>> quadrant = getQuadrant(x, y);
        x = Math.abs(x);
        y = Math.abs(y);
        if(x >= quadrant.size()) return null;
        if(y >= quadrant.get(x).size()) return null;
        if(z >= quadrant.get(x).get(y).length) return null;
        if(z < 0) return null;
        return quadrant.get(x).get(y)[z];
    }
    
    private Chunk getChunk(Vector3f location) {
        Vector3f locationTmp = new Vector3f(location);
        if(locationTmp.getX() % chunkWidth != 0 && locationTmp.getX() < 0)
            locationTmp.addLocal(-chunkWidth,0,0);
        if(locationTmp.getZ() % chunkWidth != 0 && locationTmp.getZ() < 0)
            locationTmp.addLocal(0,0,-chunkWidth);
        return getChunk(((int)locationTmp.getX())/chunkWidth, ((int)locationTmp.getZ())/chunkWidth, ((int)locationTmp.getY())/chunkHeight);
    }
    
    private boolean removeChunk(int x, int y, int z) {
        List<List<Chunk[]>> quadrant = getQuadrant(x, y);
        x = Math.abs(x);
        y = Math.abs(y);
        if(x >= quadrant.size()) return false;
        if(y >= quadrant.get(x).size()) return false;
        if(z >= quadrant.get(x).get(y).length) return false;
        quadrant.get(x).get(y)[z] = null;
        return true;
    }
    
    private boolean removeChunk(Vector3f location) {
        Vector3f locationTmp = new Vector3f(location);
        if(locationTmp.getX() % chunkWidth != 0 && locationTmp.getX() < 0)
            locationTmp.addLocal(-chunkWidth,0,0);
        if(locationTmp.getZ() % chunkWidth != 0 && locationTmp.getZ() < 0)
            locationTmp.addLocal(0,0,-chunkWidth);
        return removeChunk(((int)locationTmp.getX())/chunkWidth, ((int)locationTmp.getZ())/chunkWidth, ((int)locationTmp.getY())/chunkHeight);
    }
    
    private List<List<Chunk[]>> getQuadrant(int x, int y) {
                                                // (x,y)
        if(x >= 0 && y >= 0) return quadrantI;  // (+,+)
        if(x < 0 && y >= 0)  return quadrantII; // (-,+)
        if(x < 0 && y < 0)   return quadrantIII;// (-,-)
        else                 return quadrantIV; // (+,-)
    }

    private void moveCenter(Vector3f localTranslation, int columnIndex, int y) {
        if(precColumnIndex != columnIndex) {
            precColumnIndex = columnIndex;
            if (localTranslation.x > 0) {
                int iy = columnIndex-displayRadius;
                detachX = (int) (center.getX() - displayRadius);
                detachZ = (int) center.getY() + iy;
                attachX = (int) (center.getX() + displayRadius);
                attachZ = (int) center.getY() + iy;

                if(columnIndex+1 >= displayRadius*2)
                    center.addLocal(new Vector3f(1, 0, 0));
            } else if (localTranslation.y > 0) {
                int ix = columnIndex-displayRadius;
                detachX = (int) center.getX() + ix;
                detachZ = (int) center.getY() - displayRadius;
                attachX = (int) center.getX() + ix;
                attachZ = (int) center.getY() + displayRadius;

                if(columnIndex+1 >= displayRadius*2)
                    center.addLocal(new Vector3f(0, 1, 0));
            } else if (localTranslation.x < 0) {
                int iy = columnIndex-displayRadius;
                detachX = (int) (center.getX() + displayRadius - 1);
                detachZ = (int) center.getY() + iy;
                attachX = (int) (center.getX() - displayRadius - 1);
                attachZ = (int) center.getY() + iy;

                if(columnIndex+1 >= displayRadius*2)
                    center.addLocal(new Vector3f(-1, 0, 0));
            } else if (localTranslation.y < 0) {
                int ix = columnIndex-displayRadius;
                detachX = (int) center.getX() + ix;
                detachZ = (int) center.getY() + displayRadius - 1;
                attachX = (int) center.getX() + ix;
                attachZ = ((int) center.getY() - displayRadius) - 1;

                if(columnIndex+1 >= displayRadius*2)
                    center.addLocal(new Vector3f(0, -1, 0));
            }
        }
        Chunk chunkToAttach = getChunk(attachX, attachZ, y);
        Chunk chunkToDetach = getChunk(detachX, detachZ, y);
        chunkToDetach.detach();
        if (chunkToAttach != null) {
            chunkToAttach.attach();
            chunkToAttach.refreshDisplay();
            if (!chunkToDetach.modified())
                removeChunk(detachX, detachZ, y);
        } else {
            if (!chunkToDetach.modified()) {
                addChunk(attachX, attachZ, y, chunkToDetach);
                removeChunk(detachX, detachZ, y);
            } 
            else
                addChunk(attachX, attachZ, y, null);
        }
    }
    
    private void moveCenter(Vector3f translation, int columnIndex) {
        if (translation.x > 0) {
            int iy = columnIndex - (displayRadius);
            detachX = (int) (center.getX() - (displayRadius));
            detachZ = (int) center.getY() + iy;
            attachX = (int) (center.getX() + (displayRadius));
            attachZ = (int) center.getY() + iy;

            if (columnIndex + 1 >= (displayRadius) * 2)
                center.addLocal(new Vector3f(1, 0, 0));
        } else if (translation.y > 0) {
            int ix = columnIndex - (displayRadius);
            detachX = (int) center.getX() + ix;
            detachZ = (int) center.getY() - (displayRadius);
            attachX = (int) center.getX() + ix;
            attachZ = (int) center.getY() + (displayRadius);

            if (columnIndex + 1 >= (displayRadius) * 2)
                center.addLocal(new Vector3f(0, 1, 0));
        } else if (translation.x < 0) {
            int iy = columnIndex - (displayRadius);
            detachX = (int) (center.getX() + (displayRadius) - 1);
            detachZ = (int) center.getY() + iy;
            attachX = (int) (center.getX() - (displayRadius) - 1);
            attachZ = (int) center.getY() + iy;

            if (columnIndex + 1 >= (displayRadius) * 2)
                center.addLocal(new Vector3f(-1, 0, 0));
        } else if (translation.y < 0) {
            int ix = columnIndex - (displayRadius);
            detachX = (int) center.getX() + ix;
            detachZ = (int) center.getY() + (displayRadius) - 1;
            attachX = (int) center.getX() + ix;
            attachZ = ((int) center.getY() - (displayRadius)) - 1;

            if (columnIndex + 1 >= (displayRadius) * 2)
                center.addLocal(new Vector3f(0, -1, 0));
        }
        for (int y = systemHeight-1; y >= 0; --y) {
            Chunk chunkToAttach = getChunk(attachX, attachZ, y);
            Chunk chunkToDetach = getChunk(detachX, detachZ, y);
            chunkToDetach.detach();
            if (chunkToAttach != null) {
                chunkToAttach.attach();
                if(!chunkToDetach.modified())
                    removeChunk(detachX, detachZ, y);
            } else {
                if(!chunkToDetach.modified()) {
                    addChunk(attachX, attachZ, y, chunkToDetach);
                    removeChunk(detachX, detachZ, y);
                }
                else
                    addChunk(attachX, attachZ, y, null);
            }
        }
    }

    private Vector3f getBlockLocationInChunk(Vector3f location) {
        int blockX = Math.abs(((int)location.getX())%chunkWidth);
        int blockY = Math.abs(((int)location.getZ())%chunkWidth);
        if(blockX != 0 && location.getX() < 0) 
            blockX = invertChunkWidth(blockX);
        if(blockY != 0 && location.getZ() < 0)
            blockY = invertChunkWidth(blockY);
        return new Vector3f(blockX, blockY, Math.abs(((int)location.getY())%chunkHeight));
    }
    
    private int invertChunkWidth(int x) {
        return chunkWidth-x;
    }
    
    private void incrementNegativeLocation(Vector3f location) {
        if(location.x < 0)
            location.x = location.x-1;
        if(location.y < 0)
            location.y = location.y-1;
        if(location.z < 0)
            location.z = location.z-1;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        
    }

    @Override
    public boolean isInitialized() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setEnabled(boolean active) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void stateAttached(AppStateManager stateManager) {
        
    }

    @Override
    public void stateDetached(AppStateManager stateManager) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void render(RenderManager rm) {
        
    }

    @Override
    public void postRender() {
        
    }

    @Override
    public void cleanup() {
        
    }

    private boolean containsMoveCenter(LinkedList<WorldMovement> worldMoves) {
        for(WorldMovement move : worldMoves)
            if(move instanceof MoveCenter)
                return true;
        return false;
    }
    
    private boolean containsEnlargement(LinkedList<WorldMovement> worldMoves) {
        for(WorldMovement move : worldMoves)
            if(move instanceof Enlargement)
                return true;
        return false;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
