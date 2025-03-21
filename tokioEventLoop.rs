#[warn(unused_imports, unused_variables)]
use std::pin::Pin;
use std::sync::Arc;
use std::task;
use std::time::Duration;
use std::{future::Future, thread};

use tokio::runtime::{Builder};
use tokio::sync::mpsc;
use tokio::task::LocalSet;
use tokio::time;

struct EventLoop {
    tasks: mpsc::UnboundedSender<Pin<Box<dyn Future<Output = ()> + 'static + Send>>>,
}

impl EventLoop {
    fn new() -> Self {
        let (sender, mut receiver) = mpsc::unbounded_channel();

        let rt = Builder::new_current_thread()
            .enable_all()
            .build()
            .unwrap();

        let thread = thread::spawn(move || {
            let local = LocalSet::new();
            local.spawn_local(async move {
                while let Some(task) = receiver.recv().await {
                    task.await;
                }
            });
            rt.block_on(local)
        });
        EventLoop { tasks: sender }
    }

    fn spawn<F>(&self, task: F)
    where
        F: Future<Output = ()> + 'static + Send,
    {
        let _ = self.tasks.send(Box::pin(task));
    }
}

fn main() {
    let event_loop = Arc::new( EventLoop::new());
    
    event_loop.clone().spawn(async {
        println!("Hello from EventLoop!");
        tokio::task::spawn_local(async {
            println!("Hello from tokio::task::spawn_local!");
        });
    });

    let other = event_loop.clone();
    thread::spawn(move || {
        other.clone().spawn(async {
            println!("Hello from thread!");
            tokio::task::spawn_local(async {
                time::sleep(Duration::from_secs(1)).await;
                println!("Hello from tokio::task::spawn_local! after sleep");
            });
        });
    });

    thread::sleep(Duration::from_secs(5));
}
