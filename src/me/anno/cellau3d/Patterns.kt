package me.anno.cellau3d

import me.anno.cellau3d.CellularAutomaton.Companion.createMonochrome
import me.anno.utils.types.Booleans.toInt
import org.joml.AABBi

object Patterns {
    fun CellularAutomaton.init4Impl() {
        ensureCorrectSize()
        val cx = sizeX / 2
        val cy = sizeY / 2
        val cz = sizeZ / 2
        val bounds = AABBi(
            cx - 2, cy - 2, cz - 2,
            cx + 1, cy + 1, cz + 1
        )
        val buffer = fillData { x, y, z ->
            if (bounds.testPoint(x, y, z)) numStates - 1
            else 0
        }
        srcTexture.createMonochrome(buffer)
    }

    fun CellularAutomaton.initBoundingBoxImpl() {
        ensureCorrectSize()
        val buffer = fillData { x, y, z ->
            val x2 = x == 0 || x == sizeX - 1
            val y2 = y == 0 || y == sizeY - 1
            val z2 = z == 0 || z == sizeZ - 1
            if (x2.toInt() + y2.toInt() + z2.toInt() == 2) numStates - 1
            else 0
        }
        srcTexture.createMonochrome(buffer)
    }
}