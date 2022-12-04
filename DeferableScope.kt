suspend fun main(args: Array<String>) {
    DeferableScope {
        defer { println("1") }
        println("2")
        defer { println("3") }
    }
}
suspend inline fun <T> DeferableScope(callback :DeferrableCallbackScope.()-> T):T{
    val callbackScope = DeferrableCallbackScope()
     try {
        val res = callback(callbackScope)
        callbackScope.run()
        return res
    } catch (e: Exception) {
        callbackScope.run()
        throw e
    }
}

class DeferrableCallbackScope {
    private val defers = ArrayDeque<(suspend ()-> Unit)>()
    private val end = AtomicBoolean(false)

    public fun defer(f :suspend ()->Unit){
        defers.push(f)
    }

    //todo 懒得写异常处理了
    public suspend fun run(){
        if (end.compareAndSet(false,true)){
            while (!defers.isEmpty()) {
                defers.pop()()
            }
        }
    }

}
