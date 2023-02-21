#include <iostream>
#include <jni.h>
#include <jvmti.h>
#include <string>
#include "JPLISAgent.h"

void Debug(char *chars);

void Debug(const char *chars) {
    std::cout << std::string(chars) << std::endl;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved);

static jrawMonitorID lock;

jclass susiKlas;
jmethodID susiMetod;

void JNICALL ClassFileLoadHook(jvmtiEnv *jvmti, JNIEnv *jni,
                               jclass class_being_redefined, jobject loader,
                               const char *name, jobject protection_domain,
                               jint data_len, const unsigned char *data,
                               jint *new_data_len, unsigned char **new_data) {
    if (!name) {
        return;
    }

    std::string className = std::string(name);
    if (className.rfind("org/", 0) == 0 || className.rfind("java/", 0) == 0 || className.rfind("sun/", 0) == 0 ||
        className.rfind("javax/", 0) == 0) {
        return;
    }

    jvmti->RawMonitorEnter(lock);

    jstring jstringName = jni->NewStringUTF(name);
    jbyteArray byteArray = jni->NewByteArray(data_len);
    jni->SetByteArrayRegion(byteArray, 0, data_len, reinterpret_cast<const jbyte *>(data));

    jobject bytes = jni->CallStaticObjectMethod(susiKlas, susiMetod, loader, jstringName, class_being_redefined,
                                                protection_domain,
                                                byteArray);
    if (jni->IsSameObject(bytes, nullptr)) {
        return;
    }

    auto byteNew2 = static_cast<jbyteArray>(bytes);
    jint byteLength = jni->GetArrayLength(byteNew2);

    jbyte *body = jni->GetByteArrayElements(byteNew2, nullptr);

    unsigned char *jvmti_space = nullptr;
    jvmtiError err = jvmti->Allocate(byteLength, &jvmti_space);
    if (err) {
        Debug(std::string("allocate failed: ").append(std::to_string(err)).c_str());
        return;
    }
    memcpy(jvmti_space, body, byteLength);

    *new_data_len = byteLength;
    *new_data = jvmti_space;

    jvmti->RawMonitorExit(lock);
}

static jobject getThreadGroupClassLoader(JNIEnv *jni) {
    jclass threadClass = jni->FindClass("java/lang/Thread");
    jmethodID currentThreadMethodID = jni->GetStaticMethodID(threadClass, "currentThread", "()Ljava/lang/Thread;");
    jmethodID getThreadGroupMethodID = jni->GetMethodID(threadClass, "getThreadGroup", "()Ljava/lang/ThreadGroup;");
    jmethodID getContextClassLoaderMethodID = jni->GetMethodID(threadClass, "getContextClassLoader",
                                                               "()Ljava/lang/ClassLoader;");

    jclass threadGroupClass = jni->FindClass("java/lang/ThreadGroup");
    jmethodID activeCountMethodID = jni->GetMethodID(threadGroupClass, "activeCount", "()I");
    jmethodID enumerateMethodID = jni->GetMethodID(threadGroupClass, "enumerate", "([Ljava/lang/Thread;)I");

    jobject currentThread = jni->CallStaticObjectMethod(threadClass, currentThreadMethodID);

    jobject threadGroup = jni->CallObjectMethod(currentThread, getThreadGroupMethodID);

    jint activeCount = jni->CallIntMethod(threadGroup, activeCountMethodID);
    jobjectArray threads = jni->NewObjectArray(activeCount, threadClass, nullptr);
    jni->CallIntMethod(threadGroup, enumerateMethodID, threads);

    jobject firstThread = jni->GetObjectArrayElement(threads, 0);

    return jni->CallObjectMethod(firstThread, getContextClassLoaderMethodID);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *jni;
    jvmtiEnv *jvmti;

    jint result = vm->GetEnv((void **) &jni, JNI_VERSION_1_6);
    if (result == JNI_EDETACHED) {
        result = vm->AttachCurrentThread((void **) &jni, nullptr);
    }

    if (result != JNI_OK) {
        Debug("Failed to get Jni\n");
        exit(-1);
    }
    result = vm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_0);
    if (result != JNI_OK) {
        Debug("Failed to get JvmTi\n");
        exit(-1);
    }

    /*

    jclass urlClassLoaderClass = jni->FindClass("java/net/URLClassLoader");
    jmethodID addURLMethodID = jni->GetMethodID(urlClassLoaderClass, "addURL", "(Ljava/net/URL;)V");

    jclass fileClass = jni->FindClass("java/io/File");
    jmethodID fileConstructorMethodID = jni->GetMethodID(fileClass, "<init>", "(Ljava/lang/String;)V");
    jmethodID toURIMethodID = jni->GetMethodID(fileClass, "toURI", "()Ljava/net/URI;");

    jclass uriClass = jni->FindClass("java/net/URI");
    jmethodID toURLMethodID = jni->GetMethodID(uriClass, "toURL", "()Ljava/net/URL;");

    jobject classLoader = getThreadGroupClassLoader(jni);

    if (!jni->IsInstanceOf(classLoader, urlClassLoaderClass))
    {
        Debug("not url class loader?? wtf");
        return 0;
    }

    jobject externalFinalsCounterJARFile = jni->NewObject(fileClass, fileConstructorMethodID, jni->NewStringUTF(R"(C:\Users\azeroy\susidlo-1.0-SNAPSHOT-all.jar)"));
    jobject uri = jni->CallObjectMethod(externalFinalsCounterJARFile, toURIMethodID);
    jobject url = jni->CallObjectMethod(uri, toURLMethodID);

    if (jni->ExceptionCheck())
    {
        Debug("fuck!");
        jni->ExceptionDescribe();
        jni->ExceptionClear();
        return 0;
    }

    jni->CallVoidMethod(classLoader, addURLMethodID, url);

    jclass classLoaderClass = jni->FindClass("java/lang/ClassLoader");
    jmethodID loadClassMethodID = jni->GetMethodID(classLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    susiKlas = reinterpret_cast<jclass>(jni->CallObjectMethod(classLoader, loadClassMethodID, jni->NewStringUTF("pl.sus.Sus")));
    susiMetod = jni->GetStaticMethodID(susiKlas, "sus", "(Ljava/lang/String;[B)[B");
    */

    susiKlas = jni->FindClass("Transform");
    susiMetod = jni->GetStaticMethodID(susiKlas, "transform",
                                       "(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;Ljava/security/ProtectionDomain;[B)[B");

    jvmtiCapabilities capabilities = {0};
    capabilities.can_generate_all_class_hook_events = 1;
    capabilities.can_retransform_any_class = 1;
    capabilities.can_retransform_classes = 1;
    capabilities.can_redefine_any_class = 1;
    capabilities.can_redefine_classes = 1;
    capabilities.can_tag_objects = 1;

    jvmti->AddCapabilities(&capabilities);

    jvmtiEventCallbacks callbacks = {nullptr};

    callbacks.ClassFileLoadHook = ClassFileLoadHook;

    jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, nullptr);
    return JNI_VERSION_1_8;
}