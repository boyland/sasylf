import React, { useState, useEffect } from "react";
import { createRoot } from "react-dom/client";
import { ast } from "./types";
import Bank from "./components/bank";
import ProofArea from "./components/proof";

export default function MyApp() {
	const [rules, setRules]: any = useState(null);

	const myHandler = (compUnit: ast) => {
		setRules(<Bank compUnit={compUnit} />);
	};

	useEffect(() => {
		(window as any).electronAPI
			.getAST()
			.then((compUnit: ast) => myHandler(compUnit));
	}, []);

	return (
		<div className="d-flex flex-row">
			{rules}
			<ProofArea />
		</div>
	);
}

const appContainer = document.createElement("div");
document.body.appendChild(appContainer);

const root = createRoot(appContainer);
root.render(<MyApp />);
