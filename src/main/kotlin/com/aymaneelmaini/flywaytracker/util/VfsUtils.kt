package com.aymaneelmaini.flywaytracker.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

fun VirtualFile.relativePathIn(project: Project): String {
    val base = project.basePath ?: return path
    return if (path.startsWith(base)) path.removePrefix(base).trimStart('/') else path
}
