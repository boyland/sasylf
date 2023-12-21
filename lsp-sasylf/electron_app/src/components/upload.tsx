import Button from "react-bootstrap/Button";

function handleUpload() {
	const dialogConfig = {
        title: 'Select a file',
        buttonLabel: 'Upload',
        properties: ['openFile']
    };
    console.log("Upload clicked");
    electronAPI.openDialog('showOpenDialog', dialogConfig).then(result => console.log(result.filePaths[0]));
}

export function UploadButton() {
	return (
		<Button
			variant="primary"
			onClick={handleUpload}
			className="upload-button"
		>
			Upload
		</Button>
	);
}
