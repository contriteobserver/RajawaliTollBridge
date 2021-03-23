package org.rajawali3d.tinyloaderbridge;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.methods.SpecularMethod;
import org.rajawali3d.materials.textures.AlphaMapTexture;
import org.rajawali3d.materials.textures.NormalMapTexture;
import org.rajawali3d.materials.textures.SpecularMapTexture;
import org.rajawali3d.materials.textures.Texture;

import java.io.InputStream;
import java.util.regex.Pattern;

public class Bridge {
    static final Pattern ext = Pattern.compile("(?<=.)\\.[^.]+$");

    long mHandle;
    AssetManager mAssetManager;
    String mObjAsset;
    String mMtlAsset;
    Object3D rootObj = new Object3D();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    Bridge(AssetManager assetManager, String objAsset, String mtlAsset) {
        mHandle = createReader();
        mAssetManager = assetManager;
        mObjAsset = objAsset;
        mMtlAsset = mtlAsset;
    }

    void parse()  throws Exception {
        parse(false);
    }

    void parse(boolean ThrowWarningExceptions) throws Exception {
        if(!parseAssets(mHandle, mAssetManager, mObjAsset, mMtlAsset)) {
            String errors = getErrors(mHandle);
            throw new Exception(errors.length() > 0 ? errors : "unknown parse error");
        }
        String warnings = getWarnings(mHandle);
        if(ThrowWarningExceptions && (warnings.length() > 0)) throw new Exception(warnings);
    }

    Object3D getParsedObj() throws Exception {
        float[] vertices = getVertices(mHandle);
        float[] normals = getNormals(mHandle);
        float[] colors = getColors(mHandle);
        float[] texcoords = getTexcoords(mHandle);

        int numShapes = getNumShapes(mHandle);
        for(int i=0; i<numShapes; i++) {
            Object3D child = new Object3D();
            child.setData(
                    demuxVertices(vertices, getVertexIndices(mHandle, i)),
                    demuxNormals(normals, getNormalIndices(mHandle, i)),
                    demuxTexcoords(texcoords, getTextureIndices(mHandle, i)),
                    demuxColors(colors, getVertexIndices(mHandle, i)),
                    demuxIndices(getVertexIndices(mHandle, i)),
                    true
            );
            child.setMaterial(getParsedMtl(i));
            rootObj.addChild(child);
        }
        return rootObj;
    }

    float[] demuxVertices(float[] vertices, int[] indices) {
        float[] demux = new float[indices.length * 3];
        for(int i=0; i<indices.length; i++) {
            demux[3*i+0] = vertices[3*indices[i]+0];
            demux[3*i+1] = vertices[3*indices[i]+1];
            demux[3*i+2] = vertices[3*indices[i]+2];
        }
        return demux;
    }

    float[] demuxNormals(float[] normals, int[] indices) {
        float[] demux = new float[indices.length * 3];
        for(int i=0; i<indices.length; i++) {
            demux[3*i+0] = normals[3*indices[i]+0];
            demux[3*i+1] = normals[3*indices[i]+1];
            demux[3*i+2] = normals[3*indices[i]+2];
        }
        return demux;
    }

    float[] demuxTexcoords(float[] texcoords, int[] indices) {
        float[] demux = new float[indices.length * 2];
        for(int i=0; i<indices.length; i++) {
            demux[2*i+0] = texcoords[2*indices[i]+0];
            demux[2*i+1] = texcoords[2*indices[i]+1];
        }
        return demux;
    }

    float[] demuxColors(float[] colors, int[] indices) {
        float[] demux = new float[indices.length * 3];
        for(int i=0; i<indices.length; i++) {
            demux[3*i+0] = colors[3*indices[i]+0];
            demux[3*i+1] = colors[3*indices[i]+1];
            demux[3*i+2] = colors[3*indices[i]+2];
        }
        return demux;
    }

    int[] demuxIndices(int[] indices) {
        int[] demux = new int[indices.length];
        for(int i=0; i<indices.length; i++) {
            demux[i] = i;
        }
        return demux;
    }

    Material getParsedMtl(int shapeIndex) throws Exception {
        int[] ids = getMaterialIds(mHandle, shapeIndex);
        int id = ids[0]; // FIXME: temporary hack

        float[] rgba = getSpecularColor(mHandle, id);
        int specularColor = Color.argb(
                Math.round(0xff*rgba[3]),
                Math.round(0xff*rgba[0]),
                Math.round(0xff*rgba[1]),
                Math.round(0xff*rgba[2])
        );

        String bump = getBumpTextureName(mHandle, id);
        String diffuse = getDiffuseTextureName(mHandle, id);
        String highlight = getHighlightTextureName(mHandle, id);

        Material material = new Material();
        material.setAmbientColor(getAmbientColor(mHandle, id));
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        if(diffuse.isEmpty()) {
            material.setColor(getDiffuseColor(mHandle, id));
        } else {
            InputStream is = mAssetManager.open(diffuse);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            String name = ext.matcher(diffuse).replaceAll("");
            Texture texture = new Texture(name, bitmap);

            material.addTexture(texture);
            material.setColorInfluence(0);
        }
        if(!highlight.isEmpty()) {
            InputStream is = mAssetManager.open(diffuse);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            String name = ext.matcher(diffuse).replaceAll("");
            SpecularMapTexture texture = new SpecularMapTexture(name, bitmap);

            material.addTexture(texture);
        }
        material.setSpecularMethod(new SpecularMethod.Phong(specularColor));
        if(!bump.isEmpty()) {
            InputStream is = mAssetManager.open(diffuse);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            String name = ext.matcher(diffuse).replaceAll("");
            NormalMapTexture texture = new NormalMapTexture(name, bitmap);

            material.addTexture(texture);
        }
        material.enableLighting(true);
        return material;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    static native long createReader();

    static native boolean parseAssets(long handle, AssetManager assetManager, String objAsset, String mtlAsset);
    static native String getErrors(long handle);
    static native String getWarnings(long handle);

    static native int getNumShapes(long handle);
    static native float[] getVertices(long handle);
    static native float[] getNormals(long handle);
    static native float[] getColors(long handle);
    static native float[] getTexcoords(long handle);
    static native int[] getMaterialIds(long handle, int shapeIndex);
    static native int[] getVertexIndices(long handle, int shapeIndex);
    static native int[] getTextureIndices(long handle, int shapeIndex);
    static native int[] getNormalIndices(long handle, int shapeIndex);

    static native String getAlphaTextureName(long handle, int id);
    static native float[] getAmbientColor(long handle, int id);
    static native String getAmbientTextureName(long handle, int id);
    static native String getBumpTextureName(long handle, int id);
    static native float[] getDiffuseColor(long handle, int id);
    static native String getDiffuseTextureName(long handle, int id);
    static native float[] getEmissionColor(long handle, int id);
    static native String getEmissiveTextureName(long handle, int id);
    static native String getHighlightTextureName(long handle, int id);
    static native String getNormalTextureName(long handle, int id);
    static native float[] getSpecularColor(long handle, int id);
    static native String getSpecularTextureName(long handle, int id);
}
