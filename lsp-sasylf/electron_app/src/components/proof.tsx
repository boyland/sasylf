import React, {
	useState,
	useEffect,
	useContext,
	useRef,
	RefObject,
} from "react";
import Droppable from "./droppable";
import CloseButton from "react-bootstrap/CloseButton";
import { NodeContext, FileContext } from "./state";
import Form from "react-bootstrap/Form";
import Draggable from "./draggable";
import {
	deleteElement,
	extractPremise,
	getChildrenIds,
	getParentId,
	capitalize,
} from "./utils";
import { line, input, ast, Direction } from "../types";
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

function TopDownNode({
	prems,
	deleteHandler,
	className,
}: {
	prems: string[];
	deleteHandler: () => void;
	className: string;
}) {
	const { ast } = useContext(NodeContext);
	const file = useContext(FileContext);

	const [rule, setRule] = useState<string | null>(null);
	const [id, setId] = useState(0);
	const [trees, setTrees] = useState<line[]>([]);
	const [premises, setPremises] = useState<string[]>([]);
	const [numUsed, setNumUsed] = useState(0);
	const [conclusion, setConclusion] = useState<string | null>(null);
	const [tree, setTree] = useState<line | null>(null);
	const [showModal, setShowModal] = useState(false);
	const [errorText, setErrorText] = useState("");

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
				if (getNumPremises(ast, detail.text) < numUsed) {
					setErrorText("Wrong number of premises for specified rule");
					setShowModal(true);
				} else setRule(detail.text);
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
			.then((res: { conclusion?: string[]; errors?: string[] }) => {
				if (res.conclusion) setConclusion(res.conclusion[0]);
				else if (res.errors) {
					setErrorText(
						res.errors
							.map((error) => capitalize(error.split("error: ")[1]))
							.join("\n"),
					);
					setShowModal(true);
				} else {
					setErrorText("Invalid rule");
					setShowModal(true);
				}
			});
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
				text={errorText}
				toggleShow={() => setShowModal(!showModal)}
			/>
			{conclusion ? (
				<ProofNode
					className={className}
					conclusion={conclusion}
					tree={tree}
					topdownHandler={{
						fn: (ind: number) => deletePremise(ind),
						level: false,
					}}
					deleteHandler={deleteHandler}
					root
				/>
			) : (
				<div className={"d-flex flex-row topdown-node m-2"}>
					<CloseButton className="topdown-close" onClick={deleteHandler} />
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

function propagateUp(
	node: Element | null,
	data: { oldvar: string; newvar: string },
) {
	const parId = getParentId(node);

	if (parId < 0) return;

	const event = new CustomEvent("replace", {
		detail: { nodeId: parId, data, dir: Direction.Up },
	});

	document.dispatchEvent(event);
}

function propagateDown(
	node: Element | null,
	data: { oldvar: string; newvar: string },
) {
	const childIds = getChildrenIds(node);

	for (const id of childIds) {
		const event = new CustomEvent("replace", {
			detail: { nodeId: id, data, dir: Direction.Down },
		});

		document.dispatchEvent(event);
	}
}

interface nodeProps {
	conclusion: string;
	className?: string;
	tree: line | null;
	root?: boolean;
	ind?: number;
	topdownHandler?: TopdownHandler;
	deleteHandler?: () => void;
}

function ProofNode(props: nodeProps) {
	const { addRef, showContextMenu } = useContext(NodeContext);
	const file = useContext(FileContext);

	const [id, setId] = useState(0);
	const [args, setArgs] = useState<string[] | null>(null);
	const [tree, setTree] = useState<line | null>(null);
	const [rule, setRule] = useState<string | null>(null);
	const [conclusion, setConclusion] = useState("");
	const [show, setShow] = useState(false);
	const [errorText, setErrorText] = useState("");

	const proofNodeRef = useRef<HTMLDivElement>(null);

	const safeSetTree = (tree: line | null) =>
		setTree(tree && tree.rule === "Put rule here" ? null : tree);

	useEffect(() => {
		setId(nodeCounter++);
		safeSetTree(props.tree);
		addRef(nodeCounter++ - 1, proofNodeRef);
	}, []);

	useEffect(() => setConclusion(props.conclusion), [props.conclusion]);

	useEffect(() => {
		const listener = (event: Event) => {
			const detail = (event as CustomEvent).detail;

			if (id === detail.overId) safeSetTree(detail.tree);
		};

		const ruleListener = (event: Event) => {
			const detail = (event as CustomEvent).detail;

			if (id === detail.overId) if (!rule && !tree) setRule(detail.text);
		};

		const replaceListener = (event: Event) => {
			const detail = (event as CustomEvent).detail;

			if (id === detail.nodeId)
				(window as any).electronAPI
					.substitute(conclusion, detail.data.oldvar, detail.data.newvar, file)
					.then((res: { result?: string; errors?: string[] }) => {
						if (Object.keys(res).length != 0) {
							if (res.errors) {
								setErrorText(
									res.errors
										.map((error) => capitalize(error.split("error: ")[1]))
										.join("\n"),
								);
								setShow(true);
							} else if (res.result) {
								setConclusion(res.result);

								if ([Direction.Both, Direction.Up].includes(detail.dir))
									propagateUp(proofNodeRef.current, detail.data);

								if ([Direction.Both, Direction.Down].includes(detail.dir))
									propagateDown(proofNodeRef.current, detail.data);
							}
						}
					});
		};

		document.addEventListener("tree", listener);
		document.addEventListener("rule", ruleListener);
		document.addEventListener("replace", replaceListener);

		return () => {
			document.removeEventListener("tree", listener);
			document.removeEventListener("rule", ruleListener);
			document.removeEventListener("replace", replaceListener);
		};
	}, [id, rule, tree, conclusion]);

	useEffect(() => {
		if (rule && !tree)
			(window as any).electronAPI
				.parse(conclusion, rule, file)
				.then((res: { arguments?: string[]; errors?: string[] }) => {
					if (res.arguments) setArgs(res.arguments);
					else if (res.errors) {
						setErrorText(
							res.errors
								.map((error) => capitalize(error.split("error: ")[1]))
								.join("\n"),
						);
						setShow(true);
					} else {
						setErrorText("Invalid rule");
						setShow(true);
					}
				});
		else setArgs(null);
	}, [rule]);

	useEffect(() => safeSetTree(props.tree), [props.tree]);

	return (
		<div
			className={`d-flex flex-row proof-node ${
				props.className ? props.className : ""
			} ${props.root ? "root-node" : "m-2"}`}
			ref={proofNodeRef}
			id={id.toString()}
		>
			<ErrorModal
				show={show}
				text={errorText}
				toggleShow={() => setShow(!show)}
			/>
			{props.root ? (
				<CloseButton
					className="topdown-close"
					onClick={() => (props.deleteHandler ? props.deleteHandler() : null)}
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
							ind: props.root ? props.ind : undefined,
						}}
					>
						<span
							className="centered-text no-wrap panning-excluded"
							onContextMenu={(event) => showContextMenu(event, id)}
						>
							{conclusion}
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
					{tree || rule ? (
						<>
							{tree ? tree.rule : rule}{" "}
							<CloseButton
								onClick={() => {
									setRule(null);
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
	deleteHandler: (ind: number) => void;
}) {
	return (
		<>
			<div className="d-flex proof-area" ref={props.proofRef}>
				{props.inputs.map(({ input, free, id, type }, ind) =>
					type === "Conclusion" ? (
						<ProofNode
							className={`${free ? "free" : ""}`}
							ind={ind}
							key={id}
							conclusion={input[0]}
							tree={null}
							deleteHandler={() => {
								props.deleteHandler(ind);
							}}
							root
						/>
					) : (
						<TopDownNode
							className={`${free ? "free" : ""}`}
							prems={input}
							key={id}
							deleteHandler={() => {
								props.deleteHandler(ind);
							}}
						/>
					),
				)}
			</div>
		</>
	);
}
