import React from "react";
import Form from "react-bootstrap/Form";
import InputGroup from "react-bootstrap/InputGroup";
import Button from "react-bootstrap/Button";
import Modal from "react-bootstrap/Modal";
import { input } from "../types";

interface InputProps {
	show: boolean;
	onHide: () => void;
	inputs: input[];
	appendHandler: (inp: input) => void;
}

export default function Input(props: InputProps) {
	const handleExport: React.FormEventHandler<HTMLFormElement> = (event) => {
		event.preventDefault();
		const formData = new FormData(event.target as HTMLFormElement);

		const formJson = Object.fromEntries(formData.entries());
		const conclusion: string = formJson.conclusion as string;
		const free: boolean = formJson.hasOwnProperty("free");
		props.appendHandler({
			conclusion,
			free,
			id: Math.max(-1, ...props.inputs.map((element) => element.id)) + 1,
		});
		props.onHide();
	};

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
					<Form.Check
						type="switch"
						name="free"
						label="Allow free variables"
						className="m-1"
					/>
					<Button variant="success" type="submit">
						Create
					</Button>
				</Form>
			</Modal.Body>
		</Modal>
	);
}
