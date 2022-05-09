package me.anno.cellau3d

import me.anno.gpu.GFX
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import org.joml.Vector3i
import kotlin.math.max

// this could be transformed into a regular fragment shader, I just don't know yet how to render to them
// todo when switching from cpu to gpu, the corners get activated... why???
val shaders = NeighborHood.values()
    .associateWith { neighborHood ->
        val neighbors = neighborHood.neighbors
        ComputeShader(
            "cells", Vector3i(8), "" +
                    "layout(r8, binding = 0) uniform image3D src;\n" +
                    "layout(r8, binding = 1) uniform image3D dst;\n" +
                    "uniform int birthMask, survivalMask;\n" +
                    "uniform int maxState;\n" +
                    "uniform ivec3 size;\n" +
                    "int getValueAt(ivec3 pos){\n" +
                    "   if(all(greaterThanEqual(pos,ivec3(0))) && all(lessThan(pos,size))){\n" +
                    "       return int(imageLoad(src, pos).x * 255.0);\n" +
                    "   } else return 0;\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   ivec3 pos = ivec3(gl_GlobalInvocationID.xyz);\n" +
                    "   if(all(lessThan(pos,size))){\n" +
                    "       int oldState = getValueAt(pos);\n" +
                    "       int sum = " +
                    neighbors.joinToString("+") {
                        "min(1,getValueAt(pos+ivec3(${it.x},${it.y},${it.z})))"
                    } + ";\n" +
                    "       int birthState = ((birthMask >> sum) & 1) > 0 ? maxState : 0;\n" +
                    "       int survivalState = (oldState != maxState) || (((survivalMask >> sum) & 1) == 0) ? oldState-1 : oldState;\n" +
                    "       int newState = oldState == 0 ? birthState : survivalState;\n" +
                    "       imageStore(dst, pos, vec4(float(newState)/255.0));\n" +
                    "   }\n" +
                    "}\n"
        )
    }

fun gpuStep(ca: CellularAutomaton2) {

    GFX.check()

    val src = ca.texture0
    val dst = ca.texture1

    val shader = shaders[ca.neighborHood]!!
    val rules = ca.rules
    shader.use()
    shader.v3i("size", ca.sizeX, ca.sizeY, ca.sizeZ)
    shader.v1i("birthMask", rules.birth)
    shader.v1i("survivalMask", rules.survival)
    shader.v1i("maxState", max(2, rules.states - 1))
    ComputeShader.bindTexture(0, src, ComputeTextureMode.READ)
    ComputeShader.bindTexture(1, dst, ComputeTextureMode.WRITE)
    shader.runBySize(ca.sizeX, ca.sizeY, ca.sizeZ)

    ca.swapTextures()

    GFX.check()

}
