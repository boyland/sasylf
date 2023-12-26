import React, { useState, useEffect, useContext } from "react";
import Droppable from "./droppable";
import CloseButton from "react-bootstrap/CloseButton";
import { DroppedContext } from "./state";

let nodeCounter = 1;

interface nodeProps {
	conclusion: string;
	root: boolean;
}

function ProofNode(props: nodeProps) {
	const [dropped, remove] = useContext(DroppedContext);

	const [id, setId] = useState(0);
	const className = `d-flex flex-row proof-node m-2 ${
		props.root ? "root-node" : ""
	}`;

	useEffect(() => setId(nodeCounter++), []);

	return (
		<div className={className}>
			<div className="d-flex flex-column">
				<div className="node-line"></div>
				<span className="conclusion">{props.conclusion}</span>
			</div>
			<Droppable id={id} className="rule-drop">
				<div className="drop-area p-2">
					{id in dropped ? (
						<>
							{dropped[id]} <CloseButton onClick={() => remove(id)} />
						</>
					) : (
						"Put rule here"
					)}
				</div>
			</Droppable>
		</div>
	);
}

export default function ProofArea() {
	return (
		<div className="d-flex proof-area">
			<ProofNode conclusion="(s n1') + n2 = (s n3')" root />
		</div>
	);
}
