package me.anno.cellau3d

import me.anno.Engine
import me.anno.cellau3d.CellularAutomaton1.Companion.pool
import me.anno.cellau3d.Utils.parseFlags
import me.anno.cellau3d.grid.Grid
import me.anno.cellau3d.grid.GridType
import me.anno.cellau3d.grid.NibbleGridX64v2
import me.anno.ecs.Transform
import me.anno.ecs.annotations.*
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.shaders.Texture3DBTMaterial
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.io.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.mesh.Shapes
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.Color.toVecRGB
import me.anno.utils.structures.lists.PairArrayList
import org.apache.logging.log4j.LogManager
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.sign

/**
 * simulates a cellular automaton;
 * displays the grid with a raytracing fragment shader
 * */
@Suppress("unused")
class CellularAutomaton2 : ProceduralMesh() {

    private val texture = Texture3D("cells", 1, 1, 1)
    private val material = Material.create()
    private val shader = Texture3DBTMaterial()

    init {
        material.shader = shader
        mesh2.materialInst = material
        mesh2.inverseOutline = true // for correct outlines, since we render on the back faces
        texture.filtering = GPUFiltering.TRULY_NEAREST
    }

    @Type("Color3")
    var color0 = 0x1d73d3.toVecRGB()
        set(value) {
            field.set(value)
            setColors()
        }

    @Type("Color3")
    var color1 = 0xffffff.toVecRGB()
        set(value) {
            field.set(value)
            setColors()
        }

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
                invalidateMesh()
                reset()
            }
        }

    @Range(1.0, 1e9)
    var sizeY = 10
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
                reset()
            }
        }

    @Range(1.0, 1e9)
    var sizeZ = 10
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
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

    // todo makeCubic: set CSet
    @DebugAction
    fun makeCubic() {
        sizeY = sizeX
        sizeZ = sizeX
        invalidateMesh()
        PropertyInspector.invalidateUI()
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
            src.spawnNoise(4, 0.5f, states - 1, Random())
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

    @DebugProperty
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
    var nanosPerCell = 0L

    @DebugProperty
    @NotSerializedProperty
    var ticksPerSecond = 0f

    var asyncCompute = true

    @DebugAction
    fun step() {
        updatePeriod = Float.POSITIVE_INFINITY
        if (!isComputing) {
            val g0 = g0 ?: return
            val g1 = g1 ?: return
            step(g0, g1)
        }
    }

    override fun onUpdate(): Int {
        var g0 = g0
        var g1 = g1
        if (stateBits < 1) {
            LOGGER.warn("Missing states")
            return 1
        }
        if (g0 == null || g1 == null) {
            LOGGER.info("Creating field")
            createGrid()
            g0 = this.g0!!
            g1 = this.g1!!
            init1()
        }
        accumulatedTime += Engine.deltaTime
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
        return 1 // if (isAlive) 1 else 16 // returning 16 causes issues, why?
    }

    private val chunkMeshes = PairArrayList<Mesh, Transform>()

    private fun step(g0: Grid, g1: Grid) {
        val t0 = System.nanoTime()
        if (updatePeriod.isFinite()) {
            accumulatedTime = clamp(accumulatedTime - updatePeriod, -updatePeriod, updatePeriod)
        }
        val src = if (g0 == lastSrc) g1 else g0
        val dst = if (src === g0) g1 else g0
        computeMode.compute(pool, src, dst, rules)
        isAlive = !dst.isEmpty()
        lastSrc = src
        isComputing = false
        val t1 = System.nanoTime()
        nanosPerCell = (t1 - t0) / g0.size
        ticksPerSecond = 1e9f / (t1 - t0)
        // generate image for texture
        val sizeX = dst.sx
        val sizeY = dst.sy
        val sizeZ = dst.sz
        val data = Texture2D.bufferPool[sizeX * sizeY * sizeZ, false]
        data.position(0)
        for (z in 0 until sizeZ) {
            for (y in 0 until sizeY) {
                for (x in 0 until sizeX) {
                    val color = dst.getStateIfFull(x, y, z).toByte()
                    data.put(color)
                }
            }
        }
        data.position(0)
        if (GFX.isGFXThread()) {
            createTexture(data, sizeX, sizeY, sizeZ)
        } else {
            GFX.addGPUTask(1) {
                createTexture(data, sizeX, sizeY, sizeZ)
            }
        }
    }

    private fun createTexture(data: ByteBuffer, sizeX: Int, sizeY: Int, sizeZ: Int) {
        if (texture.w != sizeX || texture.h != sizeY || texture.d != sizeZ) {
            texture.destroy() // doesn't matter
            texture.w = sizeX
            texture.h = sizeY
            texture.d = sizeZ
        }
        texture.createMonochrome(data)
        shader.blocks = texture
        shader.size.set(sizeX, sizeY, sizeZ)
        setColors()
        Texture2D.bufferPool.returnBuffer(data)
    }

    private fun setColors() {
        val c0 = color0
        val c1 = color1
        val div = max(1, states - 2)
        shader.color0.set(c0).lerp(c1, 1f + 1f / div)
        shader.color1.set(c0).lerp(c1, 1f - 255f / div)
    }

    override fun generateMesh() {
        val base = Shapes.flatCube.back
        val positions = mesh2.positions ?: base.positions!!
        val sx = max(sizeX.toFloat() / 2f, 0.5f)
        val sy = max(sizeY.toFloat() / 2f, 0.5f)
        val sz = max(sizeZ.toFloat() / 2f, 0.5f)
        for (i in positions.indices step 3) {
            // use the sign to preserve the original shape,
            // since this function might run recursively on the original data
            positions[i + 0] = sign(positions[i + 0]) * sx
            positions[i + 1] = sign(positions[i + 1]) * sy
            positions[i + 2] = sign(positions[i + 2]) * sz
        }
        mesh2.positions = positions
        mesh2.invalidateGeometry()
    }

    override fun clone(): CellularAutomaton2 {
        val clone = CellularAutomaton2()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CellularAutomaton2
        clone.sizeX = sizeX
        clone.sizeY = sizeY
        clone.sizeZ = sizeZ
        clone.survives = survives
        clone.births = births
        clone.states = states
        clone.neighborHood = neighborHood
        clone.updatePeriod = updatePeriod
        clone.gridType = gridType
        clone.computeMode = computeMode
    }

    override fun destroy() {
        super.destroy()
        chunkMeshes.forEachA { it.destroy() }
        chunkMeshes.clear()
        texture.destroy()
    }

    override val className = "CellularAutomaton2"

    companion object {
        private val LOGGER = LogManager.getLogger(CellularAutomaton2::class)
    }

}