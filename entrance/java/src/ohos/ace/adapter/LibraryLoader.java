/*
 * Copyright (c) 2021 Huawei Device Co., Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ohos.ace.adapter;

/**
 * Load library for JNI.
 * 
 * @since 1
 */
public final class LibraryLoader {
    private static final String LOG_TAG = "LibraryLoader";

    private static final String ANDROID_LIB_NAME = "ace_android";

    private static final Object JNI_LOAD_LOCK = new Object();

    private static volatile boolean jniLoaded = false;

    private LibraryLoader() {
        /* do noting */
    }

    public static boolean isJniLoaded() {
        return jniLoaded;
    }

    private static void loadAndroidJniLibrary() {
        try {
            ALog.i(LOG_TAG, "Load android ace lib");
            System.loadLibrary(ANDROID_LIB_NAME);
            jniLoaded = true;
        } catch (UnsatisfiedLinkError error) {
            ALog.e(LOG_TAG, "Could not load android ace lib. Exception:" + error.getMessage());
        }
    }

    static boolean loadJniLibrary() {
        if (jniLoaded) {
            // JNI shared library is alreay loaded.
            ALog.i(LOG_TAG, "Has loaded ace lib");
            return true;
        }

        synchronized (JNI_LOAD_LOCK) {
            // Check again whether JNI shared library is loaded in another thread.
            if (jniLoaded) {
                // JNI shared library is alreay loaded.
                ALog.i(LOG_TAG, "Has loaded ace lib");
                return true;
            }

            try {
                ALog.i(LOG_TAG, "Load ace lib");
                loadAndroidJniLibrary();
                jniLoaded = true;
            } catch (UnsatisfiedLinkError error) {
                ALog.w(LOG_TAG, "Could not load ace lib. Exception:" + error.getMessage());
            }
        }

        ALog.i(LOG_TAG, "Load ace lib ok");
        return jniLoaded;
    }
}
