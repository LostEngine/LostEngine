use std::sync::atomic::AtomicI32;

pub struct ClientProxy {
    id_counter: AtomicI32,
}

#[allow(unused)]
impl ClientProxy {
    pub fn new() -> Self {
        Self {
            id_counter: AtomicI32::new(0),
        }
    }

    pub fn next_id(&self) -> i32 {
        let id = self
            .id_counter
            .fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        id.into()
    }
}
