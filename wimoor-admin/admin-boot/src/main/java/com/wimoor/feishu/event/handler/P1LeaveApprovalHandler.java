package com.wimoor.feishu.event.handler;

import com.lark.oapi.core.request.EventReq;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.event.CustomEventHandler;
import com.lark.oapi.event.EventDispatcher;
import com.wimoor.feishu.pojo.entity.LeaveCalendar;
import com.wimoor.feishu.service.ILeaveCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class P1LeaveApprovalHandler extends CustomEventHandler {

	EventDispatcher eventDispatcher=null;
    @Autowired 
    ILeaveCalendarService iLeaveCalendarService;
    @Autowired
    MessageSendHandler messageSendHandler;
	 
	public void setEventDispatcher(EventDispatcher eventDispatcher) {
		// TODO Auto-generated constructor stub
		this.eventDispatcher=eventDispatcher;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void handle(EventReq event) throws Exception {
		// TODO Auto-generated method stub
		String cipherEventJsonStr = eventDispatcher.parseReq(event);
        // 解密请求，如果需要的话
        String plainEventJsonStr =  eventDispatcher.decryptEvent(cipherEventJsonStr);
        // 解析关键字段
        Map<String,Object> json = Jsons.DEFAULT.fromJson(plainEventJsonStr,Map.class);
        Map<String,Object> eventmap=json.get("event")!=null?(Map<String, Object>) json.get("event"):null;
        Map<String,Object> headermap=json.get("header")!=null?(Map<String, Object>) json.get("header"):null;
        
        // 获取appid和type，支持V1和V2版本
        String appid = null;
        String type = null;
        if(headermap!=null) {
        	// V2版本：从header中获取
        	appid = headermap.get("app_id")!=null?headermap.get("app_id").toString():null;
        	type = headermap.get("event_type")!=null?headermap.get("event_type").toString():null;
        }
        if(eventmap!=null && appid==null) {
        	// V1版本：从event中获取
        	appid = eventmap.get("app_id")!=null?eventmap.get("app_id").toString():null;
        	type = eventmap.get("type")!=null?eventmap.get("type").toString():null;
        }
        
        if(eventmap!=null) {
        	String  employee_id=eventmap.get("employee_id")!=null?eventmap.get("employee_id").toString():null;
			String instance_code=eventmap.get("instance_code")!=null?eventmap.get("instance_code").toString():null;
          	//String  tenantKey=eventmap.get("tenant_key")!=null?eventmap.get("tenant_key").toString():null;
          	//String  open_id=eventmap.get("open_id")!=null?eventmap.get("open_id").toString():null;
            // 验签逻辑
            if(appid!=null&&type!=null) {
            	if(type.equals("leave_approval") && employee_id!=null) {
					// 检查数据库中是否已有该instance_code对应的记录
					List<LeaveCalendar> existingRecords = iLeaveCalendarService.lambdaQuery()
							.eq(LeaveCalendar::getUuid, instance_code)
							.eq(LeaveCalendar::getAppid, appid)
							.list();
					
					if(existingRecords != null && !existingRecords.isEmpty()) {
						// 已有记录
						LeaveCalendar existingRecord = existingRecords.get(0);
						if(existingRecord.getIsdelete() != null && existingRecord.getIsdelete()) {
							// 如果是已删除状态，改为未删除
							existingRecord.setIsdelete(false);
							iLeaveCalendarService.updateById(existingRecord);
							System.out.println("恢复已删除的请假日程: instance_code=" + instance_code);
						} else {
							// 如果是未删除状态，不做任何处理
							System.out.println("请假日程已存在且未删除，跳过处理: instance_code=" + instance_code);
						}
						return;
					}
					
					// 没有记录，走创建逻辑
                	try {
                        // 发送消息通知
                		messageSendHandler.sendLeaveApprovalMessage(appid, employee_id);
                	
                		String leave_start_time=eventmap.get("leave_start_time").toString();
                		String leave_end_time=eventmap.get("leave_end_time").toString();
                		iLeaveCalendarService.addLeaveCalandar(appid,instance_code,employee_id,leave_start_time ,leave_end_time,plainEventJsonStr);
                	}catch(Exception e) {
                		e.printStackTrace(); 
                	}
                }
                if(type.equals("leave_approval_revert")) {
                	 
                	iLeaveCalendarService.deleteLeaveCalandar(appid,instance_code);
                }
				if(type.equals("approval_instance")) {
					String status=eventmap.get("status")!=null?eventmap.get("status").toString():null;
					// CANCELED: 审批中撤回, REVERTED: 已通过后撤销
					if("CANCELED".equals(status) || "REVERTED".equals(status)) {
						 
						iLeaveCalendarService.deleteLeaveCalandar(appid,instance_code);
					}
				}
                // 处理V2版本的撤回事件 (approval.instance.status_changed_v4)
                if(type.equals("approval.instance.status_changed_v4")) {
                	String status=eventmap.get("status")!=null?eventmap.get("status").toString():null;
                	// CANCELED: 审批中撤回, REVERTED: 已通过后撤销
                	if("CANCELED".equals(status) || "REVERTED".equals(status)) {
                		 
                		iLeaveCalendarService.deleteLeaveCalandar(appid,instance_code);
                	}
                }
            }
            
        }
        
	}

}
