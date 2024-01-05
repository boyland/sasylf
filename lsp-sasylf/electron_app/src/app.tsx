import React, { useState, useRef } from "react";
import { createRoot } from "react-dom/client";
import { ast, tab } from "./types";
import Bank from "./components/bank";
import ProofArea from "./components/proof";
import Canvas from "./components/canvas";
import Export from "./components/export";
import { DroppedContext } from "./components/state";
import { DndContext, DragEndEvent, DragStartEvent } from "@dnd-kit/core";
import { snapCenterToCursor } from "@dnd-kit/modifiers";
import Tab from "react-bootstrap/Tab";
import Nav from "react-bootstrap/Nav";
import CloseButton from "react-bootstrap/CloseButton";
import Card from "react-bootstrap/Card";
import { DragOverlay } from "@dnd-kit/core";

export default function MyApp() {
	const [tabs, setTabs] = useState<tab[]>([]);
	const [activeText, setActiveText] = useState<string | null>(null);
	const [dropped, setDropped] = useState({});
	const [show, setShow] = useState(false);
	const [activeKey, setActiveKey] = useState<string | number>(0);
	let proofRef = useRef(null);

	const addTab = (compUnit: ast | null, name: string | null) => {
		const maxId = Math.max(-1, ...tabs.map((element) => element.id));
		setTabs(
			tabs.concat([
				{
					ast: compUnit,
					id: maxId + 1,
					name,
				} as tab,
			]),
		);
	};

	(window as any).electronAPI.addAST(({ compUnit, name }) =>
		addTab(compUnit, name),
	);

	(window as any).electronAPI.showModal(() => {
		setShow(true);
	});

	const removeHandler = (id: number) => {
		const newDropped = { ...dropped };
		delete newDropped[id];
		setDropped(newDropped);
	};

	const handleDragStart = (event: DragStartEvent) =>
		setActiveText(event.active.data.current?.text);

	const handleDragEnd = (event: DragEndEvent) => {
		setActiveText(null);

		if (
			event.over &&
			event.active.data.current?.ruleLike &&
			!(event.over.id in dropped)
		)
			setDropped({
				...dropped,
				[event.over.id]: event.active.data.current?.text,
			});
	};

	function handleCloseTab(id: number) {
		setTabs(tabs.filter((element) => element.id !== id));
		setActiveKey(0);
	}

	return (
		<Tab.Container
			onSelect={(eventKey) => setActiveKey(eventKey ? eventKey : 0)}
			activeKey={activeKey}
		>
			<DndContext
				modifiers={[snapCenterToCursor]}
				onDragStart={handleDragStart}
				onDragEnd={handleDragEnd}
			>
				<Nav variant="tabs" className="flex-row">
					{tabs.map((element) => (
						<Nav.Item className="d-flex flex-row" key={element.id}>
							<Nav.Link eventKey={element.id.toString()}>
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
				{tabs.map((element) => (
					<Tab.Content key={element.id}>
						<Tab.Pane eventKey={element.id.toString()}>
							<Bank compUnit={element.ast} />
							<Canvas>
								<DroppedContext.Provider value={[dropped, removeHandler]}>
									<ProofArea proofRef={proofRef} />
								</DroppedContext.Provider>
							</Canvas>
						</Tab.Pane>
					</Tab.Content>
				))}
				<DragOverlay zIndex={1060}>
					{activeText ? (
						<Card body className="exact" border="dark" text="dark">
							<code className="rule-like-text">{activeText}</code>
						</Card>
					) : null}
				</DragOverlay>
			</DndContext>
			<Export show={show} onHide={() => setShow(false)} proofRef={proofRef} />
		</Tab.Container>
	);
}

const appContainer = document.createElement("div");
document.body.appendChild(appContainer);

const root = createRoot(appContainer);
root.render(<MyApp />);
