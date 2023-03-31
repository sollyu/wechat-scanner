package com.tencent.qbar

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import kotlin.Throws

/**
 * 微信扫一扫功能
 */
class WechatScannerDelegate {

    /**
     * 初始化的ID
     */
    private var mWechatQBarId: Int? = null

    /**
     * 释放扫码必备的资源文件
     * @param context 上下文 用于获取assets
     * @param output 输出到文件夹名称 默认：qbar
     * @throws IOException 可能文件权限有问题
     * @throws RuntimeException 初始化失败
     */
    @Throws(RuntimeException::class, IOException::class)
    fun init(context: Context, output: File = context.filesDir.resolve(relative = "qbar")) {
        if (mWechatQBarId != null) {
            return
        }

        System.loadLibrary("wechatQrMod")

        if (output.exists().not()) {
            output.mkdirs()
        }

        val detectModelBinPathFile: File = output.resolve(relative = "qbar_detect.xnet")
        val superResolutionModelBinPath: File = output.resolve(relative = "qbar_sr.xnet")

        if (detectModelBinPathFile.exists().not()) {
            detectModelBinPathFile.outputStream().use { context.assets.open("qbar/${detectModelBinPathFile.name}").copyTo(it) }
        }
        if (superResolutionModelBinPath.exists().not()) {
            superResolutionModelBinPath.outputStream().use { context.assets.open("qbar/${superResolutionModelBinPath.name}").copyTo(it) }
        }

        if (detectModelBinPathFile.exists().not()) {
            throw IOException("detectModelBinPathFile not exists")
        }
        if (superResolutionModelBinPath.exists().not()) {
            throw IOException("superResolutionModelBinPath not exists")
        }


        val qbarAiModelParam: QbarNative.QbarAiModelParam = QbarNative.QbarAiModelParam()
        qbarAiModelParam.detectModelVersion = "V1.0.0.26"
        qbarAiModelParam.detect_model_bin_path_ = detectModelBinPathFile.absolutePath
        qbarAiModelParam.detect_model_param_path_ = ""

        qbarAiModelParam.superresolution_model_bin_path_ = superResolutionModelBinPath.absolutePath
        qbarAiModelParam.superresolution_model_param_path_ = ""
        qbarAiModelParam.superResolutionModelVersion = "V1.0.0.26"

        qbarAiModelParam.enable_seg = false
        qbarAiModelParam.qbar_segmentation_model_path_ = ""

        val wechatQBarId: Int = QbarNative.Init(1, true, true, true, false, 1, 0, "ANY", "UTF-8", qbarAiModelParam)
        if (wechatQBarId < 0) {
            throw RuntimeException("init failed")
        }

        val intArray: IntArray = intArrayOf(2, 1, 4, 5)
        val code: Int = QbarNative.SetReaders(intArray, intArray.size, wechatQBarId)
        if (code != 0) {
            throw RuntimeException("set readers failed code=$code")
        }

        mWechatQBarId = wechatQBarId
    }

    /**
     * 释放资源
     * 释放后需要重新init
     * 释放的是对象，不会删除已经释放的文件
     */
    fun release() {
        if (mWechatQBarId != null) {
            QbarNative.Release(mWechatQBarId!!)
        }
        mWechatQBarId = null
    }

    /**
     * 获取版本
     * @return 3.2.20190712
     */
    val version: String
        get() = QbarNative.GetVersion()

    /**
     * 获取初始化的ID
     * @return 初始化的ID
     */
    val qBarId: Int?
        get() = mWechatQBarId

    /**
     * 扫码
     * 一般用在相机的回调中，每一帧都会调用
     * @param data 相机数据 必须是YUV_420_888格式的数据
     * @param size 相机数据的宽高
     * @param crop 裁剪区域
     * @param rotation 旋转角度
     * @return 扫码结果
     */
    @Throws(RuntimeException::class)
    fun scan(data: ByteArray, size: Point, crop: Rect, rotation: Int): Array<QbarNative.QBarResultJNI> {
        val wechatQBarId: Int = mWechatQBarId ?: throw RuntimeException("not init")
        val nativeGrayRotateCropSubDataWH = IntArray(2)
        val nativeGrayRotateCropSubData = ByteArray(crop.width() * crop.height() * 3 / 2)
        val nativeGrayRotateCropSubResult: Int = QbarNative.nativeGrayRotateCropSub(data, size.x, size.y, crop.left, crop.top, crop.width(), crop.height(), nativeGrayRotateCropSubData, nativeGrayRotateCropSubDataWH, rotation, 0)
        if (nativeGrayRotateCropSubResult != 0) {
            throw RuntimeException("Native.nativeGrayRotateCropSub error: $nativeGrayRotateCropSubResult")
        }

        val scanImageResult: Int = QbarNative.ScanImage(nativeGrayRotateCropSubData.copyOf(), nativeGrayRotateCropSubDataWH[0], nativeGrayRotateCropSubDataWH[1], wechatQBarId)
        if (scanImageResult != 0) {
            throw RuntimeException("Native.ScanImage error: $scanImageResult")
        }

        val qBarResultJNIArr: Array<QbarNative.QBarResultJNI> = arrayOf(QbarNative.QBarResultJNI(), QbarNative.QBarResultJNI(), QbarNative.QBarResultJNI())
        for (qBarResultJNI: QbarNative.QBarResultJNI in qBarResultJNIArr) {
            qBarResultJNI.typeName = ""
            qBarResultJNI.charset = ""
            qBarResultJNI.data = ByteArray(1024)
        }

        val getDetailResults: Int = QbarNative.GetResults(qBarResultJNIArr, wechatQBarId)
        if (getDetailResults < 0) {
            throw RuntimeException("Native.GetDetailResults error: $getDetailResults")
        }

        return qBarResultJNIArr
    }

    /**
     * 扫码 更简洁的方法
     * 一般用在相机的回调中，每一帧都会调用
     * @param data 相机数据 必须是YUV_420_888格式的数据
     * @param size 相机数据的宽高
     * @param rotation 旋转角度
     * @return 扫码结果
     * @throws RuntimeException
     * @see scan
     */
    fun scan(data: ByteArray, size: Point, rotation: Int): List<String> {
        return scan(data, size, Rect(0, 0, size.x, size.y), rotation)
            .filter { it.typeName.isNotEmpty() }
            .map { String(it.data, Charset.forName(it.charset)) }
    }
}