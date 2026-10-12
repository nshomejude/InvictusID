package com.opesware.childtrace.child.enforce

import android.app.admin.DeviceAdminReceiver

/**
 * Device-admin receiver used only for visible policy enforcement the parent set
 * (lock at the screen-time limit, downtime windows). It never hides the app and
 * does not block uninstall — admin is deactivatable by the device owner, in
 * keeping with invariant 1. Enabling it is an explicit, visible system prompt.
 */
class ChildDeviceAdminReceiver : DeviceAdminReceiver()
