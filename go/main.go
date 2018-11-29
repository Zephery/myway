package main

import (
	"fmt"
	"github.com/patrickmn/go-cache"
	"log"
	"net/http"
	"time"
)

func set() {

	c := cache.New(5*time.Minute, 10*time.Minute)

	c.Set("foo", "bar", cache.DefaultExpiration)

	c.Set("baz", 42, cache.NoExpiration)

	foo, found := c.Get("foo")
	if found {
		fmt.Println(foo)
	}
}

func get() {

}

func main() {
	mux := http.NewServeMux()

	rh := http.RedirectHandler("http://www.baidu.com", 307)
	mux.Handle("/foo", rh)

	log.Println("Listening...")
	http.ListenAndServe(":3000", mux)

}
