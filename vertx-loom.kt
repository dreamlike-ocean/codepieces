private val cs = ContinuationScope("vertx")
fun co_launch(fn:()->Unit){
  val continuation = Continuation(cs, fn)
  continuation.run()
}
fun <T> Future<T>.getNow():T{
  val continuation = Continuation.getCurrentContinuation(cs) ?: throw RuntimeException("not in continuation")
  onComplete {
    continuation.run()
  }
  Continuation.yield(cs)
  if (!succeeded()){
    throw cause()
  }
  return result()
}
