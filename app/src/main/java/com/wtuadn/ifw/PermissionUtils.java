package com.wtuadn.ifw;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wtuadn on 2017/10/25.
 */

public class PermissionUtils {
    private static final int REQUEST_CODE = 666;
    private static Callback callback;
    private static List<Result> resultList;

    public static void checkPermissions(Activity activity, String[] permissions, Callback callback) {
        checkPermissions(activity, true, permissions, callback);
    }

    public static void checkPermissions(Activity activity, boolean request, String[] permissions, Callback callback) {
        resultList = new ArrayList<>(permissions.length);
        List<String> noPermissionList = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            boolean granted = ActivityCompat.checkSelfPermission(activity, permissions[i]) == PackageManager.PERMISSION_GRANTED;
            resultList.add(new Result(permissions[i], granted));
            if (!granted) noPermissionList.add(permissions[i]);
        }
        if (noPermissionList.isEmpty()) {
            callback.onResult(resultList);
            clear();
        } else {
            if (request) {
                PermissionUtils.callback = callback;
                ActivityCompat.requestPermissions(activity, noPermissionList.toArray(new String[noPermissionList.size()]), REQUEST_CODE);
            } else {
                callback.onResult(resultList);
                clear();
            }
        }
    }

    public static void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (callback == null) {
            clear();
            return;
        }
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    for (int j = 0; j < resultList.size(); j++) {
                        Result result = resultList.get(j);
                        if (permissions[i].equals(result.permission)) {
                            result.isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                        }
                    }
                }
                callback.onResult(resultList);
            } else {
                callback.onResult(resultList);
            }
        }
        clear();
    }

    public static boolean isAllGranted(List<Result> results) {
        boolean isAllGranted = true;
        for (int i = 0; i < results.size(); i++) {
            if (!results.get(i).isGranted) isAllGranted = false;
        }
        return isAllGranted;
    }

    private static void clear() {
        callback = null;
        resultList = null;
    }

    public static class Result {
        public String permission;
        public boolean isGranted;

        public Result(String permission, boolean isGranted) {
            this.permission = permission;
            this.isGranted = isGranted;
        }
    }

    public interface Callback {
        void onResult(List<Result> resultList);
    }
}
