package ai.duagenai.design.bot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import org.json.JSONObject

/**
 * The saved bot look. Screens that draw [BabyGrokMark] without an explicit
 * shape read this, so the Appearance pick shows on the home badge and in chat.
 */
val LocalBotAppearance = staticCompositionLocalOf { BotLook.DEFAULT }

private const val GEOMETRY_ASSET = "babygrok-geometry.json"

@Volatile
private var cachedGeometry: BabyGrokGeometry? = null

/** Reads [GEOMETRY_ASSET]. Null when the asset cannot be parsed. */
internal fun loadBabyGrokGeometry(json: String): BabyGrokGeometry {
    val root = JSONObject(json)
    val box = root.getJSONObject("viewBox")
    val viewBox = BabyGrokViewBox(
        minX = box.getDouble("minX").toFloat(),
        minY = box.getDouble("minY").toFloat(),
        width = box.getDouble("width").toFloat(),
        height = box.getDouble("height").toFloat(),
    )
    val shapesJson = root.getJSONObject("shapes")
    val shapes = HashMap<String, BabyGrokShape>(shapesJson.length())
    val keys = shapesJson.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val subpaths = parseBabyGrokSubpaths(shapesJson.getJSONObject(key).getString("path"))
        if (subpaths.isEmpty()) continue
        shapes[key] = BabyGrokShape(body = subpaths.first(), eyes = subpaths.drop(1))
    }
    return BabyGrokGeometry(viewBox, shapes)
}

@Composable
private fun rememberGeometry(): BabyGrokGeometry? {
    cachedGeometry?.let { return it }
    val context = LocalContext.current
    val loaded = runCatching {
        context.assets.open(GEOMETRY_ASSET).bufferedReader().use { it.readText() }
    }.getOrNull()?.let { json -> runCatching { loadBabyGrokGeometry(json) }.getOrNull() }
    if (loaded != null) cachedGeometry = loaded
    return loaded
}

/**
 * The Grok Bot mark: one shape filled with [color], eyes as a second fill.
 * Decorative unless [contentDescription] is set.
 *
 * [darkTheme] overrides the current Material theme when the picker must show
 * the light-theme swatch even while the app is dark.
 */
@Composable
fun BabyGrokMark(
    shape: BotShape,
    color: BotColor,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    darkTheme: Boolean? = null,
) {
    val geometry = rememberGeometry()
    val dark = darkTheme ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)
    val bodyArgb = color.argb(dark)
    val body = Color(bodyArgb)
    val eyes = Color(eyeArgb(bodyArgb))
    val described = if (contentDescription == null) {
        modifier.size(size)
    } else {
        modifier.size(size).semantics { this.contentDescription = contentDescription }
    }
    val spec = geometry?.shapes?.get(shape.key)
    if (geometry == null || spec == null) {
        Canvas(described) { drawCircle(body) }
        return
    }
    val viewBox = geometry.viewBox
    Canvas(described) {
        val sx = this.size.width / viewBox.width
        val sy = this.size.height / viewBox.height
        fun map(points: FloatArray): Path {
            val path = Path()
            var p = 0
            while (p + 1 < points.size) {
                val x = (points[p] - viewBox.minX) * sx
                val y = (points[p + 1] - viewBox.minY) * sy
                if (p == 0) path.moveTo(x, y) else path.lineTo(x, y)
                p += 2
            }
            path.close()
            return path
        }
        drawPath(map(spec.body), body)
        for (eye in spec.eyes) drawPath(map(eye), eyes)
    }
}
