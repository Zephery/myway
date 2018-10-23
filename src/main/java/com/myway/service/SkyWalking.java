package com.myway.service;

import com.myway.common.HttpUtil;

/**
 * @author wenzhihuai
 * @since 2018/10/23 22:28
 */
public class SkyWalking {
    public String getPage(String uri) throws Exception {
        String url = "http://119.29.188.224:8083" + uri;
        return HttpUtil.get(url);
    }
}
