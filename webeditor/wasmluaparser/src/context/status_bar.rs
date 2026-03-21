use std::sync::Arc;

use super::ClientProxy;

pub struct StatusBar {
    client: Arc<ClientProxy>,
}

#[derive(Debug, Clone, Copy)]
pub enum ProgressTask {
    LoadWorkspace = 0,
    DiagnoseWorkspace = 1,
    #[allow(dead_code)]
    RefreshIndex = 2,
}

impl ProgressTask {
    pub fn as_i32(&self) -> i32 {
        *self as i32
    }

    pub fn get_task_name(&self) -> &'static str {
        match self {
            ProgressTask::LoadWorkspace => "Load workspace",
            ProgressTask::DiagnoseWorkspace => "Diagnose workspace",
            ProgressTask::RefreshIndex => "Refresh index",
        }
    }
}

impl StatusBar {
    pub fn new(client: Arc<ClientProxy>) -> Self {
        Self { client }
    }
}
