package com.myway.service;

import com.myway.common.HttpUtil;
import com.myway.common.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * @author wenzhihuai
 * @since 2018/10/23 22:38
 */
@Slf4j
public class MyWebSite {
    private RedisUtil redisUtil = new RedisUtil();

    public String getPage(String uri) throws Exception {
        Jedis jedis = redisUtil.getJedis();
        String s = jedis.get(uri);
        if (s == null) {
            String url = "http://119.29.188.224:8080" + uri;
            String page = HttpUtil.get(url);
            jedis.set(uri, page);
        }
        jedis.close();
        return s;
    }

}
