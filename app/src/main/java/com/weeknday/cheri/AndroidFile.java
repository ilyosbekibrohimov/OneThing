package com.weeknday.cheri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

// Android에서 사용하는 File 관련 클래스
public class AndroidFile
{
	public static final int UERR_SUCCESS		= 0;			// 성공.
	public static final int UERR_ALREADY_EXIST	= 1;			// 이미 파일 또는 디렉터리가 존재함.
	public static final int UERR_API_CALL		= 2;			// Java API 함수에서 에러가 발생함.
	public static final int UERR_EXCEPTION		= 3;			// Exception 발생.
	public static final int UERR_NOT_EXIST		= 4;			// 파일 또는 디렉터리가 존재하지 않음.
	public static final int UERR_FILE_OPEN		= 5;			// 파일 오픈 실패.



	public final String TAG = "AndroidFile";

	/*
		@Function Name	:	createDirectory
		@brief			:	디렉터리를 생성하는 함수
		@param			:	strPath	- 생성할 디렉터리명
		@return			:	0		- 성공
							1		- 이미 디렉터리가 존재함
							2		- mkdirs() 실패
	*/
	public int createDirectory(String strPath)
	{
		int nFnErr = 0;

		File f = new File(strPath);
		if( f != null )
		{
			if( !f.exists() )
			{
				if( !f.mkdirs() )
					nFnErr = UERR_API_CALL;
			}
			else
				nFnErr = UERR_ALREADY_EXIST;
		}

		return nFnErr;
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
	public int createFile(String strFilePath, String strFileName)
	{
		int nFnErr = 0;

		int nErr = createDirectory(strFilePath); 
		if( nErr < 2 )
		{
			File f = new File( (strFilePath+strFileName) );
			if( f != null )
			{
				if( !f.exists() )
				{
					try
					{
						if( !f.createNewFile() )
							nFnErr = UERR_API_CALL;
					}
					catch( IOException e )
					{
						e.printStackTrace();
					}
					finally
					{
						nFnErr = UERR_EXCEPTION;					
					}
				}
				else
					nFnErr = UERR_ALREADY_EXIST;
			}
		}
		else
			nFnErr = nErr * 10;

		return nFnErr;
	}


	/*
		@Function Name	:	deleteFile
		@brief			:	파일 및 디렉터리를 삭제하는 함수
		@param			:	strPathName	- 삭제할 파일 및 디렉터리의 전체 경로
		@return			:	0			- 성공
							otherwise	- 실패
	 */
	public int deleteFile(String strPathName)
	{
		int nFnErr = 0;

		File f = new File( strPathName );
		if( f != null )
		{
			if( f.exists() )
			{
				if( !f.delete() )
					nFnErr = UERR_API_CALL;
			}
			else
				nFnErr = UERR_NOT_EXIST;
		}

		return nFnErr;
	}
	
	public static void removeDirectory(String strRemovePath)
	{
		File[] listFile = new File(strRemovePath).listFiles();
		
		if( listFile == null )
			return;
		
		try
		{
			if(listFile.length > 0)
			{
				for(int i = 0 ; i < listFile.length ; i++)
				{
					if(listFile[i].isFile())
						listFile[i].delete();
					else
						removeDirectory(listFile[i].getPath());
					
					listFile[i].delete();
				}
			}
		}
		catch(Exception e)
		{
			System.err.println(System.err);
			System.exit(-1); 
		}
	}

	/*
		@Function Name	:	openFile
		@brief			:	파일 및 디렉터리를 오픈하는 함수
		@param			:	strFile		- 오픈할 파일 및 디렉터리의 전체 경로
		@return			:	NOT NULL	- 성공
							NULL		- 실패
	*/
	public File openFile(String strFile)
	{
		File f = null;

		f = new File( strFile );
		if( f != null )
		{
			if( !f.exists() )
				f = null;
		}

		return f;
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
	public int writeFile( String strFile, boolean bAppend, byte[] strWriteData )
	{
		int nFnErr = 0;

		File f = openFile(strFile);
		if( f != null && strWriteData != null )
		{
			FileOutputStream fos;

			try
			{
				fos = new FileOutputStream(f, bAppend);
				try
				{
					fos.write( strWriteData );
					fos.flush();
					fos.close();
				}
				catch( IOException e )
				{
					e.printStackTrace();
				}
			}
			catch( FileNotFoundException e )
			{
				e.printStackTrace();
			}

		}
		else
			nFnErr = UERR_FILE_OPEN;

		return nFnErr;
	}


	/*
		@Function Name	:	readFile
		@brief			:	파일에 정보를 읽는 함수
		@param			:	strFile			- 읽을 파일 전체 경로
		@return			:	0				- 성공
							otherwise		- 실패
	 */
	public String readFile( String strFile )
	{
		String strReadData = null;

		File f = openFile(strFile);
		if( f != null )
		{
			int nFileLength = 0;

			try
			{
				FileInputStream fis = new FileInputStream(f);
				nFileLength = (int)f.length();

				byte[] buf = new byte[nFileLength];
				fis.read(buf);
				strReadData = new String(buf, 0, buf.length);
				fis.close();

				// 추후 Read 한 정보를 내보낸다.
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
		}

		return strReadData;
	}

}	// public class AndroidFile