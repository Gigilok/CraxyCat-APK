#include <jni.h>
#include <cstdint>
#include <android/log.h>

#define TAG "KeeloqNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

// Non-linear feedback function (NLF) do hardware Keeloq
const uint32_t KEELOQ_NLF = 0x3A5C742E;

uint32_t keeloq_decrypt(uint32_t data, uint64_t key) {
    uint32_t x = data;
    for (int i = 0; i < 528; i++) {
        uint32_t nlf = (KEELOQ_NLF >> ((x >> 1) & 0xF)) & 1;
        uint32_t bit = nlf ^ (x >> 16) ^ (x & 1) ^ ((key >> (i % 64)) & 1) ^ ((x >> 31) & 1);
        x = (x >> 1) | (bit << 31);
    }
    return x;
}

uint32_t keeloq_encrypt(uint32_t data, uint64_t key) {
    uint32_t x = data;
    for (int i = 0; i < 528; i++) {
        uint32_t nlf = (KEELOQ_NLF >> ((x >> 1) & 0xF)) & 1;
        uint32_t bit = nlf ^ (x & 1) ^ ((key >> ((528 - 1 - i) % 64)) & 1) ^ ((x >> 16) & 1);
        x = (x << 1) | bit;
    }
    return x;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_crazycat_app_crypto_KeeloqEngine_decryptNative(JNIEnv* env, jobject, jint data, jlong key) {
    uint32_t result = keeloq_decrypt((uint32_t)data, (uint64_t)key);
    return (jint)result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_crazycat_app_crypto_KeeloqEngine_encryptNative(JNIEnv* env, jobject, jint data, jlong key) {
    uint32_t result = keeloq_encrypt((uint32_t)data, (uint64_t)key);
    return (jint)result;
}
