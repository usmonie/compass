package com.usmonie.compass.core

import platform.Foundation.NSUUID

public actual fun randomUUID(): String = NSUUID().UUIDString()
