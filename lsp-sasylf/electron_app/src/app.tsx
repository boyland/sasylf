import React from "react";
import { createRoot } from "react-dom/client";

export default function MyApp() {
	return (
		<div className="d-flex flex-column">
			<MyButton />
			<MyButton />
		</div>
	);
}

function MyButton() {
	return (
		<button
			className="btn btn-primary m-1"
			style={{ textAlign: "left", width: "fit-content" }}
		>
			<code style={{ color: "inherit" }}>
				--------- sum-z
				<br />z + n = n
			</code>
		</button>
	);
}

const root = createRoot(document.body);
root.render(<MyApp />);
