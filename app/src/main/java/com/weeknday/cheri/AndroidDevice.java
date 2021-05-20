package com.weeknday.cheri;

import android.app.Activity;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

public class AndroidDevice
{
	// Device Information
	private static String m_strSDKVersion = "";
	private static String m_strSDKCodeName = "";
	private static String m_strReleaseVersion = "";
	private static String m_strBrand = "";
	private static String m_strDevice = "";
	private static String m_strDisplay = "";
	private static String m_strID = "";
	private static String m_strModel = "";
	private static String m_strManufacturer = "";
	private static String m_strProduct = "";

	// Device Graphic Information
	private static float m_fDensity		= 0.0f;
	private	static int	m_nDensityDpi	= 0;
	private static int	m_nWidthPx		= 0;
	private static int	m_nHeightPx		= 0;

	public AndroidDevice(Activity cAct)
	{
		GetDeviceInformation();
		DeviceGraphicInformation(cAct);
	}

	private void DeviceGraphicInformation(Activity cAct)
	{
		DisplayMetrics cDisplayMetrics = new DisplayMetrics();
		cAct.getWindowManager().getDefaultDisplay().getMetrics(cDisplayMetrics);

		m_fDensity		= cDisplayMetrics.density;
		m_nWidthPx		= cDisplayMetrics.widthPixels;
		m_nHeightPx		= cDisplayMetrics.heightPixels;
		m_nDensityDpi	= cDisplayMetrics.densityDpi;

		Log.d("AndroidDevice", "Density : " + m_fDensity);
		Log.d("AndroidDevice", "WidthPx : " + m_nWidthPx);
		Log.d("AndroidDevice", "HeightPx : " + m_nHeightPx);
		Log.d("AndroidDevice", "DensityDpi : " + m_nDensityDpi);
	}

	private void GetDeviceInformation()
	{
		m_strSDKVersion		= Build.VERSION.SDK;
		m_strSDKCodeName	= Build.VERSION.CODENAME;
		m_strReleaseVersion	= Build.VERSION.RELEASE;
		m_strBrand			= Build.BRAND;
		m_strDevice			= Build.DEVICE;
		m_strDisplay		= Build.DISPLAY;
		m_strID				= Build.ID;
		m_strModel			= Build.MODEL;
		m_strManufacturer	= Build.MANUFACTURER;
		m_strProduct		= Build.PRODUCT;

		Log.d("AndroidDevice", "SDKVersion : " + m_strSDKVersion);
		Log.d("AndroidDevice", "SDKCodeName : " + m_strSDKCodeName);
		Log.d("AndroidDevice", "ReleaseVersion : " + m_strReleaseVersion);
		Log.d("AndroidDevice", "Brand : " + m_strBrand);
		Log.d("AndroidDevice", "Device : " + m_strDevice);
		Log.d("AndroidDevice", "Display : " + m_strDisplay);
		Log.d("AndroidDevice", "ID : " + m_strID);
		Log.d("AndroidDevice", "Model : " + m_strModel);
		Log.d("AndroidDevice", "Manufacturer : " + m_strManufacturer);
		Log.d("AndroidDevice", "Product : " + m_strProduct);
	}

	public String GetSDKVersion()
	{
		return m_strSDKVersion;
	}

	public String GetSDKCodeName()
	{
		return m_strSDKCodeName;
	}

	public String GetReleaseVersion()
	{
		return m_strReleaseVersion;
	}

	public String GetBrand()
	{
		return m_strBrand;
	}

	public String GetDevice()
	{
		return m_strDevice;
	}

	public String GetDisplay()
	{
		return m_strDisplay;
	}

	public String GetID()
	{
		return m_strID;
	}

	public String GetModel()
	{
		return m_strModel;
	}

	public String GetManufacturer()
	{
		return m_strManufacturer;
	}

	public String GetProduct()
	{
		return m_strProduct;
	}

	public float GetDensity()
	{
		return m_fDensity;
	}

	public int GetWidthPx()
	{
		return m_nWidthPx;
	}

	public int GetHeightPx()
	{
		return m_nHeightPx;
	}

	public int GetDensityDpi()
	{
		return m_nDensityDpi;
	}
}