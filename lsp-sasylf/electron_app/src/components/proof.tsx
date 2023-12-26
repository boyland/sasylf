import React, { useState, useEffect } from "react";
import Droppable from "./droppable";
import CloseButton from "react-bootstrap/CloseButton";

let nodeCounter = 1;

interface nodeProps {
	dropped: any;
	conclusion: string;
	remove: (id: number) => void;
}

function ProofNode(props: nodeProps) {
	const [id, setId] = useState(0);

	useEffect(() => setId(nodeCounter++), []);

	return (
		<div className="d-flex flex-row proof-input m-2">
			<div className="d-flex flex-column">
				<div className="node-line"></div>
				<span className="conclusion">{props.conclusion}</span>
			</div>
			<Droppable id={id} className="rule-drop">
				<div className="drop-area p-2">
					{id in props.dropped ? (
						<>
							{props.dropped[id]}{" "}
							<CloseButton onClick={() => props.remove(id)} />
						</>
					) : (
						"Put rule here"
					)}
				</div>
			</Droppable>
		</div>
	);
}

interface proofProps {
	dropped: any;
	remove: (id: number) => void;
}

export default function ProofArea(props: proofProps) {
	return (
		<div className="d-flex proof-area">
			<ProofNode
				dropped={props.dropped}
				conclusion="(s n1') + n2 = (s n3')"
				remove={props.remove}
			/>
		</div>
	);
}
