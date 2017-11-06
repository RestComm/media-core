/*
* TeleStax, Open Source Cloud Communications
* Copyright 2011-2017, Telestax Inc and individual contributors
* by the @authors tag.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*
* @author morosev
*
*/

#if defined(_WIN32)
#define OPUS_EXPORT __declspec(dllimport)
#endif
#include "opus.h"

#include "jni.h"

#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <map>

#define MAX_FRAME_SIZE 6*80
#define MAX_PACKET_SIZE 320

JavaVM* gJvm;
jobject gOpusObserver;

extern "C" {

  JNIEXPORT jlong JNICALL Java_org_restcomm_media_codec_opus_OpusJni_createEncoderNative(JNIEnv *, jobject,
    jint, jint, jint, jint);

  JNIEXPORT jlong JNICALL Java_org_restcomm_media_codec_opus_OpusJni_createDecoderNative(JNIEnv *, jobject,
    jint, jint);

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_releaseEncoderNative(JNIEnv *, jobject,
    jlong);

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_releaseDecoderNative(JNIEnv *, jobject,
    jlong);

  JNIEXPORT jbyteArray JNICALL Java_org_restcomm_media_codec_opus_OpusJni_encodeNative(
    JNIEnv *jni, jobject, jlong, jshortArray);

  JNIEXPORT jshortArray JNICALL Java_org_restcomm_media_codec_opus_OpusJni_decodeNative(
    JNIEnv *jni, jobject, jlong, jbyteArray);

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_sayHelloNative(JNIEnv *, jobject);

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_setOpusObserverNative(JNIEnv *, jobject, jobject);

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_unsetOpusObserverNative(JNIEnv *, jobject);
}

JNIEXPORT jlong JNICALL Java_org_restcomm_media_codec_opus_OpusJni_createEncoderNative(JNIEnv *env, jobject,
  jint jSampleRate, jint jChannels, jint jApplicationType, jint jBitrate) {

  int err;

  OpusEncoder *encoder = opus_encoder_create(jSampleRate, jChannels, jApplicationType, &err);
  if (err < 0) {
    fprintf(stderr, "Failed to create an encoder: %s\n", opus_strerror(err));
    return 0;
  }

  err = opus_encoder_ctl(encoder, OPUS_SET_BITRATE(jBitrate));
  if (err < 0) {
    fprintf(stderr, "Failed to set bitrate: %s\n", opus_strerror(err));
    return 0;
  }

  return (jlong)encoder;
}

JNIEXPORT jlong JNICALL Java_org_restcomm_media_codec_opus_OpusJni_createDecoderNative(JNIEnv *env, jobject,
  jint jSampleRate, jint jChannels) {

  int err;

  OpusDecoder *decoder = opus_decoder_create(jSampleRate, jChannels, &err);
  if (err < 0) {
    fprintf(stderr, "Failed to create decoder: %s\n", opus_strerror(err));
    return 0;
  }

  return (jlong)decoder;
}

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_releaseEncoderNative(JNIEnv *env, jobject,
  jlong jEncoderAddress) {

  OpusEncoder *encoder = (OpusEncoder *)jEncoderAddress;

  opus_encoder_destroy(encoder);
}

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_releaseDecoderNative(JNIEnv *env, jobject,
  jlong jDecoderAddress) {

  OpusDecoder *decoder = (OpusDecoder *)jDecoderAddress;

  opus_decoder_destroy(decoder);
}

JNIEXPORT jbyteArray JNICALL Java_org_restcomm_media_codec_opus_OpusJni_encodeNative(
  JNIEnv *env, jobject, jlong jEncoderAddress, jshortArray jPcmData) {

  jshort *pcmData = env->GetShortArrayElements(jPcmData, NULL);
  jsize pcmLen = env->GetArrayLength(jPcmData);

  OpusEncoder *encoder = (OpusEncoder *)jEncoderAddress;

  unsigned char encoded[MAX_PACKET_SIZE];

  int packetSize;
  packetSize = opus_encode(encoder, pcmData, pcmLen, encoded, MAX_PACKET_SIZE);
  if (packetSize < 0) {
    fprintf(stderr, "Encode failed: %s\n", opus_strerror(packetSize));
    return nullptr;
  }

  env->ReleaseShortArrayElements(jPcmData, pcmData, 0);

  jbyteArray jOpusData = env->NewByteArray(packetSize);
  env->SetByteArrayRegion(jOpusData, 0, packetSize, (jbyte *)encoded);

  return jOpusData;
}

JNIEXPORT jshortArray JNICALL Java_org_restcomm_media_codec_opus_OpusJni_decodeNative(
  JNIEnv *env, jobject, jlong jDecoderAddress, jbyteArray jOpusData) {

  jbyte *opusData = env->GetByteArrayElements(jOpusData, NULL);
  jsize opusLen = env->GetArrayLength(jOpusData);

  OpusDecoder *decoder = (OpusDecoder *)jDecoderAddress;

  short decoded[MAX_FRAME_SIZE];

  int frameSize;
  frameSize = opus_decode(decoder, (unsigned char *)opusData, opusLen, decoded, MAX_FRAME_SIZE, 0);
  if (frameSize < 0) {
    fprintf(stderr, "Decoder failed: %s\n", opus_strerror(frameSize));
    return nullptr;
  }

  env->ReleaseByteArrayElements(jOpusData, opusData, 0);

  jshortArray jPcmData = env->NewShortArray(frameSize);
  env->SetShortArrayRegion(jPcmData, 0, frameSize, decoded);

  return jPcmData;
}

void OnHello() {
  void* env = nullptr;
  jint status = gJvm->GetEnv(&env, JNI_VERSION_1_4);
  if (status != JNI_OK)
    return;
  JNIEnv* jni = reinterpret_cast<JNIEnv*>(env);
  jmethodID jOnHelloMid = jni->GetMethodID(
    jni->GetObjectClass(gOpusObserver), "onHello", "()V");
  jni->CallVoidMethod(gOpusObserver, jOnHelloMid);
}

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_sayHelloNative(JNIEnv *, jobject) {
  printf("Hello World - native!\n");
  OnHello();
}

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_setOpusObserverNative(
  JNIEnv *env, jobject, jobject jObserver) {
  gOpusObserver = env->NewGlobalRef(jObserver);
}

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_unsetOpusObserverNative(
  JNIEnv *env, jobject) {
  env->DeleteGlobalRef(gOpusObserver);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {

  if (!vm) {
    printf("No Java Virtual Machine pointer");
    return -1;
  }

  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**> (&env), JNI_VERSION_1_4) != JNI_OK) {
    printf("Cannot obtain JNI environment");
    return -1;
  }

  gJvm = vm;

  return JNI_VERSION_1_4;
}
