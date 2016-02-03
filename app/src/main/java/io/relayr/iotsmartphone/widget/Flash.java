package io.relayr.iotsmartphone.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

import static android.content.Context.CAMERA_SERVICE;
import static android.content.pm.PackageManager.FEATURE_CAMERA_FLASH;
import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

public class Flash {

    private Camera camera = null;
    private Camera.Parameters cameraParameters;

    private String previousFlashMode = null;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean hasFlash(Context context) {
        if (SDK_INT >= LOLLIPOP) {
            CameraManager mCameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
            try {
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics("0");
                final Boolean flashAvailable = cameraCharacteristics.get(FLASH_INFO_AVAILABLE);
                return flashAvailable == null ? false : flashAvailable;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            return context.getPackageManager().hasSystemFeature(FEATURE_CAMERA_FLASH);
        }
        return false;
    }

    public synchronized void open() {
        try {
            camera = Camera.open();
            if (camera != null) {
                cameraParameters = camera.getParameters();
                previousFlashMode = cameraParameters.getFlashMode();
            }

            if (previousFlashMode == null) previousFlashMode = FLASH_MODE_OFF;
        } catch (Exception e) {
            Log.e("Flash", "camera failed to open");
        }
    }

    public synchronized void close() {
        if (camera != null) {
            cameraParameters.setFlashMode(previousFlashMode);
            camera.setParameters(cameraParameters);
            camera.release();
            camera = null;
        }
    }

    public synchronized boolean on() {
        if (camera != null) {
            cameraParameters.setFlashMode(FLASH_MODE_TORCH);
            camera.setParameters(cameraParameters);
            camera.startPreview();
            return true;
        }
        return false;
    }

    public synchronized boolean off() {
        if (camera != null) {
            cameraParameters.setFlashMode(FLASH_MODE_OFF);
            camera.setParameters(cameraParameters);
            camera.stopPreview();
            return true;
        }
        return false;
    }
}
