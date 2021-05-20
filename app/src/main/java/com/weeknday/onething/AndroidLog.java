package com.weeknday.onething;

import android.annotation.SuppressLint;
import android.os.Environment;

import java.util.Calendar;

// Android에서 Log를 쓰기 위한 클래스
public class AndroidLog {
    AndroidFile m_File;

    private static String m_strLogPath = "";
    private static String m_strLogFileName = "AndroidLog.txt";
    private static String m_strLogFile = "";

    public static final int LOGTYPE_INFO = 0;
    public static final int LOGTYPE_WARN = 1;
    public static final int LOGTYPE_ERROR = 2;

    public static final int TIMETYPE_LOG = 0;
    public static final int TIMETYPE_GENERAL = 1;

    private boolean m_bIsWrite = false;


    /*
        @Function Name	:	initialize
        @brief			:	Log를 쓰기 위한 초기화 함수
        @param			:	strPackageName	- 로그를 작성할 패키지명
        @param			:	strLogFileName	- 로그 파일명
        @return			:	0				- 성공
    */
    public void initialize(String strPackageName, String strLogFileName, boolean bDeleteLogFile) {
        int nRes = 0;

        m_File = new AndroidFile();
        m_strLogPath = Environment.getExternalStorageDirectory() + "/Android/data/";


        // Log Path 설정
        String strPath;
        if (strPackageName != null && strPackageName.length() > 0)
            strPath = m_strLogPath + strPackageName + "/Log/";
        else
            strPath = m_strLogPath + "Temp/Log/";

        if (bDeleteLogFile) {
            // Previous Log File in Log Directory.
            AndroidFile.removeDirectory(strPath);
        }

        if (!m_bIsWrite)
            return;

        nRes = m_File.createDirectory(strPath);
        m_strLogPath = strPath;


        // LogFile Path 설정
        String strFile;
        if (strLogFileName != null && strLogFileName.length() > 0)
            strFile = strLogFileName;
        else
            strFile = m_strLogFileName;

        nRes = m_File.createFile(m_strLogPath, strFile);
        m_strLogFileName = strFile;


        m_strLogFile = m_strLogPath + m_strLogFileName;

        //Log.i("AndroidLog", m_strLogFile);

    }

    public void SetIsWriteLog(boolean bWrite) {
        m_bIsWrite = bWrite;
    }


    /*
        @Function Name	:	deleteLogFile
        @brief			:	Log 파일을 삭제하는 함수
        @param			:	strFile	- 삭제할 로그 파일
        @return			:	0		- 성공
    */
    public int deleteLogFile(String strFile) {
        int nRes = 0;

        nRes = m_File.deleteFile(strFile);
        if (nRes > 1)
            write(LOGTYPE_ERROR, "deleteFile() Failed." + "|" + nRes + "|" + strFile);
        else
            write(LOGTYPE_INFO, "deleteFile() Success.");

        return nRes;
    }


    /*
        @Function Name	:	write
        @brief			:	Log를 작성하는 함수
        @param			:	nLogType	- 작성할 로그의 타입 (INFO, WARN, ERROR...)
        @param			:	strMsg		- 작성할 로그 내용
    */
    public void write(int nLogType, String strMsg, Object... args) {
        if (!m_bIsWrite)
            return;

        String _strMsg = strMsg;

        if ((strMsg == null) || (strMsg.length() == 0))
            return;

        if (args.length != 0)
            _strMsg = String.format(strMsg, args);

        _strMsg = getCurrentTime(TIMETYPE_LOG) + " " + getLogType(nLogType) + " " + _strMsg + "\n";

        m_File.writeFile(m_strLogFile, true, _strMsg.getBytes());
    }


    /*
        @Function Name	:	getCurrentTime
        @brief			:	현재의 시간을 얻는 함수
        @return			:	[YYYY-MM-DD HH:MM:SS] 형식으로 보낸다.
    */
    @SuppressLint("DefaultLocale")
    public String getCurrentTime(int nStrTimeType) {
        Calendar calendar = Calendar.getInstance();
        String strTime = null;

        switch (nStrTimeType) {
            case TIMETYPE_LOG: {
                strTime = String.format("[%04d-%02d-%02d %02d:%02d:%02d]", calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH),
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        calendar.get(Calendar.SECOND));
                break;
            }
            case TIMETYPE_GENERAL:
            default: {
                strTime = String.format("%04d-%02d-%02d %02d:%02d:%02d", calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH),
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        calendar.get(Calendar.SECOND));
                break;
            }
        }
        return strTime;
    }


    /*
        @Function Name	:	getLogType
        @brief			:	로그의 형식을 문자열로 반환하는 함수
        @return			:	[LOGTYPE] 형식으로 보낸다.
    */
    private static String getLogType(int nLogType) {
        String strType;

        switch (nLogType) {
            case LOGTYPE_INFO:
                strType = "[INFO]   ";
                break;
            case LOGTYPE_WARN:
                strType = "[WARN]   ";
                break;
            case LOGTYPE_ERROR:
                strType = "[ERROR]  ";
                break;
            default:
                strType = "[UNKNOWN]";
        }

        return strType;
    }
}