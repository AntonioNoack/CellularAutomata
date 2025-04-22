package me.anno.cellau3d.presets

import me.anno.cellau3d.CellMod.Companion.runSteps
import me.anno.cellau3d.CellularAutomaton
import me.anno.cellau3d.Neighborhood
import me.anno.config.DefaultConfig.style
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.editor.SettingCategory
import kotlin.math.max

object Sierpinski {

    private fun sierpinsky(ca: CellularAutomaton, numSteps: Int, neighborhood: Neighborhood) {
        val minSize = numSteps * 2 + 1
        ca.sizeX = max(ca.sizeX, minSize)
        ca.sizeY = max(ca.sizeY, minSize)
        ca.sizeZ = max(ca.sizeZ, minSize)
        ca.init1()
        ca.births = "1"
        ca.survives = ""
        ca.neighborhood = neighborhood
        // run steps asynchronously
        runSteps(ca, numSteps)
    }

    fun addSierpinskiPresets(presets: SettingCategory, ca: CellularAutomaton, onClick: () -> Unit) {
        val group = SettingCategory(NameDesc("Sierpinski"), style)
            .showByDefault()
        for (i in 1..7) {
            group.content.add(
                TextButton(NameDesc("Sierpinski #$i"), style)
                    .addLeftClickListener {
                        sierpinsky(ca, (2 shl i) - 1, Neighborhood.VON_NEUMANN)
                        onClick()
                    })
        }
        presets.content.add(group)
    }

    fun addBigCubePresets(presets: SettingCategory, ca: CellularAutomaton, onClick: () -> Unit) {
        val group = SettingCategory(NameDesc("Big Cube"), style)
            .showByDefault()
        for (i in 1..7) {
            group.content.add(
                TextButton(NameDesc("Big Cube#$i"), style)
                    .addLeftClickListener {
                        sierpinsky(ca, (2 shl i) - 1, Neighborhood.MOORE)
                        onClick()
                    })
        }
        presets.content.add(group)
    }

}