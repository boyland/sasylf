import React, { useState } from "react";
import { Menu, Submenu, Item, ItemParams } from "react-contexify";
import "react-contexify/dist/ReactContexify.css";
import Modal from "react-bootstrap/Modal";
import Form from "react-bootstrap/Form";
import Button from "react-bootstrap/Button";
import InputGroup from "react-bootstrap/InputGroup";
import { Direction } from "../types";

function ContextMenu(props: { MENU_ID: string; unicode: string[] }) {
	const [show, setShow] = useState(false);
	const [nodeId, setNodeId] = useState(0);

	const handleItemClick = (event: ItemParams) => {
		setNodeId(event.props.nodeId);
		setShow(true);
	};

	const handleReplace: React.FormEventHandler<HTMLFormElement> = (event) => {
		event.preventDefault();
		const formData = new FormData(event.target as HTMLFormElement);
		const formJson = Object.fromEntries(formData.entries());

		const nodeEvent = new CustomEvent("replace", {
			detail: { nodeId, data: formJson, dir: Direction.Both },
		});
		document.dispatchEvent(nodeEvent);

		setShow(false);
	};

	return (
		<>
			<Menu id={props.MENU_ID}>
				<Item onClick={handleItemClick}>Replace free variables</Item>
				{props.unicode.length != 0 ? (
					<Submenu label="Select unicode">
						<Item>{props.unicode.join(", ")}</Item>
					</Submenu>
				) : null}
			</Menu>

			<Modal show={show} onHide={() => setShow(false)}>
				<Modal.Header closeButton>
					<Modal.Title>Specify Replacement</Modal.Title>
				</Modal.Header>

				<Modal.Body>
					<Form onSubmit={handleReplace}>
						<InputGroup className="mb-3">
							<InputGroup.Text>Old</InputGroup.Text>
							<Form.Control name="oldvar" type="text" />
						</InputGroup>

						<InputGroup className="mb-3">
							<InputGroup.Text>New</InputGroup.Text>
							<Form.Control name="newvar" type="text" />
						</InputGroup>

						<Button variant="success" type="submit">
							Replace
						</Button>
					</Form>
				</Modal.Body>
			</Modal>
		</>
	);
}

export default ContextMenu;
