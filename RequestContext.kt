package com.example.demo

import com.example.demo.RequestContext.Companion.initRequestContext
import io.vertx.core.*
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.codec.BodyCodec
import java.lang.RuntimeException
import java.util.UUID
import java.util.function.Function
import java.util.function.Supplier

class RequestContext{
    companion object{
        val context = ThreadLocal<HashMap<String,Any>>()
        inline fun <reified T> getValue(key :String) = context.get()?.get(key)?.run { if (this is T) this else null }
        fun Router.initRequestContext(){
            route()
                .order(-1)
                .handler{
                    context.set(HashMap())
                    it.next()
                }
        }
    }
}

inline fun <T> transmittable(crossinline fn:(T)->Unit):Handler<T>{
    val nowContext = RequestContext.context.get()
    return Handler {
        val old = RequestContext.context.get()
        try {
            RequestContext.context.set(nowContext)
            fn(it)
        }finally {
            RequestContext.context.set(old)
        }
    }
}


fun <T> ((T) -> Unit).enableTransmittable():Handler<T>{
    val nowContext = RequestContext.context.get()
    val handler = this
    return Handler {
        val old = RequestContext.context.get()
        try {
            RequestContext.context.set(nowContext)
            handler(it)
        }finally {
            RequestContext.context.set(old)
        }
    }
}

inline fun <T,R> transmittableFunction(crossinline fn:(T) -> R):Function<T,R>{
    val nowContext = RequestContext.context.get()
    return Function {
        val old = RequestContext.context.get()
        try {
            RequestContext.context.set(nowContext)
            fn(it)
        }finally {
            RequestContext.context.set(old)
        }
    }
}

fun <T,R> Function<T,R>.transmittable() :Function<T,R> {
    val nowContext = RequestContext.context.get()
    val function = this
    return Function {
        val old = RequestContext.context.get()
        try {
            RequestContext.context.set(nowContext)
            function.apply(it)
        }finally {
            RequestContext.context.set(old)
        }
    }
}

fun main(){
    val vertx = Vertx.vertx()
    val s  = Supplier <Verticle>{
        object:AbstractVerticle(){
            override fun start() {
                val client = WebClient.create(vertx)
                val router = Router.router(vertx)
                router.initRequestContext()
                router.get()
                    .handler {rc->
                        val map = RequestContext.context.get()
                        val requestId = UUID.randomUUID()
                        map["id"] = requestId
                        rc.vertx().executeBlocking<Unit>(transmittable { requestId.assertId();Thread.sleep(100);it.complete() })
                            .compose(transmittableFunction {requestId.assertId(); client.request() })
                            .onSuccess({ _:String -> requestId.assertId();rc.response().end(requestId.toString());Unit }.enableTransmittable())
                    }

                vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(4399)
                    .onSuccess {
                        println("listener")
                    }
            }
        }
    }

    vertx.deployVerticle(s, DeploymentOptions().setInstances(8))
    Thread.sleep(1000)
    val testClient = WebClient.create(vertx, WebClientOptions().setMaxPoolSize(20))
    CompositeFuture.all((1..100000)
        .map { testClient.getAbs("http://localhost:4399")
            .send() })
        .onComplete {
            vertx.close()
        }


}
fun UUID.assertId() {
    if (RequestContext.context.get().get("id") != this) throw RuntimeException("context混乱")
}
fun WebClient.request() = getAbs("https://www.infoq.cn/article/mLxce8grDPv30D6XkZTO")
    .`as`(BodyCodec.string())
    .send()
    .map { it.body() }


