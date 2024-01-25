package main

import (
	"github.com/Kong/go-pdk/server"
)

const version = "1.0.0"
const priority = 2000

// New instantiates a new and empty configuration object
func New() interface{} {
	return &Configuration{}
}

func main() {
	server.StartServer(New, version, priority)
}
