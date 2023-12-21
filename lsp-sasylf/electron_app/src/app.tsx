import React from "react";
import { useState, useEffect } from "react";
import { createRoot } from "react-dom/client";
import { ast } from "./types";
import { RuleLikes } from "./components/bank";
import { UploadButton } from "./components/upload";


export default function MyApp() {
	const [rules, setRules]: any = useState(null);

	const myHandler = (compUnit: ast) => {
		setRules(<RuleLikes compUnit={compUnit} />);
	};

	useEffect(() => {
		(window as any).electronAPI
			.getAST()
			.then((compUnit: ast) => myHandler(compUnit));
	}, []);

	return <>{rules}<UploadButton/></>;
}

const appContainer = document.createElement("div");
document.body.appendChild(appContainer);

const root = createRoot(appContainer);
root.render(<MyApp />);
