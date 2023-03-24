package com.tencent.qbar.sample

import android.graphics.Point
import android.graphics.Rect
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.common.base.Stopwatch
import com.tencent.qbar.QbarNative
import com.tencent.qbar.WechatScanner
import com.tencent.qbar.sample.databinding.ActivityMainBinding
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset

@Suppress(names = ["DEPRECATION"])
class MainActivity : AppCompatActivity(), SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {

    companion object {
        const val TAG = "WeChatScanner"
    }

    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var wechatScanner: WechatScanner
    private lateinit var camera: Camera

    private var isScanFinish: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
    }

    fun onClickInit(view: View) {
        wechatScanner = WechatScanner()
        wechatScanner.releaseAssert(view.context)
        val code: Int = wechatScanner.init(view.context) ?: -1
        wechatScanner.setReader()
        viewBinding.textView.text = wechatScanner.version()
        Log.d(TAG, "LOG:MainActivity:onClickInit: code=$code")
    }

    fun onClickOpen(view: View) {
        viewBinding.surfaceView.holder.addCallback(this)

        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        camera.setPreviewDisplay(viewBinding.surfaceView.holder)
        camera.setDisplayOrientation(90)

        val parameters: Camera.Parameters = camera.parameters
        parameters.focusMode = Camera.Parameters.FLASH_MODE_AUTO

        camera.parameters = parameters
        camera.setPreviewCallback(this)
        camera.startPreview()
    }

    fun onClickFouce(view: View) {
        camera.autoFocus(this)
    }

    fun onClickReset(view: View) {
        isScanFinish = false
        viewBinding.textView.text = ""
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        if (isScanFinish)
            return

        val stopwatch: Stopwatch = Stopwatch.createStarted()
        val scanResultList: List<QbarNative.QBarResultJNI> = wechatScanner.onPreviewFrame(
            data = data,
            size = Point(camera.parameters.previewSize.width, camera.parameters.previewSize.height),
            crop = Rect(0, 0, camera.parameters.previewSize.width, camera.parameters.previewSize.height),
            rotation = 90
        )
        if (scanResultList.isNotEmpty()) {
            isScanFinish = true
            scanResultList.forEach { qBarResultJNI: QbarNative.QBarResultJNI ->
                Log.d(TAG, "LOG:MainActivity:onPreviewFrame typeName=" + qBarResultJNI.typeName + " charset=" + qBarResultJNI.charset + " data=" + String(qBarResultJNI.data, Charset.forName(qBarResultJNI.charset)))
            }
            viewBinding.textView.post { viewBinding.textView.text = scanResultList.first().let { String(it.data, Charset.forName(it.charset)) } }
            Log.d(TAG, "LOG:MainActivity:onPreviewFrame scan cost: $stopwatch")
        }
    }

    override fun onAutoFocus(success: Boolean, camera: Camera?) {
        Log.d(TAG, "LOG:MainActivity:onAutoFocus success=${success}")
    }

    override fun onDestroy() {
        super.onDestroy()
        camera.release()
        wechatScanner.release()
    }

}
