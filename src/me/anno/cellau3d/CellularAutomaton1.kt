package me.anno.cellau3d

import me.anno.Time
import me.anno.cellau3d.Utils.parseFlags
import me.anno.cellau3d.grid.Grid
import me.anno.cellau3d.grid.GridType
import me.anno.cellau3d.grid.NibbleGridX64v2
import me.anno.ecs.Transform
import me.anno.ecs.annotations.*
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.maths.Maths
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.max
import me.anno.mesh.Shapes
import me.anno.mesh.vox.model.VoxelModel
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.mixARGB2
import me.anno.utils.Color.r
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.lists.PairArrayList
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.math.ceil
import kotlin.math.log2

/**
 * simulates a cellular automaton;
 * displays the grid with instanced rendered cubes
 * */
@Suppress("unused")
class CellularAutomaton1 : MeshSpawner(), OnUpdate {

    @NotSerializedProperty
    var g0: Grid? = null

    @NotSerializedProperty
    var g1: Grid? = null

    var gridType = GridType.BYTE_ARRAY
        set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }

    @Order(9)
    @Group("Rules")
    var survives = "4"
        set(value) {
            if (field != value) {
                field = value
                rules.survival = parseFlags(value)
            }
        }

    @Order(10)
    @Group("Rules")
    var births = "4"
        set(value) {
            if (field != value) {
                field = value
                rules.birth = parseFlags(value)
            }
        }

    @Order(11)
    @Group("Rules")
    @Range(2.0, 1024.0)
    var states = 2
        set(value) {
            if (field != value && value >= 2) {
                field = value
                rules.states = value
                reset()
            }
        }

    @Order(12)
    @Group("Rules")
    var neighborHood = NeighborHood.MOORE
        set(value) {
            field = value
            rules.neighborHood = value
            if (g0 is NibbleGridX64v2) reset()
        }

    @NotSerializedProperty
    val rules = Rules(16, 16, states, neighborHood)

    @Range(1.0, 1e9)
    var sizeX = 10
        set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }

    @Range(1.0, 1e9)
    var sizeY = 10
        set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }

    @Range(1.0, 1e9)
    var sizeZ = 10
        set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }

    var computeMode = ComputeMode.SIMPLE_SERIAL

    @DebugAction
    fun reset() {
        g0 = null
        g1 = null
        isComputing = false
        accumulatedTime = 0f
    }

    @DebugAction
    fun makeCubic() {
        sizeY = sizeX
        sizeZ = sizeX
        PropertyInspector.invalidateUI(true)
    }

    @DebugAction
    fun init1() {
        clearGrid()
        val src = if (g0 == lastSrc) g1 else g0
        if (src != null) {
            val cx = sizeX / 2
            val cy = sizeY / 2
            val cz = sizeZ / 2
            src.set(cx, cy, cz, 1)
            src.setState(cx, cy, cz, states - 1)
            isAlive = true
        }
    }

    @DebugAction
    fun init4() {
        clearGrid()
        val src = if (g0 == lastSrc) g1 else g0
        if (src != null) {
            src.createNoise(4, 0.5f, states - 1, Random())
            isAlive = !src.isEmpty()
        }
    }

    @NotSerializedProperty
    var lastSrc: Grid? = null

    @DebugProperty
    @NotSerializedProperty
    var isAlive = false

    @DebugProperty
    var updatePeriod = 0.5f

    @DebugProperty
    @NotSerializedProperty
    var accumulatedTime = 0f

    @NotSerializedProperty
    var isComputing = false

    @NotSerializedProperty
    val stateBits
        get() = ceil(log2(states.toFloat())).toInt()

    private fun createGrid() {
        val sx = sizeX
        val sy = sizeY
        val sz = sizeZ
        val g0 = g0
        if ((g0 == null || g1 == null) || g0.sx != sx || g0.sy != sy || g0.sz != sz || g0.stateBits != stateBits) {
            this.g0 = gridType.create(this)
            this.g1 = gridType.create(this)
        } else {
            g0.clear()
            g1?.clear()
        }
    }

    @DebugAction
    fun clearGrid() {
        if (g0 == null || g1 == null) {
            createGrid()
        } else {
            g0?.clear()
            g1?.clear()
        }
    }

    @DebugProperty
    @NotSerializedProperty
    var nanosPerCell = 0f

    @DebugProperty
    @NotSerializedProperty
    var ticksPerSecond = 0f

    var asyncCompute = true

    override fun onUpdate() {
        var g0 = g0
        var g1 = g1
        if (stateBits < 1) {
            LOGGER.warn("Missing states")
            return
        }
        if (g0 == null || g1 == null) {
            LOGGER.info("Creating field")
            createGrid()
            g0 = this.g0!!
            g1 = this.g1!!
            init1()
        }
        accumulatedTime += Time.deltaTime.toFloat()
        if (isAlive && accumulatedTime > updatePeriod && !isComputing) {
            isComputing = true
            if (asyncCompute) {
                pool += {
                    step(g0, g1)
                }
            } else {
                step(g0, g1)
            }
        }
    }

    // nice try ^^, but our chunk generation and mesh upload is much too slow
    @Group("Chunks")
    var generateChunks = false

    @Group("Chunks")
    var chunkSize = 13

    private val chunkMeshes = PairArrayList<Mesh, Transform>()

    private fun startTimer(): Long {
        val t0 = System.nanoTime()
        if (updatePeriod.isFinite()) {
            accumulatedTime = Maths.clamp(accumulatedTime - updatePeriod, -updatePeriod, updatePeriod)
        }
        return t0
    }

    private fun stopTimer(g0: Grid, t0: Long) {
        val t1 = System.nanoTime()
        nanosPerCell = (t1 - t0).toFloat() / g0.size
        ticksPerSecond = 1e9f / (t1 - t0)
    }

    private fun step(g0: Grid, g1: Grid) {
        val t0 = startTimer()
        val src = if (g0 == lastSrc) g1 else g0
        val dst = if (src === g0) g1 else g0
        computeMode.compute(pool, src, dst, rules)
        isAlive = !dst.isEmpty()
        lastSrc = src
        isComputing = false
        stopTimer(g0, t0)
        if (generateChunks) {
            generateChunks(dst)
        }
    }

    private fun generateChunks(grid: Grid) {
        val cs = chunkSize
        val xic = ceilDiv(sizeX, cs)
        val yic = ceilDiv(sizeY, cs)
        val zic = ceilDiv(sizeZ, cs)
        val ox = (grid.sx - cs - 1) / 2.0
        val oy = (grid.sy - cs - 1) / 2.0
        val oz = (grid.sz - cs - 1) / 2.0
        var index = 0
        val global = transform!!.globalTransform
        val transforms = transforms
        val chunkMeshes = chunkMeshes
        // could & should be cached
        val palette = IntArray(states) {
            if (it == 0) 0
            else mixARGB2(0xfbff01, 0xff0000, (it - 1) * 255 / max(1, states - 2))
        }
        for (zi in 0 until zic) {
            for (yi in 0 until yic) {
                for (xi in 0 until xic) {
                    val x0 = xi * cs
                    val y0 = yi * cs
                    val z0 = zi * cs
                    val mesh0 = if (index < chunkMeshes.size) chunkMeshes.getFirst(index) else Mesh()
                    // min(cs, sizeX - x0), min(cs, sizeY - y0), min(cs, sizeZ - z0)
                    val mesh = object : VoxelModel(cs, cs, cs) {
                        override fun getBlock(x: Int, y: Int, z: Int): Int {
                            val xj = x0 + x
                            val yj = y0 + y
                            val zj = z0 + z
                            return if (grid.get(xj, yj, zj, 0) != 0) grid.getState(xj, yj, zj) else 0
                        }
                    }.createMesh(palette, null, null, mesh0)
                    mesh.invalidateGeometry()
                    if (index >= transforms.size) transforms.add(Transform())
                    val transform = transforms[index]
                    transform.globalTransform.set(global)
                        .translate(x0 - ox, y0 - oy, z0 - oz)
                    transform.teleportUpdate()
                    if (index >= chunkMeshes.size) {
                        chunkMeshes.add(mesh, transform)
                    }
                    index++
                }
            }
        }
        for (i in chunkMeshes.size - 1 downTo index) {
            chunkMeshes.getFirst(i).destroy()
            chunkMeshes.removeAt(i * 2, keepOrder = true)
        }
    }

    override fun forEachMesh(callback: (IMesh, Material?, Transform) -> Boolean) {
        if (generateChunks) {
            try {
                for (i in 0 until chunkMeshes.size) {
                    callback(chunkMeshes.getFirst(i), null, chunkMeshes.getSecond(i))
                }
            } catch (e: NullPointerException) {
                // we don't care
            }
        } else {
            val src = if (g0 == lastSrc) g1 else g0
            if (src != null) {
                val ox = (src.sx - 1) / 2.0
                val oy = (src.sy - 1) / 2.0
                val oz = (src.sz - 1) / 2.0
                var index = 0
                val mesh = mesh
                val states = max(2, states)
                val global = transform!!.globalTransform
                val materials = materials
                src.forAllFilled { x, y, z ->
                    val transform = getTransform(index++)
                    transform.globalTransform.set(global)
                        .translate(x - ox, y - oy, z - oz)
                    transform.teleportUpdate()
                    transform.validate()
                    val matIndex = (src.getState(x, y, z) - 1) * (materials.size - 1) / max(1, states - 2)
                    callback(mesh, materials[matIndex], transform)
                }
            }
        }
    }

    override fun clone(): CellularAutomaton1 {
        val clone = CellularAutomaton1()
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as CellularAutomaton1
        dst.sizeX = sizeX
        dst.sizeY = sizeY
        dst.sizeZ = sizeZ
        dst.survives = survives
        dst.births = births
        dst.states = states
        dst.neighborHood = neighborHood
        dst.updatePeriod = updatePeriod
        dst.gridType = gridType
        dst.computeMode = computeMode
    }

    override fun destroy() {
        super.destroy()
        chunkMeshes.forEach { it.first.destroy() }
        chunkMeshes.clear()
    }

    override val className = "CellularAutomaton"

    companion object {

        private val LOGGER = LogManager.getLogger(CellularAutomaton1::class)

        val mesh = Shapes.cube05Flat.front
        val pool = ProcessingGroup("cells", 1f)

        val materials = Array(256) {
            val m = Material()
            val c = mixARGB2(0xfbff01, 0xff0000, it / 255f)
            m.diffuseBase.set(c.r() / 255f, c.g() / 255f, c.b() / 255f, 1f)
            m
        }

    }

}