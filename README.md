自上次使用Openresty+Lua+Nginx的来加速自己的网站，用上了比较时髦的技术，感觉算是让自己的网站响应速度达到极限了，直到看到了Netty，公司就是打算用Netty来替代Openresty这一套，所以，自己也学了好久，琢磨了好一趟才知道怎么用，现在用来写一套HTTP代理服务器吧，之后再测试一下性能。

之前相关的文章如下：  
[【网页加速】lua redis的二次升级](http://www.wenzhihuai.com/getblogdetail.html?blogid=645)  
[使用Openresty加快网页速度](https://www.cnblogs.com/w1570631036/p/8449373.html)

## 一、Netty中的HTTP
参考自《Netty实战》
#### 一个完整的HttpRequest请求
FullHttpRequest:
<div align="center">![](http://image.wenzhihuai.com/images/20180917105954482007021.png)</div>

1. HTTP Request 第一部分是包含的头信息
2. HttpContent 里面包含的是数据，可以后续有多个 HttpContent 部分
3. LastHttpContent 标记是 HTTP request 的结束，同时可能包含头的尾部信息
4. 完整的 HTTP request

#### 一个完整的HttpResponse请求
FullHttpResponse:
<div align="center">![](https://upyuncdn.wenzhihuai.com/20180917110022386437025.png)</div>

1. HTTP response 第一部分是包含的头信息
2. HttpContent 里面包含的是数据，可以后续有多个 HttpContent 部分
3. LastHttpContent 标记是 HTTP response 的结束，同时可能包含头的尾部信息
4. 完整的 HTTP response



## 二、Netty实现HTTP代理服务器的流程
在实现Http代理服务器之前，我们先来查看一下Netty实现代理服务器的完整流程：
<div align="center">![](https://upyuncdn.wenzhihuai.com/20180909110242297408748.png)</div>

Netty的Http服务的流程是： 
1、Client向Server发送http请求，在通常的情况中，client一般指的是浏览器，也可以由自己用netty实现一个客户端。此时，客户端需要用到HttpRequestEncoder将http请求进行编码。
2、Server端对http请求进行解析，服务端中，需要用到HttpRequestDecoder来对请求进行解码，然后实现自己的业务需求。
3、Server端向client发送http响应，处理完业务需求后，将相应的内容，用HttpResponseEncoder进行编码，返回数据。
4、Client对http响应进行解析，用HttpResponseDecoder进行解码。 


而Netty实现Http代理服务器的过程跟上面的所说无意，只不过是在自己的业务层增加了回源到tomcat服务器这一过程。结合上自己之前实现过的用OpenResty+Nginx来做代理服务器这一套，此处的Netty实现的过程也与此类似。此处粘贴一下OpenResty+Nginx实现的流程图：    
<div align="center">![](https://upyuncdn.wenzhihuai.com/201862519474.png!/scale/80)</div>

而使用了Netty之后，便是将中间的OpenResty+Nginx换成了Netty，下面我们来看一下具体的实现过程。


## 三、主要代码如下：
#### HttpServer
```java
public class HttpServer {
    public void start(int port) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            // server端发送的是httpResponse，所以要使用HttpResponseEncoder进行编码
                            ch.pipeline().addLast(
                                    new HttpResponseEncoder());
                            // server端接收到的是httpRequest，所以要使用HttpRequestDecoder进行解码
                            ch.pipeline().addLast(
                                    new HttpRequestDecoder());
                            ch.pipeline().addLast(
                                    new HttpServerHandler());
                            //增加自定义实现的Handler
                            ch.pipeline().addLast(new HttpServerCodec());
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind(port).sync();

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer();
        server.start(8080);
    }
}
```

#### HttpServerHandler
```java
@Slf4j
public class HttpServerHandler extends ChannelInboundHandlerAdapter {

    private RedisUtil redisUtil = new RedisUtil();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof HttpRequest) {
            DefaultHttpRequest request = (DefaultHttpRequest) msg;
            String uri = request.uri();
            if ("/favicon.ico".equals(uri)) {
                return;
            }
            log.info(new Date().toString());
            Jedis jedis = redisUtil.getJedis();
            String s = jedis.get(uri);
            if (s == null || s.length() == 0) {
                //这里我们的处理是回源到tomcat服务器进行抓取，然后
                //将抓取的内容放回到redis里面
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
            ctx.write(response);
            ctx.flush();
        } else {
            //这里必须加抛出异常，要不然ab测试的时候一直卡住不动，暂未解决
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



## 四、性能测试
下面的是ab测试，在1GHz、2G内存的centos7机器（阿里云服务器）下进行测试，测试命令ab -c 100 -n 10000 localhost:8000/，并发数为100，总数为10000。

性能：
<div align="center">![](http://image.wenzhihuai.com/images/201809171028141608056843.png)</div>


整体响应时间的分布比（单位：ms）：
<div align="center">![](http://image.wenzhihuai.com/images/201809171028471308532170.png)</div>

看完之后，我自己也震惊了，Netty实现的不仅稳定、吞吐率还比OpenResty的高出一倍，OpenResty的居然还有那么多的失败次数，不知是不是我的代码的问题还是测试例子不规范，至今，我还是OpenResty的脑残粉。总体的来说，Netty实现的服务器性能还是比较强的，不仅能够快速地开发高性能的面向协议的服务器和客户端，还可以在Netty上轻松实现各种自定义的协议。



## 五、源码地址

[https://github.com/Zephery/myway](https://github.com/Zephery/myway)

参考：
1. 《Netty实战》  
2. [基于Netty4构建HTTP服务----浏览器访问和Netty客户端访问](https://blog.csdn.net/wangshuang1631/article/details/73251180/)