#include <jni.h>
#include <linux/input.h>
#include <string.h>
#include <sys/ioctl.h>

JNIEXPORT jboolean JNICALL
Java_com_arslan_customanimator_service_EvdevDevice_nativeGrab(JNIEnv *env, jclass clazz, jint fd, jboolean grab) {
    return ioctl(fd, EVIOCGRAB, grab ? 1 : 0) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jintArray JNICALL
Java_com_arslan_customanimator_service_EvdevDevice_nativeKeyCodes(JNIEnv *env, jclass clazz, jint fd) {
    unsigned char bits[KEY_MAX / 8 + 1];
    jint codes[KEY_MAX + 1];
    jsize count = 0;
    memset(bits, 0, sizeof(bits));
    if (ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(bits)), bits) < 0) {
        return (*env)->NewIntArray(env, 0);
    }
    for (int code = 0; code <= KEY_MAX; code++) {
        if (bits[code / 8] & (1 << (code % 8))) {
            codes[count++] = code;
        }
    }
    jintArray result = (*env)->NewIntArray(env, count);
    if (result != NULL) {
        (*env)->SetIntArrayRegion(env, result, 0, count, codes);
    }
    return result;
}
