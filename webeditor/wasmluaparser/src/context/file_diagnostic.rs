use emmy_lsp_types::{Diagnostic, Uri};
use emmylua_code_analysis::{EmmyLuaAnalysis, FileId, Profile};
use std::{collections::HashMap, sync::Arc, time::Duration};
use tokio::sync::{Mutex, RwLock};
use tokio_util::sync::CancellationToken;

use super::{ClientProxy, StatusBar};

pub struct FileDiagnostic {
    analysis: Arc<RwLock<EmmyLuaAnalysis>>,
    client: Arc<ClientProxy>,
    status_bar: Arc<StatusBar>,
    diagnostic_tokens: Arc<Mutex<HashMap<FileId, CancellationToken>>>,
    workspace_diagnostic_token: Arc<Mutex<Option<CancellationToken>>>,
}

impl FileDiagnostic {
    pub fn new(
        analysis: Arc<RwLock<EmmyLuaAnalysis>>,
        status_bar: Arc<StatusBar>,
        client: Arc<ClientProxy>,
    ) -> Self {
        Self {
            analysis,
            client,
            diagnostic_tokens: Arc::new(Mutex::new(HashMap::new())),
            workspace_diagnostic_token: Arc::new(Mutex::new(None)),
            status_bar,
        }
    }

    pub async fn add_workspace_diagnostic_task(&self, interval: u64, silent: bool) {
        let mut token = self.workspace_diagnostic_token.lock().await;
        if let Some(token) = token.as_ref() {
            token.cancel();
        }

        let cancel_token = CancellationToken::new();
        token.replace(cancel_token.clone());
        drop(token);

        let analysis = self.analysis.clone();
        let client_proxy = self.client.clone();
        let status_bar = self.status_bar.clone();
        tokio::spawn(async move {
            tokio::select! {
                _ = tokio::time::sleep(Duration::from_millis(interval)) => {
                    push_workspace_diagnostic(analysis, client_proxy, status_bar, silent, cancel_token).await
                }
                _ = cancel_token.cancelled() => {
                    println!("cancel workspace diagnostic");
                }
            }
        });
    }

    #[allow(unused)]
    pub async fn cancel_all(&self) {
        let mut tokens = self.diagnostic_tokens.lock().await;
        for (_, token) in tokens.iter() {
            token.cancel();
        }
        tokens.clear();
    }

    pub async fn cancel_workspace_diagnostic(&self) {
        let mut token = self.workspace_diagnostic_token.lock().await;
        if let Some(token) = token.as_ref() {
            token.cancel();
        }
        token.take();
    }

    pub async fn pull_file_diagnostics(
        &self,
        uri: Uri,
        cancel_token: CancellationToken,
    ) -> Vec<Diagnostic> {
        let analysis = self.analysis.read().await;
        let Some(file_id) = analysis.get_file_id(&uri) else {
            return vec![];
        };

        let diagnostics = analysis.diagnose_file(file_id, cancel_token);
        diagnostics.unwrap_or_default()
    }

    pub async fn pull_workspace_diagnostics_slow(
        &self,
        cancel_token: CancellationToken,
    ) -> Vec<(Uri, Vec<Diagnostic>)> {
        let mut token = self.workspace_diagnostic_token.lock().await;
        if let Some(token) = token.as_ref() {
            token.cancel();
        }
        token.replace(cancel_token.clone());
        drop(token);

        let mut result = Vec::new();
        let analysis = self.analysis.read().await;
        let main_workspace_file_ids = analysis
            .compilation
            .get_db()
            .get_module_index()
            .get_main_workspace_file_ids();
        drop(analysis);

        for file_id in main_workspace_file_ids {
            if cancel_token.is_cancelled() {
                break;
            }
            let analysis = self.analysis.read().await;
            if let Some(uri) = analysis.get_uri(file_id) {
                let diagnostics = analysis.diagnose_file(file_id, cancel_token.clone());
                if let Some(diagnostics) = diagnostics {
                    result.push((uri, diagnostics));
                }
            }
        }

        result
    }

    pub async fn pull_workspace_diagnostics_fast(
        &self,
        cancel_token: CancellationToken,
    ) -> Vec<(Uri, Vec<Diagnostic>)> {
        let mut token = self.workspace_diagnostic_token.lock().await;
        if let Some(token) = token.as_ref() {
            token.cancel();
        }
        token.replace(cancel_token.clone());
        drop(token);

        let mut result = Vec::new();
        let analysis = self.analysis.read().await;
        let main_workspace_file_ids = analysis
            .compilation
            .get_db()
            .get_module_index()
            .get_main_workspace_file_ids();
        drop(analysis);

        let (tx, mut rx) = tokio::sync::mpsc::channel::<Option<(Vec<Diagnostic>, Uri)>>(100);
        let valid_file_count = main_workspace_file_ids.len();

        let analysis = self.analysis.clone();
        for file_id in main_workspace_file_ids {
            let analysis = analysis.clone();
            let token = cancel_token.clone();
            let tx = tx.clone();
            tokio::spawn(async move {
                let analysis = analysis.read().await;
                let diagnostics = analysis.diagnose_file(file_id, token);
                if let Some(diagnostics) = diagnostics {
                    let uri = analysis.get_uri(file_id).unwrap();
                    let _ = tx.send(Some((diagnostics, uri))).await;
                } else {
                    let _ = tx.send(None).await;
                }
            });
        }

        let mut count = 0;
        if valid_file_count != 0 {
            let text = format!("diagnose {} files", valid_file_count);
            let _p = Profile::new(text.as_str());
            while let Some(file_diagnostic_result) = rx.recv().await {
                if cancel_token.is_cancelled() {
                    break;
                }

                if let Some((diagnostics, uri)) = file_diagnostic_result {
                    result.push((uri, diagnostics));
                }

                count += 1;
                if count == valid_file_count {
                    break;
                }
            }
        }

        result
    }
}

async fn push_workspace_diagnostic(
    analysis: Arc<RwLock<EmmyLuaAnalysis>>,
    _client_proxy: Arc<ClientProxy>,
    _status_bar: Arc<StatusBar>,
    silent: bool,
    _cancel_token: CancellationToken,
) {
    let read_analysis = analysis.read().await;
    let main_workspace_file_ids = read_analysis
        .compilation
        .get_db()
        .get_module_index()
        .get_main_workspace_file_ids();
    drop(read_analysis);
    // diagnostic files
    let (tx, mut rx) = tokio::sync::mpsc::channel::<FileId>(100);
    let valid_file_count = main_workspace_file_ids.len();

    for file_id in main_workspace_file_ids {
        let tx = tx.clone();
        tokio::spawn(async move {
            let _ = tx.send(file_id).await;
        });
    }

    let mut count = 0;
    if valid_file_count != 0 {
        if silent {
            while rx.recv().await.is_some() {
                count += 1;
                if count == valid_file_count {
                    break;
                }
            }
        } else {
            let text = format!("diagnose {} files", valid_file_count);
            let _p = Profile::new(text.as_str());
            let mut last_percentage = 0;
            while rx.recv().await.is_some() {
                count += 1;
                let percentage_done = ((count as f32 / valid_file_count as f32) * 100.0) as u32;
                if last_percentage != percentage_done {
                    last_percentage = percentage_done;
                }
                if count == valid_file_count {
                    break;
                }
            }
        }
    }
}
