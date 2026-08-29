package com.wimoor.feishu.event.controller;

import com.alibaba.fastjson.JSONObject;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.sdk.servlet.ext.ServletAdapter;
import com.wimoor.feishu.event.handler.MessageReceiveHandler;
import com.wimoor.feishu.event.handler.P1LeaveApprovalHandler;
import com.wimoor.feishu.pojo.entity.Auth;
import com.wimoor.feishu.service.IAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
public class EventController {
  @Autowired
  private ServletAdapter servletAdapter;
  @Autowired
  private IAuthService iAuthService;
  @Autowired
  P1LeaveApprovalHandler p1LeaveApprovalHandler;
  @Autowired
  MessageReceiveHandler messageReceiveHandler;

  /**
   * 可重复读取请求体的RequestWrapper
   */
  private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
      super(request);
      // 读取并缓存请求体
      try (BufferedReader reader = request.getReader()) {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line);
        }
        this.cachedBody = sb.toString().getBytes(StandardCharsets.UTF_8);
      }
    }

    @Override
    public ServletInputStream getInputStream() {
      return new ServletInputStream() {
        private final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);

        @Override
        public int read() {
          return byteArrayInputStream.read();
        }

        @Override
        public boolean isFinished() {
          return byteArrayInputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(cachedBody), StandardCharsets.UTF_8));
    }

    public String getCachedBody() {
      return new String(cachedBody, StandardCharsets.UTF_8);
    }
  }

  @PostMapping("/webhook/event/{appid}")
  public void event(@PathVariable String appid, HttpServletRequest request, HttpServletResponse response)
      throws Throwable {
	  List<Auth> auths = iAuthService.lambdaQuery().eq(Auth::getAppId, "cli_" + appid).list();
	  if (auths == null || auths.isEmpty()) {
		  log.warn("未找到飞书应用配置: appid={}", appid);
		  response.setStatus(404);
		  return;
	  }
	  Auth auth = auths.get(0);

	  // 包装请求以缓存请求体
	  CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
	  String body = cachedRequest.getCachedBody();
	  log.info("收到飞书事件回调: appid={}, body={}", appid, body);

	  // 处理challenge验证请求
	  if (body.contains("challenge")) {
		  JSONObject jsonBody = JSONObject.parseObject(body);
		  String challenge = jsonBody.getString("challenge");
		  if (challenge != null) {
			  log.info("处理challenge验证: challenge={}", challenge);
			  response.setContentType("application/json");
			  response.setCharacterEncoding("UTF-8");
			  JSONObject result = new JSONObject();
			  result.put("challenge", challenge);
			  response.getWriter().write(result.toJSONString());
			  response.getWriter().flush();
			  return;
		  }
	  }

	  // 处理其他事件（使用缓存的请求）
	  String finalAppId = auth.getAppId();
	  messageReceiveHandler.setCurrentAppId(finalAppId);
	  EventDispatcher eventDispatcher = EventDispatcher.newBuilder(auth.getVerificationToken(), auth.getEncryptKey())
			  .onP2MessageReceiveV1(messageReceiveHandler)
			  .onCustomizedEvent("leave_approval",p1LeaveApprovalHandler)
			  .onCustomizedEvent("approval",p1LeaveApprovalHandler)
			  .onCustomizedEvent("approval_instance",p1LeaveApprovalHandler)
			  .onCustomizedEvent("approval_task",p1LeaveApprovalHandler)
			  .onCustomizedEvent("leave_approvalV2",p1LeaveApprovalHandler)
			  .onCustomizedEvent("leave_approval_revert",p1LeaveApprovalHandler)
			  .onCustomizedEvent("approval.instance.status_changed_v4",p1LeaveApprovalHandler)
			  .build();
	  p1LeaveApprovalHandler.setEventDispatcher(eventDispatcher);
	  servletAdapter.handleEvent(cachedRequest, response, eventDispatcher);
  }

}
