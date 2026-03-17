use emmy_lsp_types::Uri;
use emmylua_code_analysis::EmmyLuaAnalysis;
use std::str::FromStr;
use stylua_lib::{format_code, BlockNewlineGaps, CallParenType, CollapseSimpleStatement, Config, IndentType, LineEndings, LuaVersion, QuoteStyle, SortRequiresConfig, SpaceAfterFunctionNames};
use stylua_lib::OutputVerification::Full;
use tokio_util::sync::CancellationToken;
use wasm_bindgen::prelude::*;


#[wasm_bindgen]
pub struct LuaAnalyzer {
    analysis: EmmyLuaAnalysis,
}

#[wasm_bindgen]
impl LuaAnalyzer {
    #[wasm_bindgen(constructor)]
    pub fn new() -> Self {
        Self {
            analysis: EmmyLuaAnalysis::new(),
        }
    }

    pub fn init() {
        console_error_panic_hook::set_once();
    }

    pub fn format_code(&self, code: &str) -> Result<String, JsValue> {
        format_code(code, Config {
            syntax: LuaVersion::Lua52,
            column_width: 120,
            line_endings: LineEndings::default(),
            indent_type: IndentType::default(),
            indent_width: 4,
            quote_style: QuoteStyle::default(),
            no_call_parentheses: false,
            call_parentheses: CallParenType::default(),
            collapse_simple_statement: CollapseSimpleStatement::default(),
            sort_requires: SortRequiresConfig::default(),
            space_after_function_names: SpaceAfterFunctionNames::default(),
            block_newline_gaps: BlockNewlineGaps::default(),
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
            .map_or(JsValue::NULL, |d| serde_wasm_bindgen::to_value(&d).unwrap()))
    }
}
