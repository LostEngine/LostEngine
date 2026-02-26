import fs from "fs";

const html = fs.readFileSync("dist/index.html", "utf-8");
const licenses = fs.readFileSync("dist/THIRD_PARTY_LICENSES.txt", "utf-8");

fs.writeFileSync("dist/index.html", `${html}<!--\n${licenses}\n-->`);
fs.unlinkSync("dist/THIRD_PARTY_LICENSES.txt");
