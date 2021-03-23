
#include <jni.h>
#include <string>

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <experimental/tinyobj_loader_opt.h>

#include "tinyobjloader/tiny_obj_loader.h"

extern "C" JNIEXPORT jlong JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_createReader(
        JNIEnv* env,
        jclass /* this */
) {
    env = env;
    return (jlong) new tinyobj::ObjReader();
}

extern "C" JNIEXPORT void JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_destroyReader(
        JNIEnv* env,
        jclass, /* this */
        jlong handle
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    delete reader;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_parseAssets(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jobject assetManager,
        jstring objName,
        jstring mtlName
) {
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    assert(nullptr != mgr);

    auto objFilename = env->GetStringUTFChars(objName, nullptr);
    auto objAsset = AAssetManager_open(mgr, objFilename, AASSET_MODE_UNKNOWN);
    if(objAsset == nullptr) return false;

    auto mtlFilename = env->GetStringUTFChars(mtlName, nullptr);
    auto mtlAsset = AAssetManager_open(mgr, mtlFilename, AASSET_MODE_UNKNOWN);
    if(mtlAsset == nullptr) return false;

    return reader->ParseFromString(
                reinterpret_cast<const char *>(AAsset_getBuffer(objAsset)),
                reinterpret_cast<const char *>(AAsset_getBuffer(mtlAsset))
            );
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getErrors(
        JNIEnv* env,
        jclass, /* this */
        jlong handle
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    return env->NewStringUTF(reader->Error().c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getWarnings(
        JNIEnv* env,
        jclass, /* this */
        jlong handle
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    return env->NewStringUTF(reader->Warning().c_str());
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getVertices(
        JNIEnv* env,
        jclass, /* this */
        jlong handle
) {
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto attrib = reader->GetAttrib();

    auto jArray = env->NewFloatArray(attrib.vertices.size());
    if (jArray != nullptr) {
        auto size = attrib.vertices.size();
        auto data = attrib.vertices.data();
        env->SetFloatArrayRegion(jArray, 0, size, data);
    }
    return jArray;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getNormals(
        JNIEnv* env,
        jclass, /* this */
        jlong handle
) {
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto attrib = reader->GetAttrib();

    auto jArray = env->NewFloatArray(attrib.normals.size());
    if (jArray != nullptr) {
        auto size = attrib.normals.size();
        auto data = attrib.normals.data();
        env->SetFloatArrayRegion(jArray, 0, size, data);
    }
    return jArray;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getColors(
        JNIEnv* env,
        jclass, /* this */
        jlong handle
) {
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto attrib = reader->GetAttrib();

    auto jArray = env->NewFloatArray(attrib.colors.size());
    if (jArray != nullptr) {
        auto size = attrib.colors.size();
        auto data = attrib.colors.data();
        env->SetFloatArrayRegion(jArray, 0, size, data);
    }
    return jArray;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getTexcoords(
        JNIEnv* env,
        jclass, /* this */
        jlong handle
) {
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto attrib = reader->GetAttrib();

    auto jArray = env->NewFloatArray(attrib.texcoords.size());
    if (jArray != nullptr) {
        auto size = attrib.texcoords.size();
        auto data = attrib.texcoords.data();
        env->SetFloatArrayRegion(jArray, 0, size, data);
    }
    return jArray;
}

extern "C" JNIEXPORT jint JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getNumShapes(
        JNIEnv* env,
        jclass, /* this */
        jlong handle
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    return reader->GetShapes().size();
}

extern "C" JNIEXPORT jintArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getMaterialIds(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto ids = reader->GetShapes().at(shapeIndex).mesh.material_ids;

    auto jArray = env->NewIntArray(ids.size());
    if (jArray != nullptr) {
        env->SetIntArrayRegion(jArray, 0, ids.size(), ids.data());
    }
    return jArray;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getVertexIndices(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto indices = reader->GetShapes().at(shapeIndex).mesh.indices;

    auto size = indices.size();
    auto jArray = env->NewIntArray(size);
    if (jArray != nullptr) {
        auto data = new int[size];
        for(int i=0;i<size;i++) { data[i] = indices.at(i).vertex_index; }
        env->SetIntArrayRegion(jArray, 0, size, data);
    }
    return jArray;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getTextureIndices(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto indices = reader->GetShapes().at(shapeIndex).mesh.indices;

    auto size = indices.size();
    auto jArray = env->NewIntArray(size);
    if (jArray != nullptr) {
        auto data = new int[size];
        for(int i=0;i<size;i++) { data[i] = indices.at(i).texcoord_index; }
        env->SetIntArrayRegion(jArray, 0, size, data);
    }
    return jArray;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getNormalIndices(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto indices = reader->GetShapes().at(shapeIndex).mesh.indices;

    auto size = indices.size();
    auto jArray = env->NewIntArray(size);
    if (jArray != nullptr) {
        auto data = new int[size];
        for(int i=0;i<size;i++) { data[i] = indices.at(i).normal_index; }
        env->SetIntArrayRegion(jArray, 0, size, data);
    }
    return jArray;
}


extern "C" JNIEXPORT jstring JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getAlphaTextureName(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto name = reader->GetMaterials().at(shapeIndex).alpha_texname;
    return env->NewStringUTF(name.c_str());
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getAmbientColor(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto ambient = reader->GetMaterials().at(shapeIndex).ambient;

    auto jArray = env->NewFloatArray(4);
    if (jArray != nullptr) {
        float a = 1;
        env->SetFloatArrayRegion(jArray, 0, 3, ambient);
        env->SetFloatArrayRegion(jArray, 3, 1, &a);
    }
    return jArray;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getAmbientTextureName(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto name = reader->GetMaterials().at(shapeIndex).ambient_texname;
    return env->NewStringUTF(name.c_str());
}


extern "C" JNIEXPORT jstring JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getBumpTextureName(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto name = reader->GetMaterials().at(shapeIndex).bump_texname;
    return env->NewStringUTF(name.c_str());
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getDiffuseColor(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto diffuse = reader->GetMaterials().at(shapeIndex).diffuse;

    auto jArray = env->NewFloatArray(4);
    if (jArray != nullptr) {
        float a = 1;
        env->SetFloatArrayRegion(jArray, 0, 3, diffuse);
        env->SetFloatArrayRegion(jArray, 3, 1, &a);
    }
    return jArray;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getDiffuseTextureName(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto name = reader->GetMaterials().at(shapeIndex).diffuse_texname;
    return env->NewStringUTF(name.c_str());
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getEmissionColor(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto emission = reader->GetMaterials().at(shapeIndex).emission;

    auto jArray = env->NewFloatArray(4);
    if (jArray != nullptr) {
        float a = 1;
        env->SetFloatArrayRegion(jArray, 0, 3, emission);
        env->SetFloatArrayRegion(jArray, 3, 1, &a);
    }
    return jArray;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getEmissiveTextureName(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto name = reader->GetMaterials().at(shapeIndex).emissive_texname;
    return env->NewStringUTF(name.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getHighlightTextureName(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto name = reader->GetMaterials().at(shapeIndex).specular_highlight_texname;
    return env->NewStringUTF(name.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getNormalTextureName(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto name = reader->GetMaterials().at(shapeIndex).normal_texname;
    return env->NewStringUTF(name.c_str());
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getSpecularColor(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto ambient = reader->GetMaterials().at(shapeIndex).specular;

    auto jArray = env->NewFloatArray(4);
    if (jArray != nullptr) {
        float a = 1;
        env->SetFloatArrayRegion(jArray, 0, 3, ambient);
        env->SetFloatArrayRegion(jArray, 3, 1, &a);
    }
    return jArray;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_rajawali3d_tinyloaderbridge_Bridge_getSpecularTextureName(
        JNIEnv* env,
        jclass, /* this */
        jlong handle,
        jint shapeIndex
) {
    env = env;
    auto reader = reinterpret_cast<tinyobj::ObjReader *>(handle);
    auto name = reader->GetMaterials().at(shapeIndex).specular_texname;
    return env->NewStringUTF(name.c_str());
}
