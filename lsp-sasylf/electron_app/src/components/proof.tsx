import React from "react";
import Form from "react-bootstrap/Form";

function InputArea() {
	return (
		<Form.Control
			type="text"
			placeholder="Item To Prove"
			className="m-2 text-input proof-input"
		/>
	);
}

export default function ProofArea() {
	return (
		<div className="d-flex proof-area">
			<InputArea />
		</div>
	);
}
