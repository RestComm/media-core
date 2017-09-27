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

#define FRAME_SIZE 960
#define SAMPLE_RATE 48000
#define CHANNELS 2
#define APPLICATION OPUS_APPLICATION_AUDIO
#define BITRATE 64000

#define MAX_FRAME_SIZE 6*960
#define MAX_PACKET_SIZE (3*1276)

JavaVM* gJvm;
jobject gOpusObserver;

extern "C" {

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_sayHelloNative(JNIEnv *, jobject);

  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_initNative(JNIEnv *, jobject);

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

  char *inFile;
  FILE *fin;
  char *outFile;
  FILE *fout;
  opus_int16 in[FRAME_SIZE*CHANNELS];
  opus_int16 out[MAX_FRAME_SIZE*CHANNELS];
  unsigned char cbits[MAX_PACKET_SIZE];
  int nbBytes;
  OpusEncoder *encoder;
  OpusDecoder *decoder;
  int err;

  encoder = opus_encoder_create(SAMPLE_RATE, CHANNELS, APPLICATION, &err);
  if (err < 0) {
    fprintf(stderr, "Failed to create an encoder: %s\n", opus_strerror(err));
    return;
  }

  err = opus_encoder_ctl(encoder, OPUS_SET_BITRATE(BITRATE));
  if (err < 0) {
    fprintf(stderr, "Failed to set bitrate: %s\n", opus_strerror(err));
    return;
  }

  inFile = "test_input.pcm";
  fin = fopen(inFile, "r");
  if (fin == NULL) {
    fprintf(stderr, "Failed to open input file: %s\n", strerror(errno));
    return;
  }

  decoder = opus_decoder_create(SAMPLE_RATE, CHANNELS, &err);
  if (err < 0) {
    fprintf(stderr, "Failed to create decoder: %s\n", opus_strerror(err));
    return;
  }

  outFile = "test_output.pcm";
  fout = fopen(outFile, "w");
  if (fout == NULL) {
    fprintf(stderr, "Failed to open output file: %s\n", strerror(errno));
    return;
  }

  while (1) {
    int i;
    unsigned char pcm_bytes[MAX_FRAME_SIZE * CHANNELS * 2];
    int frame_size;

    fread(pcm_bytes, sizeof(short)*CHANNELS, FRAME_SIZE, fin);
    if (feof(fin))
      break;
    /* Convert from little-endian ordering. */
    for (i = 0; i < CHANNELS * FRAME_SIZE; i++)
      in[i] = pcm_bytes[2 * i + 1] << 8 | pcm_bytes[2 * i];

    nbBytes = opus_encode(encoder, in, FRAME_SIZE, cbits, MAX_PACKET_SIZE);
    if (nbBytes < 0) {
      fprintf(stderr, "Encode failed: %s\n", opus_strerror(nbBytes));
      return;
    }

    frame_size = opus_decode(decoder, cbits, nbBytes, out, MAX_FRAME_SIZE, 0);
    if (frame_size < 0) {
      fprintf(stderr, "Decoder failed: %s\n", opus_strerror(frame_size));
      return ;
    }

    /* Convert to little-endian ordering. */
    for (i = 0; i< CHANNELS * frame_size; i++) {
      pcm_bytes[2 * i] = out[i] & 0xFF;
      pcm_bytes[2 * i + 1] = (out[i] >> 8) & 0xFF;
    }

    fwrite(pcm_bytes, sizeof(short), frame_size * CHANNELS, fout);
  }

  opus_encoder_destroy(encoder);
  opus_decoder_destroy(decoder);
  fclose(fin);
  fclose(fout);
}

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_setOpusObserverNative(
  JNIEnv *jni, jobject, jobject j_observer) {
  gOpusObserver = jni->NewGlobalRef(j_observer);
}

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_unsetOpusObserverNative(
  JNIEnv *jni, jobject) {
  jni->DeleteGlobalRef(gOpusObserver);
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