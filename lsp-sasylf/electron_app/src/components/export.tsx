import React from "react";
import Form from "react-bootstrap/Form";
import InputGroup from "react-bootstrap/InputGroup";
import Button from "react-bootstrap/Button";
import Modal from "react-bootstrap/Modal";
import { createTheorems } from "./utils";
import { input } from "../types";

interface ExportProps {
	show: boolean;
	onHide: () => void;
	proofRef: React.RefObject<HTMLDivElement>;
	inputs: input[];
}

export default function Export(props: ExportProps) {
	const handleExport: React.FormEventHandler<HTMLFormElement> = (event) => {
		event.preventDefault();
		const formData = new FormData(event.target as HTMLFormElement);

		const formJson = Object.fromEntries(formData.entries());
		const theoremNames: string[] = new Array(props.inputs.length).fill("");
		const quantifiers: string[] = new Array(props.inputs.length).fill("");
		const wanted: Set<number> = new Set();
		for (const [key, value] of formData.entries()) {
			let index: number = 0;
			if (key.includes("index")) {
				index = parseInt(key.replace("-index", ""), 10);
				wanted.add(index);
			}
			if (key.includes("forall")) {
				index = parseInt(key.replace("-forall", ""), 10);
				quantifiers[index] = value as string;
			}
			if (key.includes("name")) {
				index = parseInt(key.replace("-name", ""), 10);
				theoremNames[index] = value as string;
			}
		}
		const theorem = createTheorems(
			theoremNames,
			quantifiers,
			wanted,
			props.proofRef.current,
		);
		if ("clipboard" in formJson) {
			(window as any).electronAPI.addToClipboard(theorem);
		}
		if ("file" in formJson) {
			(window as any).electronAPI.saveFile(theorem);
		}
		props.onHide();
	};

	const rootNodes = props.proofRef.current?.getElementsByClassName("root-node");
	const rootNodesArray = rootNodes ? Array.from(rootNodes) : [];

	return (
		<Modal show={props.show} onHide={props.onHide}>
			<Modal.Header closeButton>
				<Modal.Title>Export Derivations</Modal.Title>
			</Modal.Header>
			<Modal.Body>
				<Form method="post" onSubmit={handleExport}>
					{rootNodesArray.map((rootNode, index) => {
						const containers = rootNode.getElementsByClassName("conclusion");
						const container = containers[containers.length - 1];
						const conclusion =
							container.getElementsByTagName("span")[0].textContent;

						return (
							<InputGroup size="sm" key={index}>
								<InputGroup.Checkbox name={`${index.toString()}-index`} />
								<InputGroup.Text>{conclusion}</InputGroup.Text>
								{rootNode.classList.contains("free") ? (
									<Form.Control
										name={`${index.toString()}-forall`}
										type="text"
										placeholder="Quantifiers"
									/>
								) : (
									<></>
								)}
								<Form.Control
									name={`${index.toString()}-name`}
									type="text"
									placeholder="Theorem Name"
								/>
							</InputGroup>
						);
					})}
					<Form.Check
						type="switch"
						className="m-1"
						name="clipboard"
						label="Export to clipboard"
					/>
					<Form.Check
						type="switch"
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
