package me.anno.cellau3d

import me.anno.Build
import me.anno.Time
import me.anno.cellau3d.CellularAutomaton1.Companion.pool
import me.anno.cellau3d.Utils.parseFlags
import me.anno.cellau3d.Utils.synchronizeGraphics
import me.anno.cellau3d.grid.Grid
import me.anno.cellau3d.grid.GridType
import me.anno.cellau3d.grid.NibbleGridX64v2
import me.anno.ecs.Transform
import me.anno.ecs.annotations.*
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.material.Texture3DBTMaterial
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture3D
import me.anno.maths.Maths.clamp
import me.anno.mesh.Shapes
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.Color.toVecRGB
import me.anno.utils.pooling.Pools.byteBufferPool
import me.anno.utils.structures.lists.PairArrayList
import me.anno.utils.types.Arrays.resize
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
class CellularAutomaton2 : ProceduralMesh(), OnUpdate {

    @NotSerializedProperty
    var texture0 = Texture3D("cells", 1, 1, 1)

    @NotSerializedProperty
    var texture1 = Texture3D("cells", 1, 1, 1)

    private val material = Texture3DBTMaterial()

    init {
        data.material = material.ref
        data.inverseOutline = true // for correct outlines, since we render on the back faces
        texture0.filtering = Filtering.TRULY_NEAREST
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

    fun swapTextures() {
        val tmp = texture0
        texture0 = texture1
        texture1 = tmp
    }

    @DebugAction
    fun reset() {
        g0 = null
        g1 = null
        isComputing = false
        accumulatedTime = 0f
        invalidateAABB()
    }

    @DebugAction
    fun makeCubic() {
        sizeY = sizeX
        sizeZ = sizeX
        val prefabPath = prefabPath
        root.prefab?.set(prefabPath, "sizeY", sizeY)
        root.prefab?.set(prefabPath, "sizeZ", sizeZ)
        invalidateMesh()
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
        // fake missing creation
        texture0.wasCreated = false
        texture1.wasCreated = false
    }

    @DebugProperty
    @NotSerializedProperty
    var nanosPerCell = 0f

    @DebugProperty
    @NotSerializedProperty
    var ticksPerSecond = 0f

    var asyncCompute = true

    private val gpuCompute
        get() = computeMode == ComputeMode.GPU

    @DebugAction
    fun step() {
        updatePeriod = Float.POSITIVE_INFINITY
        if (!isComputing) {
            val g0 = g0 ?: return
            val g1 = g1 ?: return
            if (gpuCompute) {
                if (!texture0.isCreated() || !texture1.isCreated()) {
                    updateTexture(g0)
                }
                gpuStep(this)
                updateTexture(g0)
            } else {
                step(g0, g1)
            }
        }
    }

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
            when {
                gpuCompute -> {
                    if (!texture0.isCreated() || !texture1.isCreated()) {
                        createTexture(g0)
                    }
                    // synchronization is needed for reliable time measurements
                    // because OpenGL is an async API
                    if (synchronizeGPU) synchronizeGraphics()
                    val t0 = startTimer()
                    gpuStep(this)
                    if (synchronizeGPU) {
                        synchronizeGraphics()
                        stopTimer(g0, t0)
                    } else invalidateTimer()
                    updateTexture(g0)
                    isComputing = false
                }

                asyncCompute -> {
                    pool += {
                        step(g0, g1)
                    }
                }

                else -> step(g0, g1)
            }
        }
    }

    @Docs("Delivers accurate time measurements, but slows down the whole engine")
    @SerializedProperty
    var synchronizeGPU = Build.isDebug

    private val chunkMeshes = PairArrayList<Mesh, Transform>()

    private fun startTimer(): Long {
        val t0 = System.nanoTime()
        if (updatePeriod.isFinite()) {
            accumulatedTime = clamp(accumulatedTime - updatePeriod, -updatePeriod, updatePeriod)
        }
        return t0
    }

    private fun stopTimer(g0: Grid, t0: Long) {
        val t1 = System.nanoTime()
        nanosPerCell = (t1 - t0).toFloat() / g0.size
        ticksPerSecond = 1e9f / (t1 - t0)
    }

    private fun invalidateTimer() {
        nanosPerCell = Float.NaN
        ticksPerSecond = Float.NaN
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
        // generate image for texture
        val data = packData(dst)
        if (GFX.isGFXThread()) {
            createTexture(dst, data)
        } else {
            addGPUTask("CA::createTexture", 1) {
                createTexture(dst, data)
            }
        }
    }

    private fun packData(dst: Grid): ByteBuffer {
        val data = byteBufferPool[sizeX * sizeY * sizeZ, false, false]
        for (z in 0 until dst.sz) {
            for (y in 0 until dst.sy) {
                for (x in 0 until dst.sx) {
                    val color = dst.getStateIfFull(x, y, z).toByte()
                    data.put(color)
                }
            }
        }
        data.flip()
        return data
    }

    private fun updateTexture(grid: Grid) {
        val sizeX = grid.sx
        val sizeY = grid.sy
        val sizeZ = grid.sz
        if (texture0.width != sizeX || texture0.height != sizeY || texture0.depth != sizeZ) {
            texture0.destroy() // doesn't matter
            texture0.width = sizeX
            texture0.height = sizeY
            texture0.depth = sizeZ
            texture1.destroy()
            texture1.width = sizeX
            texture1.height = sizeY
            texture1.depth = sizeZ
        }
        material.blocks = texture0
        setColors()
    }

    private fun createTexture(grid: Grid, data: ByteBuffer) {
        updateTexture(grid)
        texture0.createMonochrome(data)
        if (gpuCompute) texture1.createMonochrome(data)
        byteBufferPool.returnBuffer(data)
    }

    private fun createTexture(grid: Grid) {
        val data = packData(grid)
        createTexture(grid, data)
    }

    private fun setColors() {
        material.color0.set(color0)
        material.color1.set(color1)
        material.limitColors(states - 1)
    }

    override fun generateMesh(mesh: Mesh) {
        val base0 = Shapes.flatCube.back
        val base = base0.positions!!
        val positions = mesh.positions.resize(base.size)
        val sx = max(sizeX, 1).toFloat() * 0.5f
        val sy = max(sizeY, 1).toFloat() * 0.5f
        val sz = max(sizeZ, 1).toFloat() * 0.5f
        for (i in positions.indices step 3) {
            // use the sign to preserve the original shape,
            // since this function might run recursively on the original data
            positions[i + 0] = sign(base[i + 0]) * sx
            positions[i + 1] = sign(base[i + 1]) * sy
            positions[i + 2] = sign(base[i + 2]) * sz
        }
        mesh.positions = positions
        mesh.indices = base0.indices
        mesh.normals = base0.normals
        mesh.invalidateGeometry()
    }

    override fun clone(): CellularAutomaton2 {
        val clone = CellularAutomaton2()
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as CellularAutomaton2
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
        texture0.destroy()
    }

    override val className = "CellularAutomaton2"

    companion object {
        private val LOGGER = LogManager.getLogger(CellularAutomaton2::class)
    }

}