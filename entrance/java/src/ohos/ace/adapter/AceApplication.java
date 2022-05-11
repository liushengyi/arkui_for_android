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

import android.app.Application;

/**
 * This class extends from Android Application, the entry of app
 *
 * @since 1
 */
public class AceApplication extends Application {
    private static final String LOG_TAG = "AceApplication";

    /**
     * Call when Application is created.
     *
     */
    @Override
    public void onCreate() {
        ALog.setLogger(new LoggerAosp());
        ALog.i(LOG_TAG, "AceApplication onCreate called");
        super.onCreate();
    }
}
