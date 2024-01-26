import React, { useState, useRef, useEffect, RefObject } from "react";
import { createRoot } from "react-dom/client";
import { ast, tab, input, canvasState } from "./types";
import Bank from "./components/bank";
import ProofArea from "./components/proof";
import Canvas from "./components/canvas";
import Export from "./components/export";
import Input from "./components/input";
import { DroppedContext, FileContext } from "./components/state";
import {
	DndContext,
	DragEndEvent,
	DragStartEvent,
	UniqueIdentifier,
} from "@dnd-kit/core";
import { snapCenterToCursor } from "@dnd-kit/modifiers";
import Tab from "react-bootstrap/Tab";
import Nav from "react-bootstrap/Nav";
import CloseButton from "react-bootstrap/CloseButton";
import Card from "react-bootstrap/Card";
import Button from "react-bootstrap/Button";
import { DragOverlay } from "@dnd-kit/core";
import { getTree } from "./components/utils";
import styled, { keyframes } from "styled-components";

export default function MyApp() {
	const [tabs, setTabs] = useState<tab[]>([]);
	const [activeText, setActiveText] = useState<string | null>(null);
	const [dropped, setDropped] = useState({});
	const [showExport, setShowExport] = useState(false);
	const [showInput, setShowInput] = useState(false);
	const [activeKey, setActiveKey] = useState<number>(0);
	const [refs, setRefs] = useState({});
	const [show, setShow] = useState(false);
	const [marginLeft, setMarginLeft] = useState(0);
	const [Anim, setAnim] = useState(styled.div`
		margin-left: 0;
	`);
	const [canvasStates, setCanvasStates] = useState<canvasState[]>([]);

	const shiftRef = useRef(false);
	const proofRef = useRef<HTMLDivElement>(null);
	// const [proofRef, setProofRef] = useState(null);
	const bankRef = useRef<HTMLDivElement>(null);

	useEffect(() => {
		const handleKeyDown = (event: KeyboardEvent) => {
			if (event.key === "Shift") shiftRef.current = true;
		};
		const handleKeyUp = (event: KeyboardEvent) => {
			if (event.key === "Shift") shiftRef.current = false;
		};

		document.addEventListener("keydown", handleKeyDown);
		document.addEventListener("keyup", handleKeyUp);

		return () => {
			document.removeEventListener("keydown", handleKeyDown);
			document.removeEventListener("keyup", handleKeyUp);
		};
	}, []);

	const addTab = (compUnit: ast | null, name: string | null, file: string) => {
		const maxId = Math.max(-1, ...tabs.map((element) => element.id));
		setTabs(
			tabs.concat([
				{
					ast: compUnit,
					id: maxId + 1,
					name,
					inputs: [],
					file,
				} as tab,
			]),
		);
		setCanvasStates(
			canvasStates.concat([{ x: 0, y: 0, scale: 1, id: maxId + 1 }]),
		);
		setActiveKey(maxId + 1);
	};

	useEffect(() => {
		(window as any).electronAPI.addAST(({ compUnit, name, file }) =>
			addTab(compUnit, name, file),
		);

		(window as any).electronAPI.showModal(() => {
			setShowExport(true);
		});
	}, [tabs]);
	useEffect(
		() =>
			setAnim(styled.div`
				margin-left: ${show ? bankRef.current?.offsetWidth : 0}px;
			`),
		[activeKey],
	);

	const addRef = (id: number, ref: RefObject<HTMLDivElement>) =>
		setRefs({
			...refs,
			[id]: ref,
		});

	const removeHandler = (id: number) => {
		const newDropped = { ...dropped };
		delete newDropped[id];
		setDropped(newDropped);
	};

	const addHandler = (id: UniqueIdentifier, text: string) => {
		if (!(id in dropped))
			setDropped({
				...dropped,
				[id]: text,
			});
	};

	const handleDragStart = (event: DragStartEvent) =>
		setActiveText(event.active.data.current?.text);

	const deleteInput = (activeKey: number, ind: number, deleteId: number) => {
		const newTabs: tab[] = tabs.map((tab) => JSON.parse(JSON.stringify(tab)));

		for (const tab of newTabs)
			if (tab.id == activeKey) tab.inputs.splice(ind, 1);

		setTabs(newTabs);
	};

	const appendInput = (activeKey: number, inp: input) => {
		const newTabs: tab[] = tabs.map((tab) => JSON.parse(JSON.stringify(tab)));

		for (const tab of newTabs) if (tab.id == activeKey) tab.inputs.push(inp);

		setTabs(newTabs);
	};

	const handleDragEnd = (event: DragEndEvent) => {
		const { active, over } = event;
		const activeData = active.data.current;
		setActiveText(null);

		if (!over) return;

		const overData = over.data.current;
		const overType = overData?.type;
		const activeType = activeData?.type;

		if (overType === "rule" && activeType === "rule" && !(over.id in dropped))
			addHandler(over.id, activeData?.text);
		if (
			activeType === "node" &&
			((overType === "copy" && activeData?.text === overData?.text) ||
				overType === "topdown")
		) {
			const which = overType === "topdown";
			const event = new CustomEvent(which ? "topdown-tree" : "tree", {
				detail: {
					tree: getTree(refs[active.id].current),
					overId: over.id,
					text: which ? activeData?.text : "",
				},
			});
			document.dispatchEvent(event);

			if (shiftRef.current && activeData?.ind != null)
				deleteInput(activeKey, activeData?.ind, active.id as number);
		}
		if (activeType === "rule" && overType === "topdown-rule") {
			const event = new CustomEvent("topdown-rule", {
				detail: {
					overId: over.id,
					text: activeData?.text,
				},
			});
			document.dispatchEvent(event);
		}
	};

	function handleCloseTab(id: number) {
		const newTabs = tabs.filter((element) => element.id !== id);
		setTabs(newTabs);
		setCanvasStates(canvasStates.filter((element) => element.id !== id));

		if (newTabs.length) setActiveKey(newTabs[0].id);
		else {
			setActiveKey(0);
			setShow(false);
		}
	}

	useEffect(() => {
		const shift = keyframes`
            from {
                margin-left: 0;
            }
            to {
                margin-left: ${bankRef.current?.offsetWidth}px;
            }
        `;

		const NewAnim = styled.div`
			animation: ${shift} 0.3s linear;
			animation-direction: ${show ? "normal" : "reverse"};
			margin-left: ${show ? bankRef.current?.offsetWidth : "0"}px;
		`;

		setAnim(NewAnim);
	}, [show]);
	useEffect(
		() =>
			setAnim(styled.div`
				margin-left: ${marginLeft}px;
			`),
		[marginLeft],
	);

	return (
		<DndContext
			modifiers={[snapCenterToCursor]}
			onDragStart={handleDragStart}
			onDragEnd={handleDragEnd}
		>
			<Bank
				toggleShow={() => setShow(!show)}
				compUnit={tabs.find((value) => value.id === activeKey)?.ast}
				bankRef={bankRef}
			/>
			<Anim>
				<Tab.Container
					onSelect={(eventKey) => setActiveKey(eventKey ? Number(eventKey) : 0)}
					activeKey={activeKey}
				>
					<Nav variant="tabs" className="flex-row">
						{tabs.map((element) => (
							<Nav.Item className="d-flex flex-row" key={element.id}>
								<Nav.Link className="d-flex center-align" eventKey={element.id}>
									{element.name}
									<CloseButton
										onClick={(event) => {
											event.stopPropagation();
											handleCloseTab(element.id);
										}}
									/>
								</Nav.Link>
							</Nav.Item>
						))}
					</Nav>
					{tabs.map((element, index) => (
						<Tab.Content key={element.id}>
							<Tab.Pane
								eventKey={element.id}
								onEnter={() =>
									setMarginLeft(bankRef.current?.offsetWidth as number)
								}
							>
								<Canvas
									index={index}
									canvasStates={canvasStates}
									setCanvasStates={setCanvasStates}
									inputs={element.inputs}
									proofRef={proofRef}
								>
									<DroppedContext.Provider
										value={{
											dropped,
											addRef,
											removeHandler,
											addHandler,
											ast: element.ast,
										}}
									>
										<FileContext.Provider value={element.file}>
											{element.id === activeKey ? (
												<ProofArea
													proofRef={proofRef}
													inputs={element.inputs}
													deleteHandler={(ind: number, deleteId: number) =>
														deleteInput(element.id, ind, deleteId)
													}
												/>
											) : (
												<ProofArea
													inputs={element.inputs}
													deleteHandler={(ind: number, deleteId: number) =>
														deleteInput(element.id, ind, deleteId)
													}
												/>
											)}
										</FileContext.Provider>
									</DroppedContext.Provider>
								</Canvas>
								<Button
									variant="success"
									className="input-theorem"
									onClick={() => setShowInput(true)}
								>
									New Theorem
								</Button>
								<Input
									show={showInput}
									onHide={() => setShowInput(false)}
									inputs={element.inputs}
									appendHandler={(inp: input) => appendInput(element.id, inp)}
								/>
							</Tab.Pane>
							<Export
								show={showExport}
								onHide={() => setShowExport(false)}
								proofRef={proofRef}
								inputs={element.inputs}
							/>
						</Tab.Content>
					))}
				</Tab.Container>
			</Anim>
			<DragOverlay zIndex={1060}>
				{activeText ? (
					<Card
						body
						className="exact"
						border="dark"
						text={shiftRef.current ? "info" : "dark"}
					>
						<code className="rule-like-text no-wrap">{activeText}</code>
					</Card>
				) : null}
			</DragOverlay>
		</DndContext>
	);
}

const appContainer = document.createElement("div");
document.body.appendChild(appContainer);

const root = createRoot(appContainer);
root.render(<MyApp />);
