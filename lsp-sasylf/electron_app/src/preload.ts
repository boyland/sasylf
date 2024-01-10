import { contextBridge, ipcRenderer } from "electron";
import { ast } from "./types";

contextBridge.exposeInMainWorld("electronAPI", {
	parse: (conclusion: string, rule: string, file: string) =>
		ipcRenderer.invoke("parse", conclusion, rule, file),
	addAST: (
		callback: (value: { compUnit: ast; name: string; file: string }) => void,
	) =>
		ipcRenderer.on("add-ast", (_event, value) => {
			callback(value);
		}),
	addToClipboard: (content: string) =>
		ipcRenderer.invoke("add-to-clipboard", content),
	saveFile: (theorem: string) => ipcRenderer.invoke("save-file", theorem),
	showModal: (callback: () => void) =>
		ipcRenderer.on("show-modal", (_event) => callback()),
	topdownParse: (
		premises: { premises: string[] },
		rule: string,
		file: string,
	) => ipcRenderer.invoke("topdown-parse", premises, rule, file),
});
