自上次使用Openresty+Lua+Nginx的来加速自己的网站，用上了比较时髦的技术，感觉算是让自己的网站响应速度达到极限了，直到看到了Netty，公司就是打算用Netty来替代Openresty这一套，所以，自己也学了好久，琢磨了好一趟才知道怎么用，现在用来写一套HTTP代理服务器吧，之后再测试一下性能。

之前相关的文章如下：  
[【网页加速】lua redis的二次升级](http://www.wenzhihuai.com/getblogdetail.html?blogid=645)  
[使用Openresty加快网页速度](https://www.cnblogs.com/w1570631036/p/8449373.html)


## Netty实现HTTP的流程
<div align="center">

![](https://upyuncdn.wenzhihuai.com/20180909110242297408748.png)

</div>


## HttpServer

## HttpServerHandler
```java
@Slf4j
public class HttpServerHandler extends ChannelInboundHandlerAdapter {

    private RedisUtil redisUtil = new RedisUtil();
    private ByteBufToBytes reader;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof HttpRequest) {
            DefaultHttpRequest request = (DefaultHttpRequest) msg;
            String uri = request.uri();
            if ("/favicon.ico".equals(uri)) {
                return;
            }
            if (HttpUtil.isContentLengthSet(request)) {
                reader = new ByteBufToBytes(
                        (int) HttpUtil.getContentLength(request));
            }
            log.info(new Date().toString());
            Jedis jedis = redisUtil.getJedis();
            String s = jedis.get(uri);
            if (s == null || s.length() == 0) {
                try {
                    URL url = new URL("http://119.29.188.224:8080" + uri);
                    log.info(url.toString());
                    URLConnection urlConnection = url.openConnection();
                    HttpURLConnection connection = (HttpURLConnection) urlConnection;
                    connection.setRequestMethod("GET");
                    //连接
                    connection.connect();
                    //得到响应码
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader
                                (connection.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder bs = new StringBuilder();
                        String l;
                        while ((l = bufferedReader.readLine()) != null) {
                            bs.append(l).append("\n");
                        }
                        s = bs.toString();
                    }
                    jedis.set(uri, s);
                    connection.disconnect();
                } catch (Exception e) {
                    log.error("", e);
                    return;
                }
            }
            jedis.close();
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HTTP_1_1, OK, Unpooled.wrappedBuffer(s != null ? s
                    .getBytes() : new byte[0]));
            response.headers().set(CONTENT_TYPE, "text/html");
            response.headers().set(CONTENT_LENGTH,
                    response.content().readableBytes());
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//            response.headers().set(TRANSFER_ENCODING, CHUNKED);
//            response.headers().set(VARY, ACCEPT_ENCODING);
            ctx.write(response);
            ctx.flush();
        } else {
            throw new Exception();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.close();
    }
}

```



## 性能测试
下面的是ab测试，在1GHz、2G内存的centos7机器下进行测试。

>使用Netty并发数为10，总共请求10000次，结果如下：
<div align="center">

![](https://upyuncdn.wenzhihuai.com/20180909040802962739609.png)

</div>

>使用Openresty、Lua、Nginx并发数为10，总共请求10000次，结果如下：
<div align="center">

![](https://upyuncdn.wenzhihuai.com/20180909110624271597355.png)

</div>



## 源码地址

[https://github.com/Zephery/myway](https://github.com/Zephery/myway)
