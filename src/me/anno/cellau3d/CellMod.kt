package me.anno.cellau3d

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.ECSRegistry
import me.anno.engine.RemsEngine
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.extensions.ExtensionLoader
import me.anno.extensions.mods.Mod
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.ui.Panel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.PropertyInspector

@Suppress("unused")
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
            // runEngine() // alternative
            testUI("Cellular Automata", Companion::createGame)
        }

        @JvmStatic
        fun createGame(): Panel {
            registerCustomClass(Material())
            registerCustomClass(CellularAutomaton2())
            DefaultConfig["debug.ui.showFPS"] = false
            ECSRegistry.init()
            val ca = CellularAutomaton2()
            ca.sizeX = 100
            ca.sizeY = 100
            ca.sizeZ = 100
            ca.births = "1"
            ca.survives = ""
            ca.states = 5
            ca.neighborHood = NeighborHood.VON_NEUMANN
            ca.updatePeriod = 0.1f
            val world = Entity()
            world.add(ca)
            val component = world.components.first()
            EditorState.prefabSource = world.ref
            EditorState.select(component)
            val list = CustomList(false, style)
            val view = SceneView(PlayMode.EDITING, style)
            val properties = PropertyInspector({ EditorState.selection }, style)
            PrefabInspector.currentInspector = PrefabInspector(world.ref)
            view.weight = 2f
            properties.weight = 1f
            list.add(properties)
            list.add(view)
            return list
        }

        private fun runEngine() {
            RemsEngine().run()
        }
    }
}