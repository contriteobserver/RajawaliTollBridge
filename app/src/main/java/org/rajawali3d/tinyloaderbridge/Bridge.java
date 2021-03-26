package org.rajawali3d.tinyloaderbridge;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.methods.SpecularMethod;
import org.rajawali3d.materials.textures.NormalMapTexture;
import org.rajawali3d.materials.textures.SpecularMapTexture;
import org.rajawali3d.materials.textures.Texture;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
        for(int shape_id=0; shape_id<numShapes; shape_id++) {
            Set<Integer> mat_ids = new HashSet<>();
            for(int id : getFaceMaterials(mHandle, shape_id)) { mat_ids.add(id); }

            for(int mat_id : mat_ids) {
                Object3D child = new Object3D();
                child.setData(
                        demuxVertices(shape_id, mat_id, vertices),
                        demuxNormals(shape_id, mat_id, normals),
                        demuxTexcoords(shape_id, mat_id, texcoords),
                        demuxColors(shape_id, mat_id, colors),
                        demuxIndices(shape_id, mat_id),
                        true
                );
                child.setMaterial(getParsedMtl(mat_id));
                child.setTransparent(isTransparent(mat_id));
                rootObj.addChild(child);
            }
        }
        return rootObj;
    }

    float[] demuxVertices(int shape_id, int mat_id, float[] vertices) {
        int[] indices = getVertexIndices(mHandle, shape_id);
        int[] ids =  getFaceMaterials(mHandle, shape_id);
        ArrayList<Float> demux = new ArrayList<>();
        for(int i=0; i<indices.length; i++) {
            if(mat_id == ids[i/3]) {
                demux.add(vertices[3*indices[i]+0]);
                demux.add(vertices[3*indices[i]+1]);
                demux.add(vertices[3*indices[i]+2]);
            }
        }

        // ArrayList to Array Conversion
        float[] result = new float[demux.size()];
        for (int i = 0; i < demux.size(); i++)
            result[i] = demux.get(i);
        return result;
    }

    float[] demuxNormals(int shape_id, int mat_id, float[] normals) {
        int[] indices = getNormalIndices(mHandle, shape_id);
        int[] ids =  getFaceMaterials(mHandle, shape_id);
        ArrayList<Float> demux = new ArrayList<>();
        for(int i=0; i<indices.length; i++) {
            if(mat_id == ids[i/3]) {
                demux.add(normals[3*indices[i]+0]);
                demux.add(normals[3*indices[i]+1]);
                demux.add(normals[3*indices[i]+2]);
            }
        }

        // ArrayList to Array Conversion
        float[] result = new float[demux.size()];
        for (int i = 0; i < demux.size(); i++)
            result[i] = demux.get(i);
        return result;
    }

    float[] demuxTexcoords(int shape_id, int mat_id, float[] texcoords) {
        int[] indices = getTextureIndices(mHandle, shape_id);
        int[] ids =  getFaceMaterials(mHandle, shape_id);
        ArrayList<Float> demux = new ArrayList<>();
        for(int i=0; i<indices.length; i++) {
            if(mat_id == ids[i/3]) {
                demux.add(texcoords[2*indices[i]+0]);
                demux.add(texcoords[2*indices[i]+1]);
            }
        }

        // ArrayList to Array Conversion
        float[] result = new float[demux.size()];
        for (int i = 0; i < demux.size(); i++)
            result[i] = demux.get(i);
        return result;
    }

    float[] demuxColors(int shape_id, int mat_id, float[] colors) {
        int[] indices = getVertexIndices(mHandle, shape_id);
        int[] ids =  getFaceMaterials(mHandle, shape_id);
        ArrayList<Float> demux = new ArrayList<>();
        for(int i=0; i<indices.length; i++) {
            if(mat_id == ids[i/3]) {
                demux.add(colors[3*indices[i]+0]);
                demux.add(colors[3*indices[i]+1]);
                demux.add(colors[3*indices[i]+2]);
            }
        }

        // ArrayList to Array Conversion
        float[] result = new float[demux.size()];
        for (int i = 0; i < demux.size(); i++)
            result[i] = demux.get(i);
        return result;
    }

    int[] demuxIndices(int shape_id, int mat_id) {
        int[] indices = getVertexIndices(mHandle, shape_id);
        int[] ids =  getFaceMaterials(mHandle, shape_id);
        ArrayList<Integer> demux = new ArrayList<>();
        int index = 0;
        for(int i=0; i<indices.length; i++) {
            if(mat_id == ids[i/3]) {
                demux.add(index);
                index++;
            }
        }

        // ArrayList to Array Conversion
        int[] result = new int[demux.size()];
        for (int i = 0; i < demux.size(); i++)
            result[i] = demux.get(i);
        return result;
    }

    boolean isTransparent(int mat_id) {
        return getDissolve(mHandle, mat_id) < 1;
    }

    Material getParsedMtl(int id) throws Exception {
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
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        material.setSpecularMethod(new SpecularMethod.Phong(specularColor));
        material.setAmbientColor(getAmbientColor(mHandle, id));

        if(diffuse.isEmpty()) {
            material.setColor(getDiffuseColor(mHandle, id));
            material.setColorInfluence(getDissolve(mHandle, id));
        } else {
            InputStream is = mAssetManager.open(diffuse);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            String name = ext.matcher(diffuse).replaceAll("");
            Texture texture = new Texture(name, bitmap);
            texture.setInfluence(getDissolve(mHandle, id));
            material.addTexture(texture);
            material.setColorInfluence(0);
        }
        if(!highlight.isEmpty()) {
            InputStream is = mAssetManager.open(diffuse);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            String name = ext.matcher(diffuse).replaceAll("");
            SpecularMapTexture texture = new SpecularMapTexture(name, bitmap);
            texture.setInfluence(getDissolve(mHandle, id));
            material.addTexture(texture);
        }
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

    // indexed by shape
    static native String getShapeName(long handle, int shapeIndex);
    static native int[] getFaceMaterials(long handle, int shapeIndex);
    static native int[] getVertexIndices(long handle, int shapeIndex);
    static native int[] getTextureIndices(long handle, int shapeIndex);
    static native int[] getNormalIndices(long handle, int shapeIndex);

    // indexed by material
    static native String getMaterialName(long handle, int id);
    static native float getDissolve(long handle, int id);
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
