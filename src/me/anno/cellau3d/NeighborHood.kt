package me.anno.cellau3d

import me.anno.cellau3d.grid.Grid
import me.anno.cellau3d.grid.NibbleGridX64v2
import org.joml.Vector3i

@Suppress("unused")
enum class NeighborHood(val id: Int, val neighbors: Array<Vector3i>) {

    MOORE(
        0, arrayOf(
            // -z
            Vector3i(-1, -1, -1),
            Vector3i(+0, -1, -1),
            Vector3i(+1, -1, -1),
            Vector3i(-1, +0, -1),
            Vector3i(+0, +0, -1),
            Vector3i(+1, +0, -1),
            Vector3i(-1, +1, -1),
            Vector3i(+0, +1, -1),
            Vector3i(+1, +1, -1),
            // 0
            Vector3i(-1, -1, +0),
            Vector3i(+0, -1, +0),
            Vector3i(+1, -1, +0),
            Vector3i(-1, +0, +0),
            // center
            Vector3i(+1, +0, +0),
            Vector3i(-1, +1, +0),
            Vector3i(+0, +1, +0),
            Vector3i(+1, +1, +0),
            // +z
            Vector3i(-1, -1, +1),
            Vector3i(+0, -1, +1),
            Vector3i(+1, -1, +1),
            Vector3i(-1, +0, +1),
            Vector3i(+0, +0, +1),
            Vector3i(+1, +0, +1),
            Vector3i(-1, +1, +1),
            Vector3i(+0, +1, +1),
            Vector3i(+1, +1, +1),
        )
    ) {
        override fun count(grid: Grid, x: Int, y: Int, z: Int): Int {
            if (grid is NibbleGridX64v2) return grid.getCount(x, y, z)
            // if(grid is GridV2) return grid.getCount(x,y,z)
            return grid.get27(x, y, z) - grid.get(x, y, z)
        }
    },

    VON_NEUMANN(
        1, arrayOf(
            Vector3i(-1, 0, 0),
            Vector3i(+1, 0, 0),
            Vector3i(0, -1, 0),
            Vector3i(0, +1, 0),
            Vector3i(0, 0, -1),
            Vector3i(0, 0, +1),
        )
    ) {
        override fun count(grid: Grid, x: Int, y: Int, z: Int): Int {
            if (grid is NibbleGridX64v2) return grid.getCount(x, y, z)
            // if(grid is GridV2) return grid.getCount(x,y,z)
            return grid.get3x(x, y, z) - grid.get(x, y, z, 0) +
                    grid.get(x, y - 1, z, 0) + grid.get(x, y + 1, z, 0) +
                    grid.get(x, y, z - 1, 0) + grid.get(x, y, z + 1, 0)
        }
    };

    abstract fun count(grid: Grid, x: Int, y: Int, z: Int): Int


}