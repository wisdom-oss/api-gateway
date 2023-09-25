package main

import (
	"context"
	"fmt"
	"github.com/Kong/go-pdk/server"
	"github.com/redis/go-redis/v9"
	"os"
)

const version = "1.0.0"
const priority = 2000

var useRedis bool
var redisClient *redis.Client

// New instantiates a new and empty configuration object
func New() interface{} {
	return &Configuration{}
}

func init() {
	// check if the `REDIS_ENABLED` variable is set
	_, useRedis = os.LookupEnv("REDIS_ENABLED")
	// since theoretically, redis may be used if the environment variable is
	// present, try to set up the redis connection and client.
	// as soon as something goes wrong, the redis usage is disabled again
	var redisUri string
	redisUri, useRedis = os.LookupEnv("REDIS_URI")
	if useRedis {
		// since the redis uri is available, the connection may be established
		connectionOptions, err := redis.ParseURL(redisUri)
		if err != nil {
			fmt.Println(err.Error())
			useRedis = false
			goto redisInfoLog
		}
		redisClient = redis.NewClient(connectionOptions)
		err = redisClient.Ping(context.Background()).Err()
		if err != nil {
			fmt.Println(err.Error())
			useRedis = false
			goto redisInfoLog
		}
	}
redisInfoLog:
	if !useRedis {
		fmt.Println("redis disabled")
	}
}

func main() {
	err := server.StartServer(New, version, priority)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
