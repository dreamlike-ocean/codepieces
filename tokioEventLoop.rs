use std::pin::Pin;
use std::sync::Arc;
use std::sync::atomic::AtomicU64;
use std::time::Duration;
use std::{future::Future, thread};

use tokio::runtime::Builder;
use tokio::sync::mpsc;
use tokio::task::LocalSet;
use tokio::time;

struct EventLoop {
    tasks: mpsc::UnboundedSender<Pin<Box<dyn Future<Output = ()> + 'static + Send>>>
}

struct EventLoopGroup {
    event_loops: Vec<Arc<EventLoop>>,
    join_hanles: Vec<thread::JoinHandle<()>>,
    count: AtomicU64,
    shutdown_signal: tokio::sync::broadcast::Sender<()>,
}

impl EventLoopGroup {
    fn new(count: usize) -> EventLoopGroup {
        let mut event_loops = Vec::with_capacity(count);
        let mut join_handles = Vec::with_capacity(count);
        let (send, _) = tokio::sync::broadcast::channel::<()>(1);
        for _ in 0..count {
            let rx = send.subscribe();
            let (eventloop, joion_handle) = EventLoop::new(rx);
            event_loops.push(Arc::new(eventloop));
            join_handles.push(joion_handle);
        }

        EventLoopGroup {
            event_loops,
            count: AtomicU64::new(0),
            shutdown_signal: send,
            join_hanles: join_handles,
        }
    }

    fn new_actor(&self, task: impl Future<Output = ()> + 'static + Send) {
        let number = self
            .count
            .fetch_add(1, std::sync::atomic::Ordering::Acquire);
        let len = self.event_loops.len();
        self.event_loops[number as usize % len].spawn(task);
    }

    fn shutdown(self) {
        self.shutdown_signal.send(()).unwrap();
        for ele in self.join_hanles {
            ele.join().unwrap();
        }
    }
}

impl EventLoop {
    fn new(mut r: tokio::sync::broadcast::Receiver<()>) -> (Self, thread::JoinHandle<()>) {
        let (sender, mut receiver) = mpsc::unbounded_channel();

        let rt = Builder::new_current_thread().enable_all().build().unwrap();

        let join_handle = thread::spawn(move || {
            let local = LocalSet::new();
            local.spawn_local(async move {
                loop {
                    tokio::select! {
                        Some(task) = receiver.recv() => {
                            task.await;
                        }
                        _ = r.recv() => {
                            break;
                        }
                    }
                }
                
            });

            rt.block_on(local)
        });

        (EventLoop { tasks: sender }, join_handle)
    }

    fn spawn<F>(&self, task: F)
    where
        F: Future<Output = ()> + 'static + Send,
    {
        let _ = self.tasks.send(Box::pin(task));
    }
}

fn main() {
    let group = EventLoopGroup::new(1);

    group.new_actor(async {
        loop {
            println!("Hello world");
            time::sleep(Duration::from_secs(1)).await;
        }
    });

   std::thread::sleep(Duration::from_secs(1));
    group.shutdown();
}
