use std::{
    sync::atomic::AtomicU64,
    thread,
    time::{Duration, Instant},
};

use actix::{Actor, ActorContext, AsyncContext, StreamHandler};
use actix_web::{get, web::{self, Bytes}, App, Error, HttpRequest, HttpResponse, HttpServer, Responder};
use actix_web_actors::ws::{self, Message, ProtocolError};
use futures::{StreamExt, SinkExt};
use serde::Deserialize;

#[get("/")]
async fn hello(counter: web::Data<Counter>) -> impl Responder {
    counter
        .count
        .fetch_add(1, std::sync::atomic::Ordering::AcqRel);
    HttpResponse::Ok().body("actix")
}

#[get("/query")]
async fn pathEx(info: web::Query<RequestInfo>, _: HttpRequest) -> impl Responder {
    let empty = String::from("empty");
    let other = info.other.as_ref().unwrap_or(&empty);
    let handler = actix_web::rt::spawn(async {});
    format!(
        "user_id :{}, friend: {}, other:{}",
        info.user_id, info.friend, other
    )
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    let share_counter = Counter::new();
    let web_data = web::Data::new(share_counter);
    let runHandler = HttpServer::new(move || {
        println!(
            "new Thread start ！name :{}",
            thread::current().name().unwrap()
        );
        App::new()
            .app_data(web_data.clone())
            .service(ws_handler)
            .service(hello)
            .service(pathEx)
    })
    .workers(2)
    .bind(("127.0.0.1", 8080))?;
actix_web::rt::spawn(async {
    start_ws_client().await
});
   runHandler.run().await
}
struct Counter {
    count: AtomicU64,
}

#[derive(Deserialize)]
struct RequestInfo {
    user_id: i32,
    friend: String,
    other: Option<String>,
}

impl Counter {
    fn new() -> Counter {
        Counter {
            count: AtomicU64::new(0),
        }
    }
}

pub struct MyWebSocket {
    hb: Instant,
}
impl MyWebSocket {
    pub fn new() -> Self {
        MyWebSocket { hb: Instant::now() }
    }
    fn heartBeat(&self, ctx: &mut <MyWebSocket as Actor>::Context) {
        ctx.run_interval(Duration::from_secs(2), |current_actor, ctx| {
            ctx.text("123");
        });
    }
}
impl Actor for MyWebSocket {
    type Context = ws::WebsocketContext<Self>;

    fn started(&mut self, ctx: &mut Self::Context) {
        self.heartBeat(ctx)
    }
}

impl StreamHandler<Result<Message, ProtocolError>> for MyWebSocket {
    fn handle(&mut self, msg: Result<Message, ProtocolError>, ctx: &mut Self::Context) {
        println!("WS: {msg:?}");
        match msg {
            Ok(ws::Message::Ping(msg)) => {
                self.hb = Instant::now();
                ctx.pong(&msg);
            }
            Ok(ws::Message::Pong(_)) => {
                self.hb = Instant::now();
            }
            Ok(ws::Message::Text(text)) => ctx.text(text),
            Ok(ws::Message::Binary(bin)) => ctx.binary(bin),
            Ok(ws::Message::Close(reason)) => {
                ctx.close(reason);
                ctx.stop();
            }
            _ => ctx.stop(),
        }
    }
}
#[get("/ws")]
async fn ws_handler(req: HttpRequest, stream: web::Payload) -> Result<HttpResponse, Error> {
    ws::start(MyWebSocket::new(), &req, stream)
}


async fn start_ws_client() {
    println!("start ws");
    let (res, mut ws) = awc::Client::new()
    .ws("ws://localhost:8080/ws")
    .connect()
    .await.unwrap();
    loop {
        if let Some(msg) = ws.next().await {
            match msg {
                Ok(ws::Frame::Text(text)) => {
                    println!("recive from server: {}", String::from_utf8(text.as_ref().to_vec()).unwrap())
                },
                Ok(ws::Frame::Ping(_)) => {
                    // respond to ping probes
                    ws.send(ws::Message::Pong(Bytes::new())).await.unwrap();
                },
                Ok(ws::Frame::Close(_)) =>{
                    return 
                }
                _ => {
                    println!("do nothing")
                }
            }
        }
    }
}
