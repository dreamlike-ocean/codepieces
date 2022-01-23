class V extends AbstractVerticle{
    @Override
    public void start() throws Exception {
        Router r = Router.router(vertx);
        r.post("/m")
                .handler(rc -> {
                    rc.request().setExpectMultipart(true);
                    rc.request().uploadHandler(file -> System.out.println(file.filename()));
                    rc.request().endHandler(v -> {
                        System.out.println(rc.request().formAttributes());
                    });
                });
        vertx.createHttpServer()
                .requestHandler(r)
                .listen(80);
    }
}
