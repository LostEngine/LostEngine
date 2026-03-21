use std::sync::LazyLock;

use emmy_auto_require::AutoRequireCommand;
use serde_json::Value;

use crate::context::ServerContextSnapshot;

mod emmy_auto_require;

pub use emmy_auto_require::make_auto_require;

pub trait CommandSpec {
    const COMMAND: &str;

    async fn handle(context: ServerContextSnapshot, args: Vec<Value>) -> Option<()>;
}

static COMMANDS: LazyLock<Vec<String>> = LazyLock::new(|| {
    vec![
        AutoRequireCommand::COMMAND.to_string(),
    ]
});

pub fn get_commands_list() -> Vec<String> {
    COMMANDS.clone()
}

pub async fn dispatch_command(
    context: ServerContextSnapshot,
    command_name: &str,
    args: Vec<Value>,
) -> Option<()> {
    match command_name {
        AutoRequireCommand::COMMAND => AutoRequireCommand::handle(context, args).await,
        _ => Some(()),
    }
}
