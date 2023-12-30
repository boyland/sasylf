import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("electronAPI", {
	parse: (conclusion: string, rule: string) =>
		ipcRenderer.invoke("parse", conclusion, rule),
	addAST: (callback) =>
		ipcRenderer.on("add-ast", (_event, value) => callback(value)),
});
