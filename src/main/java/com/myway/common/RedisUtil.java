package com.myway.common;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author wenzhihuai
 * @since 2018/9/5 22:01
 */
public class RedisUtil {

    // 连接池
    private JedisPool jedisPool = null;

    // 构造函数
    public RedisUtil() {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            // 可用连接实例的最大数目，默认值为8；
            // 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)
            int maxTotal = 1024;
            config.setMaxTotal(maxTotal);
            // 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8
            int maxIdle = 200;
            config.setMaxIdle(maxIdle);
            // 等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException
            int maxWait = 10000;
            config.setMaxWaitMillis(maxWait);
            // 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的
            boolean testOnBorrow = true;
            config.setTestOnBorrow(testOnBorrow);
            // Redis 服务器 IP
            String address = "119.23.46.71";
            // Redis的端口号
            int port = 6340;
            // 访问密码
            String password = "root";
            // 连接 redis 等待时间
            int timeOut = 10000;
            jedisPool = new JedisPool(config, address, port, timeOut, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取 Jedis 实例
    public Jedis getJedis() {
        if (jedisPool != null) {
            return jedisPool.getResource();
        }
        return null;
    }
}
