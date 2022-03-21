package me.anno.cellau3d

import me.anno.engine.RemsEngine
import me.anno.extensions.ExtensionLoader
import me.anno.extensions.mods.Mod
import me.anno.io.ISaveable.Companion.registerCustomClass

class CellMod : Mod() {

    override fun onInit() {
        super.onInit()
        registerCustomClass(CellularAutomaton1())
        registerCustomClass(CellularAutomaton2())
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ExtensionLoader.loadMainInfo()
            RemsEngine().run()
        }
    }

}