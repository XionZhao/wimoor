package com.wimoor.common.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.annotation.Resource;

import com.wimoor.common.pojo.entity.StorageType;
import com.wimoor.common.service.ObjectHandler;
import org.springframework.stereotype.Component;

import com.wimoor.common.service.util.FTPServerUtil;
import com.wimoor.common.service.util.MinIOApiUtil;
import com.wimoor.common.service.util.OSSApiUtil;

@Component
public class StorageService {
	
	private static final int MAX_RETRY_COUNT = 3;

    
    @Resource
    OSSApiUtil oSSApiUtil;
    @Resource
    FTPServerUtil ftpServerUtil;
    @Resource
    MinIOApiUtil minIOApiUtil;


	/**
	 * 上传文件
	 * @param objectName
	 * @param stream 可以是已下方式 
	 * InputStream inputStream = new URL(url).openStream();
	 * ByteArrayInputStream inputStream=new ByteArrayInputStream(content.getBytes())
	 * InputStream inputStream = new FileInputStream(filePath);
	 * @throws Exception 
	 * 
	 */
	 public  Boolean putObject(String bucketName,String objectName,InputStream stream)  {
        // 填写Object完整路径，例如exampledir/exampleobject.txt。Object完整路径中不能包含Bucket名称。
		 // 将InputStream转为byte[]以便重试
		 byte[] data;
		 try {
			 data = toByteArray(stream);
		 } catch (IOException e) {
			 e.printStackTrace();
			 return false;
		 }
		 
		 for (int i = 0; i < MAX_RETRY_COUNT; i++) {
			 if (i > 0) {
				 System.out.println("Upload retry attempt " + i + " for " + objectName);
			 }
			 ByteArrayInputStream retryStream = new ByteArrayInputStream(data);
			 boolean success = false;
			 
			 if(oSSApiUtil.isRun()) {
				 success = oSSApiUtil.putObject(bucketName, objectName, retryStream);
			 } else if(minIOApiUtil.isRun()){
				 success = minIOApiUtil.putObject(bucketName, objectName, retryStream);
			 } else {
				 try {
					 ftpServerUtil.uploadFileOther(bucketName, objectName, retryStream);
					 success = true;
				 } catch (Exception e) {
					 e.printStackTrace();
				 }
			 }
			 
			 if (success) {
				 return true;
			 }
		 }
		 
		 System.out.println("Upload failed after " + MAX_RETRY_COUNT + " attempts for " + objectName);
		 return false;
	}

	public void removeObject(String bucketName, String objectName)  {
		// TODO Auto-generated method stub
		 if(oSSApiUtil.isRun()) {
			 oSSApiUtil.removeObject(bucketName, objectName);
		 }else  if(minIOApiUtil.isRun()){
			 minIOApiUtil.removeObject( bucketName, objectName);
		 }else {
			 try {
				 ftpServerUtil.deleteFile(bucketName, objectName);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }
	}

	public void getObject(String bucketName, String objectName, ObjectHandler handler, Map<String,Object> param)  {
		// TODO Auto-generated method stub
		if(oSSApiUtil.isRun()) {
			oSSApiUtil.getObject(bucketName, objectName,handler,param);
		}else  if(minIOApiUtil.isRun()){
			minIOApiUtil.getObject(bucketName, objectName,handler,param);
		}else {
			ftpServerUtil.getObject(bucketName, objectName,handler,param);
		}
	}


	public String getBucketName() {
		 if(oSSApiUtil.isRun()) {
			return oSSApiUtil.getBucketName();
		 }else  if(minIOApiUtil.isRun()){
			return minIOApiUtil.getBucketName( );
		 }else {
			return ftpServerUtil.getBucketName();
		 }
		 
	}



	public String getBucketPath() {
		if(oSSApiUtil.isRun()) {
			return oSSApiUtil.getBucketPath();
		 }else  if(minIOApiUtil.isRun()){
			return minIOApiUtil.getBucketPath( );
		 }else {
			return ftpServerUtil.getBucketPath();
		 }
	}


	/**
	 * 上传文件
	 * @param objectName
	 * @param stream 可以是已下方式
	 * InputStream inputStream = new URL(url).openStream();
	 * ByteArrayInputStream inputStream=new ByteArrayInputStream(content.getBytes())
	 * InputStream inputStream = new FileInputStream(filePath);
	 * @throws Exception
	 *
	 */
	public  Boolean putObject(String bucketName, String objectName, InputStream stream, StorageType storageType)  {
		// 填写Object完整路径，例如exampledir/exampleobject.txt。Object完整路径中不能包含Bucket名称。
		// 将InputStream转为byte[]以便重试
		byte[] data;
		try {
			data = toByteArray(stream);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		for (int i = 0; i < MAX_RETRY_COUNT; i++) {
			if (i > 0) {
				System.out.println("Upload retry attempt " + i + " for " + objectName);
			}
			ByteArrayInputStream retryStream = new ByteArrayInputStream(data);
			boolean success = false;
			
			if(storageType.equals(StorageType.OSS)) {
				success = oSSApiUtil.putObject(bucketName, objectName, retryStream);
			} else if(storageType.equals(StorageType.MinIO)){
				success = minIOApiUtil.putObject(bucketName, objectName, retryStream);
			} else {
				try {
					ftpServerUtil.uploadFileOther(bucketName, objectName, retryStream);
					success = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if (success) {
				return true;
			}
		}
		
		System.out.println("Upload failed after " + MAX_RETRY_COUNT + " attempts for " + objectName);
		return false;
	}

	private byte[] toByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] temp = new byte[4096];
		int bytesRead;
		while ((bytesRead = inputStream.read(temp)) != -1) {
			buffer.write(temp, 0, bytesRead);
		}
		return buffer.toByteArray();
	}

	public void removeObject(String bucketName, String objectName, StorageType storageType)  {
		// TODO Auto-generated method stub
		if(storageType.equals(StorageType.OSS)) {
			oSSApiUtil.removeObject(bucketName, objectName);
		}else  if(storageType.equals(StorageType.MinIO)){
			minIOApiUtil.removeObject( bucketName, objectName);
		}else {
			try {
				ftpServerUtil.deleteFile(bucketName, objectName);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}




	public String getBucketName(StorageType storageType) {
		if(storageType.equals(StorageType.OSS)) {
			return oSSApiUtil.getBucketName();
		}else  if(storageType.equals(StorageType.MinIO)){
			return minIOApiUtil.getBucketName( );
		}else {
			return ftpServerUtil.getBucketName();
		}

	}



	public String getBucketPath(StorageType storageType) {
		if(storageType.equals(StorageType.OSS)) {
			return oSSApiUtil.getBucketPath();
		}else  if(storageType.equals(StorageType.MinIO)){
			return minIOApiUtil.getBucketPath( );
		}else {
			return ftpServerUtil.getBucketPath();
		}
	}


	public void getObject(String bucketName, String objectName, ObjectHandler handler, Map<String,Object> param, StorageType storageType)  {
		// TODO Auto-generated method stub
		if(storageType.equals(StorageType.OSS)) {
			oSSApiUtil.getObject(bucketName, objectName,handler,param);
		}else  if(storageType.equals(StorageType.MinIO)){
			minIOApiUtil.getObject(bucketName, objectName,handler,param);
		}else {
			ftpServerUtil.getObject(bucketName, objectName,handler,param);
		}


	}
	  
}
