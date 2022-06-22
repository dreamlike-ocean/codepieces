
val queue = ConcurrentLinkedQueue<String>()
fun main() {
  queue.addAll(listOf("1","2","3"))
  val vertx = Vertx.vertx()
  val start = LocalDateTime.now()
  val job = CoroutineScope(vertx.dispatcher() as CoroutineContext).launch {
    async {
      val msg = doLongTask(vertx,2_000).onComplete {
        //这里是2
        println("oncomplete queue size ${queue.size}")
      }
        .await()
        //由于被取消了 所以15，16行没法调用，所以资源泄露了
      println("end async $msg")
      queue.offer(msg)
    }
    println("launch")
  }
  //timeout handler
  vertx.setTimer(1000){
    if (!job.isCompleted) job.cancel()
  }

  job.invokeOnCompletion {
    println(Duration.between(start,LocalDateTime.now()).seconds)
    //这里是3
    println("queue size ${queue.size}")
  }
  
}

fun doLongTask(vertx: Vertx, delay:Long):Future<String>{
  val promise = Promise.promise<String>()
  vertx.setTimer(delay){
    promise.complete(queue.poll())
  }
  return promise.future()
}
