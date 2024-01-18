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
import { replaceElement, extractPremise } from "./utils";
import { line, input, ast } from "../types";

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

function TopDownNode({ rule }: { rule: string }) {
	const { ast } = useContext(DroppedContext);
	const file = useContext(FileContext);
	const numPremises = getNumPremises(ast, rule);

	const [ids, setIds] = useState<number[]>(Array(numPremises).fill(0));
	const [trees, setTrees] = useState<(line | null)[]>(
		Array(numPremises).fill(null),
	);
	const [premises, setPremises] = useState<(string | null)[]>(
		Array(numPremises).fill(null),
	);
	const [numUsed, setNumUsed] = useState(0);
	const [conclusion, setConclusion] = useState<string | null>(null);
	const [tree, setTree] = useState<line | null>(null);

	useEffect(
		() =>
			setIds(Array.from(Array(numPremises).keys()).map((_) => nodeCounter++)),
		[],
	);
	useEffect(() => {
		const listener = (event: Event) => {
			const detail = (event as CustomEvent).detail;

			if (ids[0] <= detail.overId && detail.overId <= ids[ids.length - 1]) {
				const ind = detail.overId - ids[0];
				setTrees(replaceElement(trees, ind, detail.tree));
				setPremises(replaceElement(premises, ind, detail.text));
				setNumUsed(numUsed + 1);
			}
		};

		document.addEventListener("topdown-tree", listener);

		return () => document.removeEventListener("topdown-tree", listener);
	}, [ids, trees, premises]);
	useEffect(() => {
		if (numUsed != numPremises) {
			setConclusion(null);
			return;
		}

		(window as any).electronAPI
			.topdownParse({ premises }, rule, file)
			.then((res: string) => setConclusion(res));
	}, [numUsed]);
	useEffect(() => {
		if (!conclusion) return;
		if (trees.filter((elem) => elem == null).length) return;

		setTree({ conclusion, name: "", rule, premises: trees as line[] });
	}, [conclusion]);

	const deletePremise = (ind: number) => {
		setTrees(replaceElement(trees, ind, null));
		setPremises(replaceElement(premises, ind, null));
		setNumUsed(numUsed - 1);
	};

	return conclusion ? (
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
						<Droppable
							id={ids[ind]}
							key={ind}
							data={{ type: "topdown" }}
							className="d-flex stretch-container fill-width"
						>
							<div className="drop-node-area p-2">
								{premise ? (
									<>
										{premise} <CloseButton onClick={() => deletePremise(ind)} />
									</>
								) : (
									`Premise ${ind + 1}`
								)}
							</div>
						</Droppable>
					))}
				</div>
				<div className="node-line"></div>
				<span className="centered-text">Conclusion pending</span>
			</div>
			<span className="topdown-rule m-2">{rule}</span>
		</div>
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
