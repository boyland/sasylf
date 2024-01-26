import React, {
	useState,
	useEffect,
	useContext,
	useRef,
	RefObject,
} from "react";
import Droppable from "./droppable";
import CloseButton from "react-bootstrap/CloseButton";
import { DroppedContext, FileContext } from "./state";
import Form from "react-bootstrap/Form";
import Fade from "react-bootstrap/Fade";
import Draggable from "./draggable";
import { deleteElement, extractPremise } from "./utils";
import { line, input, ast } from "../types";
import ErrorModal from "./error";

let nodeCounter = 2;

interface TopdownHandler {
	fn: (id: number) => void;
	level: boolean;
	ind?: number;
}

function Premises(props: {
	args: string[];
	tree: line | null;
	topdownHandler: TopdownHandler | null;
}) {
	if (props.args.length <= 1) return null;

	return (
		<div className="d-flex flex-row premises">
			{props.args.slice(0, -1).map((arg, ind) => (
				<ProofNode
					className="premise"
					conclusion={arg}
					key={ind}
					tree={props.tree ? extractPremise(arg, props.tree) : null}
					{...(props.topdownHandler
						? {
								topdownHandler: {
									fn: props.topdownHandler.fn,
									level: true,
									ind,
								},
							}
						: {})}
				/>
			))}
		</div>
	);
}

const getNumPremises = (ast: ast | null, rule: string) => {
	if (!ast) return 0;

	const thm = ast?.theorems.find((elem) => elem.name === rule);
	if (thm) return thm.foralls.length;

	for (const judgment of ast.judgments) {
		const rle = judgment.rules.find((elem) => elem.name === rule);
		if (rle) return rle.premises.length;
	}

	return 0;
};

function TopDownNode({ prems }: { prems: string[] }) {
	const { ast } = useContext(DroppedContext);
	const file = useContext(FileContext);

	const [rule, setRule] = useState<string | null>(null);
	const [id, setId] = useState(0);
	const [trees, setTrees] = useState<line[]>([]);
	const [premises, setPremises] = useState<string[]>([]);
	const [numUsed, setNumUsed] = useState(0);
	const [conclusion, setConclusion] = useState<string | null>(null);
	const [tree, setTree] = useState<line | null>(null);
	const [showModal, setShowModal] = useState(false);

	useEffect(() => {
		setId(nodeCounter++);
		nodeCounter++;
		setPremises(prems);
		setNumUsed(prems.length);
		setTrees(
			prems.map((value) => {
				return {
					conclusion: value,
					name: "",
					rule: "Put rule here",
					premises: [],
				};
			}),
		);
	}, []);
	useEffect(() => {
		const listener = (event: Event) => {
			const detail = (event as CustomEvent).detail;

			if (detail.overId == id + 1) {
				if (getNumPremises(ast, detail.text) < numUsed) setShowModal(true);
				else setRule(detail.text);
			}
		};

		document.addEventListener("topdown-rule", listener);

		return () => document.removeEventListener("topdown-rule", listener);
	}, [id, numUsed]);
	useEffect(() => {
		const listener = (event: Event) => {
			const detail = (event as CustomEvent).detail;

			if (detail.overId == id) {
				setTrees([...trees, detail.tree]);
				setPremises([...premises, detail.text]);
				setNumUsed(numUsed + 1);
			}
		};

		document.addEventListener("topdown-tree", listener);

		return () => document.removeEventListener("topdown-tree", listener);
	}, [id, trees, premises]);
	useEffect(() => {
		if (!rule) return;

		if (numUsed != getNumPremises(ast, rule)) {
			setConclusion(null);
			return;
		}

		(window as any).electronAPI
			.topdownParse({ premises }, rule, file)
			.then((res: string) => setConclusion(res));
	}, [numUsed, rule]);
	useEffect(() => {
		if (!conclusion || !rule) return;
		if (trees.filter((elem) => elem == null).length) return;

		setTree({ conclusion, name: "", rule, premises: trees });
	}, [conclusion]);

	const deletePremise = (ind: number) => {
		setTrees(deleteElement(trees, ind));
		setPremises(deleteElement(premises, ind));
		setNumUsed(numUsed - 1);
	};

	return (
		<>
			<ErrorModal
				show={showModal}
				text="Wrong number of premises for specified rule"
				toggleShow={() => setShowModal(!showModal)}
			/>
			{conclusion ? (
				<ProofNode
					conclusion={conclusion}
					tree={tree}
					topdownHandler={{
						fn: (ind: number) => deletePremise(ind),
						level: false,
					}}
					root
				/>
			) : (
				<div className="d-flex flex-row topdown-node m-2">
					<div className="d-flex flex-column">
						<div className="d-flex flex-row">
							{premises.map((premise, ind) => (
								<div
									className="d-flex stretch-container fill-width p-2"
									key={ind}
								>
									<div className="drop-node-area p-2">
										{premise} <CloseButton onClick={() => deletePremise(ind)} />
									</div>
								</div>
							))}
							<Droppable
								id={id}
								data={{ type: "topdown" }}
								className="d-flex stretch-container fill-width"
							>
								<div className="drop-node-area p-2">Add premise</div>
							</Droppable>
						</div>
						<div className="node-line"></div>
						<span className="centered-text">Conclusion pending</span>
					</div>
					<Droppable
						id={id + 1}
						data={{ type: "topdown-rule" }}
						className="d-flex stretch-container"
					>
						<div className="drop-area topdown-rule p-2">
							{rule ? (
								<>
									{rule} <CloseButton onClick={() => setRule(null)} />
								</>
							) : (
								"Put rule here"
							)}
						</div>
					</Droppable>
				</div>
			)}
		</>
	);
}

interface nodeProps {
	conclusion: string;
	className?: string;
	tree: line | null;
	root?: boolean;
	ind?: number;
	topdownHandler?: TopdownHandler;
	deleteHandler?: (deleteId: number) => void;
}

function ProofNode(props: nodeProps) {
	const { dropped, addRef, removeHandler, addHandler } =
		useContext(DroppedContext);
	const file = useContext(FileContext);
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
		if (id in dropped && !tree) {
			(window as any).electronAPI
				.parse(props.conclusion, dropped[id], file)
				.then((res: string[]) => setArgs(res));
		} else setArgs(null);
	}, [dropped]);
	useEffect(() => {
		if (tree) addHandler(id, tree.rule);
	}, [tree]);
	useEffect(() => {
		safeSetTree(props.tree);

		if (props.tree && id && props.tree.rule !== "Put rule here")
			addHandler(id, props.tree.rule);
	}, [props.tree]);

	return (
		<div
			className={`d-flex flex-row proof-node ${
				props.className ? props.className : ""
			} ${props.root ? "root-node" : "m-2"}`}
			ref={proofNodeRef}
		>
			{props.root ? (
				<CloseButton
					className="topdown-close"
					onClick={() => (props.deleteHandler ? props.deleteHandler(id) : null)}
				/>
			) : null}
			{props.topdownHandler && props.topdownHandler.level ? (
				<CloseButton
					className="topdown-close"
					onClick={() =>
						props.topdownHandler?.fn(props.topdownHandler.ind as number)
					}
				/>
			) : null}
			<div className="d-flex flex-column">
				{args ? (
					<Premises
						args={args}
						tree={null}
						topdownHandler={
							props.topdownHandler && !props.topdownHandler.level
								? props.topdownHandler
								: null
						}
					/>
				) : tree ? (
					<Premises
						args={[...tree.premises.map((value) => value.conclusion), ""]}
						tree={tree}
						topdownHandler={
							props.topdownHandler && !props.topdownHandler.level
								? props.topdownHandler
								: null
						}
					/>
				) : (
					<Droppable
						id={id + 1}
						data={{ type: "copy", text: props.conclusion }}
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
							type: "node",
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
				data={{ type: "rule" }}
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
	proofRef?: RefObject<HTMLDivElement>;
	inputs: input[];
	deleteHandler: (ind: number, deleteId: number) => void;
}) {
	return (
		<div className="d-flex proof-area" ref={props.proofRef}>
			{props.inputs.map(({ conclusion, free, id }, ind) => (
				<ProofNode
					className={`${free ? "free" : ""}`}
					ind={ind}
					key={id}
					conclusion={conclusion}
					tree={null}
					deleteHandler={(deleteId: number) => {
						props.deleteHandler(ind, deleteId);
					}}
					root
				/>
			))}
		</div>
	);
}
