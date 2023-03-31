package com.tencent.qbar.sample

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.base.Stopwatch
import com.tencent.qbar.WechatScannerDelegate
import com.tencent.qbar.sample.databinding.ActivityMainBinding
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Suppress(names = ["DEPRECATION"])
class MainActivity : AppCompatActivity(), ImageAnalysis.Analyzer {

    private lateinit var viewBinding: ActivityMainBinding

    private var mCamera: androidx.camera.core.Camera? = null

    private val mIsEnabledOnCameraImageAnalyze = AtomicBoolean(true)
    private val mWechatScannerDelegate: WechatScannerDelegate = WechatScannerDelegate()
    private val mImageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .setOutputImageRotationEnabled(true)
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build()
    private val mPreview: Preview = Preview.Builder()
        .setTargetRotation(Surface.ROTATION_0)
        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
    }

    fun onClickInit(view: View) {
        mWechatScannerDelegate.init(context = this)
        viewBinding.textView.text = mWechatScannerDelegate.version
    }

    fun onClickOpen(view: View) {
        val context: Context = view.context

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
            return
        }

        // 获取相机提供者
        val cameraProvider: ProcessCameraProvider = ProcessCameraProvider.getInstance(context).get()
        val extensionsManager: ExtensionsManager = ExtensionsManager.getInstanceAsync(context, cameraProvider).get()

        // 获取相机设备来检查是否支持扩展
        val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // 预览参数
        mPreview.setSurfaceProvider(viewBinding.ivCamera.surfaceProvider)
        mImageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), this)

        // 检查是否支持 AUTO
        mCamera = if (extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.AUTO)) {
            val bokehCameraSelector: CameraSelector = extensionsManager.getExtensionEnabledCameraSelector(cameraSelector, ExtensionMode.AUTO)
            cameraProvider.bindToLifecycle(this, bokehCameraSelector, mImageAnalysis, mPreview)
        } else {
            cameraProvider.bindToLifecycle(this, cameraSelector, mPreview, mImageAnalysis)
        }
    }

    fun onClickReset(view: View) {
        mIsEnabledOnCameraImageAnalyze.set(true)
        viewBinding.textView.text = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        mWechatScannerDelegate.release()
    }

    override fun analyze(image: ImageProxy) {
        image.use { img ->
            if (mIsEnabledOnCameraImageAnalyze.get().not()) {
                return@use
            }
            val stopwatch: Stopwatch = Stopwatch.createStarted()
            val data: ByteArray = img.toFormatToNv21()
            val scanResultList: List<String> = mWechatScannerDelegate.scan(data = data, size = Point(img.width, img.height), rotation = 90)
            if (scanResultList.isEmpty()) {
                return
            }
            mIsEnabledOnCameraImageAnalyze.set(false)
            runOnUiThread { viewBinding.textView.text = scanResultList[0] }
            for (qbar: String in scanResultList) {
                Log.d("MainActivity", " scan cost:$stopwatch text:$qbar")
            }
        }
    }

}
