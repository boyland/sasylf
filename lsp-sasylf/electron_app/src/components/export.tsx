import React from "react";
import Form from "react-bootstrap/Form";
import InputGroup from "react-bootstrap/InputGroup";
import Button from "react-bootstrap/Button";
import Modal from "react-bootstrap/Modal";
import { createTheorem } from "./utils";

interface ExportProps {
	show: boolean;
	onHide: () => void;
	proofRef: React.RefObject<HTMLDivElement>;
}

export default function Export(props: ExportProps) {
	const handleExport: React.FormEventHandler<HTMLFormElement> = (event) => {
		event.preventDefault();
		const formData = new FormData(event.target as HTMLFormElement);

		const formJson = Object.fromEntries(formData.entries());
		const theoremName: string = formJson.theoremName as string;
		const theorem = createTheorem(theoremName, props.proofRef.current);
		if ("clipboard" in formJson) {
			(window as any).electronAPI.addToClipboard(theorem);
		}
		if ("file" in formJson) {
			(window as any).electronAPI.saveFile(theorem);
		}
		props.onHide();
	};

	return (
		<Modal show={props.show} onHide={props.onHide}>
			<Modal.Header closeButton>
				<Modal.Title>Export Derivation</Modal.Title>
			</Modal.Header>
			<Modal.Body>
				<Form method="post" onSubmit={handleExport}>
					<InputGroup>
						<InputGroup.Text>Theorem Name</InputGroup.Text>
						<Form.Control
							name="theoremName"
							type="text"
							placeholder="cut-admissible"
						/>
					</InputGroup>
					<Form.Check
						className="m-1"
						name="clipboard"
						label="Export to clipboard"
					/>
					<Form.Check
						className="m-1"
						name="file"
						label="Export to file"
						defaultChecked={true}
					/>
					<Button variant="success" type="submit">
						Export
					</Button>
				</Form>
			</Modal.Body>
		</Modal>
	);
}
