package voxelEngine;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

/**
 *
 * @author francois
 */
public class VoxelMaterial extends Material {

    public VoxelMaterial(AssetManager assetManager, String blockTextureFilePath) {
        super(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture texture = assetManager.loadTexture(blockTextureFilePath);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setWrap(WrapMode.Repeat);
        setTexture("ColorMap", texture);
        getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
    }
}
