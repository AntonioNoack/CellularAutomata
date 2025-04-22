package me.anno.cellau3d

import kotlin.math.max
import kotlin.math.min

object FlagParser {
    /**
     * input: 1-5, 7, 12,
     * intermediate: 1,2,3,4,5,7,12,
     * output: 1<<1 + 1<<2 + ... + 1<<5 + 1<<7 + 1<<12
     * */
    fun parseFlags(str: String, min: Int = 1, max: Int = 26): Int {
        var result = 0
        var i = 0
        var lastNumber = -1
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
                        hadSplitter && lastNumber > -1 -> {
                            // range
                            val i0 = lastNumber
                            val i1 = num
                            for (v in max(i0, min)..min(i1, max)) {
                                result = result or (1 shl v)
                            }
                            hadSplitter = false
                            lastNumber = -1
                        }
                        lastNumber in min..max -> {
                            hadSplitter = false
                            result = result or (1 shl lastNumber)
                            lastNumber = num
                        }
                        else -> {
                            hadSplitter = false
                            lastNumber = num
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
        if (lastNumber in min..max) {
            result = result or (1 shl lastNumber)
        }
        return result
    }

}