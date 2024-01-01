import Form from "react-bootstrap/Form";
import InputGroup from "react-bootstrap/InputGroup";
import Button from "react-bootstrap/Button";

function createTheorem(theoremName: string) {
	return theoremName;
}

export default function Export() {
	function handleExport(event) {
		event.preventDefault();
		const formData = new FormData(event.target);

		const formJson = Object.fromEntries(formData.entries());
		const theoremName: string = formJson.theoremName as string;
		const theorem = createTheorem(theoremName);
		if ("clipboard" in formJson) {
			(window as any).electronAPI.addToClipboard(theorem);
		}
		if ("file" in formJson) {
			(window as any).electronAPI.saveFile(theorem);
		}
		return;
	}

	return (
		<Form method="post" onSubmit={handleExport}>
			<InputGroup>
				<InputGroup.Text>Theorem Name</InputGroup.Text>
				<Form.Control
					name="theoremName"
					type="text"
					placeholder="cut-admissible"
				/>
			</InputGroup>
			<Form.Check name="clipboard" label="Export to clipboard" />
			<Form.Check name="file" label="Export to file" defaultChecked={true} />
			<Button variant="success" type="submit">
				Export
			</Button>
		</Form>
	);
}
