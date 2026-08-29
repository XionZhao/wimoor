package com.wimoor.common.service.impl;

import com.wimoor.common.pojo.entity.StorageType;
import com.wimoor.common.service.ObjectHandler;
import com.wimoor.common.service.util.*;
import io.minio.messages.Part;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class StorageLargeService {

    
    @Resource
	OSSLargeApiUtil oSSLargeApiUtil;
    @Resource
	FTPLargeServerUtil ftpLargeServerUtil;
    @Resource
	MinIOLargeApiUtil minIOLargeApiUtil;


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
		 if(oSSLargeApiUtil.isRun()) {
			 oSSLargeApiUtil.putObject( bucketName, objectName, stream);
	         return true;
		 }else   if(minIOLargeApiUtil.isRun()){
			 minIOLargeApiUtil.putObject(bucketName, objectName, stream);
	         return true;
		 }else {
			 try {
				 ftpLargeServerUtil.uploadFileOther(bucketName, objectName, stream);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 return true;
		 }
		
	}

	public void removeObject(String bucketName, String objectName)  {
		// TODO Auto-generated method stub
		 if(oSSLargeApiUtil.isRun()) {
			 oSSLargeApiUtil.removeObject(bucketName, objectName);
		 }else  if(minIOLargeApiUtil.isRun()){
			 minIOLargeApiUtil.removeObject( bucketName, objectName);
		 }else {
			 try {
				 ftpLargeServerUtil.deleteFile(bucketName, objectName);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }
	}

	public void getObject(String bucketName, String objectName, ObjectHandler handler, Map<String,Object> param)  {
		// TODO Auto-generated method stub
		if(oSSLargeApiUtil.isRun()) {
			oSSLargeApiUtil.getObject(bucketName, objectName,handler,param);
		}else  if(minIOLargeApiUtil.isRun()){
			minIOLargeApiUtil.getObject(bucketName, objectName,handler,param);
		}else {
			ftpLargeServerUtil.getObject(bucketName, objectName,handler,param);
		}
	}


	public String getBucketName() {
		 if(oSSLargeApiUtil.isRun()) {
			return oSSLargeApiUtil.getBucketName();
		 }else  if(minIOLargeApiUtil.isRun()){
			return minIOLargeApiUtil.getBucketName( );
		 }else {
			return ftpLargeServerUtil.getBucketName();
		 }
		 
	}



	public String getBucketPath() {
		if(oSSLargeApiUtil.isRun()) {
			return oSSLargeApiUtil.getBucketPath();
		 }else  if(minIOLargeApiUtil.isRun()){
			return minIOLargeApiUtil.getBucketPath( );
		 }else {
			return ftpLargeServerUtil.getBucketPath();
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
		if(storageType.equals(StorageType.OSS)) {
			oSSLargeApiUtil.putObject( bucketName, objectName, stream);
			return true;
		}else   if(storageType.equals(StorageType.MinIO)){
			minIOLargeApiUtil.putObject(bucketName, objectName, stream);
			return true;
		}else {
			try {
				ftpLargeServerUtil.uploadFileOther(bucketName, objectName, stream);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}

	}

	public void removeObject(String bucketName, String objectName, StorageType storageType)  {
		// TODO Auto-generated method stub
		if(storageType.equals(StorageType.OSS)) {
			oSSLargeApiUtil.removeObject(bucketName, objectName);
		}else  if(storageType.equals(StorageType.MinIO)){
			minIOLargeApiUtil.removeObject( bucketName, objectName);
		}else {
			try {
				ftpLargeServerUtil.deleteFile(bucketName, objectName);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}




	public String getBucketName(StorageType storageType) {
		if(storageType.equals(StorageType.OSS)) {
			return oSSLargeApiUtil.getBucketName();
		}else  if(storageType.equals(StorageType.MinIO)){
			return minIOLargeApiUtil.getBucketName( );
		}else {
			return ftpLargeServerUtil.getBucketName();
		}

	}



	public String getBucketPath(StorageType storageType) {
		if(storageType.equals(StorageType.OSS)) {
			return oSSLargeApiUtil.getBucketPath();
		}else  if(storageType.equals(StorageType.MinIO)){
			return minIOLargeApiUtil.getBucketPath( );
		}else {
			return ftpLargeServerUtil.getBucketPath();
		}
	}


	public void getObject(String bucketName, String objectName, ObjectHandler handler, Map<String,Object> param, StorageType storageType)  {
		// TODO Auto-generated method stub
		if(storageType.equals(StorageType.OSS)) {
			oSSLargeApiUtil.getObject(bucketName, objectName,handler,param);
		}else  if(storageType.equals(StorageType.MinIO)){
			minIOLargeApiUtil.getObject(bucketName, objectName,handler,param);
		}else {
			ftpLargeServerUtil.getObject(bucketName, objectName,handler,param);
		}


	}

	// ==================== 分片上传方法（仅支持 MinIO） ====================

	/**
	 * 初始化分片上传
	 * @return uploadId
	 */
	public String initiateMultipartUpload(String bucketName, String objectName) throws Exception {
		if (minIOLargeApiUtil.isRun()) {
			return minIOLargeApiUtil.initiateMultipartUpload(bucketName, objectName);
		}
		throw new UnsupportedOperationException("分片上传仅支持 MinIO 存储");
	}

	/**
	 * 上传分片到 MinIO
	 * @param partNumber 分片编号（从1开始）
	 * @return Part 对象
	 */
	public Part uploadPart(String bucketName, String objectName, String uploadId,
						   int partNumber, InputStream data, long partSize) throws Exception {
		if (minIOLargeApiUtil.isRun()) {
			return minIOLargeApiUtil.uploadPart(bucketName, objectName, uploadId, partNumber, data, partSize);
		}
		throw new UnsupportedOperationException("分片上传仅支持 MinIO 存储");
	}

	/**
	 * 合并分片
	 */
	public void completeMultipartUpload(String bucketName, String objectName,
										String uploadId, List<Part> parts) throws Exception {
		if (minIOLargeApiUtil.isRun()) {
			minIOLargeApiUtil.completeMultipartUpload(bucketName, objectName, uploadId, parts);
			return;
		}
		throw new UnsupportedOperationException("分片上传仅支持 MinIO 存储");
	}

	/**
	 * 查询已上传的分片
	 */
	public List<Integer> listUploadedParts(String bucketName, String objectName,
										   String uploadId) throws Exception {
		if (minIOLargeApiUtil.isRun()) {
			return minIOLargeApiUtil.listUploadedParts(bucketName, objectName, uploadId);
		}
		throw new UnsupportedOperationException("分片上传仅支持 MinIO 存储");
	}

	/**
	 * 查询已上传的分片完整列表（含 etag），按 partNumber 升序
	 */
	public List<Part> listParts(String bucketName, String objectName,
								String uploadId) throws Exception {
		if (minIOLargeApiUtil.isRun()) {
			return minIOLargeApiUtil.listParts(bucketName, objectName, uploadId);
		}
		throw new UnsupportedOperationException("分片上传仅支持 MinIO 存储");
	}

	/**
	 * 生成分片上传的预签名 URL，前端可直接 PUT 到 MinIO
	 */
	public String getPresignedUploadUrl(String bucketName, String objectName,
										String uploadId, int partNumber,
										int expirySeconds) throws Exception {
		if (minIOLargeApiUtil.isRun()) {
			return minIOLargeApiUtil.getPresignedUploadUrl(bucketName, objectName, uploadId, partNumber, expirySeconds);
		}
		throw new UnsupportedOperationException("分片上传仅支持 MinIO 存储");
	}

	/**
	 * 取消分片上传
	 */
	public void abortMultipartUpload(String bucketName, String objectName,
									 String uploadId) throws Exception {
		if (minIOLargeApiUtil.isRun()) {
			minIOLargeApiUtil.abortMultipartUpload(bucketName, objectName, uploadId);
			return;
		}
		throw new UnsupportedOperationException("分片上传仅支持 MinIO 存储");
	}

	/**
	 * 判断是否支持分片上传（当前仅 MinIO 支持）
	 */
	public boolean supportsMultipartUpload() {
		return minIOLargeApiUtil.isRun();
	}
}
