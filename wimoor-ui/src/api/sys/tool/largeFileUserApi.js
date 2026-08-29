import request from "@/utils/request.js";

function list(){
	return request({'method':'GET',
	                 'url':"/admin/api/v1/file/userfile/list",
	       });
}
function listCompany(){
	return request({'method':'GET',
	                 'url':"/admin/api/v1/file/userfile/listCompany",
	       });
}
function upload(FormData, onProgress){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/upload",
				    'data':FormData,
					'headers':{'Content-Type':"multipart/form-data"},
					'onUploadProgress': onProgress,
	       });
}
function uploadCompany(FormData, onProgress){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/uploadCompany",
				    'data':FormData,
					'headers':{'Content-Type':"multipart/form-data"},
					'onUploadProgress': onProgress,
	       });
}
// 分片上传：初始化
function initChunkUpload(filename, filesize){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/chunk/init",
				    'params':{'filename': filename, 'filesize': filesize},
	       });
}
// 分片上传：上传单个分片
function uploadChunk(chunkData, onProgress){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/chunk/upload",
				    'data':chunkData,
					'headers':{'Content-Type':"multipart/form-data"},
					'onUploadProgress': onProgress,
	       });
}
// 分片上传：合并分片
function mergeChunks(uploadId, filename, chunkTotal){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/chunk/merge",
				    'params':{'uploadId': uploadId, 'filename': filename, 'chunkTotal': chunkTotal},
	       });
}
// 分片上传：查询已上传分片
function getUploadedChunks(uploadId){
	return request({'method':'GET',
	                 'url':"/admin/api/v1/file/userfile/chunk/uploaded",
				    'params':{'uploadId': uploadId},
	       });
}
// 分片上传：获取预签名上传URL（前端直传MinIO）
function getPresignedUrls(uploadId, chunkTotal){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/chunk/presigned",
				    'params':{'uploadId': uploadId, 'chunkTotal': chunkTotal},
	       });
}
// 分片上传：取消上传
function cancelChunkUpload(uploadId){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/chunk/cancel",
				    'params':{'uploadId': uploadId},
	       });
}
function rename(id, name){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/rename",
					'params':{'id':id, 'name':name},
	       });
}
function renameCompany(id, name){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/renameCompany",
					'params':{'id':id, 'name':name},
	       });
}
function remove(id){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/delete",
					'params':{'id':id},
	       });
}
function removeCompany(id){
	return request({'method':'POST',
	                 'url':"/admin/api/v1/file/userfile/deleteCompany",
					'params':{'id':id},
	       });
}
function download(id){
	return request({'method':'GET',
	                 'url':"/admin/api/v1/file/userfile/download",
					'params':{'id':id},
					'responseType':'blob',
	       });
}
function getLink(id){
	return request({'method':'GET',
	                 'url':"/admin/api/v1/file/userfile/link",
					'params':{'id':id},
	       });
}
export default{
	list, listCompany, upload, uploadCompany, rename, renameCompany, remove, removeCompany, download, getLink,
	initChunkUpload, uploadChunk, mergeChunks, getUploadedChunks, getPresignedUrls, cancelChunkUpload
}