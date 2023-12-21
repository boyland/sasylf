import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('electronAPI', {
    getAST: () => ipcRenderer.invoke('getAST'), 
  openDialog: (method, config) => ipcRenderer.invoke('dialog', method, config)});
