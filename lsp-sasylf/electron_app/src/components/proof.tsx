import React, {
	useState,
	useEffect,
	useContext,
	useRef,
	MutableRefObject,
} from "react";
import Droppable from "./droppable";
import CloseButton from "react-bootstrap/CloseButton";
import { DroppedContext } from "./state";
import Form from "react-bootstrap/Form";
import Draggable from "./draggable";
import { line, extractPremise } from "./utils";
import { input } from "../types";

let nodeCounter = 2;

function Premises(props: { args: string[]; tree: line | null }) {
	return (
		<div className="d-flex flex-row premises">
			{props.args.slice(0, -1).map((arg, ind) => (
				<ProofNode
					className="premise"
					conclusion={arg}
					key={ind}
					tree={props.tree ? extractPremise(arg, props.tree) : null}
					root={false}
				/>
			))}
		</div>
	);
}

interface nodeProps {
	conclusion: string;
	className?: string;
	tree: line | null;
	root?: boolean;
	ind?: number;
}

function ProofNode(props: nodeProps) {
	const { dropped, addRef, removeHandler, addHandler } =
		useContext(DroppedContext);
	const [id, setId] = useState(0);
	const [args, setArgs] = useState<string[] | null>(null);
	const [tree, setTree] = useState<line | null>(null);
	let proofNodeRef = useRef(null);

	const safeSetTree = (tree: line | null) =>
		setTree(tree && tree.rule === "Put rule here" ? null : tree);

	useEffect(() => {
		setId(nodeCounter++);
		safeSetTree(props.tree);

		nodeCounter++;
		const localId = nodeCounter - 2;
		addRef(localId, proofNodeRef);

		if (props.tree && props.tree.rule !== "Put rule here")
			addHandler(localId, props.tree.rule);

		const listener = (event: Event) => {
			const detail = (event as CustomEvent).detail;

			if (localId + 1 === detail.overId) safeSetTree(detail.tree);
		};

		document.addEventListener("tree", listener);

		return () => document.removeEventListener("tree", listener);
	}, []);
	useEffect(() => {
		if (id in dropped && !tree)
			(window as any).electronAPI
				.parse(props.conclusion, dropped[id])
				.then((res: string[]) => setArgs(res));
		else setArgs(null);
	}, [dropped]);
	useEffect(() => {
		if (tree) addHandler(id, tree.rule);
	}, [tree]);

	return (
		<div
			className={`d-flex flex-row proof-node ${
				props.className ? props.className : ""
			} ${props.root ? "root-node" : "m-2"}`}
			ref={proofNodeRef}
		>
			<div className="d-flex flex-column">
				{args ? (
					args.length > 1 ? (
						<Premises args={args} tree={null} />
					) : null
				) : tree ? (
					<Premises
						args={[...tree.premises.map((value) => value.conclusion), ""]}
						tree={tree}
					/>
				) : (
					<Droppable
						id={id + 1}
						data={{ ruleLike: false, text: props.conclusion }}
						className="d-flex stretch-container"
					>
						<div className="drop-node-area p-2">Copy node here</div>
					</Droppable>
				)}
				<div className="node-line"></div>
				<div className="d-flex flex-row conclusion">
					<Form.Control
						size="sm"
						className="name-input panning-excluded m-1"
						type="text"
						placeholder="Name"
					/>
					<Draggable
						id={id}
						data={{
							ruleLike: false,
							text: props.conclusion,
							ind: props.root ? props.ind : null,
						}}
					>
						<span className="centered-text no-wrap panning-excluded">
							{props.conclusion}
						</span>
					</Draggable>
				</div>
			</div>
			<Droppable
				id={id}
				data={{ ruleLike: true }}
				className="d-flex stretch-container"
			>
				<div className="drop-area rule p-2">
					{id in dropped ? (
						<>
							{dropped[id]}{" "}
							<CloseButton
								onClick={() => {
									removeHandler(id);
									setTree(null);
								}}
							/>
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
				<ProofNode
					ind={ind}
					key={ind}
					conclusion={conclusion}
					tree={null}
					root
				/>
			))}
		</div>
	) : (
		<div className="d-flex proof-area">
			{props.inputs.map(({ conclusion, free }, ind) => (
				<ProofNode
					ind={ind}
					key={ind}
					conclusion={conclusion}
					tree={null}
					root
				/>
			))}
		</div>
	);
}
