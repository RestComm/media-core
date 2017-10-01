/*
* JBoss, Home of Professional Open Source
* Copyright 2011, Red Hat, Inc. and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
*/

#define OPUS_EXPORT __declspec(dllimport)
#include "opus.h"

#include "jni.h"

#include <errno.h>
#include <stdlib.h>
#include <string.h>

#define FRAME_SIZE 480
#define SAMPLE_RATE 48000
#define CHANNELS 1
#define APPLICATION OPUS_APPLICATION_VOIP
#define BITRATE 48000

#define MAX_FRAME_SIZE 6*480
#define MAX_PACKET_SIZE (3*1276)

JavaVM* gJvm;
jobject gOpusObserver;

OpusEncoder *gEncoder;
OpusDecoder *gDecoder;

extern "C" {

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_sayHelloNative(JNIEnv *, jobject);

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_initNative(JNIEnv *, jobject);

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_closeNative(JNIEnv *, jobject);

  JNIEXPORT jbyteArray JNICALL Java_org_restcomm_media_codec_opus_OpusJni_encodeNative(
    JNIEnv *jni, jobject, jshortArray);

  JNIEXPORT jshortArray JNICALL Java_org_restcomm_media_codec_opus_OpusJni_decodeNative(
    JNIEnv *jni, jobject, jbyteArray);

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_setOpusObserverNative(JNIEnv *, jobject, jobject);

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_unsetOpusObserverNative(JNIEnv *, jobject);
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

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_initNative(JNIEnv *, jobject) {

  int err;

  gEncoder = opus_encoder_create(SAMPLE_RATE, CHANNELS, APPLICATION, &err);
  if (err < 0) {
    fprintf(stderr, "Failed to create an encoder: %s\n", opus_strerror(err));
    return;
  }

  err = opus_encoder_ctl(gEncoder, OPUS_SET_BITRATE(BITRATE));
  if (err < 0) {
    fprintf(stderr, "Failed to set bitrate: %s\n", opus_strerror(err));
    return;
  }

  gDecoder = opus_decoder_create(SAMPLE_RATE, CHANNELS, &err);
  if (err < 0) {
    fprintf(stderr, "Failed to create decoder: %s\n", opus_strerror(err));
    return;
  }
}

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_closeNative(JNIEnv *, jobject) {
  opus_encoder_destroy(gEncoder);
  opus_decoder_destroy(gDecoder);
}

JNIEXPORT jbyteArray JNICALL Java_org_restcomm_media_codec_opus_OpusJni_encodeNative(
  JNIEnv *env, jobject, jshortArray jPcmData) {

  jshort *pcmData = env->GetShortArrayElements(jPcmData, NULL);
  jsize pcmLen = env->GetArrayLength(jPcmData);

  unsigned char encoded[MAX_PACKET_SIZE];

  int packetSize;
  packetSize = opus_encode(gEncoder, pcmData, pcmLen, encoded, MAX_PACKET_SIZE);
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
  JNIEnv *env, jobject, jbyteArray jOpusData) {

  jbyte *opusData = env->GetByteArrayElements(jOpusData, NULL);
  jsize opusLen = env->GetArrayLength(jOpusData);

  short decoded[MAX_FRAME_SIZE];

  int frameSize;
  frameSize = opus_decode(gDecoder, (unsigned char *)opusData, opusLen, decoded, MAX_FRAME_SIZE, 0);
  if (frameSize < 0) {
    fprintf(stderr, "Decoder failed: %s\n", opus_strerror(frameSize));
    return nullptr;
  }

  env->ReleaseByteArrayElements(jOpusData, opusData, 0);

  jshortArray jPcmData = env->NewShortArray(frameSize);
  env->SetShortArrayRegion(jPcmData, 0, frameSize, decoded);

  return jPcmData;
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