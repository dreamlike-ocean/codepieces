package com.example.starter;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.streams.WriteStream;
import io.vertx.httpproxy.Body;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyResponse;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Main {

  public static void main(String[] args) throws InterruptedException {
    Vertx vertx = Vertx.vertx();

    originServer(vertx, 4399);


    HttpClient proxyClient = vertx.createHttpClient();

    HttpProxy proxy = HttpProxy.reverseProxy(proxyClient);
    HttpServer proxyServer = vertx.createHttpServer();
    proxy.origin(4399, "localhost");
    proxy.addInterceptor(new ProxyInterceptor() {
      @Override
      public Future<Void> handleProxyResponse(ProxyContext context) {
        BufferingWriteStream stream = new BufferingWriteStream();
        ProxyResponse response = context.response();
        Body body = response.getBody();
        return body.stream().pipeTo(stream)
          .onSuccess(v -> {
            Buffer realServerResponseBody = stream.content();
            System.out.println("origin response :" + realServerResponseBody);
          })
          .flatMap(v ->  {
            response.setBody(Body.body(Buffer.buffer("mocked!")));
            return response.send();
          });
      }
    });

    proxyServer.requestHandler(proxy).listen(8080);



  }

  public static void originServer(Vertx vertx, int port) {
    HttpServer origin = vertx.createHttpServer();
    origin.requestHandler(request -> {
      request.response().end("origin");
    });
    origin.listen(port);
    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
  }

  public static class BufferingWriteStream implements WriteStream<Buffer> {

    private final Buffer content;

    public BufferingWriteStream() {
      this.content = Buffer.buffer();
    }

    public Buffer content() {
      return content;
    }

    @Override
    public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
      return this;
    }

    @Override
    public Future<Void> write(Buffer data) {
      content.appendBuffer(data);
      return Future.succeededFuture();
    }

    @Override
    public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
      content.appendBuffer(data);
      handler.handle(Future.succeededFuture());
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
      handler.handle(Future.succeededFuture());
    }

    @Override
    public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
      return this;
    }

    @Override
    public boolean writeQueueFull() {
      return false;
    }

    @Override
    public WriteStream<Buffer> drainHandler( Handler<Void> handler) {
      return this;
    }
  }


}
