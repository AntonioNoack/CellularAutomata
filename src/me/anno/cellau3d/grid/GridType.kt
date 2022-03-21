package me.anno.cellau3d.grid

import me.anno.cellau3d.CellularAutomaton1
import me.anno.cellau3d.CellularAutomaton2

enum class GridType(val id: Int) {
    BYTE_ARRAY(0) {
        override fun create(ca: CellularAutomaton1): Grid {
            return ByteArrayGrid(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
        override fun create(ca: CellularAutomaton2): Grid {
            return ByteArrayGrid(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
    },
    INT_ARRAY(0) {
        override fun create(ca: CellularAutomaton1): Grid {
            return IntArrayGrid(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
        override fun create(ca: CellularAutomaton2): Grid {
            return IntArrayGrid(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
    },
    NIBBLE(1) {
        override fun create(ca: CellularAutomaton1): Grid {
            return NibbleGrid(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
        override fun create(ca: CellularAutomaton2): Grid {
            return NibbleGrid(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
    },
    NIBBLE_X64(2) {
        override fun create(ca: CellularAutomaton1): Grid {
            return NibbleGridX64(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
        override fun create(ca: CellularAutomaton2): Grid {
            return NibbleGridX64(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits)
        }
    },
    NIBBLE_X64_V2(3) {
        override fun create(ca: CellularAutomaton1): Grid {
            return NibbleGridX64v2(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits, ca.neighborHood)
        }
        override fun create(ca: CellularAutomaton2): Grid {
            return NibbleGridX64v2(ca.sizeX, ca.sizeY, ca.sizeZ, ca.stateBits, ca.neighborHood)
        }
    }
    ;

    abstract fun create(ca: CellularAutomaton1): Grid
    abstract fun create(ca: CellularAutomaton2): Grid
}