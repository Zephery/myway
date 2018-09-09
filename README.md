自上次使用Openresty+Lua+Nginx的来加速自己的网站，用上了比较时髦的技术，感觉算是让自己的网站响应速度达到极限了，直到看到了Netty，公司就是打算用Netty来替代Openresty这一套，所以，自己也学了好久，琢磨了好一趟才知道怎么用，现在用来写一套HTTP代理服务器吧，之后再测试一下性能。

之前相关的文章如下：  
[【网页加速】lua redis的二次升级](http://www.wenzhihuai.com/getblogdetail.html?blogid=645)  
[使用Openresty加快网页速度](https://www.cnblogs.com/w1570631036/p/8449373.html)


## HTTPSERVER



## 




## 性能测试
下面的是ab测试，在1GHz、2G内存的centos7机器下进行测试。

>使用Netty并发数为10，总共请求10000次，结果如下：
<div align="center">

![](https://upyuncdn.wenzhihuai.com/20180909040802962739609.png)

</div>

>使用Openresty、Lua、Nginx并发数为10，总共请求10000次，结果如下：



