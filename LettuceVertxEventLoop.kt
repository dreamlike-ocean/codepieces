package org.example

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.coroutines
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.protocol.CommandType
import io.lettuce.core.resource.DefaultClientResources
import io.lettuce.core.resource.EventLoopGroupProvider
import io.lettuce.core.support.AsyncConnectionPoolSupport
import io.lettuce.core.support.BoundedPoolConfig
import io.netty.channel.DefaultEventLoopGroup
import io.netty.channel.EventLoop
import io.netty.channel.EventLoopGroup
import io.netty.util.concurrent.DefaultPromise
import io.netty.util.concurrent.EventExecutorGroup
import io.netty.util.concurrent.GlobalEventExecutor
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.impl.ContextInternal
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun main(): Unit = runBlocking {
    val redisURI = RedisURI.create("redis://localhost:6379")
    val vertx = Vertx.vertx()
    val resources = DefaultClientResources
        .builder()
        .eventLoopGroupProvider(ContextEventLoopGroupProvider(vertx))
        .build()
    val client = RedisClient.create(resources, redisURI)
    val pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
        {
            client.connectAsync(ByteArrayCodec(), redisURI)
                .thenCompose { c, t ->
                    c.async().get("111".toByteArray())
                        .thenCompose { s ->
                            println(String(s))
                            CompletableFuture.completedFuture(c)
                        }
                }
        },
        BoundedPoolConfig.builder().maxTotal(1)
            .build()
    )
    val connection = pool.acquire().await()
    val coroutinesCommands = connection.async()
    println(String(coroutinesCommands.get("111".toByteArray()).await()))
    println(String(coroutinesCommands.get("111".toByteArray()).await()))

    vertx.setTimer(1000) {
        coroutinesCommands.dispatch(CommandType.SET, StatusOutput(ByteArrayCodec()), CommandArgs(ByteArrayCodec()).)
        pool.release(connection)
    }
    val coroutinesCommands1 = pool.acquire().await().async()
    println(String(coroutinesCommands.get("111".toByteArray()).await()))
    println(String(coroutinesCommands.get("111".toByteArray()).await()))
}
class ContextEventLoopGroupProvider(val vertx: Vertx) : EventLoopGroupProvider {
    override fun <T : EventLoopGroup> allocate(type: Class<T>?) : T {
        return ContextEventLoopGroup(vertx) as T
    }

    override fun threadPoolSize() = 1

    override fun release(
        eventLoopGroup: EventExecutorGroup?,
        quietPeriod: Long,
        timeout: Long,
        unit: TimeUnit
    ): io.netty.util.concurrent.Future<Boolean> {
        val promise = DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE)
        promise.setSuccess(true)
        return promise
    }

    override fun shutdown(quietPeriod: Long, timeout: Long, timeUnit: TimeUnit?): io.netty.util.concurrent.Future<Boolean> {
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
