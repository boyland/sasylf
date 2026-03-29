import React, { useCallback } from 'react';
import { ProofData } from '../types/ProofTypes';

interface FileUploaderProps {
    onLoad: (data: ProofData) => void;
    onError: (error: string) => void;
}

const FileUploader: React.FC<FileUploaderProps> = ({ onLoad, onError }) => {
    const handleFileRead = useCallback((file: File) => {
        const reader = new FileReader();

        reader.onload = (e) => {
            try {
                const content = e.target?.result as string;
                const data = JSON.parse(content) as ProofData;

                // Basic validation
                if (!data.theorem || !data.proofTree) {
                    throw new Error('Invalid proof file format');
                }

                onLoad(data);
            } catch (err) {
                onError(`Failed to parse file: ${err instanceof Error ? err.message : 'Unknown error'}`);
            }
        };

        reader.onerror = () => {
            onError('Failed to read file');
        };

        reader.readAsText(file);
    }, [onLoad, onError]);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            handleFileRead(file);
        }
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        const file = e.dataTransfer.files?.[0];
        if (file && file.name.endsWith('.json')) {
            handleFileRead(file);
        } else {
            onError('Please drop a JSON file');
        }
    };

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault();
    };

    return (
        <div
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            className="relative bg-white/5 backdrop-blur-sm rounded-xl border-2 border-dashed border-purple-500/50 p-12 text-center hover:border-purple-400 transition-colors"
        >
            <div className="space-y-4">
                <div className="text-6xl">📄</div>
                <div>
                    <h3 className="text-xl font-semibold text-white mb-2">
                        Upload Proof JSON
                    </h3>
                    <p className="text-purple-200 text-sm">
                        Drag and drop a proof JSON file here, or click to browse
                    </p>
                </div>
                <div>
                    <label className="inline-block px-6 py-3 bg-purple-600 hover:bg-purple-500 text-white rounded-lg cursor-pointer transition-colors">
                        Choose File
                        <input
                            type="file"
                            accept=".json"
                            onChange={handleFileChange}
                            className="hidden"
                        />
                    </label>
                </div>
                <p className="text-purple-300 text-xs">
                    Example: sum_proof.json, lambda_proof.json
                </p>
            </div>
        </div>
    );
};

export default FileUploader;
