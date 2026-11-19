package ai.duagenai.design.bot

/**
 * Grok Bot's Appearance choices, taken from the shipped `babygrok-geometry.json`
 * (Grok Bot 1.26.0, package `ai.x.grok.bot`).
 *
 * Each shape is one filled body plus two eye subpaths (M/L/Z only). Each color
 * has a light-theme hex and a dark-theme hex: black becomes white in dark, and
 * the other colors step down so they stay readable on a dark canvas.
 */
enum class BotShape(val key: String, val label: String) {
    Blob("blob", "Blob"),
    Pebble("pebble", "Pebble"),
    Bean("bean", "Bean"),
    Egg("egg", "Egg"),
    Squircle("squircle", "Squircle"),
    Tablet("tablet", "Tablet"),
    Capsule("capsule", "Capsule"),
    Cylinder("cylinder", "Cylinder"),
    Hex("hex", "Hex"),
    Gem("gem", "Gem"),
    Crystal("crystal", "Crystal"),
    Wedge("wedge", "Wedge"),
    Shield("shield", "Shield"),
    Dome("dome", "Dome"),
    Arch("arch", "Arch"),
    Cloud("cloud", "Cloud"),
    Teardrop("teardrop", "Teardrop"),
    Leaf("leaf", "Leaf"),
    Square("square", "Square"),
    Sparkle("sparkle", "Sparkle"),
    Clover("clover", "Clover"),
    Flower("flower", "Flower"),
    Heart("heart", "Heart"),
    House("house", "House"),
    Star("star", "Star"),
    Pumpkin("pumpkin", "Pumpkin");

    companion object {
        val DEFAULT: BotShape = Blob

        fun fromKey(key: String?): BotShape =
            values().firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * Named bot colors. [light] is the swatch and the light-theme body.
 * [dark] is the body when the app is in dark theme.
 * Values are ARGB longs (`0xAARRGGBB`).
 */
enum class BotColor(val label: String, val light: Long, val dark: Long) {
    Black("Black", 0xFF000000, 0xFFFFFFFF),
    Brown("Brown", 0xFFA27952, 0xFF855C36),
    Red("Red", 0xFFFF3E51, 0xFFE02135),
    Orange("Orange", 0xFFFF781C, 0xFFFF6700),
    Yellow("Yellow", 0xFFFFAF38, 0xFFFF9800),
    Green("Green", 0xFF00C972, 0xFF009957),
    Cyan("Cyan", 0xFF1CC3B0, 0xFF00A592),
    Blue("Blue", 0xFF2A92FE, 0xFF0E74E0),
    Violet("Violet", 0xFFA97EFE, 0xFF804EE0),
    Magenta("Magenta", 0xFFFF5EB1, 0xFFE02A88),
    Gray("Gray", 0xFF959595, 0xFF777777);

    fun argb(darkTheme: Boolean): Long = if (darkTheme) dark else light

    companion object {
        val DEFAULT: BotColor = Black

        fun fromKey(key: String?): BotColor =
            values().firstOrNull { it.name == key } ?: DEFAULT
    }
}

/** One saved bot look. */
data class BotLook(val shape: BotShape, val color: BotColor) {
    companion object {
        val DEFAULT = BotLook(BotShape.DEFAULT, BotColor.DEFAULT)
    }
}

/** View box the geometry paths are authored in. */
data class BabyGrokViewBox(
    val minX: Float,
    val minY: Float,
    val width: Float,
    val height: Float,
)

/**
 * One shape: [body] is the outline, [eyes] are the two inner subpaths.
 * Each array is `x,y,x,y...` in view-box space.
 */
data class BabyGrokShape(
    val body: FloatArray,
    val eyes: List<FloatArray>,
)

data class BabyGrokGeometry(
    val viewBox: BabyGrokViewBox,
    val shapes: Map<String, BabyGrokShape>,
)

/**
 * Split one M/L/Z path into subpaths of x,y pairs.
 *
 * The shipped geometry uses only those three commands. A number may be
 * negative or decimal. `Z` closes the current subpath; the next `M` starts
 * the next one. The first subpath is the body and the rest are the eyes.
 */
internal fun parseBabyGrokSubpaths(path: String): List<FloatArray> {
    val subs = ArrayList<ArrayList<Float>>()
    var current: ArrayList<Float>? = null
    var i = 0
    fun skipSep() {
        while (i < path.length && (path[i].isWhitespace() || path[i] == ',')) i++
    }
    fun readNum(): Float {
        skipSep()
        if (i >= path.length) throw IllegalArgumentException("number missing")
        val start = i
        if (path[i] == '-' || path[i] == '+') i++
        var dot = false
        while (i < path.length) {
            val c = path[i]
            if (c.isDigit()) {
                i++
            } else if (c == '.' && !dot) {
                dot = true
                i++
            } else {
                break
            }
        }
        if (i == start || (i == start + 1 && (path[start] == '-' || path[start] == '+'))) {
            throw IllegalArgumentException("bad number at $start")
        }
        return path.substring(start, i).toFloat()
    }
    while (i < path.length) {
        skipSep()
        if (i >= path.length) break
        when (val cmd = path[i]) {
            'M' -> {
                i++
                val points = ArrayList<Float>()
                current = points
                subs.add(points)
                points.add(readNum())
                points.add(readNum())
            }
            'L' -> {
                i++
                val points = current ?: throw IllegalArgumentException("L before M")
                points.add(readNum())
                points.add(readNum())
            }
            'Z' -> i++
            else -> throw IllegalArgumentException("unsupported command $cmd")
        }
    }
    return subs.map { it.toFloatArray() }
}

/** Relative luminance of an ARGB color, 0 (black) to 1 (white). */
internal fun argbLuminance(argb: Long): Double {
    val r = ((argb shr 16) and 0xFF) / 255.0
    val g = ((argb shr 8) and 0xFF) / 255.0
    val b = (argb and 0xFF) / 255.0
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** Eyes are dark on a light body and white on a dark body. */
internal fun eyeArgb(bodyArgb: Long): Long =
    if (argbLuminance(bodyArgb) > 0.65) 0xFF1A1A1A else 0xFFFFFFFF
