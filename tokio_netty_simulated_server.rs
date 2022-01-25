
use std::sync::{Mutex, Arc};
use std::time::{Duration, SystemTime};
use tokio::io::{self, AsyncWriteExt, AsyncReadExt};
use tokio::net::{TcpListener, TcpStream};
use tokio::time::sleep;
//tokio = { version = "1", features = ["full"] }
#[tokio::main]
async fn main(){
    let server = TcpListener::bind("0.0.0.0:2001").await.unwrap();
    loop {
        match server.accept().await {
            Ok((socket,address)) => {
                println!("{:?}",address);
                process_socket(socket);
            },
            Err(e) => {
                println!("error: {}",e.to_string());
                break;
            },
        }
    }
}
fn process_socket(socket:TcpStream){
    let ip = socket.peer_addr().unwrap().ip().to_string();
   let (mut rt, mut wd) = io::split(socket);
   let is_stop = Arc::new(Mutex::new(false));
   let wd_lock = is_stop.clone();
   //定时任务写
   tokio::spawn(async move{
       loop {
           sleep(Duration::from_secs(3)).await;
           if *wd_lock.lock().unwrap() {
               break;
           }
           let name = format!("I am dreamlike,time:{}, your ip is {}",SystemTime::now().duration_since(SystemTime::UNIX_EPOCH).unwrap().as_secs(),ip).into_bytes();
           wd.write_i32(name.len() as i32).await;
           wd.write(&name).await;
       }
       println!("end")
   });
   //读数据
   tokio::spawn(async move{
       let mut buf = Vec::new();
       loop{
           match rt.read(&mut buf).await {
               Ok(0)|Err(_) => {
                   let mut stop = is_stop.lock().unwrap();
                   *stop = true;
                   break;
               },
               Ok(n) => {
                   //drop 
                   buf.clear();
               }
           }
           
       }
   });
}
