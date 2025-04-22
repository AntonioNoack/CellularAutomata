package me.anno.cellau3d

import me.anno.Time
import me.anno.cellau3d.CellularStepShader.gpuStep
import me.anno.cellau3d.FlagParser.parseFlags
import me.anno.cellau3d.Patterns.init4Impl
import me.anno.cellau3d.Patterns.initBoundingBoxImpl
import me.anno.ecs.annotations.*
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.material.Texture3DBTMaterial
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer3D
import me.anno.gpu.framebuffer.TargetType
import me.anno.maths.Maths.clamp
import me.anno.mesh.Shapes
import me.anno.utils.Color.toVecRGB
import me.anno.utils.assertions.assertEquals
import me.anno.utils.callbacks.I3I
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.pooling.Pools.byteBufferPool
import me.anno.utils.types.Arrays.resize
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.sign

/**
 * simulates a cellular automaton;
 * displays the grid with a raytracing fragment shader
 * */
@Suppress("unused")
class CellularAutomaton : ProceduralMesh(), OnUpdate {

    companion object {
        private val LOGGER = LogManager.getLogger(CellularAutomaton::class)

        private val pool = ProcessingGroup("cells", 1f)
        private val targets = listOf(TargetType.UInt8x1)

        fun Framebuffer3D.createMonochrome(data: ByteBuffer) {
            ensure()
            val texture = getTexture0()
            val pointer = texture.pointer
            texture.createMonochrome(data)
            assertEquals(pointer, texture.pointer)
        }
    }

    @NotSerializedProperty
    var srcTexture = Framebuffer3D("cells0", 1, 1, 1, targets, DepthBufferType.NONE)

    @NotSerializedProperty
    var dstTexture = Framebuffer3D("cells1", 1, 1, 1, targets, DepthBufferType.NONE)

    private val material = Texture3DBTMaterial()

    init {
        data.material = material.ref
        data.inverseOutline = true // for correct outlines, since we render on the back faces
    }

    @Type("Color3")
    var color0: Vector3f = 0x1d73d3.toVecRGB()
        set(value) {
            field.set(value)
            setColors()
        }

    @Type("Color3")
    var color1: Vector3f = 0xffffff.toVecRGB()
        set(value) {
            field.set(value)
            setColors()
        }

    @Order(9)
    @Group("Rules")
    var survives = "4"

    @Order(10)
    @Group("Rules")
    var births = "4"

    fun getSurvivalFlags(): Int {
        return parseFlags(survives)
    }

    fun getBirthFlags(): Int {
        return parseFlags(births)
    }

    @Order(11)
    @Group("Rules")
    @Range(2.0, 1024.0)
    var numStates = 2
        set(value) {
            if (value >= 2) {
                field = value
            }
        }

    @Order(12)
    @Group("Rules")
    var neighborhood = Neighborhood.MOORE

    @Range(1.0, 1e9)
    var sizeX = 10
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    @Range(1.0, 1e9)
    var sizeY = 10
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    @Range(1.0, 1e9)
    var sizeZ = 10
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    fun swapTextures() {
        val tmp = srcTexture
        srcTexture = dstTexture
        dstTexture = tmp
    }

    @DebugAction
    fun makeCubic() {
        sizeY = sizeX
        sizeZ = sizeX
        invalidateMesh()
    }

    @DebugAction
    fun init1() {
        val cx = sizeX / 2
        val cy = sizeY / 2
        val cz = sizeZ / 2
        initWithFunction { x, y, z ->
            if (cx == x && cy == y && cz == z) numStates - 1
            else 0
        }
    }

    fun initWithFunction(fillFunction: I3I) {
        ensureCorrectSize()
        val buffer = fillData(fillFunction)
        srcTexture.createMonochrome(buffer)
        dstTexture.createMonochrome(buffer)
        byteBufferPool.returnBuffer(buffer)
    }

    @DebugAction
    fun init4() {
        init4Impl()
    }

    @DebugAction
    fun initBoundingBox() {
        initBoundingBoxImpl()
    }

    @DebugProperty
    var updatePeriod = 0.5

    @DebugProperty
    @NotSerializedProperty
    var accumulatedTime = 0.0

    private fun isCreated1(): Boolean {
        return srcTexture.pointer > 0 &&
                dstTexture.pointer > 0 &&
                srcTexture.getTexture0().isCreated() &&
                dstTexture.getTexture0().isCreated()
    }

    @DebugAction
    fun step() {
        updatePeriod = Double.POSITIVE_INFINITY
        stepImpl()
    }

    private fun updateTime() {
        if (updatePeriod.isFinite()) {
            accumulatedTime = clamp(accumulatedTime - updatePeriod, -updatePeriod, updatePeriod)
        }
    }

    private fun stepImpl() {
        updateTime()
        if (!isCreated1()) init1()
        gpuStep(this)
        show()
    }

    fun show() {
        material.blocks = srcTexture.getTexture0()
        setColors()
    }

    override fun onUpdate() {
        ensureCorrectSize()
        if (!isCreated1()) {
            LOGGER.info("Creating field")
            init1()
        }
        accumulatedTime += Time.deltaTime
        if (accumulatedTime > updatePeriod) {
            stepImpl()
        }
    }

    fun fillData(fillFunction: I3I): ByteBuffer {
        val sizeX = sizeX
        val sizeY = sizeY
        val sizeZ = sizeZ
        val data = byteBufferPool[sizeX * sizeY * sizeZ, false, false]
        for (z in 0 until sizeZ) {
            for (y in 0 until sizeY) {
                for (x in 0 until sizeX) {
                    val color = fillFunction.call(x, y, z).toByte()
                    data.put(color)
                }
            }
        }
        data.flip()
        return data
    }

    fun ensureCorrectSize() {
        ensureCorrectSize(srcTexture)
        ensureCorrectSize(dstTexture)
    }

    private fun ensureCorrectSize(texture: Framebuffer3D) {
        if (texture.width != sizeX || texture.height != sizeY || texture.depth != sizeZ) {
            texture.destroy() // doesn't matter
            texture.width = sizeX
            texture.height = sizeY
            texture.depth = sizeZ
        }
    }

    private fun setColors() {
        material.color0.set(color0)
        material.color1.set(color1)
        material.limitColors(numStates - 1)
    }

    override fun generateMesh(mesh: Mesh) {
        val srcMesh = Shapes.flatCube.back
        val srcPositions = srcMesh.positions!!
        val dstPositions = mesh.positions.resize(srcPositions.size)
        val sx = max(sizeX, 1).toFloat() * 0.5f
        val sy = max(sizeY, 1).toFloat() * 0.5f
        val sz = max(sizeZ, 1).toFloat() * 0.5f
        for (i in dstPositions.indices step 3) {
            // use the sign to preserve the original shape,
            // since this function might run recursively on the original data
            dstPositions[i + 0] = sign(srcPositions[i + 0]) * sx
            dstPositions[i + 1] = sign(srcPositions[i + 1]) * sy
            dstPositions[i + 2] = sign(srcPositions[i + 2]) * sz
        }
        mesh.positions = dstPositions
        mesh.indices = srcMesh.indices
        mesh.normals = srcMesh.normals
        mesh.cullMode = srcMesh.cullMode
        mesh.invalidateGeometry()
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as CellularAutomaton
        dst.sizeX = sizeX
        dst.sizeY = sizeY
        dst.sizeZ = sizeZ
        dst.survives = survives
        dst.births = births
        dst.numStates = numStates
        dst.neighborhood = neighborhood
        dst.updatePeriod = updatePeriod
    }

    override fun destroy() {
        super.destroy()
        srcTexture.destroy()
        dstTexture.destroy()
    }

    override val className = "CellularAutomaton2"

}