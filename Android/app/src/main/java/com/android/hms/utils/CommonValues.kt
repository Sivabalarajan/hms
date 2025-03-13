package com.android.hms.utils

import androidx.core.content.FileProvider

class GenericFileProvider : FileProvider()

object UserType {
    const val ADMIN = "Administrator"
    const val GUEST = "Guest"
}
