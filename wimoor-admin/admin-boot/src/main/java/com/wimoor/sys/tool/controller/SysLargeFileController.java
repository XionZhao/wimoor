package com.wimoor.sys.tool.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wimoor.admin.common.exception.BizException;
import com.wimoor.common.mvc.FileUpload;
import com.wimoor.common.result.Result;
import com.wimoor.common.service.ObjectHandler;
import com.wimoor.common.service.impl.StorageLargeService;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.sys.tool.pojo.entity.LargeFile;
import com.wimoor.sys.tool.pojo.entity.LargeFileUser;
import com.wimoor.sys.tool.service.ILargeFileService;
import com.wimoor.sys.tool.service.ILargeFileUserService;
import io.minio.messages.Part;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Api(tags = "用户私人文件管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/file")
public class SysLargeFileController {
    final ILargeFileService iLargeFileService;
    final ILargeFileUserService iLargeFileUserService;
    final FileUpload fileUpload;
    final StorageLargeService storageLargeService;

    // 分片上传会话存储：uploadId -> 会话信息
    private final ConcurrentHashMap<String, MultipartUploadSession> uploadSessions = new ConcurrentHashMap<>();

    // 分片上传会话信息
    private static class MultipartUploadSession {
        String uploadId;
        String bucketName;
        String objectName;
        String originalFilename;
        String storageName;
        String userid;
        String companyid;
        String type;
        List<Part> uploadedParts = Collections.synchronizedList(new ArrayList<>());
        long createTime;

        MultipartUploadSession(String uploadId, String bucketName, String objectName,
                               String originalFilename, String storageName,
                               String userid, String companyid, String type) {
            this.uploadId = uploadId;
            this.bucketName = bucketName;
            this.objectName = objectName;
            this.originalFilename = originalFilename;
            this.storageName = storageName;
            this.userid = userid;
            this.companyid = companyid;
            this.type = type;
            this.createTime = System.currentTimeMillis();
        }
    }

    @ApiOperation("上传文件(通用)")
    @PostMapping(value ="/upload/{type}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> uploadFile(@PathVariable("type") String type,@RequestParam("file") MultipartFile file) {
        UserInfo userInfo = UserInfoContext.get();
        try {
            InputStream inputStream = file.getInputStream();
            String fileName = file.getOriginalFilename();
            String fileNameWithoutExtension =fileName;
            String fileExtension = "";
            if( fileName.contains(".")){
                String[] fileInfo=fileName.split("\\.");
                fileNameWithoutExtension = fileInfo[0];
                fileExtension = fileInfo[1];
            }
            if(fileExtension.equals("xml")&&fileNameWithoutExtension.matches(".*\\d{14,}.*")){
                // XML文件名已包含14位以上时间戳（如海关CEB文件），保持原文件名不变
                fileName=fileNameWithoutExtension+"."+fileExtension;
            }else{
                SimpleDateFormat TIMESTAMP_MS_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");
                fileName=fileNameWithoutExtension+TIMESTAMP_MS_FORMAT.format(new Date())+"."+fileExtension;
            }
            LargeFile largefile = iLargeFileService.uploadLargeFile(inputStream, type, userInfo.getCompanyid() , fileName);
            Map<String, Object> result = new HashMap<>();
            result.put("url", fileUpload.getPictureImage(largefile.getLocation()));
            result.put("id", largefile.getId());
            result.put("name", fileName);
            return Result.success(result);
        } catch (IOException e) {
            throw new BizException("读取异常");
        }
    }

    @ApiOperation("删除文件(通用)")
    @GetMapping(value ="/delete/{type}")
    public Result<?> deleteFile(@PathVariable("type") String type,String path) {
        UserInfo userInfo = UserInfoContext.get();
        path=fileUpload.pathToLocation(path);
        iLargeFileService.deleteLargeFile(type,path,userInfo.getCompanyid());
        return Result.success("删除成功");
    }

    @ApiOperation("上传用户私人文件")
    @PostMapping(value ="/userfile/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> uploadUserFile(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "name", required = false) String name) {
        UserInfo userInfo = UserInfoContext.get();
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            InputStream inputStream = file.getInputStream();
            String originalName = file.getOriginalFilename();
            String fileExtension = "";
            String fileNameWithoutExtension = originalName;
            if(originalName.contains(".")){
                String[] fileInfo = originalName.split("\\.");
                fileNameWithoutExtension = fileInfo[0];
                fileExtension = fileInfo[fileInfo.length - 1];
            }
            // 使用时间戳生成唯一文件名
            String storageName = fileNameWithoutExtension + "_" + format.format(new Date());
            if(!fileExtension.isEmpty()){
                storageName = storageName + "." + fileExtension;
            }
            // 上传到MinIO，类型为users
            LargeFile largefile = iLargeFileService.uploadLargeFile(inputStream, "users", userInfo.getCompanyid(), storageName);
            if(largefile == null){
                throw new BizException("文件上传失败");
            }
            // 保存用户文件关联记录
            LargeFileUser fileUser = new LargeFileUser();
            fileUser.setId(largefile.getId());
            fileUser.setUserid(userInfo.getId());
            fileUser.setShopid(userInfo.getCompanyid() != null ? userInfo.getCompanyid() : null);
            fileUser.setFileid(largefile.getId());
            // 如果用户没有指定名称，使用原始文件名
            fileUser.setName(name != null && !name.isEmpty() ? name : originalName);
            fileUser.setCreatetime(new Date());
            iLargeFileUserService.save(fileUser);
            return Result.success(fileUser);
        } catch (IOException e) {
            throw new BizException("读取异常");
        }
    }

    @ApiOperation("查询当前用户的文件列表")
    @GetMapping(value ="/userfile/list")
    public Result<?> getUserFileList() {
        UserInfo userInfo = UserInfoContext.get();
        List<LargeFileUser> fileList = iLargeFileUserService.list(
            new LambdaQueryWrapper<LargeFileUser>()
                .eq(LargeFileUser::getUserid, userInfo.getId())
                .orderByDesc(LargeFileUser::getCreatetime)
        );
        fillFileUrl(fileList);
        return Result.success(fileList);
    }

    @ApiOperation("重命名用户文件")
    @PostMapping(value ="/userfile/rename")
    public Result<?> renameUserFile(@RequestParam("id") String id, @RequestParam("name") String name) {
        UserInfo userInfo = UserInfoContext.get();
        LargeFileUser fileUser = iLargeFileUserService.getById(id);
        if(fileUser == null){
            throw new BizException("文件不存在");
        }
        // 确保只能操作自己的文件
        if(!fileUser.getUserid().equals(userInfo.getId())){
            throw new BizException("无权操作此文件");
        }
        fileUser.setName(name);
        iLargeFileUserService.updateById(fileUser);
        return Result.success(fileUser);
    }

    @ApiOperation("删除用户私人文件")
    @PostMapping(value ="/userfile/delete")
    public Result<?> deleteUserFile(@RequestParam("id") String id) {
        UserInfo userInfo = UserInfoContext.get();
        LargeFileUser fileUser = iLargeFileUserService.getById(id);
        if(fileUser == null){
            throw new BizException("文件不存在");
        }
        // 确保只能删除自己的文件
        if(!fileUser.getUserid().equals(userInfo.getId())){
            throw new BizException("无权操作此文件");
        }
        // 删除MinIO中的文件
        LargeFile largeFile = iLargeFileService.getById(fileUser.getFileid());
        if(largeFile != null){
            iLargeFileService.deleteLargeFile("users", largeFile.getLocation(), userInfo.getCompanyid());
        }
        // 删除用户文件关联记录
        iLargeFileUserService.removeById(id);
        return Result.success("删除成功");
    }

    @ApiOperation("下载用户私人文件")
    @GetMapping(value ="/userfile/download")
    public void downloadUserFile(@RequestParam("id") String id, HttpServletResponse response) {
        UserInfo userInfo = UserInfoContext.get();
        LargeFileUser fileUser = iLargeFileUserService.getById(id);
        if(fileUser == null){
            throw new BizException("文件不存在");
        }
        // 确保只能下载自己的文件
        if(!fileUser.getUserid().equals(userInfo.getId())){
            throw new BizException("无权操作此文件");
        }
        LargeFile largeFile = iLargeFileService.getById(fileUser.getFileid());
        if(largeFile == null || largeFile.getLocation() == null){
            throw new BizException("文件不存在");
        }
        String location = largeFile.getLocation();
        // 将location转换为MinIO objectName：去掉bucketName前缀
        String objectName = location.replace(storageLargeService.getBucketName() + "/", "");
        try {
            String fileName = fileUser.getName() != null ? fileUser.getName() : objectName.substring(objectName.lastIndexOf("/") + 1);
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            storageLargeService.getObject(storageLargeService.getBucketName(), objectName, new ObjectHandler() {
                @Override
                public void treatReader(InputStream is, Map<String, Object> param) {
                    try (OutputStream os = response.getOutputStream()) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                        os.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, null);
        } catch (Exception e) {
            throw new BizException("下载失败");
        }
    }

    @ApiOperation("获取用户文件下载链接(可分享)")
    @GetMapping(value ="/userfile/link")
    public Result<?> getUserFileLink(@RequestParam("id") String id) {
        LargeFileUser fileUser = iLargeFileUserService.getById(id);
        if(fileUser == null){
            throw new BizException("文件不存在");
        }
        LargeFile largeFile = iLargeFileService.getById(fileUser.getFileid());
        if(largeFile == null || largeFile.getLocation() == null){
            throw new BizException("文件不存在");
        }
        // 使用getPictureImage将location转换为可访问的URL
        String url = fileUpload.getPictureImage(largeFile.getLocation());
        return Result.success(url);
    }

    @ApiOperation("上传公司文件")
    @PostMapping(value ="/userfile/uploadCompany",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> uploadCompanyFile(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "name", required = false) String name) {
        UserInfo userInfo = UserInfoContext.get();
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            InputStream inputStream = file.getInputStream();
            String originalName = file.getOriginalFilename();
            String fileExtension = "";
            String fileNameWithoutExtension = originalName;
            if(originalName.contains(".")){
                String[] fileInfo = originalName.split("\\.");
                fileNameWithoutExtension = fileInfo[0];
                fileExtension = fileInfo[fileInfo.length - 1];
            }
            // 使用时间戳生成唯一文件名
            String storageName = fileNameWithoutExtension + "_" + format.format(new Date());
            if(!fileExtension.isEmpty()){
                storageName = storageName + "." + fileExtension;
            }
            // 上传到MinIO，类型为company
            LargeFile largefile = iLargeFileService.uploadLargeFile(inputStream, "company", userInfo.getCompanyid(), storageName);
            if(largefile == null){
                throw new BizException("文件上传失败");
            }
            // 保存公司文件关联记录，userid为null表示公司文件
            LargeFileUser fileUser = new LargeFileUser();
            fileUser.setId(largefile.getId());
            fileUser.setUserid(null); // 公司文件userid为null
            fileUser.setShopid(userInfo.getCompanyid());
            fileUser.setFileid(largefile.getId());
            fileUser.setName(name != null && !name.isEmpty() ? name : originalName);
            fileUser.setCreatetime(new Date());
            iLargeFileUserService.save(fileUser);
            return Result.success(fileUser);
        } catch (IOException e) {
            throw new BizException("读取异常");
        }
    }

    @ApiOperation("查询公司文件列表")
    @GetMapping(value ="/userfile/listCompany")
    public Result<?> getCompanyFileList() {
        UserInfo userInfo = UserInfoContext.get();
        List<LargeFileUser> fileList = iLargeFileUserService.list(
            new LambdaQueryWrapper<LargeFileUser>()
                .eq(LargeFileUser::getShopid, userInfo.getCompanyid())
                .isNull(LargeFileUser::getUserid)
                .orderByDesc(LargeFileUser::getCreatetime)
        );
        fillFileUrl(fileList);
        return Result.success(fileList);
    }

    @ApiOperation("删除公司文件")
    @PostMapping(value ="/userfile/deleteCompany")
    public Result<?> deleteCompanyFile(@RequestParam("id") String id) {
        UserInfo userInfo = UserInfoContext.get();
        LargeFileUser fileUser = iLargeFileUserService.getById(id);
        if(fileUser == null){
            throw new BizException("文件不存在");
        }
        // 公司文件只验证shopid
        if(!fileUser.getShopid().equals(userInfo.getCompanyid())){
            throw new BizException("无权操作此文件");
        }
        // 删除MinIO中的文件
        LargeFile largeFile = iLargeFileService.getById(fileUser.getFileid());
        if(largeFile != null){
            iLargeFileService.deleteLargeFile("company", largeFile.getLocation(), userInfo.getCompanyid());
        }
        // 删除文件关联记录
        iLargeFileUserService.removeById(id);
        return Result.success("删除成功");
    }

    @ApiOperation("重命名公司文件")
    @PostMapping(value ="/userfile/renameCompany")
    public Result<?> renameCompanyFile(@RequestParam("id") String id, @RequestParam("name") String name) {
        UserInfo userInfo = UserInfoContext.get();
        LargeFileUser fileUser = iLargeFileUserService.getById(id);
        if(fileUser == null){
            throw new BizException("文件不存在");
        }
        // 公司文件只验证shopid
        if(!fileUser.getShopid().equals(userInfo.getCompanyid())){
            throw new BizException("无权操作此文件");
        }
        fileUser.setName(name);
        iLargeFileUserService.updateById(fileUser);
        return Result.success(fileUser);
    }

    /**
     * 批量填充文件列表的URL字段
     */
    private void fillFileUrl(List<LargeFileUser> fileList) {
        if (fileList == null || fileList.isEmpty()) return;
        List<String> fileIds = fileList.stream()
            .map(LargeFileUser::getFileid)
            .collect(Collectors.toList());
        List<LargeFile> largeFiles = iLargeFileService.listByIds(fileIds);
        Map<String, LargeFile> fileMap = largeFiles.stream()
            .collect(Collectors.toMap(LargeFile::getId, f -> f));
        for (LargeFileUser item : fileList) {
            LargeFile largeFile = fileMap.get(item.getFileid());
            if (largeFile != null && largeFile.getLocation() != null) {
                item.setUrl(fileUpload.getPictureImage(largeFile.getLocation()));
            }
        }
    }

    // ==================== 分片上传相关接口（直传 MinIO） ====================

    @ApiOperation("初始化分片上传")
    @PostMapping(value = "/userfile/chunk/init")
    public Result<?> initChunkUpload(@RequestParam("filename") String filename,
                                     @RequestParam("filesize") Long filesize,
                                     @RequestParam(value = "isCompany", defaultValue = "false") Boolean isCompany) {
        UserInfo userInfo = UserInfoContext.get();

        // 生成存储文件名
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String originalName = filename;
        String fileExtension = "";
        String fileNameWithoutExtension = originalName;
        if (originalName.contains(".")) {
            String[] fileInfo = originalName.split("\\.");
            fileNameWithoutExtension = fileInfo[0];
            fileExtension = fileInfo[fileInfo.length - 1];
        }
        String storageName = fileNameWithoutExtension + "_" + format.format(new Date());
        if (!fileExtension.isEmpty()) {
            storageName = storageName + "." + fileExtension;
        }

        // 构建objectName: shopid/type/storageName
        String type = isCompany ? "company" : "users";
        String objectName = userInfo.getCompanyid() + "/" + type + "/" + storageName;
        String bucketName = storageLargeService.getBucketName();

        try {
            // 在MinIO上初始化分片上传
            String uploadId = storageLargeService.initiateMultipartUpload(bucketName, objectName);

            // 保存会话信息
            MultipartUploadSession session = new MultipartUploadSession(
                    uploadId, bucketName, objectName, originalName, storageName,
                    userInfo.getId(), userInfo.getCompanyid(), type);
            uploadSessions.put(uploadId, session);

            Map<String, Object> result = new HashMap<>();
            result.put("uploadId", uploadId);
            return Result.success(result);
        } catch (Exception e) {
            throw new BizException("初始化分片上传失败: " + e.getMessage());
        }
    }

    @ApiOperation("上传单个分片")
    @PostMapping(value = "/userfile/chunk/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> uploadChunk(@RequestParam("file") MultipartFile file,
                                 @RequestParam("uploadId") String uploadId,
                                 @RequestParam("chunkIndex") Integer chunkIndex,
                                 @RequestParam("chunkTotal") Integer chunkTotal) {
        if (uploadId == null || uploadId.isEmpty()) {
            throw new BizException("uploadId不能为空");
        }

        MultipartUploadSession session = uploadSessions.get(uploadId);
        if (session == null) {
            throw new BizException("上传会话不存在，请重新初始化");
        }

        try {
            // MinIO分片编号从1开始
            int partNumber = chunkIndex + 1;
            Part part = storageLargeService.uploadPart(
                    session.bucketName, session.objectName, uploadId,
                    partNumber, file.getInputStream(), file.getSize());
            session.uploadedParts.add(part);

            Map<String, Object> result = new HashMap<>();
            result.put("chunkIndex", chunkIndex);
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            throw new BizException("上传分片失败: " + e.getMessage());
        }
    }

    @ApiOperation("查询已上传的分片列表")
    @GetMapping(value = "/userfile/chunk/uploaded")
    public Result<?> getUploadedChunks(@RequestParam("uploadId") String uploadId) {
        MultipartUploadSession session = uploadSessions.get(uploadId);
        if (session == null) {
            return Result.success(new ArrayList<>());
        }

        try {
            // 从MinIO查询已上传的分片编号（1-based）
            List<Integer> partNumbers = storageLargeService.listUploadedParts(
                    session.bucketName, session.objectName, uploadId);
            // 转换为0-based索引返回给前端
            List<Integer> indices = partNumbers.stream()
                    .map(n -> n - 1)
                    .sorted()
                    .collect(Collectors.toList());
            return Result.success(indices);
        } catch (Exception e) {
            return Result.success(new ArrayList<>());
        }
    }

    @ApiOperation("获取分片预签名上传URL（前端直传MinIO，绕过服务器）")
    @PostMapping(value = "/userfile/chunk/presigned")
    public Result<?> getPresignedUrls(@RequestParam("uploadId") String uploadId,
                                       @RequestParam("chunkTotal") Integer chunkTotal) {
        MultipartUploadSession session = uploadSessions.get(uploadId);
        if (session == null) {
            throw new BizException("上传会话不存在，请重新初始化");
        }

        try {
            List<Map<String, Object>> urls = new ArrayList<>();
            // 为每个分片生成预签名 URL，有效期 30 分钟
            for (int i = 0; i < chunkTotal; i++) {
                int partNumber = i + 1; // MinIO 分片编号从1开始
                String presignedUrl = storageLargeService.getPresignedUploadUrl(
                        session.bucketName, session.objectName, uploadId,
                        partNumber, 1800);
                Map<String, Object> item = new HashMap<>();
                item.put("chunkIndex", i);
                item.put("partNumber", partNumber);
                item.put("uploadUrl", presignedUrl);
                urls.add(item);
            }
            return Result.success(urls);
        } catch (Exception e) {
            throw new BizException("生成预签名URL失败: " + e.getMessage());
        }
    }

    @ApiOperation("合并分片并保存文件")
    @PostMapping(value = "/userfile/chunk/merge")
    public Result<?> mergeChunks(@RequestParam("uploadId") String uploadId,
                                  @RequestParam("filename") String filename,
                                  @RequestParam("chunkTotal") Integer chunkTotal) {
        UserInfo userInfo = UserInfoContext.get();

        MultipartUploadSession session = uploadSessions.get(uploadId);
        if (session == null) {
            throw new BizException("上传会话不存在，请重新初始化");
        }

        try {
            // 从 MinIO 获取实际已上传的分片列表（含 etag），按 partNumber 升序排列
            List<Part> parts = storageLargeService.listParts(
                    session.bucketName, session.objectName, uploadId);

            if (parts.size() < chunkTotal) {
                throw new BizException("分片未全部上传完成，已上传: " + parts.size() + "/" + chunkTotal);
            }

            // 调用 MinIO 原生合并 API，省去服务器磁盘 IO
            storageLargeService.completeMultipartUpload(
                    session.bucketName, session.objectName, uploadId, parts);

            // 构建 location: bucketName/objectName
            String location = session.bucketName + "/" + session.objectName;

            // 保存 LargeFile 记录
            LargeFile largefile = new LargeFile();
            largefile.setLocation(location);
            largefile.setFtype(session.type);
            largefile.setShopid(session.companyid);
            largefile.setOpttime(new Date());
            iLargeFileService.save(largefile);

            // 保存文件关联记录
            LargeFileUser fileUser = new LargeFileUser();
            fileUser.setId(largefile.getId());
            fileUser.setUserid(session.userid);
            fileUser.setShopid(session.companyid != null ? session.companyid : null);
            fileUser.setFileid(largefile.getId());
            fileUser.setName(session.originalFilename);
            fileUser.setCreatetime(new Date());
            iLargeFileUserService.save(fileUser);

            // 清理会话
            uploadSessions.remove(uploadId);

            return Result.success(fileUser);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("合并分片失败: " + e.getMessage());
        }
    }

    @ApiOperation("取消分片上传")
    @PostMapping(value = "/userfile/chunk/cancel")
    public Result<?> cancelChunkUpload(@RequestParam("uploadId") String uploadId) {
        MultipartUploadSession session = uploadSessions.remove(uploadId);
        if (session == null) {
            return Result.success("会话不存在或已清理");
        }

        try {
            storageLargeService.abortMultipartUpload(
                    session.bucketName, session.objectName, uploadId);
        } catch (Exception e) {
            // 即使 MinIO 取消失败，会话也已从内存中移除
        }
        return Result.success("已取消");
    }
}
