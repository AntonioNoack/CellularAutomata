package me.anno.cellau3d.grid

import me.anno.cellau3d.CellularAutomaton

@Suppress("unused")
enum class GridType(val id: Int) {
    BYTE_ARRAY(0) {
        override fun create(ca: CellularAutomaton): Grid {
            return ByteArrayGrid(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
    },
    INT_ARRAY(1) {
        override fun create(ca: CellularAutomaton): Grid {
            return IntArrayGrid(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
    },
    NIBBLE(2) {
        override fun create(ca: CellularAutomaton): Grid {
            return NibbleGrid(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
    },
    NIBBLE_X64(3) {
        override fun create(ca: CellularAutomaton): Grid {
            return NibbleGridX64(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
    },
    NIBBLE_X64_V2(4) {
        override fun create(ca: CellularAutomaton): Grid {
            return NibbleGridX64v2(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits, ca.neighborHood)
        }
    };

    abstract fun create(ca: CellularAutomaton): Grid
}