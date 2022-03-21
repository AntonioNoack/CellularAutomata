package me.anno.cellau3d

import kotlin.math.max
import kotlin.math.min

object Utils {

    fun parseFlags(str: String, min: Int = 1, max: Int = 26): Int {
        var result = 0
        // input: 1-2, 3, 4,5
        // 3-4-5
        // output: flags
        var i = 0
        var lastNum = -1
        var hadSplitter = false
        while (i < str.length) {
            when (str[i]) {
                in '0'..'9' -> {
                    var num = 0
                    // will be true at least once
                    number@ while (i < str.length && str[i] in '0'..'9') {
                        num = 10 * num + str[i].code - '0'.code
                        i++
                    }
                    when {
                        hadSplitter && lastNum > -1 -> {
                            // range
                            val i0 = lastNum
                            val i1 = num
                            for (v in max(i0, min)..min(i1, max)) {
                                result = result or (1 shl v)
                            }
                            hadSplitter = false
                            lastNum = -1
                        }
                        lastNum in min..max -> {
                            hadSplitter = false
                            result = result or (1 shl lastNum)
                            lastNum = num
                        }
                        else -> {
                            hadSplitter = false
                            lastNum = num
                        }
                    }
                }
                '-' -> {
                    hadSplitter = true
                    i++
                }
                else -> i++
            }
        }
        if (lastNum in min..max) {
            result = result or (1 shl lastNum)
        }
        return result
    }

}