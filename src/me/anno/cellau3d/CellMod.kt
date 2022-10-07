package me.anno.cellau3d

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.RemsEngine
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.extensions.ExtensionLoader
import me.anno.extensions.mods.Mod
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.zip.InnerTmpFile
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
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
            runDemo() // showcase
        }

        private fun runDemo() {
            registerCustomClass(Material())
            registerCustomClass(CellularAutomaton2())
            testUI {
                DefaultConfig["debug.ui.showFPS"] = false
                ECSRegistry.init()
                val prefab = Prefab("Entity")
                val prefabSource = InnerTmpFile.InnerTmpPrefabFile(prefab)
                val pi = prefab.add(Path.ROOT_PATH, 'c', "CellularAutomaton2")
                prefab[pi, "sizeX"] = 100
                prefab[pi, "sizeY"] = 100
                prefab[pi, "sizeZ"] = 100
                prefab[pi, "births"] = "1"
                prefab[pi, "survives"] = ""
                prefab[pi, "states"] = 5
                prefab[pi, "neighborHood"] = NeighborHood.VON_NEUMANN
                prefab[pi, "updatePeriod"] = 0.1f
                val world = prefab.getSampleInstance() as Entity
                val component = world.components.first()
                EditorState.prefabSource = prefabSource
                EditorState.select(component)
                val list = CustomList(false, style)
                val view = SceneView(EditorState, PlayMode.EDITING, style)
                val properties = PropertyInspector({ EditorState.selection }, style)
                PrefabInspector.currentInspector = PrefabInspector(prefabSource)
                list.add(view.setWeight(2f))
                list.add(properties.setWeight(1f))
                list
            }
        }

        private fun runEngine() {
            RemsEngine().run()
        }

    }

}