import React, { useState, useEffect, useContext } from "react";
import Droppable from "./droppable";
import CloseButton from "react-bootstrap/CloseButton";
import { DroppedContext } from "./state";
import Form from "react-bootstrap/Form";

let nodeCounter = 1;

function Premises(props: { args: string[] }) {
	return (
		<div className="d-flex flex-row premises">
			{props.args.slice(0, -1).map((arg, ind) => (
				<ProofNode
					className="premise"
					conclusion={arg}
					root={false}
					key={ind}
				/>
			))}
		</div>
	);
}

interface nodeProps {
	conclusion: string;
	root: boolean;
	className?: string;
}

function ProofNode(props: nodeProps) {
	const [dropped, remove] = useContext(DroppedContext);
	const [id, setId] = useState(0);
	const [duplicate, setDuplicate] = useState(false);
	const [args, setArgs] = useState<string[] | null>(null);

	useEffect(() => setId(nodeCounter++), []);
	useEffect(() => {
		if (id in dropped)
			(window as any).electronAPI
				.parse(props.conclusion, dropped[id])
				.then((res: string[]) => setArgs(res));
		else setArgs(null);

		setDuplicate(props.conclusion in Object.values(dropped));
	}, [dropped]);

	return (
		<div
			className={`d-flex flex-row proof-node m-3 ${
				props.root ? "root-node" : ""
			}`}
		>
			<div className="d-flex flex-column">
				{args ? <Premises args={args} /> : null}
				<div className="node-line"></div>
				<div className="d-flex flex-row conclusion">
					<Form.Control
						size="sm"
						className="panning-excluded m-1"
						type="text"
						placeholder="Name"
						htmlSize={5}
					/>
					<span className="centered-text no-wrap">{props.conclusion}</span>
				</div>
			</div>
			{duplicate ? (
				<div className="d-flex drop-container">
					<div className="drop-area rule p-2 m-2" style={{ opacity: 0.5 }}>
						Elsewhere
					</div>
				</div>
			) : (
				<Droppable id={id} className="d-flex stretch-container">
					<div className="drop-area rule p-2">
						{id in dropped ? (
							<>
								{dropped[id]} <CloseButton onClick={() => remove(id)} />
							</>
						) : (
							"Put rule here"
						)}
					</div>
				</Droppable>
			)}
		</div>
	);
}

export default function ProofArea({ proofRef }) {
	return (
		<div className="d-flex proof-area" ref={proofRef}>
			<ProofNode conclusion="(s (z)) + n = (s n)" root />
		</div>
	);
}
