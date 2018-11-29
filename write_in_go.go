package main

import (
	"fmt"
	"github.com/patrickmn/go-cache"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"os"
	"time"
)

var c = cache.New(5*time.Minute, 10*time.Minute)

const (
	XForwardedFor = "X-Forwarded-For"
	XRealIP       = "X-Real-IP"
)

// RemoteIp 返回远程客户端的 IP，如 192.168.1.1
func RemoteIp(req *http.Request) string {
	remoteAddr := req.RemoteAddr
	if ip := req.Header.Get(XRealIP); ip != "" {
		remoteAddr = ip
	} else if ip = req.Header.Get(XForwardedFor); ip != "" {
		remoteAddr = ip
	} else {
		remoteAddr, _, _ = net.SplitHostPort(remoteAddr)
	}
	if remoteAddr == "::1" {
		remoteAddr = "127.0.0.1"
	}
	return remoteAddr
}
func sayHelloName(w http.ResponseWriter, r *http.Request) {
	start := time.Now()
	r.ParseForm() //解析参数，默认是不会解析的

	content, flag := c.Get(r.RequestURI)
	if flag {
		fmt.Fprint(w, content)
	} else {
		response, err := http.Get("http://119.29.188.224:8080" + r.RequestURI)
		if err != nil {
			fmt.Print(err)
		} else {
			defer response.Body.Close()
			contents, err := ioutil.ReadAll(response.Body)
			if err != nil {
				log.Print(err)
				os.Exit(1)
			}
			c.Set(r.RequestURI, string(contents), cache.DefaultExpiration)
			fmt.Fprintf(w, string(contents))
		}
	}

	fmt.Print(RemoteIp(r) + " 访问了 " + r.RequestURI + " 耗时：")
	fmt.Println(time.Now().Sub(start))
}

func main() {
	http.HandleFunc("/", sayHelloName)       //设置访问的路由
	err := http.ListenAndServe(":9090", nil) //设置监听的端口
	if err != nil {
		log.Fatal("ListenAndServe: ", err)
	}
}
