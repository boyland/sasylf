import { useState } from 'react';
import { ProofData, DerivationStep } from './types/ProofTypes';
import FileUploader from './components/FileUploader';
import ProofTreeView from './components/ProofTreeView';
import DerivationPanel from './components/DerivationPanel';

function App() {
    const [proofData, setProofData] = useState<ProofData | null>(null);
    const [selectedStep, setSelectedStep] = useState<DerivationStep | null>(null);
    const [error, setError] = useState<string | null>(null);

    const handleFileLoad = (data: ProofData) => {
        setProofData(data);
        setError(null);
        setSelectedStep(null);
    };

    const handleError = (errorMessage: string) => {
        setError(errorMessage);
        setProofData(null);
    };

    const handleStepSelect = (step: DerivationStep) => {
        setSelectedStep(step);
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
            {/* Header */}
            <header className="bg-black/20 backdrop-blur-sm border-b border-white/10">
                <div className="container mx-auto px-6 py-4">
                    <h1 className="text-3xl font-bold text-white">
                        SASyLF Proof Visualizer
                    </h1>
                    <p className="text-purple-200 text-sm mt-1">
                        Interactive visualization of proof derivations
                    </p>
                </div>
            </header>

            {/* Main Content */}
            <main className="container mx-auto px-6 py-8">
                {!proofData ? (
                    <div className="max-w-2xl mx-auto">
                        <FileUploader onLoad={handleFileLoad} onError={handleError} />
                        {error && (
                            <div className="mt-4 p-4 bg-red-500/20 border border-red-500/50 rounded-lg">
                                <p className="text-red-200">{error}</p>
                            </div>
                        )}
                    </div>
                ) : (
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* Proof Tree Visualization */}
                        <div className="lg:col-span-2">
                            <div className="bg-white/5 backdrop-blur-sm rounded-xl border border-white/10 p-6">
                                <div className="mb-4">
                                    <h2 className="text-xl font-semibold text-white">
                                        Theorem: {proofData.theorem}
                                    </h2>
                                    <p className="text-purple-200 text-sm mt-1">
                                        {proofData.proofTree.totalSteps} steps, max depth {proofData.proofTree.maxDepth}
                                    </p>
                                </div>
                                <ProofTreeView
                                    proofTree={proofData.proofTree}
                                    onStepSelect={handleStepSelect}
                                    selectedStep={selectedStep}
                                />
                            </div>
                        </div>

                        {/* Details Panel */}
                        <div className="lg:col-span-1">
                            <DerivationPanel
                                theorem={proofData.theorem}
                                foralls={proofData.foralls}
                                exists={proofData.exists}
                                selectedStep={selectedStep}
                            />
                        </div>
                    </div>
                )}
            </main>

            {/* Footer */}
            <footer className="mt-12 py-6 text-center text-purple-300 text-sm">
                <p>Built for SASyLF - Educational Proof Assistant</p>
            </footer>
        </div>
    );
}

export default App;
