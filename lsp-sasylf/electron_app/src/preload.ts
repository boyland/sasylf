import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("electronAPI", {
	addAST: (callback) =>
		ipcRenderer.on("add-ast", (_event, value) => callback(value)),
});
