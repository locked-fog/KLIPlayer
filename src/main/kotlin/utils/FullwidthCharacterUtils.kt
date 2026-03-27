package com.lockedfog.kliplayer.utils

/**
 * 全角字符工具类
 *
 * 处理 CJK 字符、Emoji、NerdFont 图标等宽字符的宽度计算
 */
object FullwidthCharacterUtils {

    // ===== 已知宽字符 Unicode 范围 =====

    /** CJK 统一表意文字扩展区 A */
    private val CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A = IntRange(0x3400, 0x4DBF)

    /** CJK 统一表意文字 */
    private val CJK_UNIFIED_IDEOGRAPHS = IntRange(0x4E00, 0x9FFF)

    /** CJK 兼容表意文字 */
    private val CJK_COMPATIBILITY_IDEOGRAPHS = IntRange(0xF900, 0xFAFF)

    /** CJK 统一表意文字扩展区 B-F */
    private val CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B = IntRange(0x20000, 0x2A6DF)

    /** CJK 统一表意文字扩展区 G */
    private val CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G = IntRange(0x30000, 0x3134F)

    /** CJK 统一表意文字增补 */
    private val CJK_UNIFIED_IDEOGRAPHS_SUPPLEMENT = IntRange(0x2A700, 0x2B73F)

    /** 中日韩兼容形式 */
    private val CJK_COMPATIBILITY_FORMS = IntRange(0xFE30, 0xFE4F)

    /** CJK 标点符号（部分全宽） */
    private val CJK_PUNCTUATION = IntRange(0x3000, 0x303F)

    /** 全角 ASCII / 符号 */
    private val FULLWIDTH_FORMS = IntRange(0xFF00, 0xFFEF)

    /** 表意数字字母 */
    private val HALFWIDTH_AND_FULLWIDTH_FORMS = IntRange(0xFF01, 0xFFEE)

    /** 小符号扩展 */
    private val SMALL_FORM_VARIANTS = IntRange(0xFE50, 0xFE6F)

    // ===== Emoji 范围 =====

    /** Emoji 表情符号（基本集） */
    private val EMOJI_PRESENTATION = IntRange(0x231A, 0x231B)
    private val EMOJI_KEYCAP = IntRange(0x2328, 0x2329)
    private val EMOJI_TRANSPORT = IntRange(0x23E9, 0x23F3)
    private val EMOJI_MISC_SYMBOLS = IntRange(0x23F8, 0x23FA)
    private val EMOJI_CIRCLED_AB = IntRange(0x24B6, 0x24CF)
    private val EMOJI_SQUARED_AB = IntRange(0x25AA, 0x25AB)
    private val EMOJI_TRIANGLE = IntRange(0x25B6, 0x25C0)
    private val EMOJI_DIAMOND = IntRange(0x25C6, 0x25C7)
    private val EMOJI_CIRCLE = IntRange(0x25CE, 0x25D4)
    private val EMOJI_FLAG = IntRange(0x2691, 0x2692)
    private val EMOJI_MISC = IntRange(0x26A0, 0x26AA)

    /** Emoji Modifier Base */
    private val EMOJI_MODIFIER_BASE = IntRange(0x1F3FB, 0x1F3FF)

    /** Emoji Flag Sequences */
    private val REGIONAL_INDICATOR = IntRange(0x1F1E6, 0x1F1FF)

    /** 更多 Emoji（U+1F300 起）*/
    private val MISC_PICTORIAL_SYMBOLS = IntRange(0x1F300, 0x1F6FF)
    private val SUPPLEMENTAL_SYMBOLS = IntRange(0x1F700, 0x1F77F)
    private val Ornamental_Dingbats = IntRange(0x1F780, 0x1F9FF)

    // ===== NerdFont 范围 =====

    /** NerdFont 使用 Unicode 私用区 (Private Use Areas) 和几何形状扩展 */
    private val NERDFONT_PUA = IntRange(0xE000, 0xF8FF)
    private val NERDFONT_SYMBOLS_EXTENDED = IntRange(0x2665, 0x2665) // ♥ 及其他

    /**
     * 判断单个码点是否为宽字符（显示宽度为 2）
     */
    fun isWide(codePoint: Int): Boolean {
        return isCJKWide(codePoint) || isEmoji(codePoint) || isNerdFont(codePoint)
    }

    /**
     * 判断是否为 CJK 宽字符
     */
    fun isCJKWide(codePoint: Int): Boolean {
        return codePoint in CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                codePoint in CJK_UNIFIED_IDEOGRAPHS ||
                codePoint in CJK_COMPATIBILITY_IDEOGRAPHS ||
                codePoint in CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
                codePoint in CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G ||
                codePoint in CJK_UNIFIED_IDEOGRAPHS_SUPPLEMENT ||
                codePoint in CJK_COMPATIBILITY_FORMS ||
                codePoint in CJK_PUNCTUATION ||
                codePoint in FULLWIDTH_FORMS
    }

    /**
     * 判断是否为 Emoji
     */
    fun isEmoji(codePoint: Int): Boolean {
        // 基本 Emoji 范围
        if (codePoint in EMOJI_PRESENTATION) return true
        if (codePoint in EMOJI_KEYCAP) return true
        if (codePoint in EMOJI_TRANSPORT) return true
        if (codePoint in EMOJI_MISC_SYMBOLS) return true
        if (codePoint in EMOJI_CIRCLED_AB) return true
        if (codePoint in EMOJI_SQUARED_AB) return true
        if (codePoint in EMOJI_TRIANGLE) return true
        if (codePoint in EMOJI_DIAMOND) return true
        if (codePoint in EMOJI_CIRCLE) return true
        if (codePoint in EMOJI_FLAG) return true
        if (codePoint in EMOJI_MISC) return true
        if (codePoint in MISC_PICTORIAL_SYMBOLS) return true
        if (codePoint in SUPPLEMENTAL_SYMBOLS) return true
        if (codePoint in Ornamental_Dingbats) return true

        // Emoji Modifier Base (肤色修饰符)
        if (codePoint in EMOJI_MODIFIER_BASE) return true

        // 地区指示符 (国旗)
        if (codePoint in REGIONAL_INDICATOR) return true

        // Emoji 零宽连接符 (ZWJ) - 组合用，本身宽度取决于基字符
        // U+200D ZWJ - 不单独计算宽度
        // U+FE0F Emoji 变体选择符 - 不单独计算宽度

        return false
    }

    /**
     * 判断是否为 NerdFont 图标
     */
    fun isNerdFont(codePoint: Int): Boolean {
        // NerdFont 主要使用 PUA 区域
        // 但直接判断 PUA 不可靠，可能误判普通私用字符
        // 实际项目中 NerdFont 图标是预定义的符号集

        // 常见 NerdFont 图标 Unicode 范围（非完整列表）
        return codePoint in NERDFONT_PUA
    }

    /**
     * 获取单个码点的显示宽度
     * @return 1 或 2
     */
    fun width(codePoint: Int): Int {
        return if (isWide(codePoint)) 2 else 1
    }

    /**
     * 计算字符串的视觉宽度（终端列数）
     */
    fun visualWidth(str: String): Int {
        var total = 0
        var index = 0
        while (index < str.length) {
            val codePoint = Character.codePointAt(str, index)
            total += width(codePoint)
            index += Character.charCount(codePoint)
        }
        return total
    }

    /**
     * 计算字符串中第 N 个视觉列对应的字符索引
     * @param str 字符串
     * @param targetColumn 目标视觉列（0-indexed）
     * @return 字符索引，如果 targetColumn 超出字符串宽度则返回 -1
     */
    fun charIndexAt(str: String, targetColumn: Int): Int {
        if (targetColumn < 0) return -1
        var currentColumn = 0
        var index = 0
        while (index < str.length) {
            val codePoint = Character.codePointAt(str, index)
            val charWidth = width(codePoint)
            if (currentColumn + charWidth > targetColumn) {
                return index
            }
            currentColumn += charWidth
            index += Character.charCount(codePoint)
        }
        return if (currentColumn <= targetColumn) str.length else -1
    }

    /**
     * 计算字符在字符串中的视觉列位置（从左到右累加）
     * @param str 字符串
     * @param charIndex 字符索引
     * @return 该字符左边缘的视觉列（0-indexed）
     */
    fun columnOf(str: String, charIndex: Int): Int {
        require(charIndex in 0..str.length) { "charIndex out of bounds: $charIndex" }
        var total = 0
        var index = 0
        while (index < charIndex) {
            val codePoint = Character.codePointAt(str, index)
            total += width(codePoint)
            index += Character.charCount(codePoint)
        }
        return total
    }

    /**
     * 截取字符串到指定视觉宽度（从左边开始）
     * @param str 原字符串
     * @param maxWidth 最大视觉宽度
     * @return 截断后的字符串
     */
    fun truncate(str: String, maxWidth: Int): String {
        if (maxWidth <= 0) return ""
        val result = StringBuilder()
        var currentWidth = 0
        var index = 0
        while (index < str.length) {
            val codePoint = Character.codePointAt(str, index)
            val cpWidth = width(codePoint)
            if (currentWidth + cpWidth > maxWidth) break
            result.appendCodePoint(codePoint)
            currentWidth += cpWidth
            index += Character.charCount(codePoint)
        }
        return result.toString()
    }

    /**
     * 截取字符串中指定视觉列范围的内容
     * @param str 原字符串
     * @param startColumn 起始视觉列（包含）
     * @param endColumn 结束视觉列（不包含）
     * @return 截取后的字符串
     */
    fun substring(str: String, startColumn: Int, endColumn: Int): String {
        val startIndex = charIndexAt(str, startColumn)
        if (startIndex < 0) return ""
        val endIndex = charIndexAt(str, endColumn)
        if (endIndex < 0) return str.substring(startIndex)
        return str.substring(startIndex, endIndex)
    }
}