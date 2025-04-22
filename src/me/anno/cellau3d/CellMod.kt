package me.anno.cellau3d

import me.anno.cellau3d.presets.GameOfLife.addGameOfWayPresets
import me.anno.cellau3d.presets.Sierpinski.addBigCubePresets
import me.anno.cellau3d.presets.Sierpinski.addSierpinskiPresets
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.systems.Systems
import me.anno.engine.Events.addEvent
import me.anno.engine.WindowRenderFlags.enableVSync
import me.anno.engine.WindowRenderFlags.showFPS
import me.anno.engine.inspector.InspectableProperty
import me.anno.engine.inspector.InspectorUtils
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
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.SettingCategory

@Suppress("unused")
class CellMod : Mod() {

    override fun onInit() {
        super.onInit()
        registerCustomClass(CellularAutomaton())
    }

    companion object {

        // todo show border for Game Of Life?

        @JvmStatic
        fun main(args: Array<String>) {

            showFPS = false
            enableVSync = true

            val ca = createDefaultAutomaton()
            val world = Entity().add(ca)
            EditorState.prefabSource = world.ref
            Systems.world = world

            val ui = CustomList(false, style)
            ui.add(createSceneUI(world), 3f)
            ui.add(createControlsUI(ca), 1f)

            testUI("Cellular Automata", ui)
        }

        private fun createSceneUI(world: Entity): Panel {
            val sceneView = SceneView(RenderView1(PlayMode.EDITING, world, style), style)
            val controls1 = sceneView.editControls as DraggingControls
            controls1.rotationTargetDegrees.set(-20f, 30f, 0f)
            controls1.zoom(10f)
            return sceneView
        }

        private fun createControlsUI(ca: CellularAutomaton): Panel {
            // create custom UI with only the properties that we want to show
            val controls = PanelListY(style)
            controls.add(TextPanel("Cellular Automata", style).apply {
                font = Font(font.name, font.size * 1.25f, isBold = true, isItalic = false)
                textAlignmentX = AxisAlignment.CENTER
            })

            // add all debug-actions
            val reflections = ca.getReflections()
            for (action in reflections.debugActions) {
                if (action.method.declaringClass != ca.javaClass) continue
                controls.add(
                    TextButton(NameDesc(action.title), style)
                        .addLeftClickListener {
                            action.method.invoke(ca)
                        })
            }

            for (numStepsPerSecond in listOf(1, 2, 5, 10)) {
                controls.add(
                    TextButton(NameDesc("Steps: $numStepsPerSecond/s"), style)
                        .addLeftClickListener {
                            ca.updatePeriod = 1.0 / numStepsPerSecond
                            ca.accumulatedTime = 0.5 * ca.updatePeriod
                        })
            }
            controls.add(
                TextButton(NameDesc("Maximum Steps/s"), style)
                    .addLeftClickListener {
                        ca.updatePeriod = 0.0
                        ca.accumulatedTime = 0.0
                    })

            val propertiesWrapper = PanelListY(style)
            showProperties(ca, propertiesWrapper)
            controls.add(propertiesWrapper)

            val presets = SettingCategory(NameDesc("Presets"), style)
                .showByDefault()

            fun onPresetClick() {
                // "update" all input fields
                // todo ideally, just set their values...
                propertiesWrapper.clear()
                showProperties(ca, propertiesWrapper)
            }

            // buttons for nice presets
            addSierpinskiPresets(presets, ca, ::onPresetClick)
            addBigCubePresets(presets, ca, ::onPresetClick)
            addGameOfWayPresets(presets, ca, ::onPresetClick)

            controls.add(presets)

            return ScrollPanelY(controls, style)
        }

        private fun createDefaultAutomaton(): CellularAutomaton {
            return CellularAutomaton().apply {
                sizeX = 100
                sizeY = 100
                sizeZ = 100
                births = "1"
                survives = ""
                numStates = 5
                neighborhood = Neighborhood.VON_NEUMANN
                updatePeriod = 0.1
            }
        }

        private fun showProperties(ca: CellularAutomaton, parent: PanelList) {

            val shownProperties = listOf(
                "color0", "color1", "sizeX", "sizeY", "sizeZ",
                "survives", "births", "numStates", "neighborhood"
            )

            val cleanInstanceForReset = createDefaultAutomaton()
            val reflections = cleanInstanceForReset.getReflections()
            InspectorUtils.showProperties(
                reflections,
                shownProperties.map { name ->
                    reflections.allProperties[name]
                        ?: throw IllegalStateException("Missing property '$name'")
                },
                ca.javaClass, listOf(ca),
                HashMap(), parent, style, true,
            ) { property, _ ->
                InspectableProperty(listOf(ca), property, cleanInstanceForReset)
            }
        }

        fun runSteps(ca: CellularAutomaton, numSteps: Int) {
            if (numSteps < 1) {
                ca.show()
                return
            }
            var i = 1
            fun nextStep() {
                ca.step()
                if (i++ < numSteps) {
                    addEvent(1, ::nextStep)
                }
            }
            nextStep()
        }
    }
}