package me.anno.cellau3d

import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.query.GPUClockNanos
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsVertexShader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.maps.LazyMap
import kotlin.math.max

object CellularStepShader {

    val shaders = LazyMap { neighborHood: Neighborhood ->
        val neighbors = neighborHood.neighbors
        Shader(
            "cells", emptyList(), coordsVertexShader,
            emptyList(), listOf(
                Variable(GLSLType.V1I, "birthMask"),
                Variable(GLSLType.V1I, "survivalMask"),
                Variable(GLSLType.V1I, "maxState"),
                Variable(GLSLType.V1I, "layerZ"),
                Variable(GLSLType.V3I, "size"),
                Variable(GLSLType.S3D, "src"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "int getValueAt(ivec3 pos){\n" +
                    "   if(all(greaterThanEqual(pos,ivec3(0))) && all(lessThan(pos,size))){\n" +
                    "       return int(texelFetch(src,pos,0).x * 255.0);\n" +
                    "   } else return 0;\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   ivec3 pos = ivec3(ivec2(gl_FragCoord.xy), layerZ);\n" +
                    "   int oldState = getValueAt(pos);\n" +
                    "   int sum = " +
                    neighbors.joinToString("+") {
                        "min(1,getValueAt(pos+ivec3(${it.x},${it.y},${it.z})))"
                    } + ";\n" +
                    "    int birthState = ((birthMask >> sum) & 1) > 0 ? maxState : 0;\n" +
                    "    int survivalState = (oldState != maxState) || (((survivalMask >> sum) & 1) == 0) ? oldState-1 : oldState;\n" +
                    "    int newState = oldState == 0 ? birthState : survivalState;\n" +
                    "    result = vec4(vec3(newState / 255.0), 1.0);\n" +
                    "}\n"
        )
    }

    private val timer = GPUClockNanos()

    fun gpuStep(ca: CellularAutomaton) {
        timeRendering("Cellular Step", timer) {
            gpuStepImpl(ca)
        }
    }

    private fun gpuStepImpl(ca: CellularAutomaton) {
        val src = ca.srcTexture
        val dst = ca.dstTexture

        assertTrue(src.pointer > 0)
        assertTrue(src.textures.isNotEmpty())
        assertTrue(src.textures.all { it.isCreated() })

        val shader = shaders[ca.neighborhood]
        shader.use()
        shader.v3i("size", ca.sizeX, ca.sizeY, ca.sizeZ)
        shader.v1i("birthMask", ca.getBirthFlags())
        shader.v1i("survivalMask", ca.getSurvivalFlags())
        shader.v1i("maxState", max(2, ca.numStates - 1))
        src.bindTexture0(shader, "src", Filtering.TRULY_NEAREST, Clamping.CLAMP)
        dst.draw(Renderer.colorRenderer) { z ->
            shader.v1i("layerZ", z)
            flat01.draw(shader)
        }

        ca.swapTextures()
    }
}