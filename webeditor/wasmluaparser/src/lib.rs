mod handlers;
mod context;
mod meta_text;
mod util;

use emmy_lsp_types::{CompletionResponse, CompletionTriggerKind, Uri};
use emmylua_code_analysis::{EmmyLuaAnalysis, Emmyrc, EmmyrcCompletion, LuaCompilation};
use emmylua_parser::LuaAstNode;
use handlers::completion::completion_builder::CompletionBuilder;
use handlers::completion::providers::add_completions;
use rowan::TokenAtOffset;
use serde_wasm_bindgen::to_value;
use std::str::FromStr;
use std::sync::Arc;
use stylua_lib::OutputVerification::Full;
use stylua_lib::{format_code, BlockNewlineGaps, CallParenType, CollapseSimpleStatement, Config, IndentType, LineEndings, LuaVersion, QuoteStyle, SortRequiresConfig, SpaceAfterFunctionNames};
use tokio_util::sync::CancellationToken;
use wasm_bindgen::prelude::*;


#[wasm_bindgen]
pub struct LuaAnalyzer {
    analysis: EmmyLuaAnalysis
}

#[wasm_bindgen]
impl LuaAnalyzer {
    #[wasm_bindgen(constructor)]
    pub fn new() -> Self {
        let emmyrc = Arc::new(Emmyrc {
            schema: None,
            completion: EmmyrcCompletion {
                enable: true,
                auto_require: true,
                auto_require_function: Default::default(),
                auto_require_naming_convention: Default::default(),
                auto_require_separator: Default::default(),
                call_snippet: true,
                postfix: Default::default(),
                base_function_includes_name: Default::default(),
            },
            diagnostics: Default::default(),
            signature: Default::default(),
            hint: Default::default(),
            runtime: Default::default(),
            workspace: Default::default(),
            resource: Default::default(),
            code_lens: Default::default(),
            strict: Default::default(),
            semantic_tokens: Default::default(),
            references: Default::default(),
            hover: Default::default(),
            document_color: Default::default(),
            code_action: Default::default(),
            inline_values: Default::default(),
            doc: Default::default(),
            format: Default::default(),
        });
        Self {
            analysis: EmmyLuaAnalysis {
                compilation: LuaCompilation::new(emmyrc.clone()),
                diagnostic: Default::default(),
                emmyrc
            }
        }
    }

    pub fn init(&self) {
        console_error_panic_hook::set_once();
    }

    #[allow(deprecated)]
    pub fn format_code(&self, code: &str, tab_size: usize, insert_spaces: bool) -> Result<String, JsValue> {
        format_code(code, Config {
            syntax: LuaVersion::Lua52,
            column_width: 120,
            line_endings: LineEndings::Unix,
            indent_type: if insert_spaces { IndentType::Spaces } else { IndentType::Tabs },
            indent_width: tab_size,
            quote_style: QuoteStyle::AutoPreferDouble,
            no_call_parentheses: false,
            call_parentheses: CallParenType::Always,
            collapse_simple_statement: CollapseSimpleStatement::Never,
            sort_requires: SortRequiresConfig::default(),
            space_after_function_names: SpaceAfterFunctionNames::Never,
            block_newline_gaps: BlockNewlineGaps::Never,
        }, None, Full).map_err(|e| JsValue::from_str(&format!("Stylua format error: {}", e)))
    }

    pub fn check_code(&mut self, content: &str) -> Result<JsValue, JsValue> {
        let uri = Uri::from_str("file://script.lua")
            .map_err(|e| JsValue::from_str(&format!("Invalid URI: {}", e)))?;

        let file_id = self.analysis.update_file_by_uri(&uri, Some(content.to_string()))
            .ok_or_else(|| JsValue::from_str("Failed to update file"))?;

        let cancel_token = CancellationToken::new();
        let diagnostics = self.analysis.diagnose_file(file_id, cancel_token);

        Ok(diagnostics
            .map_or(JsValue::NULL, |d| to_value(&d).unwrap()))
    }

    pub fn get_completion(&mut self, content: &str, line: usize, column: usize, trigger_kind: usize) -> Result<JsValue, JsValue> {
        let uri = Uri::from_str("file://script.lua")
            .map_err(|e| JsValue::from_str(&format!("Invalid URI: {}", e)))?;

        let file_id = self.analysis.update_file_by_uri(&uri, Some(content.to_string()))
            .ok_or_else(|| JsValue::from_str("Failed to update file"))?;

        let cancel_token = CancellationToken::new();
        let trigger_kind = match trigger_kind {
            1 => CompletionTriggerKind::INVOKED,
            2 => CompletionTriggerKind::TRIGGER_CHARACTER,
            3 => CompletionTriggerKind::TRIGGER_FOR_INCOMPLETE_COMPLETIONS,
            _ => CompletionTriggerKind::INVOKED
        };

        let semantic_model = self.analysis.compilation.get_semantic_model(file_id)
            .ok_or_else(|| JsValue::from_str("Failed to get semantic model"))?;
        if !semantic_model.get_emmyrc().completion.enable {
            return Ok(JsValue::NULL);
        }

        let root = semantic_model.get_root();
        let position_offset = {
            let document = semantic_model.get_document();
            document.get_offset(line, column).ok_or_else(|| JsValue::from_str("Invalid position"))?
        };

        if position_offset > root.syntax().text_range().end() {
            return Ok(JsValue::NULL);
        }

        let token = match root.syntax().token_at_offset(position_offset) {
            TokenAtOffset::Single(token) => token,
            TokenAtOffset::Between(left, _) => left,
            TokenAtOffset::None => {
                return Ok(JsValue::NULL);
            }
        };

        let mut builder = CompletionBuilder::new(
            token,
            semantic_model,
            cancel_token,
            trigger_kind,
            position_offset,
        );

        add_completions(&mut builder);
        let result = CompletionResponse::Array(builder.get_completion_items());

        to_value(&result)
            .map_err(|e| JsValue::from_str(&format!("Serialization error: {}", e)))
    }
}
