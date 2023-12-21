import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('electronAPI', {
    getAST: () => ipcRenderer.invoke('getAST')
});
