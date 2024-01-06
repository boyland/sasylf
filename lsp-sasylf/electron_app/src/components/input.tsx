import React from "react";
import Form from "react-bootstrap/Form";
import InputGroup from "react-bootstrap/InputGroup";
import Button from "react-bootstrap/Button";
import Modal from "react-bootstrap/Modal";
import Dropdown from "react-bootstrap/Dropdown";
import DropdownButton from "react-bootstrap/DropdownButton";
import { input } from "../types";

interface InputProps {
	show: boolean;
	onHide: () => void;
	inputs: input[];
}

export default function Input(props: InputProps) {
	function handleExport(event) {
		event.preventDefault();
		const formData = new FormData(event.target);

		const formJson = Object.fromEntries(formData.entries());
		const conclusion: string = formJson.conclusion as string;
		const free: boolean = formJson.hasOwnProperty("free");
		console.log(formJson);
		console.log(conclusion);
		console.log(free);
		props.inputs.push({ conclusion, free });
		props.onHide();
	}

	return (
		<Modal show={props.show} onHide={props.onHide}>
			<Modal.Header closeButton>
				<Modal.Title>Create New Theorem</Modal.Title>
			</Modal.Header>
			<Modal.Body>
				<Form method="post" onSubmit={handleExport}>
					<InputGroup>
						<InputGroup.Text>Conclusion</InputGroup.Text>
						<Form.Control
							name="conclusion"
							type="text"
							placeholder="(s (z)) + n = (s n)"
						/>
					</InputGroup>
					<Form.Check // prettier-ignore
						type="switch"
						name="free"
						label="Allow free variables"
					/>
					<Button variant="success" type="submit">
						Create
					</Button>
				</Form>
			</Modal.Body>
		</Modal>
	);
}
