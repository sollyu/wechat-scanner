package com.tencent.qbar

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * 微信扫一扫功能
 */
class WechatScanner {

    /**
     * 初始化的ID
     */
    var id: Int? = null
        private set


    /**
     * 释放扫码必备的资源文件
     *
     * 主要释放Assert下对qbar文件到 /data/data/package/files/qbar 下
     *
     * @param context 上下文
     * @param folder  输出到文件夹名称 默认：qbar
     *
     * @throws IOException 可能文件权限有问题
     */
    @Throws(IOException::class)
    fun releaseAssert(context: Context, folder: String = "qbar") {
        val outputFolder = File(context.filesDir, folder)
        if (outputFolder.exists().not()) {
            outputFolder.mkdirs()
        }
        context.assets.open("qbar/stable/qbar_detect.xnet").copyTo(FileOutputStream(File(outputFolder, "detect_model.bin")))
        context.assets.open("qbar/stable/qbar_sr.xnet").copyTo(FileOutputStream(File(outputFolder, "detect_model.param")))
        context.assets.open("qbar/srnet.bin").copyTo(FileOutputStream(File(outputFolder, "srnet.bin")))
        context.assets.open("qbar/srnet.param").copyTo(FileOutputStream(File(outputFolder, "srnet.param")))
    }


    /**
     * 初始化扫一扫模块
     *
     * 在初始化一定要释放扫码资：releaseAssert
     *
     * @param folder 释放文件的文件夹
     *
     * @see releaseAssert
     * @see release
     *
     * @throws IOException 找不到初始化的各项资源
     */
    @Throws(IOException::class)
    fun init(context: Context, folder: String = "qbar"): Int? =
        init(File(context.filesDir, folder).absoluteFile)

    /**
     * 初始化扫一扫模块
     * 此方法减少了context参数
     *
     * @param folder 释放的资源完整路径
     *
     * @return 0 => 成功
     * @throws IOException 找不到初始化的各项资源
     */
    @Throws(IOException::class)
    fun init(folder: File): Int? {

        if (id != null) throw RuntimeException("already")

        System.loadLibrary("wechatQrMod")

        val detectModelBinPathFile = File(folder, "detect_model.bin")
        val detectModelParamPath = File(folder, "detect_model.param")
        val superResolutionModelBinPath = File(folder, "srnet.bin")
        val superResolutionModelParamPath = File(folder, "srnet.param")

        if (detectModelBinPathFile.exists().not()) throw FileNotFoundException("$detectModelBinPathFile")
        if (detectModelParamPath.exists().not()) throw FileNotFoundException("$detectModelParamPath")
        if (superResolutionModelBinPath.exists().not()) throw FileNotFoundException("$superResolutionModelBinPath")
        if (superResolutionModelParamPath.exists().not()) throw FileNotFoundException("$superResolutionModelParamPath")

        val qbarAiModelParam: QbarNative.QbarAiModelParam = QbarNative.QbarAiModelParam()
        qbarAiModelParam.detect_model_bin_path_ = detectModelBinPathFile.absolutePath
        qbarAiModelParam.detect_model_param_path_ = detectModelParamPath.absolutePath
        qbarAiModelParam.superresolution_model_bin_path_ = superResolutionModelBinPath.absolutePath
        qbarAiModelParam.superresolution_model_param_path_ = superResolutionModelParamPath.absolutePath
        qbarAiModelParam.detectModelVersion = "V1.0.0.26" //  "V1.1.0.26" : "V1.0.0.26";
        qbarAiModelParam.superResolutionModelVersion = "V1.0.0.26"
        qbarAiModelParam.enable_seg = false
        qbarAiModelParam.qbar_segmentation_model_path_ = ""

        id = QbarNative.Init(1, false, false, true, false, 1,0, "ANY", "UTF-8", qbarAiModelParam)
        return id
    }

    /**
     * 设置解码器
     *
     * @param intArray 解码支持参数
     *                 具体数值暂时不清楚
     *                 请固定填写: 2, 1
     *
     * @return 0 => 成功
     */
    fun setReader(intArray: IntArray = intArrayOf(2, 1)): Int =
        QbarNative.SetReaders(intArray, intArray.size, id ?: throw IllegalArgumentException("did init ?"))

    /**
     * 当前扫一扫版本信息
     *
     * @return 3.2.20190712
     */
    fun version(): String = QbarNative.GetVersion()

    /**
     * 相机预览的数据
     *
     * @param data      相机数据
     * @param size      data对应的图片大小
     * @param crop      裁剪的图片大小
     * @param rotation  旋转图片角度
     *
     * @return 扫描完成的List
     */
    fun onPreviewFrame(data: ByteArray, size: Point, crop: Rect, rotation: Int): List<QbarNative.QBarResultJNI> {
        val qBarId: Int = id ?: throw RuntimeException("did init ?")

        val nativeGrayRotateCropSubDataWH = IntArray(2)
        val nativeGrayRotateCropSubData = ByteArray(crop.width() * crop.height() * 3 / 2)
        val nativeGrayRotateCropSubResult: Int = QbarNative.nativeGrayRotateCropSub(data, size.x, size.y, crop.left, crop.top, crop.width(), crop.height(), nativeGrayRotateCropSubData, nativeGrayRotateCropSubDataWH, rotation, 0)
        if (nativeGrayRotateCropSubResult != 0) throw RuntimeException("Native.nativeGrayRotateCropSub error: $nativeGrayRotateCropSubResult")

        val scanImageResult: Int = QbarNative.ScanImage(nativeGrayRotateCropSubData.copyOf(), nativeGrayRotateCropSubDataWH[0], nativeGrayRotateCropSubDataWH[1], qBarId)
        if (scanImageResult != 0) throw RuntimeException("Native.ScanImage error: $scanImageResult")

        val qBarResultJNIArr: Array<QbarNative.QBarResultJNI> = arrayOf(QbarNative.QBarResultJNI(), QbarNative.QBarResultJNI(), QbarNative.QBarResultJNI())
        for (qBarResultJNI in qBarResultJNIArr) {
            qBarResultJNI.typeName = ""
            qBarResultJNI.charset = ""
            qBarResultJNI.data = ByteArray(1024)
        }

        val getDetailResults: Int = QbarNative.GetResults(qBarResultJNIArr, qBarId)
        if (getDetailResults < 0) throw RuntimeException("Native.GetDetailResults error: $getDetailResults")

        return qBarResultJNIArr.filter { qBarResultJNI: QbarNative.QBarResultJNI? -> qBarResultJNI?.typeName?.isNotEmpty() == true }
    }

    /**
     * 关闭
     */
    fun release(): Int = QbarNative.Release(id ?: throw RuntimeException("did init ?"))

}