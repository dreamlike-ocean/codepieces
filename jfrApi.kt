fun main(){
    val vertx = Vertx.vertx()
    var router = Router.router(vertx)
    router.get("/startRecord")
        .handler{
            val mxBeans = ManagementFactory.getPlatformMXBeans(FlightRecorderMXBean::class.java)
            if (mxBeans.size == 0){
                it.end("不支持jfr")
                return@handler
            }
            val bean = mxBeans[0]
            val id = bean.newRecording()
            bean.setRecordingOptions(id, Map.of("destination", "test-$id.jfr", "dumpOnExit", "true"))
            bean.startRecording(id)
            it.end(it.toString())
        }

    router.get("/record/:id")
        .handler {
            val id = it.request().getParam("id")
            var fileName = "test-$id.jfr"
            var file = File(fileName)
            if (file.exists().not()) {
                it.end("文件不存在")
                return@handler
            }
            val mxBeans = ManagementFactory.getPlatformMXBeans(FlightRecorderMXBean::class.java)
            if (mxBeans.size == 0){
                it.end("不支持jfr")
                return@handler
            }
            mxBeans[0].stopRecording(id.toLong())
            it.response().sendFile(fileName)
        }


}
