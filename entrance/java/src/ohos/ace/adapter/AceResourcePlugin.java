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

import java.util.Map;

/**
 * The platform resouce plugin.
 * 
 */
public abstract class AceResourcePlugin {

    public interface IAceOnResourceEvent {
        void onEvent(String eventId, String param);
    }

    public interface IAceOnCallResourceMethod {
        String onCall(Map<String, String> param);
    }

    protected AceResourceRegister resRegister;

    private final float version;

    private final String tag;

    private IAceOnResourceEvent callback;

    public AceResourcePlugin(final String tag, final float version) {
        this.tag = tag;
        this.version = version;
        resRegister = null;
    }

    /**
     * This is called to set event callback
     * 
     * @param resRegister object of AceResourceRegister
     * @param callback fire event interface
     */
    public void setEventCallback(AceResourceRegister resRegister, IAceOnResourceEvent callback) {
        this.callback = callback;
        this.resRegister = resRegister;
    }

    /**
     * This is called to set event callback.
     * 
     * @return the event callback interface
     */
    public IAceOnResourceEvent getEventCallback() {
        return callback;
    }

    /**
     * Called to register resouce call method
     * 
     * @param method method
     * @param callMethod method call interface
     */
    public void registerCallMethod(String method, IAceOnCallResourceMethod callMethod) {
        if (resRegister == null) {
            return;
        }
        resRegister.registerCallMethod(method, callMethod);
    }

    /**
     * Called to register resouce call method
     * 
     * @param methodMap map of method
     */
    public void registerCallMethod(Map<String, IAceOnCallResourceMethod> methodMap) {
        if (resRegister == null) {
            return;
        }
        for (Map.Entry<String, IAceOnCallResourceMethod> entry : methodMap.entrySet()) {
            resRegister.registerCallMethod(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Called to unregister resouce call method
     * 
     * @param method method
     */
    public void unregisterCallMethod(String method) {
        if (resRegister == null) {
            return;
        }
        resRegister.unregisterCallMethod(method);
    }


    /**
     * Called to unregister resouce call method
     * 
     * @param methodMap map of method
     */
    public void unregisterCallMethod(Map<String, IAceOnCallResourceMethod> methodMap) {
        for (Map.Entry<String, IAceOnCallResourceMethod> entry : methodMap.entrySet()) {
            resRegister.unregisterCallMethod(entry.getKey());
        }
    }

    /**
     * Called to get resource plugin type
     * 
     * @return type of resource plugin
     */
    public String pluginType() {
        return tag;
    }

    /**
     * Get resource plugin version.
     * 
     * @return version of resource plugin
     */
    public float version() {
        return version;
    }

    /**
     * Get resrouce object by id
     * 
     * @param id id of resource object
     * @return
     */
    public abstract Object getObject(long id);

    /**
     * Create resouce by param
     * 
     * @param param param to create plugin
     * @return
     */
    public abstract long create(Map<String, String> param);

    /**
     * Release resource by id
     * 
     * @param id id of resoure
     * @return
     */
    public abstract boolean release(long id);

    /**
     * Release all resource
     * 
     */
    public abstract void release();
}