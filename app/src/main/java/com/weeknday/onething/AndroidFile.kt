package com.weeknday.onething

import java.io.*

// Android에서 사용하는 File 관련 클래스
class AndroidFile {
    val TAG = "AndroidFile"

    /*
        @Function Name	:	createDirectory
        @brief			:	디렉터리를 생성하는 함수
        @param			:	strPath	- 생성할 디렉터리명
        @return			:	0		- 성공
                            1		- 이미 디렉터리가 존재함
                            2		- mkdirs() 실패
    */
    fun createDirectory(strPath: String?): Int {
        var nFnErr = 0
        val f = File(strPath)
        if (!f.exists()) {
            if (!f.mkdirs()) nFnErr = UERR_API_CALL
        } else nFnErr = UERR_ALREADY_EXIST
        return nFnErr
    }

    /*
        @Function Name	:	createFile
        @brief			:	파일을 생성하는 함수
        @param			:	strFilePath	- 생성할 파일의 경로
        @param			:	strFileName	- 생성할 파일명
        @return			:	0			- 성공
                            1			- 이미 디렉터리가 존재함
                            2			- mkdirs() 실패
                            3			- createNewFile() Exception
                            10 단위		- createDirectory() 실패
    */
    fun createFile(strFilePath: String, strFileName: String): Int {
        var nFnErr = 0
        val nErr = createDirectory(strFilePath)
        if (nErr < 2) {
            val f = File(strFilePath + strFileName)
            if (!f.exists()) {
                try {
                    if (!f.createNewFile()) nFnErr = UERR_API_CALL
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    nFnErr = UERR_EXCEPTION
                }
            } else nFnErr = UERR_ALREADY_EXIST
        } else nFnErr = nErr * 10
        return nFnErr
    }

    /*
        @Function Name	:	deleteFile
        @brief			:	파일 및 디렉터리를 삭제하는 함수
        @param			:	strPathName	- 삭제할 파일 및 디렉터리의 전체 경로
        @return			:	0			- 성공
                            otherwise	- 실패
     */
    fun deleteFile(strPathName: String?): Int {
        var nFnErr = 0
        val f = File(strPathName)
        if (f.exists()) {
            if (!f.delete()) nFnErr = UERR_API_CALL
        } else nFnErr = UERR_NOT_EXIST
        return nFnErr
    }

    /*
        @Function Name	:	openFile
        @brief			:	파일 및 디렉터리를 오픈하는 함수
        @param			:	strFile		- 오픈할 파일 및 디렉터리의 전체 경로
        @return			:	NOT NULL	- 성공
                            NULL		- 실패
    */
    fun openFile(strFile: String?): File? {
        var f: File? = null
        f = File(strFile)
        if (!f.exists()) f = null
        return f
    }

    /*
        @Function Name	:	writeFile
        @brief			:	파일에 정보를 기록하는 함수
        @param			:	strFile			- 기록할 파일 전체 경로
        @param			:	bAppend			- true : 이어쓰기, false : 덮어쓰기
        @param			:	strWriteData	- 기록할 정보
        @return			:	0				- 성공
                            otherwise		- 실패
     */
    fun writeFile(strFile: String?, bAppend: Boolean, strWriteData: ByteArray?) {
        var nFnErr = 0
        val f = openFile(strFile)
        if (f != null && strWriteData != null) {
            val fos: FileOutputStream
            try {
                fos = FileOutputStream(f, bAppend)
                try {
                    fos.write(strWriteData)
                    fos.flush()
                    fos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        } else nFnErr = UERR_FILE_OPEN
    }

    /*
        @Function Name	:	readFile
        @brief			:	파일에 정보를 읽는 함수
        @param			:	strFile			- 읽을 파일 전체 경로
        @return			:	0				- 성공
                            otherwise		- 실패
     */
    fun readFile(strFile: String?): String? {
        var strReadData: String? = null
        val f = openFile(strFile)
        if (f != null) {
            val nFileLength: Int
            try {
                val fis = FileInputStream(f)
                nFileLength = f.length().toInt()
                val buf = ByteArray(nFileLength)
                fis.read(buf)
                strReadData = String(buf, 0, buf.size)
                fis.close()

                // 추후 Read 한 정보를 내보낸다.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return strReadData
    }

    companion object {
        const val UERR_ALREADY_EXIST = 1 // 이미 파일 또는 디렉터리가 존재함.
        const val UERR_API_CALL = 2 // Java API 함수에서 에러가 발생함.
        const val UERR_EXCEPTION = 3 // Exception 발생.
        const val UERR_NOT_EXIST = 4 // 파일 또는 디렉터리가 존재하지 않음.
        const val UERR_FILE_OPEN = 5 // 파일 오픈 실패.
        fun removeDirectory(strRemovePath: String?) {
            val listFile = File(strRemovePath).listFiles() ?: return
            try {
                if (listFile.size > 0) {
                    for (file in listFile) {
                        if (file.isFile) file.delete() else removeDirectory(file.path)
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                System.err.println(System.err)
                System.exit(-1)
            }
        }
    }
} // public class AndroidFile
