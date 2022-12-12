inline fun <reified T:Throwable ,V> Future<V>.recoverFromFailure(crossinline recover : (T) -> V) :Future<V> {
    val promise = Promise.promise<V>()
    onFailure{
        if (it is T){
            promise.complete(recover(it))
        }
    }
    onSuccess {
        promise.complete(it)
    }
    return promise.future()
}
