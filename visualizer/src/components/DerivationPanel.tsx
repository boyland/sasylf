import React from 'react';
import { DerivationStep } from '../types/ProofTypes';

interface DerivationPanelProps {
    theorem: string;
    foralls: string[];
    exists: string;
    selectedStep: DerivationStep | null;
}

const DerivationPanel: React.FC<DerivationPanelProps> = ({
    theorem,
    foralls,
    exists,
    selectedStep
}) => {
    return (
        <div className="bg-white/5 backdrop-blur-sm rounded-xl border border-white/10 p-6 space-y-6">
            {/* Theorem Info */}
            <div>
                <h3 className="text-sm font-semibold text-purple-300 uppercase tracking-wide mb-3">
                    Theorem Information
                </h3>
                <div className="space-y-2">
                    <div>
                        <span className="text-xs text-purple-400">Name:</span>
                        <p className="text-white font-mono text-sm">{theorem}</p>
                    </div>

                    {foralls.length > 0 && (
                        <div>
                            <span className="text-xs text-purple-400">Premises:</span>
                            {foralls.map((forall, idx) => (
                                <p key={idx} className="text-white font-mono text-sm">
                                    {forall}
                                </p>
                            ))}
                        </div>
                    )}

                    <div>
                        <span className="text-xs text-purple-400">Conclusion:</span>
                        <p className="text-white font-mono text-sm">{exists}</p>
                    </div>
                </div>
            </div>

            <div className="border-t border-white/10"></div>

            {/* Selected Step Details */}
            {selectedStep ? (
                <div>
                    <h3 className="text-sm font-semibold text-purple-300 uppercase tracking-wide mb-3">
                        Selected Derivation
                    </h3>
                    <div className="space-y-3">
                        <div className="bg-purple-500/10 rounded-lg p-3">
                            <span className="text-xs text-purple-400">Step Name:</span>
                            <p className="text-white font-mono text-lg font-bold">
                                {selectedStep.name}
                            </p>
                        </div>

                        <div>
                            <span className="text-xs text-purple-400">Judgment:</span>
                            <p className="text-white font-mono text-sm mt-1">
                                {selectedStep.judgment}
                            </p>
                        </div>

                        <div className="grid grid-cols-2 gap-3">
                            <div className="bg-slate-800/50 rounded p-2">
                                <span className="text-xs text-purple-400">Depth:</span>
                                <p className="text-white font-bold text-lg">
                                    {selectedStep.depth}
                                </p>
                            </div>
                            <div className="bg-slate-800/50 rounded p-2">
                                <span className="text-xs text-purple-400">Status:</span>
                                <p className={`font-bold text-lg ${selectedStep.completed ? 'text-green-400' : 'text-yellow-400'
                                    }`}>
                                    {selectedStep.completed ? '✓' : '○'}
                                </p>
                            </div>
                        </div>

                        {selectedStep.children.length > 0 && (
                            <div>
                                <span className="text-xs text-purple-400">Sub-derivations:</span>
                                <div className="mt-2 space-y-1">
                                    {selectedStep.children.map((child, idx) => (
                                        <div
                                            key={idx}
                                            className="text-sm text-purple-200 font-mono bg-slate-800/30 rounded px-2 py-1"
                                        >
                                            {child.name}: {child.judgment}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {selectedStep.info && Object.keys(selectedStep.info).length > 0 && (
                            <div>
                                <span className="text-xs text-purple-400">Additional Info:</span>
                                <div className="mt-2 space-y-1">
                                    {Object.entries(selectedStep.info).map(([key, value]) => (
                                        <div key={key} className="text-sm">
                                            <span className="text-purple-300">{key}:</span>
                                            <span className="text-white ml-2">{value}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            ) : (
                <div className="text-center py-8">
                    <p className="text-purple-300 text-sm">
                        Click on a node in the tree to see details
                    </p>
                </div>
            )}
        </div>
    );
};

export default DerivationPanel;
