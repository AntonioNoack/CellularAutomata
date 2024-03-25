package me.anno.cellau3d

import me.anno.cellau3d.grid.Grid
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.hpc.WorkSplitter

@Suppress("unused")
enum class ComputeMode(val id: Int) {
    SIMPLE_SERIAL(0) {
        override fun compute(pool: ProcessingGroup, src: Grid, dst: Grid, rules: Rules) {
            // iterate over src and apply the rules
            val neighborHood = rules.neighborHood
            val healthy = rules.states - 1
            for (z in 0 until src.sz) {
                for (y in 0 until src.sy) {
                    for (x in 0 until src.sx) {
                        when {
                            src.get(x, y, z, 0) != 0 -> {
                                // cell alive -> check if it is dying
                                var state = src.getState(x, y, z)
                                if (state != healthy || !rules.survives(neighborHood.count(src, x, y, z))) state--
                                // else cell is fine :)
                                if (state > 0) {
                                    // cell survives
                                    dst.set(x, y, z, 1)
                                    dst.setState(x, y, z, state)
                                } else {
                                    dst.set(x, y, z, 0)
                                }
                            }
                            // check if neighbor count keeps us alive
                            rules.births(neighborHood.count(src, x, y, z)) -> {
                                dst.set(x, y, z, 1)
                                dst.setState(x, y, z, healthy)
                            }
                            // else dead & stays dead
                        }
                    }
                }
            }
        }
    },
    SIMPLE_PARALLEL(1) {
        override fun compute(pool: ProcessingGroup, src: Grid, dst: Grid, rules: Rules) {
            // iterate over src and apply the rules
            val neighborHood = rules.neighborHood
            val healthy = rules.states - 1
            pool.processBalanced2d(0, 0, src.sy, src.sz, 16, 1) { y0, z0, y1, z1 ->
                for (z in z0 until z1) {
                    for (y in y0 until y1) {
                        for (x in 0 until src.sx) {
                            when {
                                src.get(x, y, z, 0) != 0 -> {
                                    // cell alive -> check if it is dying
                                    var state = src.getState(x, y, z)
                                    if (state != healthy ||
                                        !rules.survives(neighborHood.count(src, x, y, z))
                                    ) state--
                                    // else cell is fine :)
                                    if (state > 0) {
                                        // cell survives
                                        dst.set(x, y, z, 1)
                                        dst.setState(x, y, z, state)
                                    } else {
                                        dst.set(x, y, z, 0)
                                    }
                                }
                                // check if neighbor count keeps us alive
                                rules.births(neighborHood.count(src, x, y, z)) -> {
                                    dst.set(x, y, z, 1)
                                    dst.setState(x, y, z, healthy)
                                }
                                // else dead & stays dead
                            }
                        }
                    }
                }
            }
        }
    },
    NEAR_CELLS_SERIAL(2) {
        override fun compute(pool: ProcessingGroup, src: Grid, dst: Grid, rules: Rules) {

            // two ideas:
            // for all active cells, compute themselves and their potential neighbors
            // for all cells, compute themselves

            // iterate over src and apply the rules
            val neighborHood = rules.neighborHood
            val healthy = rules.states - 1

            src.forAllFilled { x, y, z ->
                // cell alive -> check if it is dying
                var state = src.getState(x, y, z)
                if (state != healthy || !rules.survives(neighborHood.count(src, x, y, z))) state--
                // else cell is fine :)
                if (state > 0) {
                    // cell survives
                    dst.set(x, y, z, 1)
                    dst.setState(x, y, z, state)
                } else dst.set(x, y, z, 0)
                val nn = neighborHood.neighbors
                for (i in nn.indices) {
                    val di = nn[i]
                    val xi = x + di.x
                    val yi = y + di.y
                    val zi = z + di.z
                    if (src.get(xi, yi, zi, 1) == 0 && rules.births(neighborHood.count(src, xi, yi, zi))) {
                        dst.set(xi, yi, zi, 1)
                        dst.setState(xi, yi, zi, healthy)
                    }
                }
            }
        }
    },

    NEAR_CELLS_PARALLEL(3) {
        override fun compute(pool: ProcessingGroup, src: Grid, dst: Grid, rules: Rules) {
            // two ideas:
            // for all active cells, compute themselves and their potential neighbors
            // for all cells, compute themselves

            // iterate over src and apply the rules
            val neighborHood = rules.neighborHood
            val healthy = rules.states - 1

            // to do speed up the neighborhoods (?) -> low priority, because rendering is slower than simulating currently anyways (50³)
            src.forAllFilled(pool) { x, y, z ->
                // cell alive -> check if it is dying
                var state = src.getState(x, y, z)
                if (state != healthy || !rules.survives(neighborHood.count(src, x, y, z))) state--
                // else cell is fine :)
                if (state > 0) {
                    // cell survives
                    dst.set(x, y, z, 1)
                    dst.setState(x, y, z, state)
                } else {
                    dst.set(x, y, z, 0)
                }
                val nn = neighborHood.neighbors
                for (i in nn.indices) {
                    val di = nn[i]
                    val xi = x + di.x
                    val yi = y + di.y
                    val zi = z + di.z
                    if (src.get(xi, yi, zi, 1) == 0 && rules.births(neighborHood.count(src, xi, yi, zi))) {
                        dst.set(xi, yi, zi, 1)
                        dst.setState(xi, yi, zi, healthy)
                    }
                }
            }
        }
    },
    GPU(4) {
        override fun compute(pool: ProcessingGroup, src: Grid, dst: Grid, rules: Rules) {
            throw UnsupportedOperationException("This mode must be executed on the GPU")
        }
    };

    abstract fun compute(pool: ProcessingGroup, src: Grid, dst: Grid, rules: Rules)
}
