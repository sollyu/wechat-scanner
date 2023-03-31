package com.tencent.qbar;

import android.graphics.Bitmap;

import androidx.annotation.Keep;

import com.tencent.qbar.QbarNative;

@Keep
public class WxQbarNative {

    public static class QBarReportMsg {
        public String binaryMethod;
        public String charsetMode;
        public float decodeScale;
        public int decodeTime;
        public int detectTime;
        public String ecLevel;
        public boolean inBlackList;
        public boolean inWhiteList;
        public int pyramidLv;
        public int qrcodeVersion;
        public String scaleList;
        public int srTime;
    }

    public static native int EncodeCustom(byte[] bArr, int[] iArr, String str, int i15, int i16, String str2, int i17, int i18, boolean z14, Bitmap bitmap, Bitmap bitmap2, Bitmap bitmap3, Bitmap bitmap4, Bitmap bitmap5, Bitmap bitmap6);

    public static native int FocusInit(int i15, int i16, boolean z14, int i17, int i18);

    public static native boolean FocusPro(byte[] bArr, boolean z14, boolean[] zArr);

    public static native int FocusRelease();

    public static native int GetDominantColors(Bitmap bitmap, int[] iArr);

    public static native int QIPUtilYUVCrop(byte[] bArr, byte[] bArr2, int i15, int i16, int i17, int i18, int i19, int i24);

    public static native int TestGenQRCode(Bitmap bitmap, Bitmap bitmap2, Bitmap bitmap3, Bitmap bitmap4, Bitmap bitmap5, Bitmap bitmap6, Bitmap bitmap7);

    public static native int focusedEngineForBankcardInit(int i15, int i16, int i17, boolean z14);

    public static native int focusedEngineGetVersion();

    public static native int focusedEngineProcess(byte[] bArr);

    public static native int focusedEngineRelease();

    public native int AddBlackInternal(int i15, int i16);

    public native int AddBlackList(String str, int i15);

    public native int AddWhiteList(String str, int i15);

    public native String GetCallSnapshot(int i15);

    public native String GetDebugString(int i15);

    public native int GetDetailResults(QbarNative.QBarResultJNI[] qBarResultJNIArr, QbarNative.QBarPoint[] qBarPointArr, QBarReportMsg[] qBarReportMsgArr, int i15);

    public native int GetDetailResultsNew(QbarNative.QBarResultJNI[] qBarResultJNIArr, QbarNative.QBarPoint[] qBarPointArr, QBarReportMsg[] qBarReportMsgArr, int i15);

    public native int GetDetectInfoByFrames(QbarNative.QBarCodeDetectInfo qBarCodeDetectInfo, QbarNative.QBarPoint qBarPoint, int i15);

    public native int GetOneResultReport(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, int[] iArr, int[] iArr2, int i15);

    public native int GetZoomInfo(QbarNative.QBarZoomInfo qBarZoomInfo, int i15);

    public native void Reset(int i15, boolean z14);

    public native int ScanImage712(byte[] bArr, int i15, int i16, int i17, boolean z14);

    public native int SetCenterCoordinate(int i15, int i16, int i17, int i18, int i19);

    public native int SetScanTryHarder(int i15, int i16, int i17, float f15);

    public native int SetTouchCoordinate(int i15, float f15, float f16);
}