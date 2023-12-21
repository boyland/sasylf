import { contextBridge, ipcRenderer, OpenDialogOptions } from 'electron';

contextBridge.exposeInMainWorld('electronAPI', {
    getAST: () => ipcRenderer.invoke('getAST'),
    openDialog: (method: string, config: OpenDialogOptions) => ipcRenderer.invoke('dialog', method, config)
});
