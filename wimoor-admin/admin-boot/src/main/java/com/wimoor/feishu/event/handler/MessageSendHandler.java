package com.wimoor.feishu.event.handler;

import com.lark.oapi.Client;
import com.lark.oapi.core.response.RawResponse;
import com.lark.oapi.core.token.AccessTokenType;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.im.v1.enums.MsgTypeEnum;
import com.lark.oapi.service.im.v1.model.ext.MessageText;
import com.wimoor.feishu.config.FeiShuClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书消息发送处理器
 */
@Component
public class MessageSendHandler {

    @Autowired
    private FeiShuClientBuilder clientBuilder;

    /**
     * 发送请假审批通过消息通知
     * @param appid 飞书应用ID
     * @param employee_id 员工ID
     */
    public void sendLeaveApprovalMessage(String appid, String employee_id) {
        sendMessage(appid, employee_id, "审核已处理，并自动为您创建请假日程，感谢您的辛苦工作");
    }

    public void sendMessage(String appid, String employee_id, String content) {
        try {
            Client client = clientBuilder.getClient(appid);
            Map<String, Object> body = new HashMap<>();
            body.put("receive_id", employee_id);
            body.put("content", MessageText.newBuilder()
                    .atUser(employee_id, "深圳市科方达科技有限公司")
                    .text(content)
                    .build());
            body.put("msg_type", MsgTypeEnum.MSG_TYPE_TEXT.getValue());

            // 发起请求
            RawResponse resp = client.post(
                    "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=user_id"
                    , body
                    , AccessTokenType.Tenant);
            // 处理结果
            System.out.println(resp.getStatusCode());
            System.out.println(Jsons.DEFAULT.toJson(resp.getHeaders()));
            System.out.println(new String(resp.getBody()));
            System.out.println(resp.getRequestID());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
