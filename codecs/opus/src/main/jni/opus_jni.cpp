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


#include "opus_jni.h"

#include "opus.h"

JavaVM* gJvm;
jobject gOpusObserver;

extern "C" {
  JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_sayHelloNative(JNIEnv *, jobject);

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

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_setOpusObserverNative(
  JNIEnv *jni, jobject, jobject j_observer) {
  gOpusObserver = jni->NewGlobalRef(j_observer);
}

JNIEXPORT void JNICALL Java_org_restcomm_media_codec_opus_OpusJni_unsetOpusObserverNative(
  JNIEnv *jni, jobject) {
  jni->DeleteGlobalRef(gOpusObserver);
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {

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