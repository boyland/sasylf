import React from "react";
import Modal from "react-bootstrap/Modal";

export default function ErrorModal({
	show,
	text,
	toggleShow,
}: {
	show: boolean;
	text: string;
	toggleShow: () => void;
}) {
	return (
		<Modal show={show} onHide={toggleShow}>
			<Modal.Header closeButton>
				<Modal.Title>Invalid Operation</Modal.Title>
			</Modal.Header>
			<Modal.Body>{text}</Modal.Body>
		</Modal>
	);
}
