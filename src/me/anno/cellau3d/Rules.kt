package me.anno.cellau3d

class Rules(var survival: Int, var birth: Int, var states: Int, var neighborHood: NeighborHood) {

    fun survives(neighbors: Int): Boolean {
        return (survival and (1 shl neighbors)) != 0
    }

    fun spawns(neighbors: Int): Boolean {
        return (birth and (1 shl neighbors)) != 0
    }

}