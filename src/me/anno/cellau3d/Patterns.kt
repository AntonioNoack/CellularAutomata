package me.anno.cellau3d

import me.anno.utils.types.Booleans.toInt
import org.joml.AABBi

object Patterns {
    fun CellularAutomaton.init4Impl() {
        val cx = sizeX / 2
        val cy = sizeY / 2
        val cz = sizeZ / 2
        val bounds = AABBi(
            cx - 2, cy - 2, cz - 2,
            cx + 1, cy + 1, cz + 1
        )
        initWithFunction { x, y, z ->
            if (bounds.testPoint(x, y, z)) numStates - 1
            else 0
        }
    }

    fun CellularAutomaton.initBoundingBoxImpl() {
        initWithFunction { x, y, z ->
            val x2 = x == 0 || x == sizeX - 1
            val y2 = y == 0 || y == sizeY - 1
            val z2 = z == 0 || z == sizeZ - 1
            if (x2.toInt() + y2.toInt() + z2.toInt() == 2) numStates - 1
            else 0
        }
    }

    fun CellularAutomaton.initConway(lines: List<String>) {
        val cx = (sizeX - lines[0].length) / 2
        val cz = (sizeZ - lines.size) / 2
        initWithFunction { x, _, z ->
            val rx = x - cx
            val rz = z - cz
            val line = lines.getOrNull(rz) ?: ""
            val char = if (rx in line.indices) line[rx] else ' '
            if (char != ' ') numStates - 1 else 0
        }
    }
}