package com.igalata.bubblepicker.rendering

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.View
import com.igalata.bubblepicker.*
import com.igalata.bubblepicker.model.Color
import com.igalata.bubblepicker.model.PickerItem
import com.igalata.bubblepicker.physics.Border
import com.igalata.bubblepicker.physics.CircleBody
import com.igalata.bubblepicker.physics.Engine
import com.igalata.bubblepicker.rendering.BubbleShader.A_POSITION
import com.igalata.bubblepicker.rendering.BubbleShader.A_UV
import com.igalata.bubblepicker.rendering.BubbleShader.U_BACKGROUND
import com.igalata.bubblepicker.rendering.BubbleShader.fragmentShader
import com.igalata.bubblepicker.rendering.BubbleShader.vertexShader
import org.jbox2d.collision.AABB
import org.jbox2d.common.Vec2
import java.nio.FloatBuffer
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Created by irinagalata on 1/19/17.
 */
class PickerRenderer(val glView: View) : GLSurfaceView.Renderer {

    var backgroundColor: Color? = null
    var maxSelectedCount: Int? = null
        set(value) {
            Engine.maxSelectedCount = value
        }
    var textSize = 0.25f
    var bubbleSize = 50
        set(value) {
            Engine.radius = value
        }
    var listener: BubblePickerListener? = null
    lateinit var items: ArrayList<PickerItem>
    val selectedItems: List<PickerItem?>
        get() = Engine.selectedBodies.map { circles.firstOrNull { circle -> circle.circleBody == it }?.pickerItem }
    var centerImmediately = false
        set(value) {
            field = value
            Engine.centerImmediately = value
        }

    private var programId = 0
    private var verticesBuffer: FloatBuffer? = null
    private var textVerticesBuffer: FloatBuffer? = null
    private var uvBuffer: FloatBuffer? = null
    private var vertices: FloatArray? = null
    private var textVertices: FloatArray? = null
    private var textureVertices: FloatArray? = null
    private var textureIds: IntArray? = null

    private val scaleX: Float
        get() = if (glView.width < glView.height) glView.height.toFloat() / glView.width.toFloat() else 1f
    private val scaleY: Float
        get() = if (glView.width < glView.height) 1f else glView.width.toFloat() / glView.height.toFloat()

    val circles = ArrayList<MyItem>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(backgroundColor?.red ?: 1f, backgroundColor?.green ?: 1f,
                backgroundColor?.blue ?: 1f, backgroundColor?.alpha ?: 1f)
        enableTransparency()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        initialize()
    }

    override fun onDrawFrame(gl: GL10?) {
        calculateVertices()
        Engine.move()
        drawFrame()
    }

    private fun initialize() {
        clear()
        Engine.centerImmediately = centerImmediately

        Engine.build(items.size, scaleX, scaleY).forEachIndexed { index, body ->
            circles.add(MyItem(items[index], body))
        }



        items.forEach { if (it.isSelected) Engine.resize(circles.first { circle -> circle.pickerItem == it }, circles) }
        if (textureIds == null) textureIds = IntArray(circles.size * 3)
        initializeArrays()
    }


    fun getAllPickerItems(): ArrayList<MyItem>
    {
        return circles
    }

    private fun initializeArrays() {
        vertices = FloatArray(circles.size * 8)
        textVertices = FloatArray(circles.size * 8)
        textureVertices = FloatArray(circles.size * 8)
        circles.forEachIndexed { i, item -> initializeItem(item, i) }
        verticesBuffer = vertices?.toFloatBuffer()
        textVerticesBuffer = textVertices?.toFloatBuffer()
        uvBuffer = textureVertices?.toFloatBuffer()
    }

    private fun initializeItem(item: MyItem, index: Int) {
        initializeVertices(item, index)
        textureVertices?.passTextureVertices(index)
        item.bindTextures(textureIds ?: IntArray(0), index)
    }

    private fun calculateVertices() {
        circles.forEachIndexed { i, item -> initializeVertices(item, i) }
        vertices?.forEachIndexed { i, float -> verticesBuffer?.put(i, float) }
        textVertices?.forEachIndexed { i, float -> textVerticesBuffer?.put(i, float) }
    }

    private fun initializeVertices(body: MyItem, index: Int) {
        val radius = body.radius
        val radiusX = radius * scaleX
        val radiusY = radius * scaleY

        val textW = textSize*scaleX
        val textH = textSize*scaleY

        vertices?.put(8 * index, floatArrayOf(-radiusX, radiusY, -radiusX, -radiusY,
                radiusX, radiusY, radiusX, -radiusY))
         textVertices?.put(8 * index, floatArrayOf(-textW, textH, - textW,- textH,
                 textW, textH, textW, -textH))
    }

    private fun drawFrame() {
        glClear(GL_COLOR_BUFFER_BIT)
        glUniform4f(glGetUniformLocation(programId, U_BACKGROUND), 1f, 1f, 1f, 0f)
        verticesBuffer?.passToShader(programId, A_POSITION)
        uvBuffer?.passToShader(programId, A_UV)
        circles.forEachIndexed { i, circle -> circle.drawBubbleRect(programId, i, scaleX, scaleY) }

        textVerticesBuffer?.passToShader(programId, A_POSITION)
        circles.forEachIndexed { i, circle -> circle.drawTextRext(programId, i, scaleX, scaleY) }
    }

    private fun enableTransparency() {
        glEnable(GLES20.GL_BLEND)
        glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        attachShaders()
    }

    private fun attachShaders() {
        programId = createProgram(createShader(GL_VERTEX_SHADER, vertexShader),
                createShader(GL_FRAGMENT_SHADER, fragmentShader))
        glUseProgram(programId)
    }

    fun createProgram(vertexShader: Int, fragmentShader: Int) = glCreateProgram().apply {
        glAttachShader(this, vertexShader)
        glAttachShader(this, fragmentShader)
        glLinkProgram(this)
    }

    fun createShader(type: Int, shader: String) = GLES20.glCreateShader(type).apply {
        glShaderSource(this, shader)
        glCompileShader(this)
    }

    fun swipe(x: Float, y: Float) = Engine.swipe(x.convertValue(glView.width, scaleX),
            y.convertValue(glView.height, scaleY))

    fun release() = Engine.release()

    private fun AABBcheck(aabb: AABB,point:Vec2): Boolean{

        return (aabb.lowerBound.x<=point.x && point.x<=aabb.upperBound.x &&
                aabb.lowerBound.y<=point.y && point.y<=aabb.upperBound.y)
    }

    private fun getItem(position: Vec2):MyItem?{
        val x = position.x.convertPoint(glView.width, scaleX)
        val y = position.y.convertPoint(glView.height, scaleY)

        val item = circles.find{
            val ts = Vec2(it.textPixelSize.x*0.26f*scaleX,it.textPixelSize.y*0.26f*scaleY)
            AABBcheck(AABB(Vec2(x,y).sub(ts),Vec2(x,y).add(ts)),Vec2(it.x,it.y))
        }

        if(item!=null){
            return item
        }
        
        return circles.find { Math.sqrt(((x - it.x).sqr() + (y - it.y).sqr()).toDouble()) <= it.radius }
        
    }

    fun resize(x: Float, y: Float) = getItem(Vec2(x, glView.height - y))?.apply {
        if (Engine.resize(this, circles)) {
            listener?.let {

                if (circleBody.increased) {

                    it.onBubbleDeselected(pickerItem)
                } else {

                    it.onBubbleSelected(pickerItem)
                }
            }
        }
    }

    fun getBodies(): ArrayList<CircleBody> {
        return Engine.getBodies()
    }

    private fun clear() {
        circles.clear()
        Engine.clear()
    }

}