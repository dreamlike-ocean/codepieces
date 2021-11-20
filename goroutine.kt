typealias chan<E> = BlockingQueue<E>
typealias WaitGroup = CountDownLatch
val unsafe = run {
    val f = Unsafe::class.java.getDeclaredField("theUnsafe")
    f.isAccessible = true
    f.get(null) as Unsafe
}
val offset = unsafe.objectFieldOffset(Class.forName("java.lang.ThreadBuilders\$VirtualThreadBuilder").getDeclaredField("scheduler"))
fun main() {
    val executor = Executors.newFixedThreadPool(1)
    val wg = WaitGroup(3)
    val chan = make<String>(1)
    go {
        chan `←` "ad"
        wg.Done()
    }()
    go(fun(){
        Thread.sleep(2000)
        val e = chan.`→`()
        println("${Thread.currentThread()} receive ${e}")
        wg.Done()
    })()
    wg.Wait()
    println("main end")
    executor.shutdown()
}

fun customerScheduler(scheduler:Executor,runnable: Runnable):Thread{
    val builder = Thread.ofVirtual()
    unsafe.putObject(builder, offset,scheduler)
    return builder.start(runnable)
}
fun go(task:()->Unit) = { -> Thread.ofVirtual().start(task)}
fun go(scheduler:Executor?=null,task:()->Unit): () -> Thread ={if (scheduler == null) go(task)() else customerScheduler(scheduler,task)}
fun <T> make(bufferSize:Int=0):chan<T> = when(bufferSize){
    0,1 -> SynchronousQueue()
    Int.MAX_VALUE-> LinkedBlockingDeque()
    else -> ArrayBlockingQueue(bufferSize)
}
infix fun <E> chan<E>.`←`(e:E){
    this.put(e)
}
fun <E> chan<E>.`→`():E = this.take()
fun WaitGroup.Done()=this.countDown()
fun WaitGroup.Wait()=this.await()
