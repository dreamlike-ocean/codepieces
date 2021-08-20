class CacheVerticle:AbstractVerticle(){
  class HandlerWrapper<T>(val keyAndHandler:Pair<String,(T?) -> Unit>)
  class AddWrapper<T>(val addPair:Pair<String,T>)
  companion object{
    private const val GET_CACHE_ADDRESS = "get_cache_address"
    private const val ADD_CACHE_ADDRESS = "add_cache_address"
    private const val REMOVE_CACHE_ADDRESS = "remove_cache_address"
    fun <T> addCache(pair: Pair<String,T>) = Vertx.currentContext().owner().eventBus().send(ADD_CACHE_ADDRESS,AddWrapper(pair))
    fun <T> getCache(key:String, handler: (T?) -> Unit){
      Vertx.currentContext().owner().eventBus().send(GET_CACHE_ADDRESS,HandlerWrapper(key to handler))
    }
    fun remove(key:String){
      Vertx.currentContext().owner().eventBus().send(REMOVE_CACHE_ADDRESS,key)
    }
  }
  private val cache = mutableMapOf<String,Any>()

  override fun start() {
   val eventbus = vertx.eventBus()
    eventbus.registerDefaultCodec(HandlerWrapper::class.java,UnModifiableObjectCodec(HandlerWrapper::class.java))
    eventbus.registerDefaultCodec(AddWrapper::class.java,UnModifiableObjectCodec(AddWrapper::class.java))

    eventbus.localConsumer<HandlerWrapper<Any>>(GET_CACHE_ADDRESS){
      val keyAndHandler = it.body().keyAndHandler
      keyAndHandler.second(cache[keyAndHandler.first])
    }

    eventbus.localConsumer<AddWrapper<Any>>(ADD_CACHE_ADDRESS){
      val add = it.body().addPair
      cache[add.first] = add.second
    }

    eventbus.localConsumer<String>(REMOVE_CACHE_ADDRESS){
      val key = it.body()
      cache.remove(key)
    }
  }
  class UnModifiableObjectCodec<T>(private val msgClass:Class<T>):MessageCodec<T,T>{
    override fun encodeToWire(buffer: Buffer?, s: T) {
      TODO("Not yet implemented")
    }
    override fun decodeFromWire(pos: Int, buffer: Buffer?): T {
      TODO("Not yet implemented")
    }
    override fun transform(s: T)=s

    override fun name()=msgClass.name

    override fun systemCodecID(): Byte {
      return -1
    }
  }
}

