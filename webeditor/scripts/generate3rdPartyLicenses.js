import fs from 'fs';
import path from 'path';
import {fileURLToPath} from 'url';
import {execSync} from 'child_process';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const output = execSync(
    `npx license-checker --production --json`,
    { encoding: 'utf8', cwd: path.resolve(__dirname, '..') }
);

const licensesData = JSON.parse(output);

const outputDir = path.resolve(__dirname, '../dist');
const outputFile = path.join(outputDir, 'THIRD_PARTY_LICENSES.md');

let wasmluaparserLicense;
try {
    wasmluaparserLicense = fs.readFileSync(path.resolve(__dirname, '../wasmluaparser/THIRD_PARTY_LICENSES.md'), 'utf8');
} catch (err) {
    console.error('Error reading wasmluaparser/THIRD_PARTY_LICENSES.md:', err);
    process.exit(1);
}

const texts = new Set();

wasmluaparserLicense.split("\n---\n").forEach(value => {
    if (!texts.has(value)) texts.add(value);
});

for (const [moduleName, info] of Object.entries(licensesData)) {
    // We skip it if it is a readme file, this happens for some dependencies
    // that have a license text that is also included somewhere else
    if (!info.licenseFile || info.licenseFile.toLowerCase().endsWith("readme.md")) continue;

    let licenseText;
    try {
        licenseText = fs.readFileSync(info.licenseFile, 'utf8').trim();
    } catch (err) {
        console.warn(`Warning: could not read license file for ${moduleName}:`, err.message);
        continue;
    }

    if (licenseText !== "" && !texts.has(licenseText)) texts.add(licenseText);
}

if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
}

const outputContent = Array.from(texts).filter(str => str.trim() !== "").join('\n---\n');

fs.writeFileSync(outputFile, outputContent, 'utf8');