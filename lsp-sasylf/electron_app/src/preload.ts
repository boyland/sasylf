import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("electronAPI", {
	getAST: () => ipcRenderer.invoke("getAST"),
	parse: (conclusion: string, rule: string) =>
		ipcRenderer.invoke("parse", conclusion, rule),
});
