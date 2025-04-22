package me.anno.cellau3d

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.systems.Systems
import me.anno.engine.RemsEngine
import me.anno.engine.WindowRenderFlags.enableVSync
import me.anno.engine.WindowRenderFlags.showFPS
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView1
import me.anno.engine.ui.render.SceneView
import me.anno.extensions.mods.Mod
import me.anno.fonts.Font
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
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

            val controls = view.editControls as DraggingControls
            controls.rotationTargetDegrees.set(-20f, 30f, 0f)
            controls.zoom(10f)

            val properties = PropertyInspector({ logic }, style, Unit)
            PrefabInspector.currentInspector = PrefabInspector(world.ref)
            list.add(view, 3f)
            list.add(properties, 1f)

            // todo create custom UI with only the properties that we want to show

            val properties2 = PanelListY(style)
            properties2.add(TextPanel("Cellular Automata", style).apply {
                font = Font(font.name, font.size * 1.25f, isBold = true, isItalic = false)
            })

            // add all debug-actions
            val reflections = logic.getReflections()
            for (action in reflections.debugActions) {
                if (action.method.declaringClass != logic.javaClass) continue
                properties2.add(
                    TextButton(NameDesc(action.title), style)
                        .addLeftClickListener {
                            action.method.invoke(logic)
                        })
            }

            list.add(ScrollPanelY(properties2, style), 1f)
            return list
        }

        private fun runEngine() {
            RemsEngine().run()
        }
    }
}