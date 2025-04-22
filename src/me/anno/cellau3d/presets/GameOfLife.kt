package me.anno.cellau3d.presets

import me.anno.cellau3d.CellMod
import me.anno.cellau3d.CellularAutomaton
import me.anno.cellau3d.Neighborhood
import me.anno.cellau3d.Patterns.initConway
import me.anno.config.DefaultConfig
import me.anno.language.translation.NameDesc
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.editor.SettingCategory
import kotlin.math.max

object GameOfLife {

    private fun gameOfLife(ca: CellularAutomaton, pattern: List<String>, numSteps: Int = 0) {
        ca.sizeX = max(ca.sizeX, pattern[0].length)
        ca.sizeZ = max(ca.sizeZ, pattern.size)
        ca.sizeY = 1
        ca.neighborhood = Neighborhood.MOORE_2D
        ca.survives = "2,3"
        ca.births = "3"
        ca.numStates = 2
        ca.initConway(pattern)
        CellMod.Companion.runSteps(ca, numSteps)
    }

    fun addGameOfWayPresets(presets: SettingCategory, ca: CellularAutomaton, onClick: () -> Unit) {
        val group = SettingCategory(NameDesc("Conway's Game Of Life"), DefaultConfig.style)
            .showByDefault()

        // spaceships
        group.content.add(
            TextButton(NameDesc("Glider"), DefaultConfig.style)
                .addLeftClickListener {
                    gameOfLife(
                        ca, listOf(
                            "  x",
                            "x x",
                            " xx"
                        )
                    )
                    onClick()
                })

        // oscillators
        group.content.add(
            TextButton(NameDesc("Blinker"), DefaultConfig.style)
                .addLeftClickListener { gameOfLife(ca, listOf("xxx")); onClick() })
        group.content.add(
            TextButton(NameDesc("Toad"), DefaultConfig.style)
                .addLeftClickListener { gameOfLife(ca, listOf(" xxx", "xxx ")); onClick() })
        group.content.add(
            TextButton(NameDesc("Beacon"), DefaultConfig.style)
                .addLeftClickListener { gameOfLife(ca, listOf("xx  ", "xx ", "  xx", "  xx")); onClick() })
        group.content.add(
            TextButton(NameDesc("Pulsar"), DefaultConfig.style)
                .addLeftClickListener {
                    gameOfLife(
                        ca, listOf(
                            "  xxx   xxx  ",
                            "             ",
                            "x    x x    x",
                            "x    x x    x",
                            "x    x x    x",
                            "  xxx   xxx  ",
                            "             ",
                            "  xxx   xxx  ",
                            "x    x x    x",
                            "x    x x    x",
                            "x    x x    x",
                            "             ",
                            "  xxx   xxx  "
                        )
                    )
                    onClick()
                })

        // needs some time to generate tons of stuff
        group.content.add(
            TextButton(NameDesc("R-Pentomino"), DefaultConfig.style)
                .addLeftClickListener {
                    gameOfLife(
                        ca, listOf(
                            " xx",
                            "xx ",
                            " x "
                        )
                    )
                    onClick()
                })
        group.content.add(
            TextButton(NameDesc("Diehard"), DefaultConfig.style)
                .addLeftClickListener {
                    gameOfLife(
                        ca, listOf(
                            "      x ",
                            "xx      ",
                            " x   xxx"
                        )
                    )
                    onClick()
                })
        group.content.add(
            TextButton(NameDesc("Acorn"), DefaultConfig.style)
                .addLeftClickListener {
                    gameOfLife(
                        ca, listOf(
                            " x     ",
                            "   x   ",
                            "xx  xxx"
                        )
                    )
                    onClick()
                })

        presets.content.add(group)
    }
}