use emmy_lsp_types::{ClientCapabilities, ServerCapabilities};

pub(crate) mod completion;
pub(crate) mod command;
pub(crate) mod hover;

pub trait RegisterCapabilities {
    fn register_capabilities(
        server_capabilities: &mut ServerCapabilities,
        client_capabilities: &ClientCapabilities,
    );
}