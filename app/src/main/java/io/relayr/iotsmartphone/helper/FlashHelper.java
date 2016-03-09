package io.relayr.iotsmartphone.helper;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import static android.content.Context.CAMERA_SERVICE;
import static android.content.pm.PackageManager.FEATURE_CAMERA_FLASH;
import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE;
import static android.os.Build.VERSION.SDK_INT;

public class FlashHelper {

    private Camera camera = null;
    private Camera.Parameters cameraParameters;
    private String previousFlashMode = null;

    private String cameraId = null;
    private CameraManager cameraManager;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean hasFlash(Context context) {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (cameraManager == null)
                cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
            try {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics("0");
                final Boolean flashAvailable = cameraCharacteristics.get(FLASH_INFO_AVAILABLE);
                return flashAvailable == null ? false : flashAvailable;
            } catch (CameraAccessException e) {
                Crashlytics.log(Log.ERROR, "FlashH", "Check flash availability failed.");
                e.printStackTrace();
            }
        } else {
            return context.getPackageManager().hasSystemFeature(FEATURE_CAMERA_FLASH);
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public synchronized void open(Context context) throws Exception {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (cameraManager == null)
                cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
            for (String camId : cameraManager.getCameraIdList()) {
                final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(camId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) cameraId = camId;
            }
        } else {
            int numberOfCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    camera = Camera.open(i);
                    if (this.camera != null) {
                        cameraParameters = this.camera.getParameters();
                        previousFlashMode = cameraParameters.getFlashMode();
                    }
                    if (previousFlashMode == null) previousFlashMode = FLASH_MODE_OFF;
                    break;
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) public synchronized boolean on() {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (cameraManager != null && cameraId != null)
                try {
                    cameraManager.setTorchMode(cameraId, true);
                    return true;
                } catch (CameraAccessException e) {
                    Crashlytics.log(Log.ERROR, "FlashH", "Failed to access camera when turning ON.");
                    e.printStackTrace();
                }
        } else {
            if (camera != null) {
                cameraParameters.setFlashMode(FLASH_MODE_TORCH);
                camera.setParameters(cameraParameters);
                camera.startPreview();
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) public synchronized boolean off() {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            if (cameraManager != null && cameraId != null)
                try {
                    cameraManager.setTorchMode(cameraId, false);
                    return true;
                } catch (CameraAccessException e) {
                    Crashlytics.log(Log.ERROR, "FlashH", "Failed to access camera when turning OFF.");
                    e.printStackTrace();
                }
        } else {
            if (camera != null) {
                cameraParameters.setFlashMode(FLASH_MODE_OFF);
                camera.setParameters(cameraParameters);
                camera.stopPreview();
                return true;
            }
        }
        return false;
    }

    public synchronized void close() {
        if (camera != null) {
            off();
            cameraParameters.setFlashMode(previousFlashMode);
            camera.setParameters(cameraParameters);
            camera.release();
            camera = null;
        }
    }
}
