package com.kylecorry.trail_sense.shared

import android.app.Activity
import com.kylecorry.trailsensecore.infrastructure.persistence.Cache

class PermissionService(private val activity: Activity) {

    private var lastRequestCode = 0
    private val completionListeners =
        mutableMapOf<Int, (permissions: List<PermissionResult>) -> Unit>()
    private val partialResults = mutableMapOf<Int, List<PermissionResult>>()
    private val requestQueue = mutableMapOf<Int, List<Permission>>()
    private val cache by lazy { Cache(activity.applicationContext) }

    fun request(
        permissions: List<Permission>,
        onComplete: (permissions: List<PermissionResult>) -> Unit
    ) {
        request(permissions, listOf(), onComplete)
    }

    private fun request(
        remaining: List<Permission>,
        results: List<PermissionResult>,
        onComplete: (permissions: List<PermissionResult>) -> Unit
    ) {
        val notGranted =
            remaining.filterNot { PermissionUtils.hasPermission(activity, it.permission) }
        if (notGranted.isEmpty()) {
            onComplete.invoke(checkPermissionStatus(remaining.map { it.permission }) + results)
            return
        }
        val requestCode = ++lastRequestCode
        completionListeners[requestCode] = onComplete
        val granted = remaining.filter { PermissionUtils.hasPermission(activity, it.permission) }
        partialResults[requestCode] = checkPermissionStatus(granted.map { it.permission }) + results

        val noRationalNeeded = notGranted.filter { it.rationale == null }
        val rationalNeeded = notGranted.filter { it.rationale != null }

        if (noRationalNeeded.isNotEmpty()) {
            requestQueue[requestCode] = rationalNeeded
            PermissionUtils.requestPermissions(
                activity,
                noRationalNeeded.map { it.permission },
                requestCode
            )
            return
        }

        val nextPermission = rationalNeeded.first()
        requestQueue[requestCode] = rationalNeeded.subList(1, rationalNeeded.size)

        if (cache.getBoolean(nextPermission.permission) == true){
            onPermissionResult(requestCode, listOf(nextPermission.permission))
        } else {
            cache.putBoolean(nextPermission.permission, true)
            PermissionUtils.requestPermissionsWithRationale(
                activity,
                listOf(nextPermission.permission),
                nextPermission.rationale!!,
                requestCode
            )
        }
    }

    fun onPermissionResult(requestCode: Int, permissions: List<String>) {
        val listener = completionListeners[requestCode] ?: return
        val results =
            permissions.map { PermissionResult(it, PermissionUtils.hasPermission(activity, it)) }
        val fullResults = (partialResults[requestCode] ?: listOf()) + results
        val queue = requestQueue[requestCode] ?: listOf()
        partialResults.remove(requestCode)
        completionListeners.remove(requestCode)
        requestQueue.remove(requestCode)

        if (queue.isEmpty()) {
            listener.invoke(fullResults)
        } else {
            request(queue, fullResults, listener)
        }
    }

    private fun checkPermissionStatus(permissions: List<String>): List<PermissionResult> {
        return permissions.map { PermissionResult(it, PermissionUtils.hasPermission(activity, it)) }
    }

}