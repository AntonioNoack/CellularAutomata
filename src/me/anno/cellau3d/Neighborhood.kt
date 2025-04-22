package me.anno.cellau3d

import org.joml.Vector3i

enum class Neighborhood(val id: Int, val neighbors: Array<Vector3i>) {
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
    ),
    VON_NEUMANN(
        1, arrayOf(
            Vector3i(-1, 0, 0),
            Vector3i(+1, 0, 0),
            Vector3i(0, -1, 0),
            Vector3i(0, +1, 0),
            Vector3i(0, 0, -1),
            Vector3i(0, 0, +1),
        )
    );
}