package com.rescue.util;

import org.springframework.stereotype.Component;

@Component
public class SmsUtil {

    // 空方法，不执行任何发送，不会报错
    // 以后你配置好腾讯云，再替换这里即可
    public void sendSms(String phone, String[] params) {
        try {
            System.out.println("【模拟短信发送】手机号：" + phone + "，内容：" + params[1]);
        } catch (Exception e) {
            // 不抛任何错
        }
    }
}