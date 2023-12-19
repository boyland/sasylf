import React from "react";
import { createRoot } from "react-dom/client";

export default function MyApp() {
	return (
		<div className="d-flex flex-column">
			<MyButton />
			<MyButton />
			<UploadButton />
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

function handleUpload() {
	const dialogConfig = {
        title: 'Select a file',
        buttonLabel: 'Upload',
        properties: ['openFile']
    };
    console.log("Upload clicked");
    electron.openDialog('showOpenDialog', dialogConfig).then(result => console.log(result.filePaths[0]));
}

function UploadButton() {
	return (
		<button
			className="btn btn-primary"
			onClick={handleUpload}
			style={{ textAlign: "right", width: "fit-content" }}
		>
			Upload
		</button>
	);
}

const root = createRoot(document.body);
root.render(<MyApp />);
