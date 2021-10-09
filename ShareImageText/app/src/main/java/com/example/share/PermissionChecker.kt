package com.example.share

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*


/**
 * Android Runtime Permission 체크를 위한 UTIL
 */
object PermissionChecker {

    fun checkPermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        var result: Int
        val permissionList: MutableList<String> = ArrayList()
        for (pm in permissions) {
            result = ContextCompat.checkSelfPermission(activity, pm)
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(pm)
            }
        }
        if (permissionList.size > 0) {
            val requiredPermissions = permissionList.toTypedArray()
            ActivityCompat.requestPermissions(activity, requiredPermissions, requestCode)
            return false
        }
        return true
    }


    fun checkUserAcceptPermissions(grantResults: IntArray): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        if (grantResults.isEmpty()) {
            return false
        }
        for (result in grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

}