import React, {
	useState,
	useEffect,
	useContext,
	useRef,
	RefObject,
} from "react";
import Droppable from "./droppable";
import CloseButton from "react-bootstrap/CloseButton";
import { DroppedContext } from "./state";
import Form from "react-bootstrap/Form";
import Fade from "react-bootstrap/Fade";
import Draggable from "./draggable";
import { extractPremise } from "./utils";
import { line, input } from "../types";

let nodeCounter = 2;

function Premises(props: { args: string[]; tree: line | null }) {
	if (props.args.length <= 1) return null;

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
	deleteHandler?: () => void;
}

function ProofNode(props: nodeProps) {
	const { dropped, addRef, removeHandler, addHandler } =
		useContext(DroppedContext);
	const [id, setId] = useState(0);
	const [args, setArgs] = useState<string[] | null>(null);
	const [tree, setTree] = useState<line | null>(null);
	const [open, setOpen] = useState(true);
	let proofNodeRef = useRef(null);

	const safeSetTree = (tree: line | null) =>
		setTree(tree && tree.rule === "Put rule here" ? null : tree);

	useEffect(() => {
		setId(nodeCounter++);
		safeSetTree(props.tree);

		nodeCounter++;
		const localId = nodeCounter - 2;
		addRef(localId, proofNodeRef);
		console.log(localId, props.conclusion);

		if (props.tree && props.tree.rule !== "Put rule here")
			addHandler(localId, props.tree.rule);

		const listener = (event: Event) => {
			const detail = (event as CustomEvent).detail;

			if (localId + 1 === detail.overId) {
				console.log(detail.tree);
				safeSetTree(detail.tree);
				setTimeout(() => setOpen(true), 300);
			}
		};

		document.addEventListener("tree", listener);

		const openListener = (event: Event) => {
			const detail = (event as CustomEvent).detail;

			if (localId + 1 === detail.deleteId || localId === detail.deleteId)
				setOpen(false);
		};

		document.addEventListener("fade", openListener);

		return () => {
			document.removeEventListener("tree", listener);
			document.removeEventListener("fade", openListener);
		};
	}, []);
	useEffect(() => {
		if (id in dropped && !tree) {
			(window as any).electronAPI
				.parse(props.conclusion, dropped[id])
				.then((res: string[]) => setArgs(res));
		} else setArgs(null);
	}, [dropped]);
	useEffect(() => setOpen(true), [args]);
	useEffect(() => {
		if (tree) addHandler(id, tree.rule);
		setTimeout(() => setOpen(true), 300);
	}, [tree]);
	useEffect(() => {
		if (props.tree) addHandler(id, props.tree.rule);
	}, [props.tree]);

	return (
		<Fade in={open}>
			<div
				className={`d-flex flex-row proof-node ${
					props.className ? props.className : ""
				} ${props.root ? "root-node" : "m-2"}`}
				ref={proofNodeRef}
			>
				<div className="d-flex flex-column">
					{args ? (
						<Premises args={args} tree={null} />
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
										setOpen(false);
										setTimeout(() => {
											removeHandler(id);
											setTree(null);
										}, 300);
									}}
								/>
							</>
						) : (
							"Put rule here"
						)}
					</div>
				</Droppable>
			</div>
		</Fade>
	);
}

export default function ProofArea(props: {
	proofRef?: RefObject<HTMLDivElement>;
	inputs: input[];
	deleteHandler: (ind: number) => void;
}) {
	return props.hasOwnProperty("proofRef") ? (
		<div className="d-flex proof-area" ref={props.proofRef}>
			{props.inputs.map(({ conclusion, free, id }, ind) => (
				<ProofNode
					ind={ind}
					key={id}
					conclusion={conclusion}
					tree={null}
					deleteHandler={() => props.deleteHandler(ind)}
					root
				/>
			))}
		</div>
	) : (
		<div className="d-flex proof-area">
			{props.inputs.map(({ conclusion, free, id }, ind) => (
				<ProofNode
					ind={ind}
					key={id}
					conclusion={conclusion}
					tree={null}
					deleteHandler={() => props.deleteHandler(ind)}
					root
				/>
			))}
		</div>
	);
}
