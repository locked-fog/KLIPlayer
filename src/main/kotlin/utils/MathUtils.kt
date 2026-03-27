package com.lockedfog.kliplayer.utils

import com.lockedfog.kliplayer.exception.ParseException

object MathUtils {
    
    /**
     * Calculates the power of a base raised to an exponent.
     * @param base the base number
     * @param exponent the exponent
     * @return the result of base raised to the power of exponent
     */
    fun pow(base: Double, exponent: Double): Double {
        if (base == 0.0 && exponent <= 0.0) {
            throw ParseException("Base cannot be zero when exponent is less than or equal to zero.")
        }
        return Math.pow(base, exponent)
    }

    /**
     * Calculates the logarithm of a value with a specified base.
     * @param value the value for which to calculate the logarithm
     * @param base the base of the logarithm
     * @return the result of the logarithm calculation
     */
    fun log(value: Double, base: Double): Double {
        if (value <= 0) {
            throw ParseException("Value must be greater than zero.")
        }
        if (base == 1.0) {
            throw ParseException("Base must be greater than one.")
        }
        return Math.log(value) / Math.log(base)
    }

    /**
     * Rounds a double value to the nearest integer.
     * @param value the value to round
     * @return the rounded integer
     */
    fun round(value: Double): Int {
        return Math.round(value).toInt()
    }
}