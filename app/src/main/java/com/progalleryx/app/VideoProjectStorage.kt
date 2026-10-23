package com.progalleryx.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object VideoProjectStorage {

    private const val PROJECTS_DIR = "video_projects"

    fun getProjectsDirectory(context: Context): File {
        val dir = File(context.filesDir, PROJECTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveProjectToJsonString(project: VideoStudioProject): String {
        val root = JSONObject()
        root.put("id", project.id)
        root.put("title", project.title)
        root.put("aspectRatio", project.aspectRatio.name)
        root.put("targetHeight", project.targetHeight)
        root.put("bitrate", project.bitrate)
        root.put("isMasterOriginalMuted", project.isMasterOriginalMuted)
        root.put("masterOriginalVolume", project.masterOriginalVolume.toDouble())
        root.put("createdAt", project.createdAt)

        // Clips
        val clipsArray = JSONArray()
        for (clip in project.clips) {
            val c = JSONObject()
            c.put("id", clip.id)
            c.put("sourceUri", clip.sourceUri?.toString() ?: "")
            c.put("displayName", clip.displayName)
            c.put("startMs", clip.startMs)
            c.put("endMs", clip.endMs)
            c.put("originalDurationMs", clip.originalDurationMs)
            c.put("rotationDegrees", clip.rotationDegrees)
            c.put("speed", clip.speed.toDouble())
            c.put("volume", clip.volume.toDouble())
            c.put("isMuted", clip.isMuted)
            c.put("filterId", clip.filterId)
            c.put("brightness", clip.brightness.toDouble())
            c.put("contrast", clip.contrast.toDouble())
            c.put("saturation", clip.saturation.toDouble())
            clipsArray.put(c)
        }
        root.put("clips", clipsArray)

        // Overlays
        val overlaysArray = JSONArray()
        for (overlay in project.overlays) {
            val o = JSONObject()
            o.put("id", overlay.id)
            o.put("type", overlay.type.name)
            o.put("text", overlay.text)
            o.put("stickerEmoji", overlay.stickerEmoji)
            o.put("startMs", overlay.startMs)
            o.put("endMs", overlay.endMs)
            o.put("normalizedX", overlay.normalizedX.toDouble())
            o.put("normalizedY", overlay.normalizedY.toDouble())
            o.put("scale", overlay.scale.toDouble())
            o.put("rotationDegrees", overlay.rotationDegrees.toDouble())
            o.put("textColorArgb", overlay.textColorArgb)
            o.put("backgroundColorArgb", overlay.backgroundColorArgb)
            o.put("fontIndex", overlay.fontIndex)
            o.put("isBold", overlay.isBold)
            o.put("isItalic", overlay.isItalic)
            overlaysArray.put(o)
        }
        root.put("overlays", overlaysArray)

        // Soundtrack
        if (project.soundtrack != null) {
            val s = JSONObject()
            s.put("uri", project.soundtrack.uri.toString())
            s.put("displayName", project.soundtrack.displayName)
            s.put("audioDurationMs", project.soundtrack.audioDurationMs)
            s.put("startOffsetMs", project.soundtrack.startOffsetMs)
            s.put("projectStartMs", project.soundtrack.projectStartMs)
            s.put("volume", project.soundtrack.volume.toDouble())
            s.put("isMuted", project.soundtrack.isMuted)
            root.put("soundtrack", s)
        }

        return root.toString(2)
    }

    fun loadProjectFromJsonString(jsonString: String): VideoStudioProject {
        val root = JSONObject(jsonString)
        val id = root.optString("id", "")
        val title = root.optString("title", "Project")
        val aspectRatioName = root.optString("aspectRatio", VideoAspectRatio.ORIGINAL.name)
        val aspectRatio = try {
            VideoAspectRatio.valueOf(aspectRatioName)
        } catch (_: Exception) {
            VideoAspectRatio.ORIGINAL
        }
        val targetHeight = root.optInt("targetHeight", VideoQualityRules.HEIGHT_AUTO)
        val bitrate = root.optInt("bitrate", VideoQualityRules.AUTO)
        val isMasterOriginalMuted = root.optBoolean("isMasterOriginalMuted", false)
        val masterOriginalVolume = root.optDouble("masterOriginalVolume", 1.0).toFloat()
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())

        val clips = mutableListOf<VideoClipItem>()
        val clipsArray = root.optJSONArray("clips") ?: JSONArray()
        for (i in 0 until clipsArray.length()) {
            val c = clipsArray.getJSONObject(i)
            val uriStr = c.optString("sourceUri", "")
            val parsedUri = if (uriStr.isNotEmpty()) try { Uri.parse(uriStr) } catch (_: Exception) { null } else null
            val originalDuration = c.optLong("originalDurationMs", 0L)
            clips.add(
                VideoClipItem(
                    id = c.optString("id", ""),
                    sourceUri = parsedUri,
                    displayName = c.optString("displayName", ""),
                    originalDurationMs = originalDuration,
                    startMs = c.optLong("startMs", 0L),
                    endMs = c.optLong("endMs", originalDuration),
                    speed = c.optDouble("speed", 1.0).toFloat(),
                    volume = c.optDouble("volume", 1.0).toFloat(),
                    isMuted = c.optBoolean("isMuted", false),
                    rotationDegrees = c.optInt("rotationDegrees", 0),
                    filterId = c.optString("filterId", PhotoFilterPresets.ORIGINAL.id),
                    brightness = c.optDouble("brightness", 0.0).toFloat(),
                    contrast = c.optDouble("contrast", 1.0).toFloat(),
                    saturation = c.optDouble("saturation", 1.0).toFloat()
                )
            )
        }

        val overlays = mutableListOf<VideoTimedOverlay>()
        val overlaysArray = root.optJSONArray("overlays") ?: JSONArray()
        for (i in 0 until overlaysArray.length()) {
            val o = overlaysArray.getJSONObject(i)
            val typeStr = o.optString("type", VideoOverlayType.TEXT.name)
            val type = try {
                VideoOverlayType.valueOf(typeStr)
            } catch (_: Exception) {
                VideoOverlayType.TEXT
            }
            overlays.add(
                VideoTimedOverlay(
                    id = o.optString("id", ""),
                    type = type,
                    text = o.optString("text", ""),
                    stickerEmoji = o.optString("stickerEmoji", ""),
                    startMs = o.optLong("startMs", 0L),
                    endMs = o.optLong("endMs", 0L),
                    normalizedX = o.optDouble("normalizedX", 0.5).toFloat(),
                    normalizedY = o.optDouble("normalizedY", 0.5).toFloat(),
                    scale = o.optDouble("scale", 1.0).toFloat(),
                    rotationDegrees = o.optDouble("rotationDegrees", 0.0).toFloat(),
                    textColorArgb = o.optLong("textColorArgb", 0xFFFFFFFF),
                    backgroundColorArgb = o.optLong("backgroundColorArgb", 0x88000000),
                    fontIndex = o.optInt("fontIndex", 0),
                    isBold = o.optBoolean("isBold", false),
                    isItalic = o.optBoolean("isItalic", false)
                )
            )
        }

        val soundtrack = if (root.has("soundtrack") && !root.isNull("soundtrack")) {
            val s = root.getJSONObject("soundtrack")
            val sUriStr = s.optString("uri", "")
            val parsedSUri = if (sUriStr.isNotEmpty()) try { Uri.parse(sUriStr) } catch (_: Exception) { Uri.EMPTY } else Uri.EMPTY
            VideoAudioTrack(
                uri = parsedSUri,
                displayName = s.optString("displayName", "Soundtrack"),
                audioDurationMs = s.optLong("audioDurationMs", 0L),
                startOffsetMs = s.optLong("startOffsetMs", 0L),
                projectStartMs = s.optLong("projectStartMs", 0L),
                volume = s.optDouble("volume", 1.0).toFloat(),
                isMuted = s.optBoolean("isMuted", false)
            )
        } else {
            null
        }

        return VideoStudioProject(
            id = id,
            title = title,
            clips = clips,
            overlays = overlays,
            soundtrack = soundtrack,
            masterOriginalVolume = masterOriginalVolume,
            isMasterOriginalMuted = isMasterOriginalMuted,
            aspectRatio = aspectRatio,
            targetHeight = targetHeight,
            bitrate = bitrate,
            createdAt = createdAt
        )
    }

    fun saveProject(context: Context, project: VideoStudioProject): File {
        val dir = getProjectsDirectory(context)
        val file = File(dir, "${project.id}.json")
        val json = saveProjectToJsonString(project)
        file.writeText(json)
        return file
    }

    fun loadProject(file: File): VideoStudioProject? {
        return try {
            if (!file.exists()) return null
            val content = file.readText()
            loadProjectFromJsonString(content)
        } catch (_: Exception) {
            null
        }
    }

    fun listSavedProjects(context: Context): List<VideoStudioProject> {
        val dir = getProjectsDirectory(context)
        val files = dir.listFiles { f -> f.extension.equals("json", ignoreCase = true) } ?: return emptyList()
        return files.mapNotNull { f -> loadProject(f) }.sortedByDescending { p -> p.createdAt }
    }

    fun deleteProject(context: Context, projectId: String): Boolean {
        val dir = getProjectsDirectory(context)
        val file = File(dir, "$projectId.json")
        return if (file.exists()) file.delete() else false
    }
}
