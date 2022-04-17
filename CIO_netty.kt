import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.SocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun main() {
  val handler = ChannelReadHandler()
  val bootstrap = Bootstrap()

  val channelFuture = bootstrap.channel(NioSocketChannel::class.java)
    .handler(handler)
    .group(NioEventLoopGroup(1))
    .connect("localhost", 4399)
    .await()

  val channel = channelFuture.channel()
  channel.config().setAutoRead(false)
  val channelCoroutine = ChannelCoroutine(channel, handler)
  val array = ByteArray(1024)
  while (true){
    val length = channelCoroutine.read(array)
    if (length == -1) break
    println(String(array,0,length)+" ${Thread.currentThread()}")
  }
  println("end")
}


class ChannelCoroutine(private val channel: Channel,private val channelReadHandler: ChannelReadHandler){
  suspend fun read(array: ByteArray) = suspendCoroutine<Int> { continuation ->
    if (!channel.isActive) {
      continuation.resume(-1)
      return@suspendCoroutine
    }
    val queue = channelReadHandler.queue
    if (!queue.isEmpty()) {
      readFromQueue(queue, array, continuation)
      return@suspendCoroutine
    }
    channelReadHandler.readCompleteHandler = {
      readFromQueue(queue, array, continuation)
    }
    channel.config().isAutoRead = true
  }
  suspend fun write(array: ByteArray) = suspendCoroutine<Int> { continuation -> 
    channel.writeAndFlush(array).addListener { 
      if (it.isSuccess) continuation.resume(array.size)
      else continuation.resumeWithException(it.cause())
    }
  }

  private fun readFromQueue(
    queue: ConcurrentLinkedQueue<ByteBuf>,
    array: ByteArray,
    continuation: Continuation<Int>
  ) {
    val peek = queue.peek()
    val length = array.size.coerceAtMost(peek.readableBytes())
    peek.readBytes(array, 0, length)
    if (peek.readableBytes() == 0) {
      queue.poll()
    }
    if (queue.isEmpty()) {
      channel.config().isAutoRead = false
    }
    continuation.resume(length)
  }

}
class ChannelReadHandler: ChannelDuplexHandler(){
  val queue = ConcurrentLinkedQueue<ByteBuf>()
  var readCompleteHandler: (() -> Unit) = {  }
  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    queue.offer(msg as ByteBuf)
  }

  override fun channelReadComplete(ctx: ChannelHandlerContext?) {
    readCompleteHandler()
  }

  override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
    var bytes = msg as ByteArray
    var buffer = ctx.alloc().directBuffer(msg.size)
    buffer.writeBytes(bytes,0,bytes.size)
    super.write(ctx, buffer, promise)
  }
}

