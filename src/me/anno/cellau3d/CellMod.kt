package me.anno.cellau3d

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.systems.Systems
import me.anno.engine.RemsEngine
import me.anno.engine.WindowRenderFlags.enableVSync
import me.anno.engine.WindowRenderFlags.showFPS
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView1
import me.anno.engine.ui.render.SceneView
import me.anno.extensions.mods.Mod
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.ui.Panel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.PropertyInspector

@Suppress("unused")
class CellMod : Mod() {

    override fun onInit() {
        super.onInit()
        registerCustomClass(CellularAutomaton())
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            // runEngine() // alternative
            testUI("Cellular Automata", Companion::createGame)
        }

        @JvmStatic
        fun createGame(): Panel {

            showFPS = false
            enableVSync = true

            val logic = CellularAutomaton().apply {
                sizeX = 100
                sizeY = 100
                sizeZ = 100
                births = "1"
                survives = ""
                states = 5
                neighborHood = NeighborHood.VON_NEUMANN
                updatePeriod = 0.1
            }

            val world = Entity().add(logic)
            EditorState.prefabSource = world.ref
            Systems.world = world
            val list = CustomList(false, style)
            val view = SceneView(RenderView1(PlayMode.PLAYING, world, style), style)
            view.playControls = view.editControls
            val properties = PropertyInspector({ logic }, style, Unit)
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