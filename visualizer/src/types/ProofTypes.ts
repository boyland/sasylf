// TypeScript interfaces for SASyLF proof data

export interface ProofData {
    theorem: string;
    kind: string;
    foralls: string[];
    exists: string;
    proofTree: ProofTree;
}

export interface ProofTree {
    theoremName: string;
    totalSteps: number;
    maxDepth: number;
    roots: DerivationStep[];
}

export interface DerivationStep {
    name: string;
    judgment: string;
    depth: number;
    completed: boolean;
    info?: Record<string, string>;
    children: DerivationStep[];
}

export interface TreeNode extends DerivationStep {
    x?: number;
    y?: number;
    id: string;
    parent?: TreeNode;
}

export interface VisualizerState {
    proofData: ProofData | null;
    selectedStep: DerivationStep | null;
    currentStepIndex: number;
    isPlaying: boolean;
    playbackSpeed: number;
}
