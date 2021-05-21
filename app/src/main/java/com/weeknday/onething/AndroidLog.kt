package com.weeknday.onething

import android.annotation.SuppressLint
import android.os.Environment
import com.weeknday.onething.AndroidFile.Companion.removeDirectory
import java.util.*

// Android에서 Log를 쓰기 위한 클래스
class AndroidLog {
    var m_File: AndroidFile? = null
    private var m_bIsWrite = false

    /*
        @Function Name	:	initialize
        @brief			:	Log를 쓰기 위한 초기화 함수
        @param			:	strPackageName	- 로그를 작성할 패키지명
        @param			:	strLogFileName	- 로그 파일명
        @return			:	0				- 성공
    */
    fun initialize(strPackageName: String?, strLogFileName: String?, bDeleteLogFile: Boolean) {
        var nRes = 0
        m_File = AndroidFile()
        m_strLogPath = Environment.getExternalStorageDirectory().toString() + "/Android/data/"


        // Log Path 설정
        val strPath: String
        strPath =
            if (strPackageName != null && strPackageName.length > 0) m_strLogPath + strPackageName + "/Log/" else m_strLogPath + "Temp/Log/"
        if (bDeleteLogFile) {
            // Previous Log File in Log Directory.
            removeDirectory(strPath)
        }
        if (!m_bIsWrite) return
        nRes = m_File!!.createDirectory(strPath)
        m_strLogPath = strPath


        // LogFile Path 설정
        val strFile: String
        strFile =
            if (strLogFileName != null && strLogFileName.length > 0) strLogFileName else m_strLogFileName
        nRes = m_File!!.createFile(m_strLogPath, strFile)
        m_strLogFileName = strFile
        m_strLogFile = m_strLogPath + m_strLogFileName

        //Log.i("AndroidLog", m_strLogFile);
    }

    fun SetIsWriteLog(bWrite: Boolean) {
        m_bIsWrite = bWrite
    }

    /*
        @Function Name	:	deleteLogFile
        @brief			:	Log 파일을 삭제하는 함수
        @param			:	strFile	- 삭제할 로그 파일
        @return			:	0		- 성공
    */
    fun deleteLogFile(strFile: String): Int {
        var nRes = 0
        nRes = m_File!!.deleteFile(strFile)
        if (nRes > 1) write(
            LOGTYPE_ERROR,
            "deleteFile() Failed.|$nRes|$strFile"
        ) else write(LOGTYPE_INFO, "deleteFile() Success.")
        return nRes
    }

    /*
        @Function Name	:	write
        @brief			:	Log를 작성하는 함수
        @param			:	nLogType	- 작성할 로그의 타입 (INFO, WARN, ERROR...)
        @param			:	strMsg		- 작성할 로그 내용
    */
    fun write(nLogType: Int, strMsg: String?, vararg args: Any?) {
        if (!m_bIsWrite) return
        var _strMsg = strMsg
        if (strMsg == null || strMsg.length == 0) return
        if (args.size != 0) _strMsg = String.format(strMsg, *args)
        _strMsg = """${getCurrentTime(TIMETYPE_LOG)} ${
            getLogType(nLogType)
        } $_strMsg
"""
        m_File!!.writeFile(m_strLogFile, true, _strMsg.toByteArray())
    }

    /*
        @Function Name	:	getCurrentTime
        @brief			:	현재의 시간을 얻는 함수
        @return			:	[YYYY-MM-DD HH:MM:SS] 형식으로 보낸다.
    */
    @SuppressLint("DefaultLocale")
    fun getCurrentTime(nStrTimeType: Int): String {
        val calendar = Calendar.getInstance()
        var strTime: String? = null
        strTime = when (nStrTimeType) {
            TIMETYPE_LOG -> {
                String.format(
                    "[%04d-%02d-%02d %02d:%02d:%02d]",
                    calendar[Calendar.YEAR],
                    calendar[Calendar.MONTH] + 1,
                    calendar[Calendar.DAY_OF_MONTH],
                    calendar[Calendar.HOUR_OF_DAY],
                    calendar[Calendar.MINUTE],
                    calendar[Calendar.SECOND]
                )
            }
            TIMETYPE_GENERAL -> {
                String.format(
                    "%04d-%02d-%02d %02d:%02d:%02d",
                    calendar[Calendar.YEAR],
                    calendar[Calendar.MONTH] + 1,
                    calendar[Calendar.DAY_OF_MONTH],
                    calendar[Calendar.HOUR_OF_DAY],
                    calendar[Calendar.MINUTE],
                    calendar[Calendar.SECOND]
                )
            }
            else -> {
                String.format(
                    "%04d-%02d-%02d %02d:%02d:%02d",
                    calendar[Calendar.YEAR],
                    calendar[Calendar.MONTH] + 1,
                    calendar[Calendar.DAY_OF_MONTH],
                    calendar[Calendar.HOUR_OF_DAY],
                    calendar[Calendar.MINUTE],
                    calendar[Calendar.SECOND]
                )
            }
        }
        return strTime
    }

    companion object {
        private var m_strLogPath = ""
        private var m_strLogFileName = "AndroidLog.txt"
        private var m_strLogFile = ""
        const val LOGTYPE_INFO = 0
        const val LOGTYPE_WARN = 1
        const val LOGTYPE_ERROR = 2
        const val TIMETYPE_LOG = 0
        const val TIMETYPE_GENERAL = 1

        /*
        @Function Name	:	getLogType
        @brief			:	로그의 형식을 문자열로 반환하는 함수
        @return			:	[LOGTYPE] 형식으로 보낸다.
    */
        private fun getLogType(nLogType: Int): String {
            val strType: String
            strType = when (nLogType) {
                LOGTYPE_INFO -> "[INFO]   "
                LOGTYPE_WARN -> "[WARN]   "
                LOGTYPE_ERROR -> "[ERROR]  "
                else -> "[UNKNOWN]"
            }
            return strType
        }
    }
}