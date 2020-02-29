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

	realIp := RemoteIp(r)

	content, flag := c.Get(r.RequestURI)
	if flag {
		fmt.Fprint(w, content)
	} else {
		req, _ := http.NewRequest("GET", "http://127.0.0.1:8080"+r.RequestURI, nil)
		// 比如说设置个token
		req.Header.Set(XForwardedFor, realIp)

		client := http.DefaultClient
		response, err := client.Do(req)

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
	t := time.Now()
	format := t.Format("2006-01-02 15:04:05")
	fmt.Print(format + " " + realIp + " 访问了 " + r.RequestURI + " 耗时：")
	fmt.Println(time.Now().Sub(start))
}

func main() {
	http.HandleFunc("/", sayHelloName)     //设置访问的路由
	err := http.ListenAndServe(":80", nil) //设置监听的端口
	if err != nil {
		log.Fatal("ListenAndServe: ", err)
	}
}
