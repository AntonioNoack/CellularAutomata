package me.anno.cellau3d

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
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.SettingCategory
import kotlin.math.max

@Suppress("unused")
class CellMod : Mod() {

    override fun onInit() {
        super.onInit()
        registerCustomClass(CellularAutomaton())
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            showFPS = false
            enableVSync = true

            val ca = CellularAutomaton().apply {
                sizeX = 100
                sizeY = 100
                sizeZ = 100
                births = "1"
                survives = ""
                numStates = 5
                neighborhood = Neighborhood.VON_NEUMANN
                updatePeriod = 0.1
            }

            val world = Entity().add(ca)
            EditorState.prefabSource = world.ref
            Systems.world = world

            val ui = CustomList(false, style)
            val sceneView = SceneView(RenderView1(PlayMode.EDITING, world, style), style)
            val controls = sceneView.editControls as DraggingControls
            controls.rotationTargetDegrees.set(-20f, 30f, 0f)
            controls.zoom(10f)
            ui.add(sceneView, 3f)

            // create custom UI with only the properties that we want to show
            val properties = PanelListY(style)
            properties.add(TextPanel("Cellular Automata", style).apply {
                font = Font(font.name, font.size * 1.25f, isBold = true, isItalic = false)
                textAlignmentX = AxisAlignment.CENTER
            })

            // add all debug-actions
            val reflections = ca.getReflections()
            for (action in reflections.debugActions) {
                if (action.method.declaringClass != ca.javaClass) continue
                properties.add(
                    TextButton(NameDesc(action.title), style)
                        .addLeftClickListener {
                            action.method.invoke(ca)
                        })
            }

            for (numStepsPerSecond in listOf(1, 2, 5, 10)) {
                properties.add(
                    TextButton(NameDesc("Steps: $numStepsPerSecond/s"), style)
                        .addLeftClickListener {
                            ca.updatePeriod = 1.0 / numStepsPerSecond
                            ca.accumulatedTime = 0.5 * ca.updatePeriod
                        })
            }
            properties.add(
                TextButton(NameDesc("Maximum Steps/s"), style)
                    .addLeftClickListener {
                        ca.updatePeriod = 0.0
                        ca.accumulatedTime = 0.0
                    })

            val shownProperties = listOf(
                "color0", "color1", "sizeX", "sizeY", "sizeZ",
                "survives", "births", "numStates", "neighborhood"
            )

            val cleanInstanceForReset = CellularAutomaton()
            InspectorUtils.showProperties(
                reflections,
                shownProperties.map { name ->
                    reflections.allProperties[name]
                        ?: throw IllegalStateException("Missing property '$name'")
                },
                ca.javaClass, listOf(ca),
                HashMap(), properties, style, true,
            ) { property, _ ->
                InspectableProperty(listOf(ca), property, cleanInstanceForReset)
            }

            val presets = SettingCategory(NameDesc("Presets"), style)
                .showByDefault()

            // buttons for nice presets

            fun sierpinsky(numSteps: Int, neighborhood: Neighborhood = Neighborhood.VON_NEUMANN) {
                val minSize = numSteps * 2 + 1
                ca.sizeX = max(ca.sizeX, minSize)
                ca.sizeY = max(ca.sizeY, minSize)
                ca.sizeZ = max(ca.sizeZ, minSize)
                ca.init1()
                ca.births = "1"
                ca.survives = ""
                ca.neighborhood = neighborhood
                // run steps asynchronously
                var i = 1
                fun nextStep() {
                    ca.step()
                    if (i++ < numSteps) {
                        addEvent(1, ::nextStep)
                    }
                }
                nextStep()
            }

            val sierpinskiGroup = SettingCategory(NameDesc("Sierpinski"), style)
                .showByDefault()

            for (i in 1..7) {
                sierpinskiGroup.content.add(
                    TextButton(NameDesc("Sierpinski #$i"), style)
                        .addLeftClickListener { sierpinsky((2 shl i) - 1) })
            }

            presets.content.add(sierpinskiGroup)

            val cubeGroup = SettingCategory(NameDesc("Big Cube"), style)
                .showByDefault()

            for (i in 1..7) {
                cubeGroup.content.add(
                    TextButton(NameDesc("Big Cube#$i"), style)
                        .addLeftClickListener { sierpinsky((2 shl i) - 1, Neighborhood.MOORE) })
            }

            presets.content.add(cubeGroup)
            properties.add(presets)

            ui.add(ScrollPanelY(properties, style), 1f)
            testUI("Cellular Automata", ui)
        }
    }
}