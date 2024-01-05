import React from "react";
import Form from "react-bootstrap/Form";
import InputGroup from "react-bootstrap/InputGroup";
import Button from "react-bootstrap/Button";
import Modal from "react-bootstrap/Modal";

type line = {
	conclusion: string;
	name: string;
	rule: string;
	premises: string[];
};

function theoremHelper(proofNode) {
	const containers = proofNode.getElementsByClassName("conclusion");
	if (containers.length === 0) return [];
	const container = containers[containers.length - 1];
	const conclusion = container.getElementsByTagName("span")[0].textContent;
	const name = container.getElementsByTagName("input")[0].value;
	const rules = proofNode.getElementsByClassName("rule");
	const rule = rules[rules.length - 1].textContent;

	let content: line[] = [];
	let premises: string[] = [];
	const premiseNodes = proofNode.getElementsByClassName("premises");
	for (const premise of premiseNodes) {
		const prev = theoremHelper(premise);
		if (prev.length > 0) {
			premises.push(prev[prev.length - 1].name);
			content = content.concat(prev);
		}
	}

	content.push({ conclusion, name, rule, premises });
	return content;
}

function createTheorem(theoremName: string, dom) {
	const root = dom.getElementsByClassName("root-node")[0];
	const nodes = theoremHelper(root);
	const conclusion = nodes[0].conclusion;
	let content = `theorem ${theoremName} : ${conclusion}.\n`;
	for (const node of nodes) {
		content += `${node.name}: ${node.conclusion} by rule ${node.rule.trim()}${
			node.premises.length === 0 ? "" : ` on ${node.premises.join(", ")}`
		}\n`;
	}
	return content;
}

interface ExportProps {
	show: boolean;
	onHide: () => void;
	proofRef: React.RefObject<HTMLDivElement>;
}

export default function Export(props: ExportProps) {
	function handleExport(event) {
		event.preventDefault();
		const formData = new FormData(event.target);

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
	}

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
