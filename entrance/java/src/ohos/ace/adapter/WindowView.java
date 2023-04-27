/*
 * Copyright (c) 2023 Huawei Device Co., Ltd.
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

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.nio.ByteBuffer;

/**
 * This class is AceView implement and handles the lifecycle of surface.
 *
 * @since 1
 */
public class WindowView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String LOG_TAG = "WindowView";

    private long nativeWindowPtr = 0L;

    private int surfaceWidth = 0;
    private int surfaceHeight = 0;

    private Surface delayNotifyCreateSurface = null;
    private boolean delayNotifySurfaceChanged = false;
    private boolean delayNotifySurfaceDestroyed = false;

    private InputConnectionClient inputClient = null;

    /**
     * Constructor of WindowView
     *
     * @param context Application context
     * @param instanceId the instance id
     */
    public WindowView(Context context) {
        super(context);
        initView();
    }

    public void setInputConnectionClient(InputConnectionClient inputConnectionClient)
    {
        inputClient = inputConnectionClient;
    }

    private void initView() {
        ALog.i(LOG_TAG, "WindowView created");
        setFocusableInTouchMode(true);
        getHolder().addCallback(this);
        setZOrderOnTop(true);
    }

    /**
     * Called by native to register Window Handle.
     *
     * @param windowHandle the handle of navive window
     */
    public void registerWindow(long windowHandle) {
        nativeWindowPtr = windowHandle;
        delayNotifyIfNeeded();
    }

    /**
     * Called by native to unregister Window Handle.
     */
    public void unRegisterWindow() {
        nativeWindowPtr = 0L;
    }

    private void delayNotifyIfNeeded() {
        if (nativeWindowPtr == 0L) {
            ALog.e(LOG_TAG, "delay notify, nativeWindow is invalid!");
            return;
        }

        if (delayNotifyCreateSurface != null) {
            ALog.i(LOG_TAG, "delay notify surfaceCreated");
            nativeSurfaceCreated(nativeWindowPtr, delayNotifyCreateSurface);
            delayNotifyCreateSurface = null;
        }

        if (delayNotifySurfaceChanged) {
            ALog.i(LOG_TAG,
                    "delay notify surface changed w=" + surfaceWidth + " h=" + surfaceHeight);
            nativeSurfaceChanged(
                nativeWindowPtr, surfaceWidth, surfaceHeight, getResources().getDisplayMetrics().density);
            delayNotifySurfaceChanged = false;
        }

        if (delayNotifySurfaceDestroyed) {
            ALog.i(LOG_TAG, "delay notify surfaceDestroyed");
            nativeSurfaceDestroyed(nativeWindowPtr);
            delayNotifySurfaceDestroyed = false;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        setFocusable(true);
        requestFocus();
        Surface surface = holder.getSurface();
        holder.setFormat(PixelFormat.TRANSLUCENT);
        if (nativeWindowPtr == 0L) {
            ALog.w(LOG_TAG, "surfaceCreated nativeWindow not ready, delay notify");
            delayNotifyCreateSurface = surface;
        } else {
            ALog.i(LOG_TAG, "surfaceCreated");
            nativeSurfaceCreated(nativeWindowPtr, surface);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        if (nativeWindowPtr == 0L) {
            ALog.w(LOG_TAG, "surfaceChanged nativeWindow not ready, delay notify");
            delayNotifySurfaceChanged = true;
        } else {
            ALog.i(LOG_TAG, "surfaceChanged w=" + surfaceWidth + " h=" + surfaceHeight);
            nativeSurfaceChanged(
                nativeWindowPtr, surfaceWidth, surfaceHeight, getResources().getDisplayMetrics().density);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (nativeWindowPtr == 0L) {
            ALog.w(LOG_TAG, "surfaceDestroyed nativeWindow not ready, delay notify");
            delayNotifySurfaceDestroyed = true;
        } else {
            ALog.i(LOG_TAG, "surfaceDestroyed");
            nativeSurfaceDestroyed(nativeWindowPtr);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (nativeWindowPtr == 0L) {
            ALog.w(LOG_TAG, "onWindowFocusChanged: nativeWindow is null");
            return;
        }
        ALog.i(LOG_TAG, "onWindowFocusChanged");
        nativeOnWindowFocusChanged(nativeWindowPtr, hasWindowFocus);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (inputClient != null) {
            return inputClient.onCreateInputConnection(this, outAttrs);
        }
        return super.onCreateInputConnection(outAttrs);
    }

    /**
     * Notify nativeWindow foreground.
     */
    public void foreground() {
        if (nativeWindowPtr == 0L) {
            ALog.w(LOG_TAG, "foreground: nativeWindow is null");
            return;
        }
        ALog.i(LOG_TAG, "foreground called");
        nativeForeground(nativeWindowPtr);
    }

    /**
     * Notify nativeWindow background.
     */
    public void background() {
        if (nativeWindowPtr == 0L) {
            ALog.w(LOG_TAG, "background: nativeWindow is null");
            return;
        }
        ALog.i(LOG_TAG, "background called");
        nativeBackground(nativeWindowPtr);
    }

    /**
     * Notify nativeWindow destroy.
     */
    public void destroy() {
        if (nativeWindowPtr == 0L) {
            ALog.w(LOG_TAG, "destroy: nativeWindow is null");
            return;
        }
        ALog.i(LOG_TAG, "destroy called");
        nativeDestroy(nativeWindowPtr);
    }

    /**
     * Notify nativeWindow backPressed.
     *
     * @return true if process BackPressed, false otherwise.
     */
    public boolean backPressed() {
        if (nativeWindowPtr == 0L) {
            ALog.w(LOG_TAG, "backPressed: nativeWindow is null");
            return false;
        }
        ALog.i(LOG_TAG, "backPressed called");
        return nativeBackPressed(nativeWindowPtr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (nativeWindowPtr == 0L) {
            return super.onTouchEvent(event);
        }

        try {
            ByteBuffer packet = AceEventProcessorAosp.processTouchEvent(event);
            nativeDispatchPointerDataPacket(nativeWindowPtr, packet, packet.position());
            return true;
        } catch (AssertionError error) {
            ALog.e(LOG_TAG, "process touch event failed: " + error.getMessage());
            return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (nativeWindowPtr == 0L) {
            return super.onKeyDown(keyCode, event);
        }

        if (nativeDispatchKeyEvent(nativeWindowPtr, event.getKeyCode(), event.getAction(), event.getRepeatCount(),
                event.getEventTime(), event.getDownTime())) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (nativeWindowPtr == 0L) {
            return super.onKeyUp(keyCode, event);
        }

        if (nativeDispatchKeyEvent(nativeWindowPtr, event.getKeyCode(), event.getAction(), event.getRepeatCount(),
                event.getEventTime(), event.getDownTime())) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private native void nativeSurfaceCreated(long windowPtr, Surface surface);

    private native void nativeSurfaceChanged(long windowPtr, int width, int height, float density);

    private native void nativeSurfaceDestroyed(long windowPtr);

    private native void nativeOnWindowFocusChanged(long windowPtr, boolean hasWindowFocus);

    private native void nativeForeground(long windowPtr);

    private native void nativeBackground(long windowPtr);

    private native void nativeDestroy(long windowPtr);

    private native boolean nativeBackPressed(long windowPtr);

    private native boolean nativeDispatchPointerDataPacket(long windowPtr, ByteBuffer buffer, int position);

    private native boolean nativeDispatchKeyEvent(long windowPtr, int keyCode, int action, int repeatTime,
            long timeStamp, long timeStampStart);
}
