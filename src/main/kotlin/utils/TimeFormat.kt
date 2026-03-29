package com.lockedfog.kliplayer.utils

import com.lockedfog.kliplayer.exception.ParseException

object TimeFormat {
    // use regex to match time format
    // type absolute time format: mm:ss.xxx
    val regexAbs = Regex("""(\d{2}):(\d{2})\.(\d{3})""")
    // type relative time format: +xxx (no digit limit)
    val regexRel = Regex("""\+(\d+)""")
    // type absolute beat format: xxb or xx.xxb (no digit limit)
    val regexBeat = Regex("""(\d+(?:\.\d+)?)b""")
    // type relative beat format: +xxb or +xx.xxb (no digit limit)
    val regexRelBeat = Regex("""\+(\d+(?:\.\d+)?)b""")
    // type relative fractional beat format: +xby, where x is the denominator and y is the numerator
    val regexRelFracBeat = Regex("""\+(\d+)b(\d+)""")
    /***
        * timeFormat函数：将时间字符串转换为时间戳（单位：毫秒）
        * @param time 时间字符串，支持以下格式：
        * 1. 绝对时间格式：mm:ss.xxx（例如：01:30.500）
        * 2. 相对时间格式：+xxx（例如：+500，表示在当前时间基础上增加500毫秒）
        * 3. 绝对节拍格式：xxb或xx.xxb（例如：4b或4.5b，表示第4拍或第4.5拍）
        * 4. 相对节拍格式：+xxb或+xx.xxb（例如：+4b或+4.5b，表示在当前节拍基础上增加4拍或4.5拍）
        * 5. 相对分数节拍格式：+xby，其中x是分母，y是分子（例如：+4b2，表示在当前节拍基础上增加1/2拍）
        * @param timeLast 当前时间戳（单位：毫秒），用于计算相对时间
        * @param bpm 当前节拍率（单位：每分钟节拍数），用于计算节拍时间
        * @param bpmStart 当前节拍开始时间戳（单位：毫秒），用于计算节拍时间
        * @return 转换后的时间戳（单位：毫秒）
     */
    fun timeFormat(time: String ,timeLast: Long?,bpm: Double?,bpmStart: Long?): Long {
        var timeStamp: Long

        when {
            regexAbs.matches(time) -> {
                val (mm, ss, xxx) = regexAbs.find(time)!!.destructured
                timeStamp = mm.toLong() * 60 * 1000 + ss.toLong() * 1000 + xxx.toLong()
            }
            regexRel.matches(time) -> {
                val (xxx) = regexRel.find(time)!!.destructured
                timeStamp = timeLast!! + xxx.toLong()
            }
            regexBeat.matches(time) -> {
                val (beat) = regexBeat.find(time)!!.destructured
                timeStamp = bpmStart!! + ((beat.toDouble() / bpm!!) * 60 * 1000).toLong()
            }
            regexRelBeat.matches(time) -> {
                val (beat) = regexRelBeat.find(time)!!.destructured
                timeStamp = timeLast!! + ((beat.toDouble() / bpm!!) * 60 * 1000).toLong()
            }
            regexRelFracBeat.matches(time) -> {
                val (denominator, numerator) = regexRelFracBeat.find(time)!!.destructured
                timeStamp = timeLast!! + (((1.0 / denominator.toDouble()) * numerator.toDouble() / bpm!!) * 60 * 1000).toLong()
            }
            else -> throw ParseException("Invalid time format: $time")
        }

        return timeStamp
    }
}