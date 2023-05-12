package com.example.demo

import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import io.lettuce.core.resource.EventLoopGroupProvider
import io.lettuce.core.resource.NettyCustomizer
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.util.concurrent.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.Context
import io.vertx.core.DeploymentOptions
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.impl.ContextInternal
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.impl.VertxEventLoopGroup
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisAPI
import io.vertx.redis.client.RedisOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlConnectOptions
import jdk.jshell.spi.ExecutionControl.RunException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import reactor.core.publisher.toMono
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.function.Consumer


suspend fun main(args : Array<String>) {
    val vertx = Vertx.vertx()
    val port = if (args.isEmpty()) {
        8080
    }else {
        args[0].toIntOrNull()?:8080
    }
    val pool = Pool.pool(
        vertx,
        SqlConnectOptions().setDatabase("petclinic")
            .setHost("localhost")
            .setPassword("123456789")
            .setUser("root")
            .setPort(3306),
        PoolOptions().setMaxSize(10)
    )

    val config = DefaultClientResources.builder()
        .eventLoopGroupProvider(ContextEventLoopGroupProvider(vertx))
        .eventExecutorGroup(vertx.nettyEventLoopGroup())
        .build()
    val redis = RedisClient.create(config, "redis://localhost/0")

    vertx.deployVerticle(
        { HttpVerticle(port, pool,redis) },
        DeploymentOptions().setInstances(Runtime.getRuntime().availableProcessors())
    ).await()
    println("startup end")
}


class HttpVerticle(val port :Int = 8080,val pool: Pool,val redis: RedisClient) : CoroutineVerticle() {

    override suspend fun start() {
        val client = redis.connectAsync(StringCodec(), RedisURI.create("redis://localhost/0")).await().async()
        val vertxClient = RedisAPI.api(Redis.createClient(vertx, "redis://localhost/0"))
        val router = Router.router(vertx)
        router
            .get("/hello")
            .handler { it ->
                val context = vertx.orCreateContext
                CoroutineScope(context.dispatcher()).launch {
                    //    println("before ${System.identityHashCode(vertx.orCreateContext)}: ${Thread.currentThread()}")
                    client.get("123")
                        .whenComplete { t, u ->
                            println("${Thread.currentThread()}")
                        }
                        .await()
                    //  println("after ${System.identityHashCode(vertx.orCreateContext)}: ${Thread.currentThread()}")
                    delay(5)
                    val list = pool.query("select * from owners where id = 1")
                        .mapping { it.toJson() }
                        .execute()
                        .await().toList()
                    it.json(list)
                }
            }
        router
            .get("/empty")
            .handler {
                it.end("hello")
            }
        router
            .get("/lettuce")
            .handler {
                val context = vertx.orCreateContext
                CoroutineScope(context.dispatcher()).launch {
                    for (index in 1..10) {
                        client.get("123").whenComplete { t, u ->
                            println("${Thread.currentThread()}")
                        }.await()
                    }
                    it.end(Thread.currentThread().toString())
                }
            }

        router.get("/vertx-redis")
            .handler {
                val context = vertx.orCreateContext
                CoroutineScope(context.dispatcher()).launch {
                    for (index in 1..10) {
                        vertxClient.get("123").await().toString()
                    }
                }
                it.end(Thread.currentThread().toString())
            }

        val server = vertx.createHttpServer()
            .requestHandler(router)
            .listen(port).await()
        println("listen on $port")
    }
}

class ContextEventLoopGroupProvider(val vertx: Vertx) : EventLoopGroupProvider{
    override fun <T : EventLoopGroup?> allocate(type: Class<T>?) : T {
        return ContextEventLoopGroup(vertx) as T
    }

    override fun threadPoolSize() = 1

    override fun release(
        eventLoopGroup: EventExecutorGroup?,
        quietPeriod: Long,
        timeout: Long,
        unit: TimeUnit?
    ): Future<Boolean> {
        val promise = DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE)
        promise.setSuccess(true)
        return promise
    }

    override fun shutdown(quietPeriod: Long, timeout: Long, timeUnit: TimeUnit?): Future<Boolean> {
        val promise = DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE)
        promise.setSuccess(true)
        return promise
    }
}

class ContextEventLoopGroup(val vertx: Vertx) : DefaultEventLoopGroup() {
    override fun next(): EventLoop {
        val contextInternal = vertx.orCreateContext as ContextInternal
        return contextInternal.nettyEventLoop()
    }
}
