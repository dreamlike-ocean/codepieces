public class HttpVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        WebClient client = WebClient.create(vertx);
        Router router = Router.router(vertx);
        router.get("/example")
                .handler(rc -> {
                    client.getAbs("http://localhost/t")
                            .as(BodyCodec.jsonObject())
                            .send()
                            .map(HttpResponse::body)
                            .onSuccess(jo -> {
                                //做转换
                                rc.end(jo.toString());
                            })
                            .onFailure(rc::fail);

                });
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(80);
    }
}
