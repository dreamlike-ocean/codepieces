
val queue = ConcurrentLinkedQueue<String>()
fun main(){
    val vertx = Vertx.vertx()
    queue.addAll(listOf("1","2","3"))
    val coroutineDispatcher = vertx.dispatcher() as  CoroutineContext

    CoroutineScope(coroutineDispatcher).launch {

        withTimeout(1000){
            vertx.leakFun()
        }

    }.invokeOnCompletion {
        println("超时了！应该结束了 此时的元素数量是${queue.size}")
        vertx.setTimer(2000){
            println("再等两秒 此时的元素数量是${queue.size}")
        }
    }


}
suspend fun Vertx.leakFun(){
    var resource : String?=null
    try {
        resource = getResource().await()
        println("用资源做点事情.....")
    }finally {
        if (resource != null) {
            queue.offer(resource)
        }
    }
}
suspend fun Vertx.unLeakFun(){
    val job = currentCoroutineContext().job
    val resource = getResource()
            .onSuccess{ s -> job.invokeOnCompletion { queue.offer(s) }}.await()
    println("用资源做点事情.....")

}
// 10 connection pool
fun Vertx.getResource():Future<String>{
    val promise = Promise.promise<String>()
    setTimer(2000){
        promise.complete(queue.poll())
    }
    return promise.future()
}

