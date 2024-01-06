import React, {
	useState,
	useEffect,
	useContext,
	MutableRefObject,
} from "react";
import Droppable from "./droppable";
import CloseButton from "react-bootstrap/CloseButton";
import { DroppedContext } from "./state";
import Form from "react-bootstrap/Form";
import Draggable from "./draggable";
import { input } from "../types";

let nodeCounter = 1;

function Premises(props: { args: string[] }) {
	return (
		<div className="d-flex flex-row premises">
			{props.args.slice(0, -1).map((arg, ind) => (
				<ProofNode className="premise" conclusion={arg} key={ind} />
			))}
		</div>
	);
}

interface nodeProps {
	conclusion: string;
	className?: string;
}

function ProofNode(props: nodeProps) {
	const [dropped, remove] = useContext(DroppedContext);
	const [id, setId] = useState(0);
	const [args, setArgs] = useState<string[] | null>(null);

	useEffect(() => {
		setId(nodeCounter++);
		nodeCounter++;
	}, []);
	useEffect(() => {
		if (id in dropped)
			(window as any).electronAPI
				.parse(props.conclusion, dropped[id])
				.then((res: string[]) => setArgs(res));
		else setArgs(null);
	}, [dropped]);

	return (
		<div className={"d-flex flex-row proof-node m-2"}>
			<div className="d-flex flex-column">
				{args ? (
					args.length > 1 ? (
						<Premises args={args} />
					) : null
				) : (
					<Droppable id={id + 1} className="d-flex stretch-container">
						<div className="drop-node-area p-2">Copy node here</div>
					</Droppable>
				)}
				<div className="node-line"></div>
				<div className="d-flex flex-row conclusion">
					<Form.Control
						size="sm"
						className="panning-excluded m-1"
						type="text"
						placeholder="Name"
						htmlSize={5}
					/>
					<Draggable id={id} data={{ ruleLike: false, text: props.conclusion }}>
						<span className="centered-text no-wrap panning-excluded">
							{props.conclusion}
						</span>
					</Draggable>
				</div>
			</div>
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
		</div>
	);
}

export default function ProofArea(props: {
	proofRef?: MutableRefObject<null>;
	inputs: input[];
}) {
	return props.hasOwnProperty("proofRef") ? (
		<div className="d-flex proof-area" ref={props.proofRef}>
			{props.inputs.map(({ conclusion, free }, ind) => (
				<ProofNode key={ind} conclusion={conclusion} />
			))}
		</div>
	) : (
		<div className="d-flex proof-area">
			{props.inputs.map(({ conclusion, free }, ind) => (
				<ProofNode key={ind} conclusion={conclusion} />
			))}
		</div>
	);
}
