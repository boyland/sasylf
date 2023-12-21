import Button from "react-bootstrap/Button";
import { OpenDialogReturnValue } from "electron";

function handleUpload() {
	const dialogConfig = {
		title: "Select a file",
		buttonLabel: "Upload",
		properties: ["openFile"],
	};

	(window as any).electronAPI
		.openDialog("showOpenDialog", dialogConfig)
		.then((result: OpenDialogReturnValue) => console.log(result.filePaths[0]));
}

export function UploadButton() {
	return (
		<Button variant="primary" onClick={handleUpload} className="upload-button">
			Upload
		</Button>
	);
}
